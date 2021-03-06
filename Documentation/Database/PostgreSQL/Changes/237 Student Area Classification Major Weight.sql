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

alter table student_area_clasf_major add weight double precision default 1.0;
alter table pit_stu_aa_major_clasf add concentration_id bigint;
alter table pit_stu_aa_major_clasf add weight double precision default 1.0;

alter table pit_stu_aa_major_clasf add constraint fk_pit_stuamc_to_cc foreign key (concentration_id)
	references pos_major_conc (uniqueid) on delete cascade;

/*
 * Update database version
 */

update application_config set value='237' where name='tmtbl.db.version';

commit;
