package at.rovo.awsxray.config.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "ssl")
public class SSLSettings {

    @NestedConfigurationProperty
    private KeyStoreSettings keyStore;
    @NestedConfigurationProperty
    private TrustStoreSettings trustStore;

}
