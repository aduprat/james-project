package org.apache.james.jmap.utils;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.model.mailbox.SortOrder;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

/**
 * Created by aduprat on 25/03/16.
 */
public class MailboxUtils<Id extends MailboxId> {

    private static final boolean DONT_RESET_RECENT = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxUtils.class);

    private final MailboxManager mailboxManager;
    private final MailboxMapperFactory<Id> mailboxMapperFactory;

    @Inject
    @VisibleForTesting
    public MailboxUtils(MailboxManager mailboxManager, MailboxMapperFactory<Id> mailboxMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
    }

    public Optional<Mailbox> mailboxFromMailboxPath(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            Optional<Role> role = Role.from(mailboxPath.getName());
            MessageManager.MetaData mailboxMetaData = getMailboxMetaData(mailboxPath, mailboxSession);
            return Optional.ofNullable(Mailbox.builder()
                    .id(getMailboxId(mailboxPath, mailboxSession))
                    .name(mailboxPath.getName())
                    .role(role)
                    .unreadMessages(mailboxMetaData.getUnseenCount())
                    .totalMessages(mailboxMetaData.getMessageCount())
                    .sortOrder(SortOrder.getSortOrder(role))
                    .build());
        } catch (MailboxException e) {
            LOGGER.warn("Cannot find mailbox for :" + mailboxPath.getName(), e);
            return Optional.empty();
        }
    }

    private String getMailboxId(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        return mailboxMapperFactory.getMailboxMapper(mailboxSession)
                .findMailboxByPath(mailboxPath)
                .getMailboxId()
                .serialize();
    }

    private MessageManager.MetaData getMailboxMetaData(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        return mailboxManager.getMailbox(mailboxPath, mailboxSession)
                .getMetaData(DONT_RESET_RECENT, mailboxSession, MessageManager.MetaData.FetchGroup.UNSEEN_COUNT);
    }

    public Optional<org.apache.james.mailbox.store.mail.model.Mailbox<Id>> getMailboxFromId(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        return mailboxMapperFactory.getMailboxMapper(mailboxSession)
                .list().stream()
                .filter(mailbox -> mailbox.getMailboxId().serialize().equals(mailboxId))
                .findFirst();
    }

    public Optional<String> getMailboxNameFromId(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        return getMailboxFromId(mailboxId, mailboxSession)
                .map(org.apache.james.mailbox.store.mail.model.Mailbox::getName);
    }

    public Optional<String> getParentIdFromId(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        Optional<String> mailboxName = getMailboxNameFromId(mailboxId, mailboxSession);
        if (mailboxName.isPresent()) {
            String[] paths = mailboxName.get().split(String.valueOf(mailboxSession.getPathDelimiter()));
            String parentPath = Joiner.on(mailboxSession.getPathDelimiter()).join(paths);
        }
        return mailboxMapperFactory.getMailboxMapper(mailboxSession)
                .list().stream()
                .filter(mailbox -> mailbox.getMailboxId().serialize().equals(mailboxId))
                .map(mailbox -> mailbox.getName())
                .findFirst();
    }
}
