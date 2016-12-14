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

package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;

public class StripAttachmentTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TemporaryFolder folder;
    private String folderPath;

    @Before
    public void setUp() throws IOException {
        folder = new TemporaryFolder();
        folder.create();
        folderPath = folder.getRoot().getPath() + "/";
    }

    @After
    public void tearDown() throws IOException {
        folder.delete();
    }

    @Test
    public void getBooleanParameterShouldReturnFalseWhenNullAndDefaultFalse() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter(null, false);
        assertThat(actual).isFalse();
    }
    
    @Test
    public void getBooleanParameterShouldReturnTrueWhenTrueAndDefaultFalse() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("true", false);
        assertThat(actual).isTrue();
    }
    
    @Test
    public void getBooleanParameterShouldReturnTrueWhenYesAndDefaultFalse() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("yes", false);
        assertThat(actual).isTrue();
    }

    @Test
    public void getBooleanParameterShouldReturnFalseWhenOtherAndDefaultFalse() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("other", false);
        assertThat(actual).isFalse();
    }

    @Test
    public void getBooleanParameterShouldReturnTrueWhenNullAndDefaultTrue() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter(null, true);
        assertThat(actual).isTrue();
    }

    @Test
    public void getBooleanParameterShouldReturnFalseWhenNoAndDefaultTrue() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("no", true);
        assertThat(actual).isFalse();
    }

    @Test
    public void getBooleanParameterShouldReturnFalseWhenFalseAndDefaultTrue() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("false", true);
        assertThat(actual).isFalse();
    }

    @Test
    public void getBooleanParameterShouldReturnTrueWhenOtherAndDefaultTrue() throws Exception {
        boolean actual = StripAttachment.getBooleanParameter("other", true);
        assertThat(actual).isTrue();
    }

    @Test
    public void serviceShouldNotModifyMailWhenNotMultipart() throws MessagingException, IOException {
        Mailet mailet = initMailet();
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));

        MimeBodyPart part = new MimeBodyPart();
        part.setText("simple text");
        
        message.setSubject("test");
        message.setContent(part, "text/plain");
        message.saveChanges();

        MimeMessage expectedMessage = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setSubject("test");
        message.setContent(part, "text/plain");
        message.saveChanges();

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        Mail expectedMail = FakeMail.builder()
                .mimeMessage(expectedMessage)
                .build();

        mailet.service(mail);

        assertThat(mail).isEqualToComparingFieldByField(expectedMail);
    }
    
    @Test
    public void serviceShouldSaveAttachmentInAFolderWhenPatternMatch() throws MessagingException, IOException {
        Mailet mailet = initMailet();
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));

        MimeMultipart multiPart = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart();
        part.setText("simple text");
        multiPart.addBodyPart(part);
        multiPart.addBodyPart(createAttachmentBodyPart("\u0023\u00A4\u00E3\u00E0\u00E9", "10.tmp"));
        multiPart.addBodyPart(createAttachmentBodyPart("\u0014\u00A3\u00E1\u00E2\u00E4", "temp.zip"));
        
        message.setSubject("test");
        message.setContent(multiPart);
        message.saveChanges();

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();

        mailet.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage,
                new String[]{"Bcc", "Content-Length", "Message-ID"});

        @SuppressWarnings("unchecked")
        Collection<String> c = (Collection<String>) mail
                .getAttribute(StripAttachment.SAVED_ATTACHMENTS_ATTRIBUTE_KEY);
        assertThat(c).isNotNull();
        assertThat(c.size()).isEqualTo(1);

        String name = c.iterator().next();

        assertThat(new File(folderPath + name)).hasContent("\u0023\u00A4\u00E3\u00E0\u00E9");
    }

    private MimeBodyPart createAttachmentBodyPart(String body, String fileName) throws MessagingException, UnsupportedEncodingException {
        MimeBodyPart part = createBodyPart(body);
        part.setDisposition("attachment");
        part.setFileName(fileName);
        return part;
    }

    private MimeBodyPart createBodyPart(String body) throws MessagingException, UnsupportedEncodingException {
        return new MimeBodyPart(new ByteArrayInputStream(
                ("Content-Transfer-Encoding: 8bit\r\nContent-Type: application/octet-stream; charset=utf-8\r\n\r\n"
                        + body).getBytes("UTF-8")));
    }

    @Test
    public void serviceShouldSaveAttachmentInAFolderWhenNotPatternDontMatch() throws MessagingException, IOException {
        Mailet mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folderPath)
                .setProperty("remove", "all")
                .setProperty("notpattern", "^(winmail\\.dat$)")
                .build();
        mailet.init(mci);

        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));

        MimeMultipart multiPart = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart();
        part.setText("simple text");
        multiPart.addBodyPart(part);
        multiPart.addBodyPart(createAttachmentBodyPart("\u0023\u00A4\u00E3\u00E0\u00E9", "temp.tmp"));
        multiPart.addBodyPart(createAttachmentBodyPart("\u0014\u00A3\u00E1\u00E2\u00E4", "winmail.dat"));
        
        message.setSubject("test");
        message.setContent(multiPart);
        message.saveChanges();

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();

        mailet.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage,
                new String[]{"Bcc", "Content-Length", "Message-ID"});

        @SuppressWarnings("unchecked")
        Collection<String> c = (Collection<String>) mail
                .getAttribute(StripAttachment.SAVED_ATTACHMENTS_ATTRIBUTE_KEY);
        assertThat(c).isNotNull();
        assertThat(c.size()).isEqualTo(1);

        String name = c.iterator().next();

        assertThat(new File(folderPath + name)).hasContent("\u0023\u00A4\u00E3\u00E0\u00E9");
    }

    @Test
    public void serviceShouldDecodeFilenameAndSaveAttachmentInAFolderWhenPatternMatchAndDecodeFilenameTrue() throws MessagingException, IOException {
        Mailet mailet = initMailet();

        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));

        MimeMultipart multiPart = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart();
        part.setText("simple text");
        multiPart.addBodyPart(part);
        multiPart.addBodyPart(createAttachmentBodyPart("\u0023\u00A4\u00E3\u00E0\u00E9", "=?iso-8859-15?Q?=E9_++++Pubblicit=E0_=E9_vietata____Milano9052.tmp?="));
        multiPart.addBodyPart(createAttachmentBodyPart("\u0014\u00A3\u00E1\u00E2\u00E4", "temp.zip"));
        
        message.setSubject("test");
        message.setContent(multiPart);
        message.saveChanges();

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();

        mailet.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage,
                new String[]{"Bcc", "Content-Length", "Message-ID"});

        @SuppressWarnings("unchecked")
        Collection<String> c = (Collection<String>) mail
                .getAttribute(StripAttachment.SAVED_ATTACHMENTS_ATTRIBUTE_KEY);
        assertThat(c).isNotNull();
        assertThat(c.size()).isEqualTo(1);

        String name = c.iterator().next();

        assertThat(name.startsWith("e_Pubblicita_e_vietata_Milano9052")).isTrue();
        
        assertThat(new File(folderPath + name)).hasContent("\u0023\u00A4\u00E3\u00E0\u00E9");
    }

    @Test
    public void initShouldThrowWhenPatternAndNotPatternAreNull() throws MessagingException {
        Mailet mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folderPath)
                .setProperty("remove", "all")
                .build();

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("No value for pattern parameter was provided.");
        mailet.init(mci);
    }

    @Test
    public void initShouldThrowOnWrongPattern() throws MessagingException {
        Mailet mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folderPath)
                .setProperty("remove", "all")
                .setProperty("pattern", ".****\\.tmp")
                .build();

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("Could not compile regex [.****\\.tmp]");
        mailet.init(mci);
    }

    @Test
    public void initShouldThrowOnWrongNotPattern() throws MessagingException {
        Mailet mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folderPath)
                .setProperty("remove", "all")
                .setProperty("notpattern", ".****\\.tmp")
                .build();

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("Could not compile regex [.****\\.tmp]");
        mailet.init(mci);
    }

    @Test
    public void initShouldSetRemoveAttachmentsToMatched() throws MessagingException {
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folderPath)
                .setProperty("remove", "matched")
                .setProperty("pattern", ".*\\.tmp")
                .build();
        mailet.init(mci);
        
        assertThat(mailet.removeAttachments).isEqualTo("matched");
    }

    @Test
    public void serviceShouldThrowWhenUnretrievableMessage() throws MessagingException {
        StripAttachment mailet = new StripAttachment();
        
        Mail mail = mock(Mail.class);
        when(mail.getMessage())
            .thenThrow(new MessagingException("Test exception"));

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("Could not retrieve message from Mail object");
        
        mailet.service(mail);
    }

    @Test
    public void serviceShouldThrowWhenUnretrievableContentTypeMessage() throws MessagingException {
        StripAttachment mailet = new StripAttachment();

        MimeMessage message = mock(MimeMessage.class);
        Mail mail = mock(Mail.class);
        when(mail.getMessage())
            .thenReturn(message);
        when(message.isMimeType("multipart/*"))
            .thenThrow(new MessagingException("Test exception"));

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("Could not retrieve contenttype of message.");
        
        mailet.service(mail);
    }

    @Test
    public void serviceShouldThrowWhenNotAnalyzableMessage() throws Exception {
        StripAttachment mailet = new StripAttachment();

        MimeMessage message = mock(MimeMessage.class);
        when(message.isMimeType("multipart/*"))
            .thenThrow(new RuntimeException("Test exception"));
        Mail mail = mock(Mail.class);
        when(mail.getMessage())
            .thenReturn(message);

        expectedException.expect(MailetException.class);
        expectedException.expectMessage("Could not analyse message.");

        mailet.service(mail);
    }

    @Test
    public void getMailetInfoShouldReturn() throws MessagingException {
        StripAttachment mailet = new StripAttachment();

        assertThat(mailet.getMailetInfo()).isEqualTo("StripAttachment");
    }

    @Test
    public void analyseMultipartPartMessageShouldReturnFalseWhenPartIsNotMultipart() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();
        Part part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        Mail mail = mock(Mail.class);
        //When
        boolean actual = mailet.analyseMultipartPartMessage(part, mail);
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void analyseMultipartPartMessageShouldReturnTrueWhenAtLeastOneMultipartShouldHaveBeenRemoved() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "all")
                .setProperty("pattern", ".*")
                .build();
        mailet.init(mci);
        MimeMultipart multiPart = new MimeMultipart();
        
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("removeMe.tmp");
        multiPart.addBodyPart(part);
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(multiPart);
        message.saveChanges();
        Mail mail = mock(Mail.class);
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void analyseMultipartPartMessageShouldReturnTrueWhenAtLeastOneMultipartShouldHaveBeenRemovedAndPartialRemove() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "matched")
                .setProperty("pattern", ".*")
                .build();
        mailet.init(mci);
        MimeMultipart multiPart = new MimeMultipart();
        
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("removeMe.tmp");
        multiPart.addBodyPart(part);
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(multiPart);
        message.saveChanges();
        Mail mail = mock(Mail.class);
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void analyseMultipartPartMessageShouldPutTwoPartsInDefaultAttributeWhenTwoPartsMatch() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "matched")
                .setProperty("directory", folderPath)
                .setProperty("pattern", ".*")
                .build();
        mailet.init(mci);
        MimeMultipart multiPart = new MimeMultipart();
        
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("removeMe.tmp");
        multiPart.addBodyPart(part);
        multiPart.addBodyPart(part);
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(multiPart);
        message.saveChanges();
        Mail mail = FakeMail.builder().build();
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isTrue();
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>)mail.getAttribute(StripAttachment.SAVED_ATTACHMENTS_ATTRIBUTE_KEY);
        assertThat(values).hasSize(2);
    }

    @Test
    public void analyseMultipartPartMessageShouldPutTwoPartsInCustomAttributeWhenTwoPartsMatch() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "matched")
                .setProperty("directory", folderPath)
                .setProperty("pattern", ".*")
                .setProperty("attribute", "my.custom.attribute")
                .build();
        mailet.init(mci);
        MimeMultipart multiPart = new MimeMultipart();
        
        MimeBodyPart part1 = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part1.setFileName("removeMe1.tmp");
        MimeBodyPart part2 = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part2.setFileName("removeMe2.tmp");
        multiPart.addBodyPart(part1);
        multiPart.addBodyPart(part2);
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(multiPart);
        message.saveChanges();
        Mail mail = FakeMail.builder().build();
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, byte[]> values = (Map<String, byte[]>)mail.getAttribute("my.custom.attribute");
        assertThat(values).hasSize(2);
    }

    @Test
    public void analyseMultipartPartMessageShouldReturnTrueWhenAtLeastOneSubMultipartShouldHaveBeenRemoved() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "all")
                .setProperty("pattern", ".*\\.tmp")
                .build();
        mailet.init(mci);
        MimeMultipart mainMultiPart = new MimeMultipart();
        MimeMessage subMessage = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        MimeMultipart subMultiPart = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("removeMe.tmp");
        subMultiPart.addBodyPart(part);
        subMessage.setContent(subMultiPart);
        subMessage.saveChanges();
        mainMultiPart.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("Content-Type: multipart/mixed".getBytes(Charsets.US_ASCII)))
                ,
                IOUtils.toByteArray(subMessage.getInputStream())));
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(mainMultiPart);
        message.saveChanges();
        Mail mail = mock(Mail.class);
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void analyseMultipartPartMessageShouldReturnFalseWhenNoPartHasBeenRemovedInSubMultipart() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "matched")
                .setProperty("pattern", ".*\\.tmp")
                .build();
        mailet.init(mci);
        MimeMultipart mainMultiPart = new MimeMultipart();
        MimeMessage subMessage = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        MimeMultipart subMultiPart = new MimeMultipart();
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("dontRemoveMe.other");
        subMultiPart.addBodyPart(part);
        subMessage.setContent(subMultiPart);
        subMessage.saveChanges();
        mainMultiPart.addBodyPart(new MimeBodyPart(new InternetHeaders(new ByteArrayInputStream("Content-Type: multipart/mixed".getBytes(Charsets.US_ASCII)))
                ,
                IOUtils.toByteArray(subMessage.getInputStream())));
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(mainMultiPart);
        message.saveChanges();
        Mail mail = mock(Mail.class);
        //When
        boolean actual = mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void analyseMultipartPartMessageShouldRemovePartWhenOnePartShouldHaveBeenRemoved() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("remove", "all")
                .setProperty("pattern", ".*")
                .build();
        mailet.init(mci);
        MimeMultipart multiPart = new MimeMultipart();
        
        MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("removeMe.tmp");
        multiPart.addBodyPart(part);
        MimeMessage message = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        message.setContent(multiPart);
        message.saveChanges();
        Mail mail = mock(Mail.class);
        //When
        mailet.analyseMultipartPartMessage(message, mail);
        //Then
        assertThat(multiPart.getCount()).isZero();
    }

    @Test
    public void saveAttachmentShouldUsePartNameIfNoFilename() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();
        FakeMailetConfig mci = FakeMailetConfig.builder()
                .setProperty("directory", folderPath)
                .setProperty("pattern", ".*\\.tmp")
                .build();
        mailet.init(mci);
        Part part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        part.setFileName("example.tmp");
        String fileName = null;
        //When
        String actual = mailet.saveAttachmentToFile(part, fileName);
        //Then
        assertThat(actual).startsWith("example");
        assertThat(actual).endsWith(".tmp");
    }
    
    @Test
    public void saveAttachmentShouldThrowWhenNoFilenameAtAll() throws Exception {
        StripAttachment mailet = new StripAttachment();
        FakeMailetConfig mci = FakeMailetConfig.builder()
                .setProperty("directory", folderPath)
                .setProperty("pattern", ".*\\.tmp")
                .build();
        mailet.init(mci);
        Part part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        String fileName = null;

        expectedException.expect(NullPointerException.class);
        mailet.saveAttachmentToFile(part, fileName);
    }
    
    @Test
    public void saveAttachmentShouldAddBinExtensionWhenNoFileNameExtension() throws Exception {
        //Given
        StripAttachment mailet = new StripAttachment();
        FakeMailetConfig mci = FakeMailetConfig.builder()
                .setProperty("directory", folderPath)
                .setProperty("pattern", ".*")
                .build();
        mailet.init(mci);
        Part part = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
        String fileName = "exampleWithoutSuffix";
        //When
        String actual = mailet.saveAttachmentToFile(part, fileName);
        //Then
        assertThat(actual).startsWith("exampleWithoutSuffix");
        assertThat(actual).endsWith(".bin");
    }
    
    private Mailet initMailet() throws MessagingException {
        Mailet mailet = new StripAttachment();

        FakeMailetConfig mci = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("directory", folder.getRoot().getPath())
                .setProperty("remove", "all")
                .setProperty("pattern", ".*\\.tmp")
                .setProperty("decodeFilename", "true")
                .setProperty("replaceFilenamePattern",
                    "/[\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5]/A//,"
                            + "/[\u00C6]/AE//,"
                            + "/[\u00C8\u00C9\u00CA\u00CB]/E//,"
                            + "/[\u00CC\u00CD\u00CE\u00CF]/I//,"
                            + "/[\u00D2\u00D3\u00D4\u00D5\u00D6]/O//,"
                            + "/[\u00D7]/x//," + "/[\u00D9\u00DA\u00DB\u00DC]/U//,"
                            + "/[\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5]/a//,"
                            + "/[\u00E6]/ae//,"
                            + "/[\u00E8\u00E9\u00EA\u00EB]/e/r/,"
                            + "/[\u00EC\u00ED\u00EE\u00EF]/i//,"
                            + "/[\u00F2\u00F3\u00F4\u00F5\u00F6]/o//,"
                            + "/[\u00F9\u00FA\u00FB\u00FC]/u//,"
                            + "/[^A-Za-z0-9._-]+/_/r/")
                .build();

        mailet.init(mci);
        return mailet;
    }

}
