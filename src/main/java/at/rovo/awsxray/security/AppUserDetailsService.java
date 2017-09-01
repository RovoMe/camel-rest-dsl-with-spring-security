package at.rovo.awsxray.security;

import at.rovo.awsxray.domain.UserService;
import at.rovo.awsxray.domain.views.AuthenticationUserViewEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Fake user lookup service. This should usually read user data from a backing datastore such as MongoDB, SQL or similar
 * and return the loaded data as a {@link User} implementation.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    @Resource
    private UserService userService;

    protected AppUser createAppUser(AuthenticationUserViewEntity user) {

        List<String> roles = user.getRoles();

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        if (null != roles && roles.size() > 0) {
            for (String role : roles) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        } else {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_UNAUTHENTICATED"));
        }

        return new AppUser(user.getUserId(), user.getPasswordHash(), user.getUserKey(), grantedAuthorities);
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        AuthenticationUserViewEntity user = userService.findUserView(userId);

        if (user == null) {
            throw new UsernameNotFoundException("No matching user with given credentials found");
        }

        return createAppUser(user);
    }
}
