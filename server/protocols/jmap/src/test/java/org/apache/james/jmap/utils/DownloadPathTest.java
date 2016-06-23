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

package org.apache.james.jmap.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DownloadPathTest {

    @Test(expected=IllegalArgumentException.class)
    public void fromShouldThrowWhenPathIsNull() {
        DownloadPath.from(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void fromShouldThrowWhenPathIsEmpty() {
        DownloadPath.from("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void fromShouldThrowWhenNoBlobId() {
        DownloadPath.from("/");
    }

    @Test
    public void fromShouldParseWhenBlobId() {
        String expectedBlobId = "123456789";
        DownloadPath downloadPath = DownloadPath.from("/" + expectedBlobId);
        assertThat(downloadPath.getBlobId()).isEqualTo(expectedBlobId);
    }

    @Test
    public void nameShouldBeEmptyWhenNone() {
        DownloadPath downloadPath = DownloadPath.from("/123456789");
        assertThat(downloadPath.getName()).isEmpty();
    }

    @Test
    public void nameShouldBeEmptyWhenNoneButSlash() {
        DownloadPath downloadPath = DownloadPath.from("/123456789/");
        assertThat(downloadPath.getName()).isEmpty();
    }

    @Test
    public void nameShouldBePresentWhenGiven() {
        String expectedName = "myName";
        DownloadPath downloadPath = DownloadPath.from("/123456789/" + expectedName);
        assertThat(downloadPath.getName()).hasValue(expectedName);
    }

    @Test
    public void fromShouldParseWhenExtraPathVariables() {
        String expectedBlobId = "123456789";
        String expectedName = "myName";
        DownloadPath downloadPath = DownloadPath.from("/" + expectedBlobId + "/" + expectedName + "/132/456/789");
        assertThat(downloadPath.getBlobId()).isEqualTo(expectedBlobId);
        assertThat(downloadPath.getName()).hasValue(expectedName);
    }
}
