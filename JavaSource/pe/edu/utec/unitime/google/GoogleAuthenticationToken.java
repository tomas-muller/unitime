package pe.edu.utec.unitime.google;

import java.util.Collection;

import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class GoogleAuthenticationToken extends AbstractAuthenticationToken {
    
    private static final long serialVersionUID = 8303047831754762526L;
    
    private final Credentials credentials;
    
    private UserDetails userDetails = null;
    
    private UserProfile userProfile = null;
    
    private final String clientName;
    
    public GoogleAuthenticationToken(final Credentials credentials, final String clientName) {
        super(null);
        this.credentials = credentials;
        this.clientName = clientName;
        setAuthenticated(false);
    }
    
    public GoogleAuthenticationToken(final Credentials credentials, final String clientName,
    		final UserDetails userDetails, final UserProfile userProfile, final Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.credentials = credentials;
        this.clientName = clientName;
        this.userDetails = userDetails;
        this.userProfile = userProfile;
        setAuthenticated(true);
    }
    
    public Object getCredentials() {
        return this.credentials;
    }
    
    public Object getPrincipal() {
        if (this.userDetails != null) {
            return this.userDetails;
        } else {
            return null;
        }
    }
    
    public UserProfile getUserProfile() {
        return this.userProfile;
    }
    
    public String getClientName() {
        return this.clientName;
    }
}