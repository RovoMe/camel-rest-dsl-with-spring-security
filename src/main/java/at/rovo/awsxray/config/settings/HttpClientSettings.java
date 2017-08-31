package at.rovo.awsxray.config.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "httpClient")
public class HttpClientSettings {

    /**
     * The time span the application will wait for a connection to get established. If the connection
     * is not established within the given amount of time a ConnectionTimeoutException will be raised.
     * <p>
     * The value has to be applied in seconds!
     **/
    @NotNull
    private int conTimeout;

    /**
     * Monitors the time passed between two consecutive incoming messages over the connection and
     * raises a SocketTimeoutException if no message was received within the given timeout interval.
     * <p>
     * The value has to be applied in seconds!
     **/
    @NotNull
    private int soTimeout;
}
