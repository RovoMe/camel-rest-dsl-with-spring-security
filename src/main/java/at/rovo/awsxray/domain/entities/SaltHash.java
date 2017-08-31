package at.rovo.awsxray.domain.entities;

public interface SaltHash {

    String getPassword();

    void setPassword(final String password);

    String getPasswordHash();

    void setPasswordHash(final String hash);
}
