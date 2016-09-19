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

package org.apache.james.transport.mailets.redirect;

import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class AddressExtractor {

    private final MailetContext mailetContext;

    public AddressExtractor(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    public List<MailAddress> from(String addressList, List<String> allowedSpecials) throws MessagingException {
        try {
            return toMailAddresses(ImmutableList.copyOf(InternetAddress.parse(addressList, false)), allowedSpecials);
        } catch (AddressException e) {
            throw new MessagingException("Exception thrown parsing: " + addressList, e);
        }
    }

    private List<MailAddress> toMailAddresses(List<InternetAddress> addresses, List<String> allowedSpecials) throws MessagingException {
        ImmutableList.Builder<MailAddress> builder = ImmutableList.builder();
        for (InternetAddress address : addresses) {
            builder.add(toMailAddress(address, allowedSpecials));
        }
        return builder.build();
    }

    private MailAddress toMailAddress(InternetAddress address, List<String> allowedSpecials) throws MessagingException {
        try {
            Optional<MailAddress> specialAddress = getSpecialAddress(address.getAddress(), allowedSpecials);
            if (specialAddress.isPresent()) {
                return specialAddress.get();
            }
            return new MailAddress(address);
        } catch (Exception e) {
            throw new MessagingException("Exception thrown parsing: " + address.getAddress());
        }
    }

    /**
     * Returns an {@link Optional} the {@link SpecialAddress} that corresponds to an init parameter
     * value. The init parameter value is checked against a List<String> of allowed
     * values. The checks are case insensitive.
     *
     * @param addressString   the string to check if is a special address
     * @param allowedSpecials a List<String> with the allowed special addresses
     * @return a SpecialAddress if found, absent if not found or addressString is
     *         null
     * @throws MessagingException if is a special address not in the allowedSpecials list
     */
    public Optional<MailAddress> getSpecialAddress(String addressString, List<String> allowedSpecials) throws MessagingException {
        if (Strings.isNullOrEmpty(addressString)) {
            return Optional.absent();
        }

        Optional<MailAddress> specialAddress = asSpecialAddress(addressString);
        if (specialAddress.isPresent()) {
            if (!isAllowed(addressString, allowedSpecials)) {
                throw new MessagingException("Special (\"magic\") address found not allowed: " + addressString + ", allowed values are \"" + asString(allowedSpecials) + "\"");
            }
            return specialAddress;
        }
        return Optional.absent();
    }

    private String asString(List<String> allowedSpecials) {
        return "[" + Joiner.on(", ").join(allowedSpecials) + "]";
    }

    private Optional<MailAddress> asSpecialAddress(String addressString) {
        String lowerCaseTrimed = addressString.toLowerCase(Locale.US).trim();
        if (lowerCaseTrimed.equals("postmaster")) {
            return Optional.of(mailetContext.getPostmaster());
        }
        if (lowerCaseTrimed.equals("sender")) {
            return Optional.of(SpecialAddress.SENDER);
        }
        if (lowerCaseTrimed.equals("reversepath")) {
            return Optional.of(SpecialAddress.REVERSE_PATH);
        }
        if (lowerCaseTrimed.equals("from")) {
            return Optional.of(SpecialAddress.FROM);
        }
        if (lowerCaseTrimed.equals("replyto")) {
            return Optional.of(SpecialAddress.REPLY_TO);
        }
        if (lowerCaseTrimed.equals("to")) {
            return Optional.of(SpecialAddress.TO);
        }
        if (lowerCaseTrimed.equals("recipients")) {
            return Optional.of(SpecialAddress.RECIPIENTS);
        }
        if (lowerCaseTrimed.equals("delete")) {
            return Optional.of(SpecialAddress.DELETE);
        }
        if (lowerCaseTrimed.equals("unaltered")) {
            return Optional.of(SpecialAddress.UNALTERED);
        }
        if (lowerCaseTrimed.equals("null")) {
            return Optional.of(SpecialAddress.NULL);
        }
        return Optional.absent();
    }

    private boolean isAllowed(String addressString, List<String> allowedSpecials) {
        for (String allowedSpecial : allowedSpecials) {
            if (addressString.equals(allowedSpecial.toLowerCase(Locale.US).trim())) {
                return true;
            }
        }
        return false;
    }
}
