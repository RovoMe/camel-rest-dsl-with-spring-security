package at.rovo.awsxray.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Custom user object managed by Spring Security.
 */
public class AppUser extends User {

    private String userKey;
    private String remoteAddress;

    public AppUser(String username, String password, String userKey, Collection<? extends GrantedAuthority> authorities)
    {
        super(username, password, authorities);
        this.userKey = userKey;
    }

    public AppUser(String username, String password, String userKey, boolean enabled, boolean accountNonExpired,
                   boolean credentialsNonExpired, boolean accountNonLocked,
                   Collection<? extends GrantedAuthority> authorities)
    {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userKey = userKey;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    public String getUserKey() {
        return this.userKey;
    }
}
