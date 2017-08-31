package at.rovo.awsxray.utils;

public class MongodbPersistenceUtil
{

  /**
   * Internal helper function to clean input strings of any problematic characters. Use whitelisting
   * for security reasons!
   */
  public static String sanitize(final String input) {
    if ((input == null) || input.isEmpty()) {
      return input;
    }

    // The dash must be the last character, otherwise it's interpreted as A-Z
    // Exclude "';\%
    // MongoDB security $, see http://docs.mongodb.org/manual/faq/developers/#how-does-mongodb-address-sql-or-query-injection
    return input.replaceAll("[^A-Za-z0-9@ÄÖÜäöüß.:&/ +_-]", "");
  }


  /**
   * Internal helper function to escape the input to be regex safe for a full match.
   */
  public static String prepareForRegex(final String input) {
    if ((input == null) || input.isEmpty()) {
      return input;
    }

    return prepareForRegexStartsWith(input) + "$";
  }


  /**
   * Internal helper function to escape the input to be regex safe for the start of a string.
   */
  public static String prepareForRegexStartsWith(final String input) {
    if ((input == null) || input.isEmpty()) {
      return input;
    }

    // Remove any \E which is used to terminate the full regular expression.
    return "^\\Q" + sanitize(input.replace("\\E", "")) + "\\E";
  }

}
