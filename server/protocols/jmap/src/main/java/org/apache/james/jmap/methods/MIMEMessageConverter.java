/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.methods;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessage.DraftEmailer;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mime4j.Charsets;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.codec.EncoderUtil.Usage;
import org.apache.james.mime4j.dom.FieldParser;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.message.MultipartBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.NameValuePair;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;

public class MIMEMessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIMEMessageConverter.class);

    private static final String PLAIN_TEXT_MEDIA_TYPE = MediaType.PLAIN_TEXT_UTF_8.withoutParameters().toString();
    private static final String HTML_MEDIA_TYPE = MediaType.HTML_UTF_8.withoutParameters().toString();
    private static final NameValuePair UTF_8_CHARSET = new NameValuePair("charset", Charsets.UTF_8.name());
    private static final String ALTERNATIVE_SUB_TYPE = "alternative";
    private static final String MIXED_SUB_TYPE = "mixed";
    private static final String FIELD_PARAMETERS_SEPARATOR = ";";
    private static final String QUOTED_PRINTABLE = "quoted-printable";
    private static final String BASE64 = "base64";

    private final BasicBodyFactory bodyFactory;

    public MIMEMessageConverter() {
        this.bodyFactory = new BasicBodyFactory();
    }

    public byte[] convert(ValueWithId.CreationMessageEntry creationMessageEntry, ImmutableList<MessageAttachment> messageAttachments) {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DefaultMessageWriter writer = new DefaultMessageWriter();
        try {
            writer.writeMessage(convertToMime(creationMessageEntry, messageAttachments), buffer);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return buffer.toByteArray();
    }

    @VisibleForTesting Message convertToMime(ValueWithId.CreationMessageEntry creationMessageEntry, ImmutableList<MessageAttachment> messageAttachments) {
        if (creationMessageEntry == null || creationMessageEntry.getValue() == null) {
            throw new IllegalArgumentException("creationMessageEntry is either null or has null message");
        }

        Message.Builder messageBuilder = Message.Builder.of();
        if (isMultipart(creationMessageEntry.getValue(), messageAttachments)) {
            messageBuilder.setBody(createMultipart(creationMessageEntry.getValue(), messageAttachments));
        } else {
            messageBuilder.setBody(createTextBody(creationMessageEntry.getValue()))
                .setContentTransferEncoding(QUOTED_PRINTABLE);
        }
        buildMimeHeaders(messageBuilder, creationMessageEntry.getCreationId(), creationMessageEntry.getValue(), messageAttachments);
        return messageBuilder.build();
    }

    private void buildMimeHeaders(Message.Builder messageBuilder, CreationMessageId creationId, CreationMessage newMessage, ImmutableList<MessageAttachment> messageAttachments) {
        Optional<Mailbox> fromAddress = newMessage.getFrom().filter(DraftEmailer::hasValidEmail).map(this::convertEmailToMimeHeader);
        fromAddress.ifPresent(messageBuilder::setFrom);
        fromAddress.ifPresent(messageBuilder::setSender);

        messageBuilder.setReplyTo(newMessage.getReplyTo().stream()
                .map(this::convertEmailToMimeHeader)
                .collect(Collectors.toList()));
        messageBuilder.setTo(newMessage.getTo().stream()
                .filter(DraftEmailer::hasValidEmail)
                .map(this::convertEmailToMimeHeader)
                .collect(Collectors.toList()));
        messageBuilder.setCc(newMessage.getCc().stream()
                .filter(DraftEmailer::hasValidEmail)
                .map(this::convertEmailToMimeHeader)
                .collect(Collectors.toList()));
        messageBuilder.setBcc(newMessage.getBcc().stream()
                .filter(DraftEmailer::hasValidEmail)
                .map(this::convertEmailToMimeHeader)
                .collect(Collectors.toList()));
        messageBuilder.setSubject(newMessage.getSubject());
        messageBuilder.setMessageId(creationId.getId());

        // note that date conversion probably lose milliseconds!
        messageBuilder.setDate(Date.from(newMessage.getDate().toInstant()), TimeZone.getTimeZone(newMessage.getDate().getZone()));
        newMessage.getInReplyToMessageId().ifPresent(addInReplyToHeader(messageBuilder::addField));
        if (!isMultipart(newMessage, messageAttachments)) {
            newMessage.getHtmlBody().ifPresent(x -> messageBuilder.setContentType(HTML_MEDIA_TYPE, UTF_8_CHARSET));
        }
    }

    private Consumer<String> addInReplyToHeader(Consumer<Field> headerAppender) {
        return msgId -> {
            FieldParser<UnstructuredField> parser = UnstructuredFieldImpl.PARSER;
            RawField rawField = new RawField("In-Reply-To", msgId);
            headerAppender.accept(parser.parse(rawField, DecodeMonitor.SILENT));
        };
    }

    private boolean isMultipart(CreationMessage newMessage, ImmutableList<MessageAttachment> messageAttachments) {
        return (newMessage.getTextBody().isPresent() && newMessage.getHtmlBody().isPresent())
                || hasAttachment(messageAttachments);
    }

    private boolean hasAttachment(ImmutableList<MessageAttachment> messageAttachments) {
        return !messageAttachments.isEmpty();
    }

    private TextBody createTextBody(CreationMessage newMessage) {
        String body = newMessage.getHtmlBody()
                        .orElse(newMessage.getTextBody()
                                .orElse(""));
        return bodyFactory.textBody(body, Charsets.UTF_8);
    }

    private Multipart createMultipart(CreationMessage newMessage, ImmutableList<MessageAttachment> messageAttachments) {
        try {
            if (hasAttachment(messageAttachments)) {
                MultipartBuilder builder = MultipartBuilder.create(MIXED_SUB_TYPE);
                addBody(newMessage, builder);
    
                Consumer<MessageAttachment> addAttachment = addAttachment(builder);
                messageAttachments.forEach(addAttachment);
    
                return builder.build();
            } else {
                return createMultipartAlternativeBody(newMessage);
            }
        } catch (IOException e) {
            LOGGER.error("Error while creating textBody \n"+ newMessage.getTextBody().get() +"\n or htmlBody \n" + newMessage.getHtmlBody().get(), e);
            throw Throwables.propagate(e);
        }
    }

    private void addBody(CreationMessage newMessage, MultipartBuilder builder) throws IOException {
        if (newMessage.getHtmlBody().isPresent() && newMessage.getTextBody().isPresent()) {
            Multipart body = createMultipartAlternativeBody(newMessage);
            builder.addBodyPart(BodyPartBuilder.create().setBody(body).build());
        }
        else {
            addText(builder, newMessage.getTextBody());
            addHtml(builder, newMessage.getHtmlBody());
        }
    }

    private Multipart createMultipartAlternativeBody(CreationMessage newMessage) throws IOException {
        MultipartBuilder bodyBuilder = MultipartBuilder.create(ALTERNATIVE_SUB_TYPE);
        addText(bodyBuilder, newMessage.getTextBody());
        addHtml(bodyBuilder, newMessage.getHtmlBody());
        return bodyBuilder.build();
    }

    private void addText(MultipartBuilder builder, Optional<String> textBody) throws IOException {
        if (textBody.isPresent()) {
            builder.addBodyPart(BodyPartBuilder.create()
                .use(bodyFactory)
                .setBody(textBody.get(), Charsets.UTF_8)
                .setContentType(PLAIN_TEXT_MEDIA_TYPE, UTF_8_CHARSET)
                .setContentTransferEncoding(QUOTED_PRINTABLE)
                .build());
        }
    }

    private void addHtml(MultipartBuilder builder, Optional<String> htmlBody) throws IOException {
        if (htmlBody.isPresent()) {
            builder.addBodyPart(BodyPartBuilder.create()
                .use(bodyFactory)
                .setBody(htmlBody.get(), Charsets.UTF_8)
                .setContentType(HTML_MEDIA_TYPE, UTF_8_CHARSET)
                .setContentTransferEncoding(QUOTED_PRINTABLE)
                .build());
        }
    }

    private Consumer<MessageAttachment> addAttachment(MultipartBuilder builder) {
        return att -> { 
            try {
                builder.addBodyPart(attachmentBodyPart(att));
            } catch (IOException e) {
                LOGGER.error("Error while creating attachment", e);
                throw Throwables.propagate(e);
            }
        };
    }

    private BodyPart attachmentBodyPart(MessageAttachment att) throws IOException {
        BodyPartBuilder builder = BodyPartBuilder.create()
            .use(bodyFactory)
            .setBody(new BasicBodyFactory().binaryBody(ByteStreams.toByteArray(att.getAttachment().getStream())))
            .setField(contentTypeField(att))
            .setField(contentDispositionField(att.isInline()))
            .setContentTransferEncoding(BASE64);
        contentId(builder, att);
        return builder.build();
    }

    private void contentId(BodyPartBuilder builder, MessageAttachment att) {
        if (att.getCid().isPresent()) {
            builder.setField(new RawField("Content-ID", att.getCid().get().getValue()));
        }
    }

    private ContentTypeField contentTypeField(MessageAttachment att) {
        Builder<String, String> parameters = ImmutableMap.builder();
        if (att.getName().isPresent()) {
            parameters.put("name", encode(att.getName().get()));
        }
        String type = att.getAttachment().getType();
        if (type.contains(FIELD_PARAMETERS_SEPARATOR)) {
            return Fields.contentType(contentTypeWithoutParameters(type), parameters.build());
        }
        return Fields.contentType(type, parameters.build());
    }

    private String encode(String name) {
        return EncoderUtil.encodeEncodedWord(name, Usage.TEXT_TOKEN);
    }

    private String contentTypeWithoutParameters(String type) {
        return Splitter.on(FIELD_PARAMETERS_SEPARATOR).splitToList(type).get(0);
    }

    private ContentDispositionField contentDispositionField(boolean isInline) {
        if (isInline) {
            return Fields.contentDisposition("inline");
        }
        return Fields.contentDisposition("attachment");
    }

    private Mailbox convertEmailToMimeHeader(DraftEmailer address) {
        if (!address.hasValidEmail()) {
            throw new IllegalArgumentException("address");
        }
        CreationMessage.EmailUserAndDomain emailUserAndDomain = address.getEmailUserAndDomain();
        return new Mailbox(address.getName().orElse(null), null, emailUserAndDomain.getUser().orElse(null), emailUserAndDomain.getDomain().orElse(null));
    }
}
