package at.rovo.awsxray.utils;

import at.rovo.awsxray.domain.CompanyService;
import at.rovo.awsxray.domain.UserService;
import at.rovo.awsxray.domain.entities.CompanyEntity;
import at.rovo.awsxray.domain.entities.UserEntity;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class DatabasePopulator implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private UserService userService;
    @Resource
    private CompanyService companyService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userService, "UserService must not be null");

        int removed = userService.dropAll();
        int removedComps = companyService.dropAll();
        LOG.debug("Preparing database with new entries. Removed {} old users and {} old companies",
                  removed, removedComps);

        CompanyEntity comp1 = new CompanyEntity("Test", "1234567890123");
        companyService.persist(comp1);

        CompanyEntity comp2 = new CompanyEntity("Sample", "3210987654321");
        companyService.persist(comp2);

        UserEntity admin = new UserEntity("admin", "admin123", "ADMINKEY");
        admin.setCompany(comp1);
        admin.setRoles(Arrays.asList("ADMIN"));
        userService.persist(admin);

        UserEntity compUser = new UserEntity("sample", "sample", "SAMPLE");
        compUser.setRoles(Arrays.asList("USER"));
        compUser.setCompany(comp2);
        userService.persist(compUser);

        UserEntity user = new UserEntity("user", "user", "USERKEY");
        userService.persist(user);
    }
}
