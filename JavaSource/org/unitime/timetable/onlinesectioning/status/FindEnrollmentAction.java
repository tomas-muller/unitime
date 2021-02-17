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
package org.unitime.timetable.onlinesectioning.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentMap;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.gwt.client.sectioning.SectioningStatusFilterBox.SectioningStatusFilterRpcRequest;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.server.DayCode;
import org.unitime.timetable.gwt.server.Query;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.WaitListMode;
import org.unitime.timetable.model.FixedCreditUnitConfig;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentSectioningStatus;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentSectioningStatusDAO;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.custom.CustomStudentEnrollmentHolder;
import org.unitime.timetable.onlinesectioning.model.XAreaClassificationMajor;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XEnrollments;
import org.unitime.timetable.onlinesectioning.model.XExpectations;
import org.unitime.timetable.onlinesectioning.model.XInstructor;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XRoom;
import org.unitime.timetable.onlinesectioning.model.XSection;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.model.XStudentId;
import org.unitime.timetable.onlinesectioning.model.XSubpart;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequest;
import org.unitime.timetable.onlinesectioning.status.StatusPageSuggestionsAction.CourseLookup;
import org.unitime.timetable.solver.studentsct.StudentSolver;

/**
 * @author Tomas Muller
 */
public class FindEnrollmentAction implements OnlineSectioningAction<List<ClassAssignmentInterface.Enrollment>> {
	private static final long serialVersionUID = 1L;
	protected static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	protected Query iQuery;
	protected Long iCourseId, iClassId;
	protected boolean iConsentToDoCourse;
	protected boolean iCanShowExtIds = false, iCanRegister = false, iCanUseAssistant = false;
	protected Set<Long> iMyStudents;
	protected boolean iIsAdmin = false, iIsAdvisor = false, iCanEditMyStudents = false, iCanEditOtherStudents = false, iCanSelect = false;
	
	public FindEnrollmentAction withParams(String query, Long courseId, Long classId, boolean isConsentToDoCourse, boolean canShowExtIds, boolean canRegister, boolean canUseAssistant, Set<Long> myStudents) {
		iQuery = new Query(query);
		iCourseId = courseId;
		iClassId = classId;
		iConsentToDoCourse = isConsentToDoCourse;
		iCanShowExtIds = canShowExtIds;
		iCanRegister = canRegister;
		iCanUseAssistant = canUseAssistant;
		iMyStudents = myStudents;
		return this;
	}
	
	public FindEnrollmentAction withPermissions(boolean isAdmin, boolean isAdvisor, boolean canEditMyStudents, boolean canEditOtherStudents, boolean canSelectOtherRole) {
		iIsAdmin = isAdmin; iIsAdvisor = isAdvisor;
		iCanEditMyStudents = canEditMyStudents; iCanEditOtherStudents = canEditOtherStudents;
		iCanSelect = canSelectOtherRole;
		return this;
	}
	
	protected SectioningStatusFilterRpcRequest iFilter = null;
	public FindEnrollmentAction withFilter(SectioningStatusFilterRpcRequest filter) {
		iFilter = filter;
		return this;
	}
	
	public Query query() { return iQuery; }

	public Long courseId() { return iCourseId; }
	
	public Long classId() { return iClassId; }
	
	public boolean isConsentToDoCourse() { return iConsentToDoCourse; }
	
	public boolean isMyStudent(XStudentId student) {
		return iMyStudents != null && iMyStudents.contains(student.getStudentId());
	}
	
	public boolean isCanSelect(XStudentId student) {
		if (iIsAdmin) return true;
		if (iIsAdvisor) {
			if (iCanEditOtherStudents || (iCanEditMyStudents && isMyStudent(student))) return true;
		} else {
			if (iCanSelect) return true;
		}
		return false;
	}

