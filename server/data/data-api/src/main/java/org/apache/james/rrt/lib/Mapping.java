/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.rrt.lib;

import java.util.Optional;
import java.util.function.Supplier;

import javax.mail.internet.AddressException;

import org.apache.james.core.Domain;
import org.apache.james.core.MailAddress;

import com.google.common.base.Preconditions;

public interface Mapping {

    interface ValidationMode {
        ValidationMode STRICT = new StrictMode();
        ValidationMode LENIENT = new LenientMode();

        Optional<MailAddress> asMailAddress(Mapping mapping);
    }

    class LenientMode implements ValidationMode {
        @Override
        public Optional<MailAddress> asMailAddress(Mapping mapping) {
            if (mapping.getType() != Type.Address && mapping.getType() != Type.Forward) {
                return Optional.empty();
            }
            try {
                return Optional.of(new MailAddress(mapping.getType().withoutPrefix(mapping.asString())));
            } catch (AddressException e) {
                return Optional.empty();
            }
        }
    }

    class StrictMode extends LenientMode {
        @Override
        public Optional<MailAddress> asMailAddress(Mapping mapping) {
            Preconditions.checkState(mapping.getType() == Type.Address || mapping.getType() == Type.Forward);
            return super.asMailAddress(mapping);
        }
    }

    static Type detectType(String input) {
        if (input.startsWith(Type.Regex.asPrefix())) {
            return Type.Regex;
        }
        if (input.startsWith(Type.Domain.asPrefix())) {
            return Type.Domain;
        }
        if (input.startsWith(Type.Error.asPrefix())) {
            return Type.Error;
        }
        if (input.startsWith(Type.Forward.asPrefix())) {
            return Type.Forward;
        }
        return Type.Address;
    }

    Optional<MailAddress> asMailAddress(ValidationMode validationMode);

    default Optional<MailAddress> asMailAddress() {
        return asMailAddress(ValidationMode.STRICT);
    }

    enum Type {
        Regex("regex:", 3),
        Domain("domain:", 1),
        Error("error:", 3),
        Forward("forward:", 2),
        Address("", 3);

        private final String asPrefix;
        private final int order;

        Type(String asPrefix, Integer order) {
            this.asPrefix = asPrefix;
            this.order = order;
        }

        public String asPrefix() {
            return asPrefix;
        }

        public String withoutPrefix(String input) {
            Preconditions.checkArgument(input.startsWith(asPrefix));
            return input.substring(asPrefix.length());
        }

        public static boolean hasPrefix(String mapping) {
            return mapping.startsWith(Regex.asPrefix())
                || mapping.startsWith(Domain.asPrefix())
                || mapping.startsWith(Error.asPrefix())
                || mapping.startsWith(Forward.asPrefix());
        }

        public int getOrder() {
            return order;
        }
    }

    Type getType();
    
    String asString();

    boolean hasDomain();

    Mapping appendDomainIfNone(Supplier<Domain> domainSupplier);

    String getErrorMessage();

}