/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.model;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.unitime.timetable.model.base.BaseTeachingScheduleAssignment;
import org.unitime.timetable.util.Constants;

public class TeachingScheduleAssignment extends BaseTeachingScheduleAssignment {
	private static final long serialVersionUID = 1965118942805374030L;

	public TeachingScheduleAssignment() {
		super();
	}

	public Date getStartTime(int offset) {
		Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(getMeeting().getMeetingDate());
        int min = (getMeeting().getStartPeriod().intValue()*Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN)+(getMeeting().getStartOffset()==null?0:getMeeting().getStartOffset());
        min += offset;
        c.set(Calendar.HOUR, min/60);
        c.set(Calendar.MINUTE, min%60);
        return c.getTime();
	}
}