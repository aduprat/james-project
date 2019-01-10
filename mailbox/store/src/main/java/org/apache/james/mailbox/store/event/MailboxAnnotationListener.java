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
package org.apache.james.mailbox.store.event;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.SessionProvider;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailboxAnnotationListener implements MailboxListener.GroupMailboxListener {
    private static final class MailboxAnnotationListenerGroup extends Group {}

    private static final Logger logger = LoggerFactory.getLogger(MailboxAnnotationListener.class);
    private static final Group GROUP = new MailboxAnnotationListenerGroup();

    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;
    private final SessionProvider sessionProvider;

    @Inject
    public MailboxAnnotationListener(MailboxSessionMapperFactory mailboxSessionMapperFactory, SessionProvider sessionProvider) {
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Group getGroup() {
        return GROUP;
    }

    @Override
    public void event(Event event) {
        if (event instanceof MailboxDeletion) {
            try {
                MailboxSession mailboxSession = sessionProvider.createSystemSession(event.getUser().asString());
                AnnotationMapper annotationMapper = mailboxSessionMapperFactory.getAnnotationMapper(mailboxSession);
                MailboxId mailboxId = ((MailboxDeletion) event).getMailboxId();

                deleteRelatedAnnotations(mailboxId, annotationMapper);
            } catch (MailboxException e) {
                logger.error("Unable to look up AnnotationMapper", e);
            }
        }
    }

    private void deleteRelatedAnnotations(MailboxId mailboxId, AnnotationMapper annotationMapper) {
        List<MailboxAnnotation> annotations = annotationMapper.getAllAnnotations(mailboxId);
        for (MailboxAnnotation annotation : annotations) {
            try {
                annotationMapper.deleteAnnotation(mailboxId, annotation.getKey());
            } catch (Exception e) {
                logger.error("Unable to delete annotation {} cause {}", annotation.getKey(), e.getMessage());
            }
        }
    }
}
