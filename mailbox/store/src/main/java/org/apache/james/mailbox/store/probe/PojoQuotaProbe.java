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

package org.apache.james.mailbox.store.probe;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.SerializableQuota;

public class PojoQuotaProbe implements QuotaProbe {

    @Override
    public String getQuotaRoot(String namespace, String user, String name) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public SerializableQuota getMessageCountQuota(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public SerializableQuota getStorageQuota(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public long getMaxMessageCount(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getMaxStorage(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getDefaultMaxMessageCount() throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getDefaultMaxStorage() throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setMaxMessageCount(String quotaRoot, long maxMessageCount) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setMaxStorage(String quotaRoot, long maxSize) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setDefaultMaxMessageCount(long maxDefaultMessageCount) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setDefaultMaxStorage(long maxDefaultSize) throws MailboxException {
        throw new NotImplementedException();
    }

}
