package at.rovo.awsxray.security;

import java.security.SecureRandom;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyGenerator
{

  private static final Logger LOG = LoggerFactory.getLogger(KeyGenerator.class);

  private static final String V1_APPKEY_ID = "H4E";

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public static String generateAppKey(String name, String version) {
                /*
		 * Generate AppKeys with 48 digits length according to the following convention
		 * 
		 * V*_APPKEY_ID (3 digits)
		 * Base64(SHA256(name)) (first 10 digits)
		 * Base64(SHA256(version)) (first 3 digits)
		 * 
		 */
    StringBuffer appKey = new StringBuffer();
    appKey.append(V1_APPKEY_ID);

    String nameBase64Sha256 = Base64.encodeBase64String(DigestUtils.sha256(name));
    appKey.append(nameBase64Sha256.substring(0, 10));

    String versionBase64Sha256 = Base64.encodeBase64String(DigestUtils.sha256(version));
    appKey.append(versionBase64Sha256.substring(0, 3));

    String random = createRandomAlphaNumeric(32);
    appKey.append(random);

    assert (appKey.length() == 48);
    String appKeyS = appKey.toString();
    LOG.debug("Created random {}", appKeyS);
    LOG.info("Created app key {} for {}", appKeyS, name);
    return appKeyS;
  }

  public static String generateUserKey(String userId) {
    return generateUserKey(userId, 86);
  }


  public static String generateUserKey(String userId, int length) {
    String random = RandomStringUtils.randomAlphanumeric(length);
    LOG.info("Created user key {} for {}", random, userId);
    return random;
  }

  private static String createRandomAlphaNumeric(int count) {
    return RandomStringUtils.random(count, 0, 0, true, true, null, SECURE_RANDOM);
  }

}
