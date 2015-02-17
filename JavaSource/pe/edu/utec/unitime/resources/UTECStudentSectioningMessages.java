package pe.edu.utec.unitime.resources;

import org.unitime.timetable.gwt.resources.StudentSectioningMessages;

public interface UTECStudentSectioningMessages extends StudentSectioningMessages {
	@DefaultMessage("You are already registered.")
	String failedEligibilityAlreadyRegistered();
	
	@DefaultMessage("Unrecognized response received.")
	String failedEligibilityBadResponse();

	@DefaultMessage("Unrecognized response received.")
	String failedEnrollmentBadResponse();
	
	@DefaultMessage("Bad domain. Please authenticated with your {0} email address.")
	String authenticationBadDomain(String domain);
}
