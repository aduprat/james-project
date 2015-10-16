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

import java.time.LocalDate;
import java.util.List;

public class RecurrenceDto {

    public String frequency;
    public int interval;
    public int firstDayOfWeek;
    public List<Integer> byDay;
    public List<Integer> byDate;
    public List<Integer> byMonth;
    public List<Integer> byYearDay;
    public List<Integer> byWeekNo;
    public List<Integer> byHour;
    public List<Integer> byMinute;
    public List<Integer> bySecond;
    public List<Integer> bySetPosition;
    public int count;
    public LocalDate until;
}
