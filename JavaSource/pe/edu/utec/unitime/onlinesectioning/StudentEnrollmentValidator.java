package pe.edu.utec.unitime.onlinesectioning;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck.EligibilityFlag;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.custom.StudentEnrollmentProvider;
import org.unitime.timetable.onlinesectioning.model.XStudent;

public class StudentEnrollmentValidator implements StudentEnrollmentProvider {
	
	protected Connection obtainConnection() throws SQLException {
		SessionImplementor session = (SessionImplementor)new _RootDAO().getSession();
        return session.getJdbcConnectionAccess().obtainConnection();
	}
	
	protected void releaseConnection(Connection connection) throws SQLException {
		SessionImplementor session = (SessionImplementor)new _RootDAO().getSession();
		session.getJdbcConnectionAccess().releaseConnection(connection);
	}
	
	@Override
	public void checkEligibility(OnlineSectioningServer server, OnlineSectioningHelper helper, EligibilityCheck check, XStudent student) throws SectioningException {
		// Cannot enroll -> no additional check is needed
		if (!check.hasFlag(EligibilityFlag.CAN_ENROLL)) return;

		try {
			// REQ.03 call stored procedure, disable enrollment when no success
			String p1 = server.getAcademicSession().getTerm();
			String p2 = student.getExternalId();
			String procedure = ApplicationProperties.getProperty("utec.validation.eligibility", "call sp_validar_acceso(?, ?, ?)");
			String ret = null;
			
	        Connection connection = obtainConnection();
			try {
				CallableStatement statement = connection.prepareCall(procedure);
				try {
					statement.setString(1, p1);
					statement.setString(2, p2);
					statement.registerOutParameter(3, java.sql.Types.VARCHAR);
					statement.execute();
					ret = statement.getString(3);
				} finally {
					statement.close();
				}
			} finally {
				releaseConnection(connection);
			}
			
			helper.getAction().addOptionBuilder().setKey("response").setValue(ret);
			if ("1".equals(ret) || ret.startsWith("1||")) {
				// Eligibility check succeeded: no action is needed
			} else if (ret.startsWith("0||")) {
				// Eligibility check failed: disable enrollment, return the given message
				check.setMessage(ret.substring(3));
				check.setFlag(EligibilityFlag.CAN_ENROLL, false);
			} else {
				check.setFlag(EligibilityFlag.CAN_ENROLL, false);
				throw new SectioningException("Unrecognized response received.");
			}
		} catch (SectioningException e) {
			helper.info("Eligibility check failed: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			helper.warn("Eligibility check failed: " + e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		}
	}

	@Override
	public List<EnrollmentFailure> enroll(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student, List<EnrollmentRequest> enrollments, Set<Long> lockedCourses) throws SectioningException {
		try {
			// REQ.02 call stored procedure, throw an exception when failed
			String p1 = server.getAcademicSession().getTerm();
			String p2 = student.getExternalId();
			String p3 = "";
			Set<Long> courseIds = new HashSet<Long>();
			for (EnrollmentRequest req: enrollments) {
				if (courseIds.add(req.getCourse().getCourseId()))
					p3 += (p3.isEmpty() ? "" : "||") + req.getCourse().getCourseName();
			}
			helper.getAction().addOptionBuilder().setKey("request").setValue(p3);
			String procedure = ApplicationProperties.getProperty("utec.validation.enrollment", "call sp_validar_reglas_matricula(?, ?, ?, ?)");
			String ret = null;
			
	        Connection connection = obtainConnection();
			try {
				CallableStatement statement = connection.prepareCall(procedure);
				try {
					statement.setString(1, p1);
					statement.setString(2, p2);
					statement.setString(3, p3);
					statement.registerOutParameter(4, java.sql.Types.VARCHAR);
					statement.execute();
					ret = statement.getString(4);
				} finally {
					statement.close();
				}
			} finally {
				releaseConnection(connection);
			}
			
			helper.getAction().addOptionBuilder().setKey("response").setValue(ret);
			if ("1".equals(ret) || ret.startsWith("1||")) {
				// All OK, return no failures
				return new ArrayList<EnrollmentFailure>();
			} else if (ret.startsWith("0||")) {
				// Validation failed, throw the given error
				throw new SectioningException(ret.substring(3));
			} else {
				throw new SectioningException("Unrecognized response received.");
			}
		} catch (SectioningException e) {
			helper.info("Enrollment validation failed: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			helper.warn("Enrollment validation failed: " + e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		}
	}

	@Override
	public boolean requestUpdate(OnlineSectioningServer server, OnlineSectioningHelper helper, Collection<XStudent> students) throws SectioningException {
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isAllowWaitListing() {
		return false;
	}

	@Override
	public boolean isCanRequestUpdates() {
		return false;
	}

}
