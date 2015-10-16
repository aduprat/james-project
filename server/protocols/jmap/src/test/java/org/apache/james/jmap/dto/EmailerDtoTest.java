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

package org.apache.james.jmap.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.model.Emailer;
import org.junit.Test;

public class EmailerDtoTest {

    @Test
    public void fromShouldWork() {
        String expectedName = "name";
        String expectedEmail = "name@apache.org";

        Emailer emailer = Emailer.builder()
                .name(expectedName)
                .email(expectedEmail)
                .build();

        EmailerDto dto = EmailerDto.from(emailer);

        assertThat(dto.name).isEqualTo(expectedName);
        assertThat(dto.email).isEqualTo(expectedEmail);
    }
}
