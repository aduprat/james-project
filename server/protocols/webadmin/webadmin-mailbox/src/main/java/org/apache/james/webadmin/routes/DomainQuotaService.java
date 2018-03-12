package org.apache.james.webadmin.routes;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.webadmin.dto.QuotaDTO;

import com.github.fge.lambdas.Throwing;

public class DomainQuotaService {

    private final MaxQuotaManager maxQuotaManager;

    @Inject
    public DomainQuotaService(MaxQuotaManager maxQuotaManager) {
        this.maxQuotaManager = maxQuotaManager;
    }

    public Optional<QuotaCount> getMaxCountQuota(String domain) {
        return maxQuotaManager.getDomainMaxMessage(domain);
    }

    public void setMaxCountQuota(String domain, QuotaCount quotaCount) throws MailboxException {
        maxQuotaManager.setDomainMaxMessage(domain, quotaCount);
    }

    public void remoteMaxQuotaCount(String domain) throws MailboxException {
        maxQuotaManager.removeDomainMaxMessage(domain);
    }

    public Optional<QuotaSize> getMaxSizeQuota(String domain) {
        return maxQuotaManager.getDomainMaxStorage(domain);
    }

    public void setMaxSizeQuota(String domain, QuotaSize quotaSize) throws MailboxException {
        maxQuotaManager.setDomainMaxStorage(domain, quotaSize);
    }

    public void remoteMaxQuotaSize(String domain) throws MailboxException {
        maxQuotaManager.removeDomainMaxStorage(domain);
    }

    public QuotaDTO getQuota(String domain) {
        return QuotaDTO
            .builder()
            .count(maxQuotaManager.getDomainMaxMessage(domain))
            .size(maxQuotaManager.getDomainMaxStorage(domain))
            .build();
    }

    public void defineQuota(String domain, QuotaDTO quota) {
        quota.getCount()
            .ifPresent(Throwing.consumer(count -> maxQuotaManager.setDomainMaxMessage(domain, count)));
        quota.getSize()
            .ifPresent(Throwing.consumer(size -> maxQuotaManager.setDomainMaxStorage(domain, size)));
    }
}
