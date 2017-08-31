package at.rovo.awsxray.config.settings;

import com.google.common.base.Joiner;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "mongo")
@Validated
public class MongoSettings {

    private String user;
    private String password;
    @NotNull
    private String database;
    @NotNull
    private Integer port;

    private String host;
    private String[] replica;

    public String getReplicaSetAsString() {
        return Joiner.on(',').skipNulls().join(Arrays.asList(replica));
    }

    @Override
    public String toString() {
        if (host != null) {
            return String.format("host: %s, port: %s, db: %s, user: %s", host, port, database, user);
        } else {
            return String.format("replica: %s, port: %s, db: %s, user: %s", getReplicaSetAsString(), port, database, user);
        }
    }
}
