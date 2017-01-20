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

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.mail.MessagingException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.transport.mailets.model.ICAL;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import net.fortuna.ical4j.model.Calendar;

/**
 * ICALToJsonAttribute takes a map of ICAL4J objects attached as attribute, and output the map of corresponding json bytes as
 * an other attribute, with unique String keys.
 *
 * The JSON contains the following fields :
 *
 * <ul>
 *     <li><b>ical</b> : the raw ical string, in UTF-8</li>
 *     <li><b>sender</b> : the sender of the mail (compulsory, mail without sender will be discarded)</li>
 *     <li><b>recipient</b> : the recipient of the mail. If the mail have several recipients, each recipient will have
 *     its own JSON.</li>
 *     <li><b>uid</b> : the UID of the ical (optional)</li>
 *     <li><b>sequence</b> : the sequence of the ical (optional)</li>
 *     <li><b>dtstamp</b> : the date stamp of the ical (optional)</li>
 *     <li><b>method</b> : the method of the ical (optional)</li>
 *     <li><b>recurrence-id</b> : the recurrence-id of the ical (optional)</li>
 * </ul>
 *
 * Example are included in test call ICalToJsonAttributeTest.
 *
 *  Configuration example :
 *
 * <pre>
 *     <code>
 *         &lt;mailet matcher=??? class=ICALToJsonAttribute&gt;
 *             &lt;sourceAttribute&gt;icalendars&lt;/sourceAttribute&gt;
 *             &lt;destinationAttribute&gt;icalendarJson&lt;/destinationAttribute&gt;
 *         &lt;/mailet&gt;
 *     </code>
 * </pre>
 */
public class ICALToJsonAttribute extends GenericMailet {

    public static final String SOURCE_ATTRIBUTE_NAME = "source";
    public static final String DESTINATION_ATTRIBUTE_NAME = "destination";
    public static final String DEFAULT_SOURCE_ATTRIBUTE_NAME = "icalendar";
    public static final String DEFAULT_DESTINATION_ATTRIBUTE_NAME = "icalendarJson";

    private final ObjectMapper objectMapper;
    private String sourceAttributeName;
    private String destinationAttributeName;

    public ICALToJsonAttribute() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
    }

    public String getSourceAttributeName() {
        return sourceAttributeName;
    }

    public String getDestinationAttributeName() {
        return destinationAttributeName;
    }

    @Override
    public String getMailetInfo() {
        return "ICALToJson Mailet";
    }

    @Override
    public void init() throws MessagingException {
        sourceAttributeName = getInitParameter(SOURCE_ATTRIBUTE_NAME, DEFAULT_SOURCE_ATTRIBUTE_NAME);
        destinationAttributeName = getInitParameter(DESTINATION_ATTRIBUTE_NAME, DEFAULT_DESTINATION_ATTRIBUTE_NAME);
        if (Strings.isNullOrEmpty(sourceAttributeName)) {
            throw new MessagingException(SOURCE_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(destinationAttributeName)) {
            throw new MessagingException(DESTINATION_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (mail.getAttribute(sourceAttributeName) == null) {
            return;
        }
        if (mail.getSender() == null) {
            log("Skipping " + mail.getName() + " because no sender");
            return;
        }
        try {
            Map<String, Calendar> calendars = getCalendarMap(mail);
            log("ICALToJsonAttribute: input: " + calendars);
            Map<String, byte[]> jsonsInByteForm = calendars.entrySet()
                .stream()
                .flatMap(calendar -> toJson(calendar, mail))
                .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue));
            log("ICALToJsonAttribute: output: " + jsonsInByteForm);
            mail.setAttribute(destinationAttributeName, (Serializable) jsonsInByteForm);
        } catch (ClassCastException e) {
            log("Received a mail with " + sourceAttributeName + " not being an ICAL object for mail " + mail.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Calendar> getCalendarMap(Mail mail) {
        return (Map<String, Calendar>) mail.getAttribute(sourceAttributeName);
    }

    private Stream<Pair<String, byte[]>> toJson(Map.Entry<String, Calendar> entry, Mail mail) {
        return mail.getRecipients()
            .stream()
            .flatMap(recipient -> toICAL(entry.getValue(), recipient, mail.getSender()))
            .flatMap(ical -> toJson(ical, mail.getName()))
            .map(json -> Pair.of(UUID.randomUUID().toString(), json.getBytes(Charsets.UTF_8)));
    }

    private Stream<String> toJson(ICAL ical, String mailName) {
        try {
            String json = objectMapper.writeValueAsString(ical);
            log("ICALToJsonAttribute: json: " + json);
            return Stream.of(json);
        } catch (JsonProcessingException e) {
            log("Error while serializing Calendar for mail " + mailName, e);
            return Stream.of();
        } catch (Exception e) {
            log("Exception caught while attaching ICAL to the email as JSON for mail " + mailName, e);
            return Stream.of();
        }
    }

    private Stream<ICAL> toICAL(Calendar calendar, MailAddress recipient, MailAddress sender) {
        try {
            ICAL ical = ICAL.builder()
                .from(calendar)
                .recipient(recipient)
                .sender(sender)
                .build();
            log("ICALToJsonAttribute: ical: " + ical);
            return Stream.of(ical);
        } catch (Exception e) {
            log("Exception while converting calendar to ICAL", e);
            return Stream.of();
        }
    }
}
