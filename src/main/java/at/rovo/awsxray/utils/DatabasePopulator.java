package at.rovo.awsxray.utils;

import at.rovo.awsxray.domain.AuditLogService;
import at.rovo.awsxray.domain.CompanyService;
import at.rovo.awsxray.domain.UserService;
import at.rovo.awsxray.domain.entities.jpa.AuditLogEntity;
import at.rovo.awsxray.domain.entities.jpa.LogRoleEntity;
import at.rovo.awsxray.domain.entities.jpa.LogUserEntity;
import at.rovo.awsxray.domain.entities.mongo.CompanyEntity;
import at.rovo.awsxray.domain.entities.mongo.UserEntity;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

//@Transactional(transactionManager = "dbTransactionManager")
public class DatabasePopulator implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private UserService userService;
    @Resource
    private CompanyService companyService;
    @Resource
    private AuditLogService auditLogService;

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

        // MySQL
        auditLogService.clearTable("AuditLogEntity");
        auditLogService.clearTable("LogUserEntity");
        auditLogService.clearTable("LogRoleEntity");

        LogRoleEntity adminRole = new LogRoleEntity();
        adminRole.setRoleName("Admin");

        LogRoleEntity userRole = new LogRoleEntity();
        userRole.setRoleName("User");

        LogRoleEntity customRole = new LogRoleEntity();
        customRole.setRoleName("Custom");

        LogUserEntity logUserAdmin = new LogUserEntity();
        logUserAdmin.setUserId(admin.getUserId());
        logUserAdmin.setName(admin.getFirstName() + " " + admin.getLastName());
        logUserAdmin.setRoles(Arrays.asList(adminRole));
        logUserAdmin = auditLogService.persist(logUserAdmin);

        LogUserEntity logUserCompUser = new LogUserEntity();
        logUserCompUser.setUserId(compUser.getUserId());
        logUserCompUser.setName(compUser.getFirstName() + " " + compUser.getLastName());
        logUserCompUser.setRoles(Arrays.asList(userRole));
        logUserCompUser = auditLogService.persist(logUserCompUser);

        AuditLogEntity adminLog = new AuditLogEntity();
        adminLog.setLog("TestLog");
        adminLog.setUser(logUserAdmin);
        auditLogService.persist(adminLog);
    }
}
