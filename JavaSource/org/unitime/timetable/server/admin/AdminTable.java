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
package org.unitime.timetable.server.admin;

import org.unitime.timetable.gwt.shared.SimpleEditInterface;
import org.unitime.timetable.security.SessionContext;

/**
 * @author Tomas Muller
 */
public interface AdminTable {
	public SimpleEditInterface.PageName name();
	
	public SimpleEditInterface load(SessionContext context, org.hibernate.Session hibSession);
	
	public void save(SimpleEditInterface data, SessionContext context, org.hibernate.Session hibSession);
	
	public abstract void save(SimpleEditInterface.Record record, SessionContext context, org.hibernate.Session hibSession);
	
	public abstract void update(SimpleEditInterface.Record record, SessionContext context, org.hibernate.Session hibSession);
	
	public abstract void delete(SimpleEditInterface.Record record, SessionContext context, org.hibernate.Session hibSession);
	
	public interface HasFilter {
		public SimpleEditInterface.Filter getFilter(SessionContext context, org.hibernate.Session hibSession);
		public SimpleEditInterface load(String[] filter, SessionContext context, org.hibernate.Session hibSession);
		public void save(String[] filter, SimpleEditInterface data, SessionContext context, org.hibernate.Session hibSession);
	}
	
	public interface HasLazyFields {
		public void load(SimpleEditInterface.Record record, SessionContext context, org.hibernate.Session hibSession);
	}
}
