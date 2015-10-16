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

public class CalendarEventDto {

    public String id;
    public String calendarId;
    public String summary;
    public String description;
    public String location;
    public boolean showAsFree;
    public boolean isAllDay;
    public LocalDate start;
    public LocalDate end;
    public String startTimeZone;
    public String endTimeZone;
    public RecurrenceDto recurrence;
    public List<LocalDate> inclusions;
    public List<LocalDate> exceptions;
    public List<AlertDto> alerts;
    public ParticipantDto organizer;
    public List<ParticipantDto> attendees;
    public List<FileDto> attachments;
}
