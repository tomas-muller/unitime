package pe.edu.utec.unitime.onlinesectioning;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.onlinesectioning.custom.CourseMatcherProvider;
import org.unitime.timetable.onlinesectioning.match.AbstractCourseMatcher;
import org.unitime.timetable.onlinesectioning.match.CourseMatcher;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.security.SessionContext;

/**
 * REQ.01: A student can only select a course that is in his/her last-like course demands 
 */
public class LastLikeDemandsCourseMatcherProvider implements CourseMatcherProvider {

	@Override
	public CourseMatcher getCourseMatcher(SessionContext context, Long studentId) {
		org.hibernate.Session hibSession = StudentDAO.getInstance().createNewSession();
		try {
			Set<Long> courseIds = new HashSet<Long>((List<Long>)hibSession.createQuery(
					"select co.uniqueId from CourseOffering co, LastLikeCourseDemand d, Student s where " +
					"d.subjectArea.session = s.session and d.student.externalUniqueId = s.externalUniqueId and s.uniqueId = :studentId " +
					"and co.subjectArea = d.subjectArea and co.courseNbr = d.courseNbr")
					.setLong("studentId", studentId)
					.setCacheable(true).list());
			return new LastLikeDemandsCourseMatcher(courseIds);
		} finally {
			hibSession.close();
		}
	}
	
	public static class LastLikeDemandsCourseMatcher extends AbstractCourseMatcher {
		private static final long serialVersionUID = 1L;
		private Set<Long> iCourseIds;
		
		LastLikeDemandsCourseMatcher(Set<Long> courseIds) {
			iCourseIds = courseIds;
		}
		
		@Override
		public boolean match(XCourseId course) {
			return iCourseIds.contains(course.getCourseId());
		}
		
	}

}
