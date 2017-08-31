package at.rovo.awsxray.security;

import com.google.common.base.Strings;
import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordAuthenticationProvider extends MongoDBAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    protected boolean authenticate(String userId, String password, AppUser user) throws AuthenticationException
    {
        LOG.debug("Checking userId and password for user {}", userId);
        boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

        return Strings.nullToEmpty(user.getUsername()).equals(userId) && isPasswordValid;
    }
}
