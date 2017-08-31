package at.rovo.awsxray.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * This logging converter will mask passwords found in log lines that were marked as
 * <em>CONFIDENTIAL</em> with a <em>XXX</em> character sequence to hide the actual password from log
 * lines.
 * <p>
 * The converter has to be registered in the respective logback configuration file first before it
 * will take effect. To register this converter the following line has to be appended to the
 * respective logback configuration:
 * <pre><code>    &lt;conversionRule conversionWord="mask"
 *                  converterClass="at.erpel.logging.converter.MaskingConverter" /></code></pre>
 * Once this converter got registered log lines marked as <em>CONVERTED</em> can be masked simply
 * by using the following snippet:
 * <pre><code>    %mask(%msg)</code></pre>
 * inside the log pattern definition.
 * <p>
 * Note that currently masking passwords in URL typical <em>key=value</em> pairs, credentials
 * specified in URLs in the form of <em>protocol://username:password@domain:port/path</em> as well
 * as obfuscating basic authentication strings are supported.
 *
 * @param <E> The type of the log event triggering the transformation prcoess
 */
public class MaskingConverter<E extends ILoggingEvent> extends CompositeConverter<E>
{

  public static final String CONFIDENTIAL = "CONFIDENTIAL";
  public static final Marker CONFIDENTIAL_MARKER = MarkerFactory.getMarker(CONFIDENTIAL);

  private Pattern keyValPattern;
  private Pattern basicAuthPattern;
  private Pattern urlAuthorizationPattern;

  @Override
  public void start() {
    keyValPattern = Pattern.compile("(pw|pwd|password)=.*?(&|$)");
    basicAuthPattern = Pattern.compile("(B|b)asic ([a-zA-Z0-9+/=]{3})[a-zA-Z0-9+/=]*([a-zA-Z0-9+/=]{3})");
    urlAuthorizationPattern = Pattern.compile("//(.*?):.*?@");
    super.start();
  }

  @Override
  protected String transform(E event, String in) {
    if (!started) {
      return in;
    }
    Marker marker = event.getMarker();
    if (null != marker && CONFIDENTIAL.equals(marker.getName())) {
      // key=value[&...] matching
      Matcher keyValMatcher = keyValPattern.matcher(in);
      // Authorization: Basic dXNlcjpwYXNzd29yZA==
      Matcher basicAuthMatcher = basicAuthPattern.matcher(in);
      // sftp://user:password@host:port/path/to/resource
      Matcher urlAuthMatcher = urlAuthorizationPattern.matcher(in);

      if (keyValMatcher.find()) {
        String replacement = "$1=XXX$2";
        return keyValMatcher.replaceAll(replacement);
      } else if (basicAuthMatcher.find()) {
        return basicAuthMatcher.replaceAll("$1asic $2XXX$3");
      } else if (urlAuthMatcher.find()) {
        return urlAuthMatcher.replaceAll("//$1:XXX@");
      }
    }
    return in;
  }
}
