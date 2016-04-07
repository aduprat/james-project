package org.apache.james.mailbox.caching;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

/**
 * A MailboxSessionMapperFactory that uses the underlying MailboxSessionMapperFactory to provide
 * caching variants of MessageMapper and MailboxMapper built around the MessageMapper and MailboxMapper
 * provided by it
 * 
 * @param <Id>
 */
public class CachingMailboxSessionMapperFactory<Id extends MailboxId> extends
		MailboxSessionMapperFactory<Id> {

	private final MailboxSessionMapperFactory<Id> underlying;
	private final MailboxByPathCache<Id> mailboxByPathCache;
	private final MailboxMetadataCache<Id> mailboxMetadataCache;

	public CachingMailboxSessionMapperFactory(MailboxSessionMapperFactory<Id> underlying, MailboxByPathCache<Id> mailboxByPathCache, MailboxMetadataCache<Id> mailboxMetadataCache) {
		this.underlying = underlying;
		this.mailboxByPathCache = mailboxByPathCache;
		this.mailboxMetadataCache = mailboxMetadataCache;
	}
	
	@Override
	public MessageMapper<Id> createMessageMapper(MailboxSession session)
			throws MailboxException {
		return new CachingMessageMapper<Id>(underlying.createMessageMapper(session), mailboxMetadataCache);
	}

	@Override
	public MailboxMapper<Id> createMailboxMapper(MailboxSession session)
			throws MailboxException {
		return new CachingMailboxMapper<Id>(underlying.createMailboxMapper(session), mailboxByPathCache);
	}

	@Override
	public SubscriptionMapper createSubscriptionMapper(MailboxSession session)
			throws SubscriptionException {
		return underlying.createSubscriptionMapper(session);
	}

    @Override
    public AttachmentMapper createAttachmentMapper() {
        throw new UnsupportedOperationException();
    }

}
