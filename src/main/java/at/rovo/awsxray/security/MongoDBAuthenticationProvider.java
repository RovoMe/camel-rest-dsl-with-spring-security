package at.rovo.awsxray.security;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Authentication provider which looks up user details from an injected user details service and performs some sanity
 * checks.
 */
@Service
public abstract class MongoDBAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private UserDetailsService lookupService;
    @Resource
    private Environment env;

    @PostConstruct
    public void initialize() {
        Assert.notNull(env, "Spring environment must be set");
        Assert.notNull(env.getActiveProfiles(), "An active spring profile must be set");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "Authentication cannot be null");
        Assert.isInstanceOf(String.class, authentication.getCredentials(), "Credentials must be of type string");

        String userId = authentication.getName();
        String password = (String) authentication.getCredentials();

        UserDetails details = lookupService.loadUserByUsername(userId);
        if (!(details instanceof AppUser)) {
            throw new BadCredentialsException("Invalid user credentials found");
        }

        if (authenticate(userId, password, (AppUser)details)) {
            Collection<? extends GrantedAuthority> grantedAuths = details.getAuthorities();
            LOG.debug("Authentication of user {} succeeded. Assigned role: {}", userId, grantedAuths);

            if (authentication.getDetails() instanceof WebAuthenticationDetails) {
                WebAuthenticationDetails webAuthenticationDetails = (WebAuthenticationDetails) authentication.getDetails();
                ((AppUser)details).setRemoteAddress(webAuthenticationDetails.getRemoteAddress());
            }

            return new UsernamePasswordAuthenticationToken(userId, password, grantedAuths);
        } else {
            throw new BadCredentialsException("Invalid user credentials found");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    protected abstract boolean authenticate(String userId, String password, AppUser user) throws AuthenticationException;
}
