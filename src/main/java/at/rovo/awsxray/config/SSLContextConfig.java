package at.rovo.awsxray.config;

import at.rovo.awsxray.config.settings.HttpServerSettings;
import at.rovo.awsxray.config.settings.KeyStoreSettings;
import at.rovo.awsxray.config.settings.SSLSettings;
import at.rovo.awsxray.config.settings.TrustStoreSettings;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.apache.camel.util.jsse.FilterParameters;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SecureSocketProtocolsParameters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({HttpServerSettings.class, SSLSettings.class, KeyStoreSettings.class, TrustStoreSettings.class})
public class SSLContextConfig {

    @Resource
    private HttpServerSettings serverSettings;

    /**
     * Configures a SSLContextParameter used by Camel's embedded HTTP server to set up a HTTPS/SSL secured connection.
     *
     * @return The configured SSLContextParameter
     */
    @Bean(name = "sslContextParameters")
    public SSLContextParameters sslContextParameters() {
        URL keyStoreUrl = this.getClass().getResource(serverSettings.getSsl().getKeyStore().getResource());

        // http://camel.apache.org/jetty.html
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(keyStoreUrl.getPath());
        ksp.setPassword(serverSettings.getSsl().getKeyStore().getPassword());

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(serverSettings.getSsl().getKeyStore().getCertPassword());

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);

        // Jetty support only TLSv1.2 by default as insecure ciphers are excluded, which are basically all TLSv1.0 and
        // TLSv1.1 ciphers, hence clients not supporting TLSv1.2 protocol will fail
        List<String> supportedSslProtocols = Arrays.asList("TLSv1", "TLSv1.1", "TLSv1.2");
        SecureSocketProtocolsParameters protocolsParameters = new SecureSocketProtocolsParameters();
        protocolsParameters.setSecureSocketProtocol(supportedSslProtocols);
        scp.setSecureSocketProtocols(protocolsParameters);

        // TLS 1.0 / 1.1 have been disabled by jetty 9.3
        // this is a first attempt to re-enable them
        // see
        // - https://www.eclipse.org/jetty/documentation/9.3.x/configuring-ssl.html
        // - https://github.com/eclipse/jetty.project/issues/860
        // - http://camel.apache.org/camel-configuration-utilities.html
        FilterParameters cipherParameters = new FilterParameters();
        cipherParameters.getInclude().add(".*");
        cipherParameters.getExclude().add("^.*_(MD5|SHA1)$");
        scp.setCipherSuitesFilter(cipherParameters);


        // we also limit the supported ciphers and throw out those that are considered unsafe
/*    List<String> supportedCiphers = Arrays.asList("TLS_RSA_WITH_AES_128_CBC_SHA",
                                                  "TLS_RSA_WITH_AES_128_CBC_SHA256",
                                                  "TLS_RSA_WITH_AES_128_GCM_SHA256",
                                                  "TLS_RSA_WITH_AES_256_CBC_SHA",
                                                  "TLS_RSA_WITH_AES_256_CBC_SHA256",
                                                  "TLS_RSA_WITH_AES_256_GCM_SHA384",
                                                  "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                                                  "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                                                  "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                                                  "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                                                  "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                                                  "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                                                  // tls 1.0
                                                  "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                                  "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                                                  "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                                                  "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                                  "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                                                  "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                                                  "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                                                  "TLS_RSA_WITH_AES_128_CBC_SHA",
                                                  "TLS_RSA_WITH_AES_256_CBC_SHA"

    );
    CipherSuitesParameters cipherSuites = new CipherSuitesParameters();
    cipherSuites.setCipherSuite(supportedCiphers);
    scp.setCipherSuites(cipherSuites);*/

        //    log.info("Jetty ssl context: ", scp.toString());
        return scp;
    }
}
