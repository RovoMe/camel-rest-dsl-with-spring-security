package at.rovo.awsxray.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import java.lang.invoke.MethodHandles;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class MonitorServicesAspect {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean traceMethodArguments;

    public MonitorServicesAspect() {
        this(true);
    }

    public MonitorServicesAspect(boolean traceMethodArguments) {
        this.traceMethodArguments = traceMethodArguments;
    }

    @Pointcut("execution(* at.rovo.awsxray.domain.UserService.*(..)) " +
              "|| execution(* at.rovo.awsxray.domain.CompanyService.*(..)) " +
              "|| execution(* at.rovo.awsxray.domain.FileService.*(..)) " +
              "|| execution(* at.rovo.awsxray.domain.AuditLogService.*(..)) "
    )
    public void invokedService() { }

    @Around("invokedService()")
    public Object monitorService(ProceedingJoinPoint jp) throws Throwable {
        String name = getMethodName(jp, traceMethodArguments);
        LOG.debug("Attempting to weave {}", name);

        try {
            AWSXRay.getCurrentSegment();
        } catch (Exception ex) {
            LOG.debug("No active segment found. Skip weaving of method {}", name);
            return jp.proceed();
        }
        try {
            AWSXRay.getCurrentSubsegment();
        } catch (Exception ex) {
            LOG.debug("No active subsegment to add results to found. Skip weaving of method {}", name);
            return jp.proceed();
        }

        Subsegment current = AWSXRay.beginSubsegment(name);
        try {
            LOG.debug("Advising joint-point {}", jp);
            return jp.proceed();
        } catch (Throwable t) {
            LOG.debug("Execution of joint-point failed due to {}", t.getLocalizedMessage());
            current.addException(t);
            throw t;
        } finally {
            LOG.debug("Finished recording of joint-point metrices on joint-point {}", jp);
            current.close();
        }
    }

    private String getMethodName(ProceedingJoinPoint jp, boolean includeParammeters) {
        MethodSignature signature = (MethodSignature)jp.getSignature();
        String name = signature.getDeclaringType().getSimpleName() + "::" + signature.getMethod().getName();
        if (includeParammeters) {
            if (null != signature.getParameterNames()) {
                name += " # ";
                StringBuilder params = new StringBuilder();
                for (String param : signature.getParameterNames()) {
                    if (params.length() > 0) {
                        params.append(" & ");
                    }
                    params.append(param);
                }
                name += params.toString();
            } else if (null != signature.getParameterTypes()) {
                name += " # ";
                StringBuilder params = new StringBuilder();
                for (Class<?> paramTypes : signature.getParameterTypes()) {
                    if (params.length() > 0) {
                        params.append(" & ");
                    }
                    params.append(paramTypes.getSimpleName());
                }
                name += params.toString();
            }
        }
        return name;
    }
}
