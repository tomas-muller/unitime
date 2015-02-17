package pe.edu.utec.unitime.google;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

public class GoogleAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationFilter.class);
    
    private Clients clients;
    
    public GoogleAuthenticationFilter(final String suffixUrl) {
        super(suffixUrl);
    }
    
    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        CommonHelper.assertNotNull("clients", this.clients);
        this.clients.init();
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        
        // context
        final WebContext context = new J2EContext(request, response);
        
        // get the right client
        final Client client = this.clients.findClient(context);
        
        // get credentials
        Credentials credentials;
        try {
            credentials = client.getCredentials(context);
        } catch (final RequiresHttpAction e) {
            logger.info("Requires additionnal HTTP action", e);
            return null;
        }
        logger.debug("credentials : {}", credentials);
        // if credentials/profile is null, return to the saved request url
        if (credentials == null) {
            getSuccessHandler().onAuthenticationSuccess(request, response, null);
            return null;
        }
        // and create token from credential
        final GoogleAuthenticationToken token = new GoogleAuthenticationToken(credentials, client.getName());
        // set details
        token.setDetails(this.authenticationDetailsSource.buildDetails(request));
        logger.debug("token : {}", token);
        
        // authenticate
        final Authentication authentication = getAuthenticationManager().authenticate(token);
        logger.debug("authentication : {}", authentication);
        return authentication;
    }
    
    public Clients getClients() {
        return this.clients;
    }
    
    public void setClients(final Clients clients) {
        this.clients = clients;
    }
}