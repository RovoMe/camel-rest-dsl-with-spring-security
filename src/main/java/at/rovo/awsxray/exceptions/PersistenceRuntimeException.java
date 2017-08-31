package at.rovo.awsxray.exceptions;

public class PersistenceRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 9021092547666823206L;

  public PersistenceRuntimeException() {
    super();
  }

  public PersistenceRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public PersistenceRuntimeException(final String message) {
    super(message);
  }

  public PersistenceRuntimeException(final Throwable cause) {
    super(cause);
  }

}
