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

package org.apache.james.util.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Sends the message through daemonized SpamAssassin (spamd), visit <a
 * href="SpamAssassin.org">SpamAssassin.org</a> for info on configuration.
 */
public class SpamAssassinInvoker {

    /** The mail attribute under which the status get stored */
    public static final String STATUS_MAIL_ATTRIBUTE_NAME = "org.apache.james.spamassassin.status";

    /** The mail attribute under which the flag get stored */
    public static final String FLAG_MAIL_ATTRIBUTE_NAME = "org.apache.james.spamassassin.flag";

    private final String spamdHost;

    private final int spamdPort;

    /**
     * Init the spamassassin invoker
     * 
     * @param spamdHost
     *            The host on which spamd runs
     * @param spamdPort
     *            The port on which spamd listen
     */
    public SpamAssassinInvoker(String spamdHost, int spamdPort) {
        this.spamdHost = spamdHost;
        this.spamdPort = spamdPort;
    }

    /**
     * Scan a MimeMessage for spam by passing it to spamd.
     * 
     * @param message
     *            The MimeMessage to scan
     * @return true if spam otherwise false
     * @throws MessagingException
     *             if an error on scanning is detected
     */
    public SpamAssassinResult scanMail(MimeMessage message) throws MessagingException {
        try (Socket socket = new Socket(spamdHost, spamdPort);
                OutputStream out = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.write("CHECK SPAMC/1.2\r\n\r\n".getBytes());

            // pass the message to spamd
            message.writeTo(out);
            out.flush();
            socket.shutdownOutput();
            String line;
            while ((line = in.readLine()) != null) {
                if (isSpam(line)) {
                    return processSpam(line);
                }
            }
            return SpamAssassinResult.empty();
        } catch (UnknownHostException e1) {
            throw new MessagingException("Error communicating with spamd. Unknown host: " + spamdHost);
        } catch (IOException | MessagingException e1) {
            throw new MessagingException("Error communicating with spamd on " + spamdHost + ":" + spamdPort + " Exception: " + e1);
        }
    }

    private SpamAssassinResult processSpam(String line) {
        String[] split = Pattern.compile(" ").split(line);
        boolean spam = spam(split[1]);
        String hits = split[3];
        String required = split[5];
        SpamAssassinResult.Builder builder = SpamAssassinResult.builder()
            .hits(hits)
            .requiredHits(required);

        if (spam) {
            builder.putHeader(FLAG_MAIL_ATTRIBUTE_NAME, "YES");
            builder.putHeader(STATUS_MAIL_ATTRIBUTE_NAME, "Yes, hits=" + hits + " required=" + required);
            return builder.build();
        } else {
            builder.putHeader(FLAG_MAIL_ATTRIBUTE_NAME, "NO");
            builder.putHeader(STATUS_MAIL_ATTRIBUTE_NAME, "No, hits=" + hits + " required=" + required);
            return builder.build();
        }
    }

    private boolean spam(String string) {
        try {
            return Boolean.valueOf(string);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSpam(String line) {
        return line.startsWith("Spam:");
    }
}
