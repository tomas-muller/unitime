package pe.edu.utec.unitime.google;

import org.pac4j.oauth.profile.google2.Google2Profile;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.security.context.UniTimeUserContext;

import pe.edu.utec.unitime.resources.UTECStudentSectioningMessages;

public class GoogleAuthenticationUserDetailsService implements AuthenticationUserDetailsService<GoogleAuthenticationToken> {
	protected static UTECStudentSectioningMessages MESSAGES = Localization.create(UTECStudentSectioningMessages.class); 
	
	@Override
	public UserDetails loadUserDetails(GoogleAuthenticationToken token) throws UsernameNotFoundException {
		Google2Profile profile = (Google2Profile)token.getUserProfile();
		
		String domain = ApplicationProperties.getProperty("utec.authentication.google.domain");
		if (domain != null && !domain.isEmpty()) {
			if (!profile.getEmail().endsWith("@" + domain))
				throw new UsernameNotFoundException(MESSAGES.authenticationBadDomain(domain));
		}
		
		String idAttribute = ApplicationProperties.getProperty("utec.authentication.google.id_attribute");
		String id = null;
		if (idAttribute != null && !idAttribute.isEmpty()) {
			Object idObj = profile.getAttribute(idAttribute);
			if (idObj != null) id = idObj.toString();
		}
		
		if (id == null || id.isEmpty()) {
			org.hibernate.Session hibSession = new _RootDAO<>().createNewSession();
			try {
				id = (String)hibSession.createQuery(
						"select externalUniqueId from TimetableManager where emailAddress = :email")
						.setString("email", profile.getEmail()).setMaxResults(1).uniqueResult();
				
				if (id == null)
					id = (String) hibSession.createQuery(
							"select externalUniqueId from DepartmentalInstructor where email = :email")
							.setString("email", profile.getEmail()).setMaxResults(1).uniqueResult();
				
				if (id == null)
					id = (String) hibSession.createQuery(
							"select externalUniqueId from Student where email = :email")
							.setString("email", profile.getEmail()).setMaxResults(1).uniqueResult();
			} finally {
				hibSession.close();
			}
		}
		
		if (id != null)
			return new UniTimeUserContext(id, profile.getEmail(), profile.getDisplayName(), null);
		else
			return new UniTimeUserContext("", profile.getEmail(), profile.getDisplayName(), null);
	}

}