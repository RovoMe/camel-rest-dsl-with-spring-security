package at.rovo.awsxray.domain;

import at.rovo.awsxray.domain.entities.mongo.UserEntity;
import at.rovo.awsxray.domain.views.AuthenticationUserViewEntity;
import at.rovo.awsxray.domain.views.CompanyUserViewEntity;
import at.rovo.awsxray.security.KeyGenerator;
import at.rovo.awsxray.utils.MongodbPersistenceUtil;
import com.mongodb.WriteResult;
import java.util.ArrayList;
import org.mongodb.morphia.query.Query;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseMongoService<UserEntity> {

    /** Number of characters in the default user key **/
    public static int USER_KEY_LENGTH = 42;

    public UserService() {
        super(UserEntity.class);
    }

    public UserEntity findUser(String userId, String password) {
        return findUser(userId, password, false);
    }

    public UserEntity findUser(final String userId, final String password,
                               final Boolean includeDisabled) {
        if ((password == null) || password.isEmpty()) {
            return null;
        }

        UserEntity user = findUser(userId, includeDisabled);
        if ((user != null) && passwordEncoder.matches(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public UserEntity findUser(final String userId) {
        return findUser(userId, false);
    }

    public UserEntity findUser(final String userId, final Boolean includeDisabled) {
        if ((userId == null) || userId.isEmpty()) {
            return null;
        }

        Query<UserEntity> query = mongoDataStore.find(UserEntity.class).field("userId")
                .equal(MongodbPersistenceUtil.sanitize(userId).toLowerCase());
        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }
        return query.get();
    }

    public AuthenticationUserViewEntity findUserView(final String userId) {
        return findUserView(userId, false);
    }

    public AuthenticationUserViewEntity findUserView(final String userId, final Boolean includeDisabled) {
        if ((userId == null) || userId.isEmpty()) {
            return null;
        }

        Query<AuthenticationUserViewEntity> query = mongoDataStore.find(AuthenticationUserViewEntity.class)
                .field("userId").equal(MongodbPersistenceUtil.sanitize(userId).toLowerCase());
        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }
        return query.get();
    }

    public CompanyUserViewEntity findCompanyUser(final String userId) {
        return findCompanyUser(userId, false);
    }

    public CompanyUserViewEntity findCompanyUser(final String userId, final Boolean includeDisabled) {
        if ((userId == null) || userId.isEmpty()) {
            return null;
        }

        Query<CompanyUserViewEntity> query = mongoDataStore.find(CompanyUserViewEntity.class)
                .field("userId").equal(MongodbPersistenceUtil.sanitize(userId).toLowerCase());
        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }
        return query.get();
    }

    public Iterable<UserEntity> findUsersByUserId(String containedString, int start, int limit) {
        return findUsersByUserId(containedString, start, limit, false);
    }

    public Iterable<UserEntity> findUsersByUserId(String containedString, int start,
                                                  int limit, boolean includeDisabled) {
        if ((containedString == null) || containedString.isEmpty()) {
            return new ArrayList<>();
        }

        Query<UserEntity> query = mongoDataStore.find(UserEntity.class).field("userId")
                .contains(MongodbPersistenceUtil.sanitize(containedString).toLowerCase()).offset(start);

        if (!includeDisabled) {
            query.limit(limit).field("disabled").notEqual(true);
        }

        return query.fetch();
    }

    public Iterable<UserEntity> findUsersByFirstName(String containedString, int start,
                                                     int limit) {
        return findUsersByFirstName(containedString, start, limit, false);
    }

    public Iterable<UserEntity> findUsersByFirstName(final String containedString, final int start,
                                                     final int limit, boolean includeDisabled) {

        if ((containedString == null) || containedString.isEmpty()) {
            return new ArrayList<>();
        }

        Query<UserEntity> query = mongoDataStore.find(UserEntity.class).field("firstName")
                .containsIgnoreCase(MongodbPersistenceUtil.sanitize(containedString)).offset(start)
                .limit(limit);

        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }

        return query.fetch();
    }


    public Iterable<UserEntity> findUsersByLastName(String containedString, int start,
                                                    int limit) {
        return findUsersByLastName(containedString, start, limit, false);
    }

    public Iterable<UserEntity> findUsersByLastName(final String containedString, final int start,
                                                    final int limit, boolean includeDisabled) {

        if ((containedString == null) || containedString.isEmpty()) {
            return new ArrayList<>();
        }

        Query<UserEntity> query = mongoDataStore.find(UserEntity.class).field("lastName")
                .containsIgnoreCase(MongodbPersistenceUtil.sanitize(containedString)).offset(start)
                .limit(limit);

        if (!includeDisabled) {
            query.field("disabled").notEqual(true);
        }

        return query.fetch();
    }

    public UserEntity createNewUser(String email, String password) {

        UserEntity newUser = new UserEntity(email, password, KeyGenerator.generateUserKey(email, USER_KEY_LENGTH));
        newUser.setDisabled(false);

        return newUser;
    }

    public int dropAll() {
        WriteResult result = mongoDataStore.delete(mongoDataStore.find(UserEntity.class));
        return result.getN();
    }
}
