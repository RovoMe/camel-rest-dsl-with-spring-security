package at.rovo.awsxray.security;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

public class UserKeyAuthenticationProvider extends MongoDBAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected boolean authenticate(String userId, String password, AppUser user) throws AuthenticationException {
        if (null == user) {
            LOG.info("No user object passed to the authenticate method");
            return false;
        }
        if (null == user.getUserKey()) {
            LOG.info("No user key present in user {}", user);
            return false;
        }

        return user.getUsername().equals(userId) && user.getUserKey().equals(password);
    }
}
