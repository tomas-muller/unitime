package pe.edu.utec.unitime.google;

import java.util.ArrayList;
import java.util.Collection;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

public class GoogleAuthenticationProvider implements AuthenticationProvider, InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationProvider.class);
    
    private Clients clients;
    
    private AuthenticationUserDetailsService<GoogleAuthenticationToken> userDetailsService;
    
    private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        logger.debug("authentication : {}", authentication);
        if (!supports(authentication.getClass())) {
            logger.debug("unsupported authentication class : {}", authentication.getClass());
            return null;
        }
        final GoogleAuthenticationToken token = (GoogleAuthenticationToken) authentication;
        
        // get the credentials
        final Credentials credentials = (Credentials) authentication.getCredentials();
        logger.debug("credentials : {}", credentials);
        
        // get the right client
        final String clientName = token.getClientName();
        final Client client = this.clients.findClient(clientName);
        // get the user profile
        final UserProfile userProfile = client.getUserProfile(credentials, null);
        logger.debug("userProfile : {}", userProfile);
        
        // by default, no authorities
        Collection<? extends GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        // get user details and check them
        final GoogleAuthenticationToken tmpToken = new GoogleAuthenticationToken(credentials, clientName, null, userProfile, null);
        
        final UserDetails userDetails = this.userDetailsService.loadUserDetails(tmpToken);
        logger.debug("userDetails : {}", userDetails);
        if (userDetails != null) {
        	this.userDetailsChecker.check(userDetails);
        	authorities = userDetails.getAuthorities();
        	logger.debug("authorities : {}", authorities);
        }
        
        // new token with credentials (like previously) and user profile and authorities
        final GoogleAuthenticationToken result = new GoogleAuthenticationToken(credentials, clientName, userDetails, userProfile, authorities);
        result.setDetails(authentication.getDetails());
        logger.debug("result : {}", result);
        return result;
    }
    
    public boolean supports(final Class<?> authentication) {
        return (GoogleAuthenticationToken.class.isAssignableFrom(authentication));
    }
    
    public void afterPropertiesSet() {
        CommonHelper.assertNotNull("clients", this.clients);
        CommonHelper.assertNotNull("userDetailsService", this.userDetailsService);
        this.clients.init();
        
    }
    
    public Clients getClients() {
        return this.clients;
    }
    
    public void setClients(final Clients clients) {
        this.clients = clients;
    }
    
    public AuthenticationUserDetailsService<GoogleAuthenticationToken> getUserDetailsService() {
        return this.userDetailsService;
    }
    
    public void setUserDetailsService(final AuthenticationUserDetailsService<GoogleAuthenticationToken> userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    public UserDetailsChecker getUserDetailsChecker() {
        return this.userDetailsChecker;
    }
    
    public void setUserDetailsChecker(final UserDetailsChecker userDetailsChecker) {
        this.userDetailsChecker = userDetailsChecker;
    }
}
