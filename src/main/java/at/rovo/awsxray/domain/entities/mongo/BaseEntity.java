package at.rovo.awsxray.domain.entities.mongo;

import java.util.Date;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Version;

@Indexes(@Index(fields = {@Field("uuid")}, options = @IndexOptions(unique = true)))
public class BaseEntity implements DomainObject {

    @Id
    protected ObjectId id;

    protected String uuid;

    protected Date creationDate;
    protected Date lastChange;

    @Version
    protected long version;

    protected boolean disabled = false;

    @Override
    public ObjectId getId() {
        return this.id;
    }

    protected void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getCreationDate() {
        if (creationDate == null) {
            return null;
        }
        return (Date) creationDate.clone();
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = (Date) creationDate.clone();
    }

    public Date getLastChange() {
        if (lastChange == null) {
            return null;
        }
        return (Date) lastChange.clone();
    }

    protected void setLastChange(final Date lastChange) {
        this.lastChange = lastChange;
    }

    public long getVersion() {
        return version;
    }

    protected void setVersion(final long version) {
        this.version = version;
    }

    public void setDisabled(final Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    @PrePersist
    public void prePersist() {
        creationDate = (creationDate == null) ? new Date() : creationDate;
        lastChange = (lastChange == null) ? creationDate : new Date();
        uuid = (uuid == null) ? UUID.randomUUID().toString() : uuid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BaseEntity other = (BaseEntity) obj;
        if (uuid == null) {
            return false;
        } else if (uuid.equals(other.uuid)) {
            return true;
        }
        return false;
    }
}
