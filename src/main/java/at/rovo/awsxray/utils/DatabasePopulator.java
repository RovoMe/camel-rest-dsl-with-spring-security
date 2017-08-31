package at.rovo.awsxray.utils;

import at.rovo.awsxray.domain.UserService;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userService, "UserService must not be null");

        int removed = userService.dropAll();
        LOG.debug("Preparing database with new entries. Removed {} old entries", removed);

        UserEntity admin = new UserEntity("admin", "admin123", "ADMINKEY");
        admin.setRoles(Arrays.asList("ADMIN"));

        userService.persist(admin);

        UserEntity user1 = new UserEntity("sample", "sample", "SAMPLE");
        user1.setRoles(Arrays.asList("USER"));

        userService.persist(user1);
    }
}
