package at.rovo.awsxray.config.settings;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "sql")
@Validated
public class MySQLSettings {
    private String user;
    private String password;
    @NotNull
    private String database;
    @NotNull
    private String host;
    @NotNull
    private Integer port;
}
