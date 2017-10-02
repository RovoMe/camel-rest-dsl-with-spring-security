package at.rovo.awsxray.domain;

import at.rovo.awsxray.domain.entities.jpa.AuditLogEntity;
import at.rovo.awsxray.domain.entities.jpa.BaseJpaEntity;
import at.rovo.awsxray.domain.entities.jpa.LogUserEntity;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = "dbTransactionManager")
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @PersistenceContext(unitName = "pun")
    protected EntityManager entityManager;

    public LogUserEntity findUserByUserId(String userId) {
        TypedQuery<LogUserEntity> q = entityManager.createNamedQuery("findUserViaUserId", LogUserEntity.class);
        q.setParameter("userId", userId);
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (Exception e) {
            LOG.error("Not matching user was found.");
            return null;
        }
    }

    public List<AuditLogEntity> findUserLogs(String userId) {
        LogUserEntity user = findUserByUserId(userId);
        if (null != user) {
            TypedQuery<AuditLogEntity> q = entityManager.createNamedQuery("findLogsViaUser", AuditLogEntity.class);
            q.setParameter("user", user);
            return q.getResultList();
        }
        return Collections.emptyList();
    }

    public <E extends BaseJpaEntity> E persist(E entity) {
        return entityManager.merge(entity);
    }

    public <E extends BaseJpaEntity> E find(Long id, Class<E> entityClazz) {
        return entityManager.find(entityClazz, id);
    }

    public void clearTable(String tableName) {
        Query q = entityManager.createQuery("DELETE FROM " + tableName);
        int removed = q.executeUpdate();
        LOG.info("Removed {} entris from {}", removed, tableName);
    }
}
