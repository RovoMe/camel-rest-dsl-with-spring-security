package at.rovo.awsxray.config;

import at.rovo.awsxray.security.AppUserDetailsService;
import at.rovo.awsxray.security.AuthFilterStrategy;
import at.rovo.awsxray.security.PasswordAuthenticationProvider;
import at.rovo.awsxray.security.SpringSecurityContextLoader;
import at.rovo.awsxray.security.UserKeyAuthenticationProvider;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.google.common.base.Joiner;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.Filter;
import org.apache.camel.component.spring.security.AuthenticationAdapter;
import org.apache.camel.component.spring.security.SpringSecurityAccessPolicy;
import org.apache.camel.component.spring.security.SpringSecurityAuthorizationPolicy;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleHierarchyVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

/**
 * Add support for spring-security based basic authentication on camel jetty endpoints.
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard().withPlugin(new EC2Plugin());

        AWSXRay.setGlobalRecorder(builder.build());
    }

    public SpringSecurityConfig() {
        super();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // does not work in combination with Camel Jetty component and Camel's REST DSL ...
        LOG.debug("Initializing AWS XRay servlet filter");
        http.addFilterBefore(new AWSXRayServletFilter("services"), WebAsyncManagerIntegrationFilter.class)

                .authorizeRequests()
                    .anyRequest().hasAnyRole("ROLE_USER", "ROLE_ADMIN")
                .and()
                    .httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(userKeyAuthProvider())
                .userDetailsService(appUserDetailsService());
    }

    @Bean(name = "userKeyAuthProvider")
    public AuthenticationProvider userKeyAuthProvider() {
        return new UserKeyAuthenticationProvider();
    }

    @Bean(name = "passwordAuthProvider")
    public AuthenticationProvider passwordAuthProvider() {
        return new PasswordAuthenticationProvider();
    }

    @Bean
    public UserDetailsService appUserDetailsService() {
        return new AppUserDetailsService();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        List<AccessDecisionVoter<?>> decisionVoters = new ArrayList<>();
        decisionVoters.add(new RoleVoter());
        return new AffirmativeBased(decisionVoters);
    }

    @Bean
    public SpringSecurityContextLoader contextLoader() {
        return new SpringSecurityContextLoader();
    }

    @Bean(name = AuthFilterStrategy.ID)
    public HeaderFilterStrategy authHeaderFilter() {
        return new AuthFilterStrategy();
    }

    /**
     * Specifies a hierarchical role order where <em>SUPERADMIN</em> is also an <em>ADMIN</em> as well
     * as a <em>USER</em>, while an <em>ADMIN</em> is only an <em>ADMIN</em> as well as a <em>USER</em>
     * and last but not least a <em>USER</em> is only a <em>USER</em>
     *
     * @return An object containing the specified hierarchical role order
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();

        // A note on the roles: These roles correspond to the ShiroRoleEntity.Role enum, prefixed with 'ROLE_'
        // for spring-security compatibility

        // ROLE_SUPER_ADMIN: can login into admin and has all permissions. Can create new users of any type.
        // ROLE_READ_ONLY_ADMIN:  can login into admin but can only read (with some exceptions to mappings) and not see
        //                        any billing pages. Cannot create new users.
        // ROLE_ADMIN: cannot login into admin but into webedi / portal and create new users with role ROLE_USER
        // ROLE_USER: cannot login into admin but into webedi. Cannot create new users

        String hierarchy = Joiner.on(' ').join(
                "ROLE_SUPER ADMIN      > ROLE_READ_ONLY_ADMIN",
                "ROLE_READ_ONLY_ADMIN  > ROLE_BILLING_ADMIN",
                "ROLE_BILLING_ADMIN    > ROLE_ADMIN",
                "ROLE_ADMIN            > ROLE_USER",
                "ROLE_USER             > ROLE_UNAUTHENTICATED");

        // see http://docs.spring.io/spring-security/site/docs/3.1.5.RELEASE/reference/authz-arch.html
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    @Bean
    public RoleHierarchyVoter roleHierarchyVoter() {
        return new RoleHierarchyVoter(roleHierarchy());
    }

//    private AuthenticationAdapter authenticationAdapter() {
//        return new AuthenticationAdapter() {
//            public Authentication toAuthentication(Subject subject) {
//                if (subject == null || subject.getPrincipals().size() == 0) {
//                    return null;
//                }
//                Set<Authentication> authentications  = subject.getPrincipals(Authentication.class);
//                if (authentications.size() > 0) {
//                    // just return the first one
//                    return authentications.iterator().next();
//                } else {
//                    return convertToAuthentication(subject);
//                }
//            }
//
//            /**
//             * You can add the customer convert code here
//             */
//            protected Authentication convertToAuthentication(Subject subject) {
//                return new UsernamePasswordAuthenticationToken(subject.getPrincipals(), subject.getPublicCredentials());
//            }
//        };
//    }

    @Bean(name = "authenticated")
    public SpringSecurityAuthorizationPolicy authenticated_access() throws Exception {
        SpringSecurityAuthorizationPolicy policy = new SpringSecurityAuthorizationPolicy();
        policy.setId("authenticated");
        policy.setAuthenticationManager(authenticationManagerBean());
        policy.setAccessDecisionManager(accessDecisionManager());
        policy.setSpringSecurityAccessPolicy(new SpringSecurityAccessPolicy("ROLE_USER,ROLE_ADMIN"));
//        policy.setAuthenticationAdapter(authenticationAdapter());
        return policy;
    }

    @Bean(name = "admin_access")
    public SpringSecurityAuthorizationPolicy admin_access() throws Exception {
        SpringSecurityAuthorizationPolicy policy = new SpringSecurityAuthorizationPolicy();
        policy.setId("admin_access");
        policy.setAuthenticationManager(authenticationManagerBean());
        policy.setAccessDecisionManager(accessDecisionManager());
        policy.setSpringSecurityAccessPolicy(new SpringSecurityAccessPolicy("ROLE_ADMIN"));
        return policy;
    }

      @Bean
      public Filter TracingFilter() {
        return new AWSXRayServletFilter("test");
      }
}
