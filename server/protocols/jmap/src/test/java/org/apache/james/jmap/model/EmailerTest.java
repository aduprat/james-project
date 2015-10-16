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

package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.dto.EmailerDto;
import org.junit.Test;

public class EmailerTest {

    @Test(expected=NullPointerException.class)
    public void nameShouldThrowWhenNameIsNull() {
        Emailer.builder()
            .name(null);
    }

    @Test(expected=NullPointerException.class)
    public void emailShouldThrowWhenEmailIsNull() {
        Emailer.builder()
            .email(null);
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsNull() {
        Emailer.builder().build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsEmpty() {
        Emailer.builder()
            .name("")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenEmailIsNull() {
        Emailer.builder()
            .name("name")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenEmailIsEmpty() {
        Emailer.builder()
            .name("name")
            .email("")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenEmailDoesNotContainsAt() {
        Emailer.builder()
            .name("name")
            .email("email")
            .build();
    }

    @Test
    public void buildShouldWork() {
        String expectedName = "name";
        String expectedEmail = "name@apache.org";

        Emailer emailer = Emailer.builder()
            .name(expectedName)
            .email(expectedEmail)
            .build();

        assertThat(emailer.getName()).isEqualTo(expectedName);
        assertThat(emailer.getEmail()).isEqualTo(expectedEmail);
    }

    @Test
    public void fromShouldWork() {
        String expectedName = "name";
        String expectedEmail = "name@apache.org";

        EmailerDto dto = new EmailerDto();
        dto.name = expectedName;
        dto.email = expectedEmail;

        Emailer emailer = Emailer.from(dto);

        assertThat(emailer.getName()).isEqualTo(expectedName);
        assertThat(emailer.getEmail()).isEqualTo(expectedEmail);
    }
}
