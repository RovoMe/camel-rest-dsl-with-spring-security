package at.rovo.awsxray.domain.entities.jpa;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="user")
@NamedQueries({
        @NamedQuery(name = "findUserViaUserId", query = "SELECT user FROM LogUserEntity user WHERE user.userId = :userId")
})
public class LogUserEntity extends BaseJpaEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    @Column(name = "user_name")
    private String name;
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "user_roles",
            joinColumns= { @JoinColumn(name = "userId", nullable = false, updatable = false)},
            inverseJoinColumns = { @JoinColumn(name = "roleId", nullable = false, updatable = false)}
    )
    private List<LogRoleEntity> roles =  new ArrayList<>(0);
    @OneToMany(mappedBy="user")
    private List<AuditLogEntity> auditLogs = new ArrayList<>(0);

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LogRoleEntity> getRoles() {
        return this.roles;
    }

    public void setRoles(List<LogRoleEntity> roles) {
        this.roles = roles;
    }

    public List<AuditLogEntity> getAuditLogs() {
        return this.auditLogs;
    }

    public void setAuditLog(List<AuditLogEntity> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
