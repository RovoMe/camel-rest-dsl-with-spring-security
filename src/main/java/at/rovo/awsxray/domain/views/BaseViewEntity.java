package at.rovo.awsxray.domain.views;

import at.rovo.awsxray.domain.entities.mongo.DomainObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

/**
 * Marker interface for MongoDB views created for certain collections.
 * <p/>
 * MongoDB collection views are read-only collections and thus do not support any updates done to it hence we use this
 * interface to check whether an entity is a view or a regular collection entity.
 */
public class BaseViewEntity implements DomainObject {

    @Id
    protected ObjectId id;
    protected String uuid;

    protected boolean disabled = false;

    @Override
    public ObjectId getId() {
        return this.id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public Boolean getDisabled() {
        return disabled;
    }
}
