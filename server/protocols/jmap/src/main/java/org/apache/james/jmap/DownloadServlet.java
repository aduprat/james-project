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
package org.apache.james.jmap;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.james.jmap.utils.DownloadPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.model.Attachment;
import org.apache.james.mailbox.store.mail.model.AttachmentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class DownloadServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServlet.class);

    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;

    @Inject
    @VisibleForTesting DownloadServlet(MailboxSessionMapperFactory mailboxSessionMapperFactory) {
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        String pathInfo = req.getPathInfo();
        try {
            download(getMailboxSession(req), DownloadPath.from(pathInfo), resp);
        } catch (IllegalArgumentException e) {
            LOGGER.error(String.format("Bad path '%s'", pathInfo), e);
            resp.setStatus(SC_BAD_REQUEST);
        }
    }

    @VisibleForTesting void download(MailboxSession mailboxSession, DownloadPath downloadPath, HttpServletResponse resp) {
        String blobId = downloadPath.getBlobId();
        try {
            addHeader(downloadPath.getName(), resp);

            AttachmentMapper attachmentMapper = mailboxSessionMapperFactory.createAttachmentMapper(mailboxSession);
            Attachment attachment = attachmentMapper.getAttachment(AttachmentId.from(blobId));
            IOUtils.copy(attachment.getStream(), resp.getOutputStream());

            resp.setStatus(SC_OK);
        } catch (AttachmentNotFoundException e) {
            LOGGER.info(String.format("Attachment '%s' not found", blobId), e);
            resp.setStatus(SC_NOT_FOUND);
        } catch (MailboxException | IOException e) {
            LOGGER.error("Error while downloading", e);
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void addHeader(Optional<String> optionalName, HttpServletResponse resp) {
        optionalName.ifPresent(name -> resp.addHeader("Content-Disposition", name));
    }

    private MailboxSession getMailboxSession(HttpServletRequest req) {
        return (MailboxSession) req.getAttribute(AuthenticationFilter.MAILBOX_SESSION);
    }
}