	@Override
	public List<ClassAssignmentInterface.Enrollment> execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		List<ClassAssignmentInterface.Enrollment> ret = new ArrayList<ClassAssignmentInterface.Enrollment>();
		XCourse course = server.getCourse(courseId());
		if (course == null) return ret;
		XOffering offering = server.getOffering(course.getOfferingId());
		if (offering == null) return ret;
		XEnrollments enrollments = server.getEnrollments(course.getOfferingId());
		DistanceMetric m = server.getDistanceMetric();
		XExpectations expectations = server.getExpectations(offering.getOfferingId());
		OverExpectedCriterion overExp = server.getOverExpectedCriterion();
		AcademicSessionInfo session = server.getAcademicSession();
		CourseLookup lookup = new CourseLookup(session);
		boolean solver = (server instanceof StudentSolver);
		Set<Long> studentIds = null;
		if (!solver)
			studentIds = (iFilter == null ? null : server.createAction(SectioningStatusFilterAction.class).forRequest(iFilter).getStudentIds(server, helper));
		Set<String> regStates = new HashSet<String>();
		Set<String> assStates = new HashSet<String>();
		boolean waitListEnabled = CustomStudentEnrollmentHolder.isAllowWaitListing();
		Set<String> wlStates = new HashSet<String>();
		Set<String> noSubStates = new HashSet<String>();
		Session dbSession = SessionDAO.getInstance().get(session.getUniqueId());
		for (StudentSectioningStatus status: StudentSectioningStatusDAO.getInstance().findAll(helper.getHibSession())) {
			if (StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.enabled)
					|| (iIsAdmin && StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.admin))
					|| (iIsAdvisor && StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.advisor))
					) assStates.add(status.getReference());
				if (StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.regenabled)
					|| (iIsAdmin && StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.regadmin))
					|| (iIsAdvisor && StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.regadvisor))
					) regStates.add(status.getReference());
				if (waitListEnabled && StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.waitlist))
					wlStates.add(status.getReference());
				else if (StudentSectioningStatus.hasEffectiveOption(status, dbSession, StudentSectioningStatus.Option.nosubs))
					noSubStates.add(status.getReference());
		}
		
		for (XCourseRequest request: enrollments.getRequests()) {
			if (request.getEnrollment() != null && !request.getEnrollment().getCourseId().equals(courseId())) continue;
			if (classId() != null && request.getEnrollment() != null && !request.getEnrollment().getSectionIds().contains(classId())) continue;
			if (request.getEnrollment() == null && !request.getCourseIds().contains(course)) continue;
			if (studentIds != null && !studentIds.contains(request.getStudentId())) continue;
			XStudent student = server.getStudent(request.getStudentId());
			if (student == null) continue;
			if (request.getEnrollment() == null && !student.canAssign(request)) continue;
			if (!query().match(new StatusPageSuggestionsAction.CourseRequestMatcher(session, course, student, offering, request, isConsentToDoCourse(), isMyStudent(student), lookup, server))) continue;
			if (classId() != null && request.getEnrollment() == null) {
				boolean hasEnrollment = false;
				Assignment<Request, Enrollment> assignment = new AssignmentMap<Request, Enrollment>();
				CourseRequest r = SectioningRequest.convert(assignment, request, server);
				Section s = r.getSection(classId());
				values: for (Enrollment en: r.values(assignment)) {
					if (!en.getSections().contains(s)) continue;
					if (!query().match(new StatusPageSuggestionsAction.CourseRequestMatcher(session, course, student, offering, request, isConsentToDoCourse(), isMyStudent(student), lookup, server).setEnrollment(new XEnrollment(en)))) continue;
					for (Request x: r.getStudent().getRequests()) {
						Enrollment xe = assignment.getValue(x);
						if (!x.equals(r) && xe != null && xe.isOverlapping(en)) {
							continue values;
						}
					}
					hasEnrollment = true; break;
				}
				if (!hasEnrollment) continue;
			}
			
			ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
			st.setId(student.getStudentId());
			st.setSessionId(session.getUniqueId());
			st.setExternalId(student.getExternalId());
			st.setCanShowExternalId(iCanShowExtIds);
			String status = (student.getStatus() == null ? session.getDefaultSectioningStatus() : student.getStatus());
			if ((status == null && waitListEnabled) || (status != null && wlStates.contains(status)))
				st.setWaitListMode(WaitListMode.WaitList);
			else if (status != null && noSubStates.contains(status))
				st.setWaitListMode(WaitListMode.NoSubs);
			else
				st.setWaitListMode(WaitListMode.None);
			st.setCanRegister(iCanRegister && (status == null || regStates.contains(status)));
			st.setCanUseAssistant(iCanUseAssistant && (status == null || assStates.contains(status)));
			st.setCanSelect(isCanSelect(student));
			st.setName(student.getName());
			for (XAreaClassificationMajor acm: student.getMajors()) {
				st.addArea(acm.getArea());
				st.addClassification(acm.getClassification());
				st.addMajor(acm.getMajor());
			}
			for (XAreaClassificationMajor acm: student.getMinors()) {
				st.addMinor(acm.getMajor());
			}
			for (XStudent.XGroup gr: student.getGroups()) {
				st.addGroup(gr.getType(), gr.getAbbreviation(), gr.getTitle());
			}
			for (String acc: student.getAccomodations()) {
				st.addAccommodation(acc);
			}
			for (XStudent.XAdvisor a: student.getAdvisors()) {
				if (a.getName() != null) st.addAdvisor(a.getName());
			}
			
			ClassAssignmentInterface.Enrollment e = new ClassAssignmentInterface.Enrollment();
			e.setStudent(st);
			e.setPriority(1 + request.getPriority());
			CourseAssignment c = new CourseAssignment();
			c.setCourseId(course.getCourseId());
			c.setSubject(course.getSubjectArea());
			c.setCourseNbr(course.getCourseNumber());
			c.setTitle(course.getTitle());
			c.setHasCrossList(offering.hasCrossList());
			e.setCourse(c);
			e.setWaitList(request.isWaitlist());
			if (request.getEnrollment() == null)
				e.setEnrollmentMessage(request.getEnrollmentMessage());
			if (!request.getCourseIds().get(0).equals(course))
				e.setAlternative(request.getCourseIds().get(0).getCourseName());
			if (request.isAlternative()) {
				for (XRequest r: student.getRequests()) {
					if (r instanceof XCourseRequest && !r.isAlternative() && ((XCourseRequest)r).getEnrollment() == null) {
						e.setAlternative(((XCourseRequest)r).getCourseIds().get(0).getCourseName());
					}
				}
			}
			if (request.getTimeStamp() != null)
				e.setRequestedDate(request.getTimeStamp());
			if (request.getEnrollment() != null) {
				if (request.getEnrollment().getReservation() != null) {
					switch (request.getEnrollment().getReservation().getType()) {
					case Individual:
					case IndividualOverride:
						e.setReservation(MSG.reservationIndividual());
						break;
					case Group:
					case GroupOverride:
						e.setReservation(MSG.reservationGroup());
						break;
					case Course:
						e.setReservation(MSG.reservationCourse());
						break;
					case Curriculum:
					case CurriculumOverride:
						e.setReservation(MSG.reservationCurriculum());
						break;
					case LearningCommunity:
						e.setReservation(MSG.reservationLearningCommunity());
						break;
					}
				}
				if (request.getEnrollment().getTimeStamp() != null)
					e.setEnrolledDate(request.getEnrollment().getTimeStamp());
				if (request.getEnrollment().getApproval() != null) {
					e.setApprovedDate(request.getEnrollment().getApproval().getTimeStamp());
					e.setApprovedBy(request.getEnrollment().getApproval().getName());
				}
				
				for (XSection section: offering.getSections(request.getEnrollment())) {
					ClassAssignmentInterface.ClassAssignment a = e.getCourse().addClassAssignment();
					a.setAlternative(request.isAlternative());
					a.setClassId(section.getSectionId());
					a.setSubpart(section.getSubpartName());
					a.setSection(section.getName(course.getCourseId()));
					a.setExternalId(section.getExternalId(course.getCourseId()));
					a.setClassNumber(section.getName(-1l));
					a.setCancelled(section.isCancelled());
					a.setLimit(new int[] {enrollments.countEnrollmentsForSection(section.getSectionId()), section.getLimit()});
					if (section.getTime() != null) {
						for (DayCode d : DayCode.toDayCodes(section.getTime().getDays()))
							a.addDay(d.getIndex());
						a.setStart(section.getTime().getSlot());
						a.setLength(section.getTime().getLength());
						a.setBreakTime(section.getTime().getBreakTime());
						a.setDatePattern(section.getTime().getDatePatternName());
					}
					if (section.getNrRooms() > 0) {
						for (XRoom rm: section.getRooms()) {
							a.addRoom(rm.getUniqueId(), rm.getName());
						}
					}
					if (section.getInstructors() != null) {
						for (XInstructor instructor: section.getInstructors()) {
							a.addInstructor(instructor.getName());
							a.addInstructoEmail(instructor.getEmail());
						}
					}
					if (section.getParentId()!= null)
						a.setParentSection(offering.getSection(section.getParentId()).getName(course.getCourseId()));
					a.setSubpartId(section.getSubpartId());
					a.addNote(course.getNote());
					a.addNote(section.getNote());
					XSubpart subpart = offering.getSubpart(section.getSubpartId());
					a.setCredit(subpart.getCredit(course.getCourseId()));
					a.setCreditRange(subpart.getCreditMin(course.getCourseId()), subpart.getCreditMax(course.getCourseId()));
					Float creditOverride = section.getCreditOverride(c.getCourseId());
					if (creditOverride != null) a.setCredit(FixedCreditUnitConfig.formatCredit(creditOverride));
					int dist = 0;
					String from = null;
					TreeSet<String> overlap = new TreeSet<String>();
					for (XRequest q: student.getRequests()) {
						if (q instanceof XCourseRequest) {
							XEnrollment otherEnrollment = ((XCourseRequest)q).getEnrollment();
							if (otherEnrollment == null) continue;
							XOffering otherOffering = server.getOffering(otherEnrollment.getOfferingId());
							for (XSection otherSection: otherOffering.getSections(otherEnrollment)) {
								if (otherSection.equals(section) || otherSection.getTime() == null) continue;
								int d = otherSection.getDistanceInMinutes(section, m);
								if (d > dist) {
									dist = d;
									from = "";
									for (Iterator<XRoom> k = otherSection.getRooms().iterator(); k.hasNext();)
										from += k.next().getName() + (k.hasNext() ? ", " : "");
								}
								if (otherSection.isDistanceConflict(student, section, m))
									a.setDistanceConflict(true);
								if (section.getTime() != null && section.getTime().hasIntersection(otherSection.getTime()) && !section.isToIgnoreStudentConflictsWith(offering.getDistributions(), otherSection.getSectionId())) {
									XCourse otherCourse = otherOffering.getCourse(otherEnrollment.getCourseId());
									XSubpart otherSubpart = otherOffering.getSubpart(otherSection.getSubpartId());
									overlap.add(MSG.clazz(otherCourse.getSubjectArea(), otherCourse.getCourseNumber(), otherSubpart.getName(), otherSection.getName(otherCourse.getCourseId())));
								}
							}
						}
					}
					if (!overlap.isEmpty()) {
						String note = null;
						for (Iterator<String> j = overlap.iterator(); j.hasNext(); ) {
							String n = j.next();
							if (note == null)
								note = MSG.noteAllowedOverlapFirst(n);
							else if (j.hasNext())
								note += MSG.noteAllowedOverlapMiddle(n);
							else
								note += MSG.noteAllowedOverlapLast(n);
						}
						a.setOverlapNote(note);
					}
					a.setBackToBackDistance(dist);
					a.setBackToBackRooms(from);
					a.setSaved(true);
					if (a.getParentSection() == null)
						a.setParentSection(course.getConsentLabel());
					a.setExpected(overExp.getExpected(section.getLimit(), expectations.getExpectedSpace(section.getSectionId())));
				}
			}
			ret.add(e);
		}
		return ret;
	}

	@Override
	public String name() {
		return "find-enrollments";
	}

}
