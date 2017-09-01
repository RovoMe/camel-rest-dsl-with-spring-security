package at.rovo.awsxray.domain.views;

import at.rovo.awsxray.security.AESEncryptor;
import at.rovo.awsxray.utils.EncryptedString;
import java.util.List;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "authUserView", noClassnameStored = true)
public class AuthenticationUserViewEntity extends BaseViewEntity {

    protected String userId;
    protected String passwordHash;
    @Transient
    protected String userKey;
    @Embedded
    protected EncryptedString userKeyEncrypted;
    protected List<String> roles;

    public AuthenticationUserViewEntity() {
        super();
    }

    public String getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getUserKey() {
        if ((userKey == null) && (userKeyEncrypted != null)) {
            userKey = AESEncryptor.decrypt(userKeyEncrypted.getEncryptedAttribute(), userKeyEncrypted.getSalt());
        }
        return userKey;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "AuthenticationUserViewEntity [uuid=" + uuid + ", userId=" + userId + ", userKey=" + userKey + ", role=" + roles + "]";
    }
}
