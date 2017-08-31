package at.rovo.awsxray.domain;

import at.rovo.awsxray.domain.entities.BaseEntity;
import at.rovo.awsxray.domain.entities.SaltHash;
import at.rovo.awsxray.exceptions.PersistenceRuntimeException;
import at.rovo.awsxray.utils.MongodbPersistenceUtil;
import javax.annotation.Resource;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;

import static at.rovo.awsxray.config.MongoSpringConfig.BCRYPT_ENCODER;

public class BaseMongoService<E extends BaseEntity> {

    @Resource
    protected Datastore mongoDataStore;
    @Resource(name = BCRYPT_ENCODER)
    protected PasswordEncoder passwordEncoder;

    protected final Class<E> clazz;

    protected BaseMongoService(Class<E> clazz) {
        this.clazz = clazz;
    }

    public ObjectId persist(E entity) throws PersistenceRuntimeException {

        if (entity instanceof SaltHash) {
            SaltHash credentialEntity = (SaltHash)entity;
            credentialEntity.setPasswordHash(passwordEncoder.encode(credentialEntity.getPassword()));
            credentialEntity.setPassword(null);
        }

        mongoDataStore.save(entity);
        return entity.getId();
    }

    public E get(String uuid) {
        return get(uuid, false);
    }

    public E get(String uuid, boolean includeDisabled) {
        return get(uuid, includeDisabled, true);
    }

    public E get(String uuid, boolean includeDisabled, boolean includeFields, String ... fields) {
        if ((uuid == null) || uuid.isEmpty()) {
            return null;
        }

        Query<E> query = mongoDataStore.find(clazz).field("uuid")
                .equal(MongodbPersistenceUtil.sanitize(uuid));

        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }
        if((fields != null) && (fields.length > 0 )){
            for (String field : fields) {
                query.project(field, includeFields);
            }
        }

        return query.get();
    }

    public ObjectId getId(final String uuid) {
        return getId(uuid, false);
    }

    public ObjectId getId(final String uuid, final boolean includeDisabled) {
        if ((uuid == null) || uuid.isEmpty()) {
            return null;
        }

        Query<E> query = mongoDataStore.find(clazz).field("uuid")
                .equal(MongodbPersistenceUtil.sanitize(uuid));
        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }


        query.project("_id", true);
        E entity = query.get();

        if (entity == null) {
            return null;
        } else {
            return entity.getId();
        }
    }

    public E get(final ObjectId id) {
        return get(id, false, true);
    }

    public E get(final ObjectId id, final boolean includeDisabled) {
        return get(id, includeDisabled, true);
    }

    public E get(final ObjectId id, final boolean includeDisabled, final boolean includeFields, final String... fields) {
        if ((id == null)) {
            return null;
        }

        Query<E> query = mongoDataStore.find(clazz).field("id").equal(id);
        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }

        if((fields != null) && (fields.length > 0 )){
            for (String field : fields) {
                query.project(field, includeFields);
            }
        }

        return query.get();
    }

    public E getLatest() {
        Query<E> query = mongoDataStore.find(clazz).field("disabled").notEqual(true).order("-lastChange");

        return query.get();
    }
}
