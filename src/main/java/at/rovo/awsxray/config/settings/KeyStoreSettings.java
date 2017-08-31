package at.rovo.awsxray.config.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "keyStore")
public class KeyStoreSettings {

    private String resource;
    private String password;
    private String certPassword;
}
