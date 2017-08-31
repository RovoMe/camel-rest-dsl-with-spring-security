package at.rovo.awsxray.utils;

import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some basic http helper methods.
 */
public class HttpUtil {

  private static final Logger LOG = LoggerFactory.getLogger(HttpUtil.class);

  /**
   * Decodes a basic authentication header.
   *
   * @see "https://en.wikipedia.org/wiki/Basic_access_authentication"
   *
   * @param header The header value for the HTTP Authorization header field
   * @param characterSet the character set in which the base64 header is encoded
   * @return null if decoding failed or a String[] with 2 elements: 1st element == username, 2nd element == password
   */
  public static String[] decodeBasicAuthHeader(String header, Charset characterSet) {

    if (Strings.isNullOrEmpty(header)) {
      return null;
    }

    if (characterSet == null) {
      characterSet = StandardCharsets.UTF_8;
    }

    if (!header.toLowerCase().startsWith("basic")) {
      LOG.warn("Not a basic auth header {}", header);
      return null;
    }

    byte[] base64Token;
    base64Token = header.substring(6).getBytes(characterSet);

    byte[] decoded;
    decoded = Base64.decodeBase64(base64Token);

    String token;
    try {
      token = new String(decoded, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e.getMessage(), e);
      return null;
    }

    int numDelimiters = StringUtils.countMatches(token, ":");

    if (numDelimiters < 1) {
      return null;
    } else if (numDelimiters == 1) {
      int delim = token.indexOf(":");
      return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    } else {
      return token.split(":");
    }
  }

  /**
   * Shortcut for UTF-8 encoded header
   *
   * @param header The header value for the HTTP Authorization header field
   * @return The decoded user credentials provided within the HTTP authorization header
   */
  public static String[] decodeBasicAuthHeader(String header) {
    return decodeBasicAuthHeader(header, null);
  }

}
