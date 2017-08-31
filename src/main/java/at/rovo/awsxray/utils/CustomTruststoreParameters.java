package at.rovo.awsxray.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.camel.util.jsse.TrustManagersParameters;

/**
 * Creates a custom truststore which automatically accepts all incoming certificates.
 * <p/>
 * Note that this is rather insecure but due to the inflexibility to add or chain truststores with
 * unstrusted/self-signed certificates at runtime, this is the easiest solution by far.
 */
public class CustomTruststoreParameters extends TrustManagersParameters
{

  @Override
  public TrustManager[] createTrustManagers() throws GeneralSecurityException, IOException {
    return  new TrustManager[]{
        new X509TrustManager() {
          public void checkClientTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
          }

          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          public void checkServerTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
            // This will never throw an exception.
            // This doesn't check anything at all: it's insecure.
          }
        }
    };
  }
}
