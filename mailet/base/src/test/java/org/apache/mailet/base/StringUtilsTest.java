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
package org.apache.mailet.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void arrayToStringShouldReturnNullWhenArrayIsNull() {
        String arrayToString = StringUtils.arrayToString(null);
        assertThat(arrayToString).isEqualTo("null");
    }

    @Test
    public void arrayToStringShouldReturnOnlyBracketsWhenArrayIsEmpty() {
        String arrayToString = StringUtils.arrayToString(new String[] {});
        assertThat(arrayToString).isEqualTo("[]");
    }

    @Test
    public void arrayToStringShouldReturnOneElementWhenArrayContainsOneElement() {
        String arrayToString = StringUtils.arrayToString(new String[] { "first" });
        assertThat(arrayToString).isEqualTo("[first]");
    }

    @Test
    public void arrayToStringShouldReturnSeparatedElementsWhenArrayContainsMultipleElements() {
        String arrayToString = StringUtils.arrayToString(new String[] { "first", "second", "fourth" });
        assertThat(arrayToString).isEqualTo("[first,second,fourth]");
    }
}
