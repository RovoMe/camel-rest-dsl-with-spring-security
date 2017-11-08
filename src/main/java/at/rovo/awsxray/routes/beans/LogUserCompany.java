package at.rovo.awsxray.routes.beans;

import at.rovo.awsxray.domain.UserService;
import at.rovo.awsxray.domain.views.CompanyUserViewEntity;

import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.component.aws.xray.XRayTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test class which fetches the authenticated user from the headers and invokes a service which looks up the data
 * from a MongoDB View rather than a collection.
 */
@XRayTrace
public class LogUserCompany {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private UserService userService;

    @Handler
    public void logUserCompany(@Header("userId")String userId) {

        if (null == userId) {
            LOG.debug("No user found in the headers");
            return;
        }

        CompanyUserViewEntity companyUser = userService.findCompanyUser(userId);
        if (null == companyUser) {
            LOG.debug("Seems like user {} does not work for any of the registered companies", userId);
        } else {
            LOG.debug("User {} works for company with UUID {}", userId, companyUser.getCompanyUuid());
        }
    }
}
