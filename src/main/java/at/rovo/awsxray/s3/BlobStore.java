package at.rovo.awsxray.s3;

import java.io.IOException;

/**
 * Common interface for a file store that provides basic storage and retrieval methods.
 * <p>
 * A message content can be stored either {@link #syncStoreMessage(byte[], String, long, String) synchronously}, by
 * blocking further execution until the file was uploaded successfully or failed the general upload, or
 * {@link #asyncStoreMessage(byte[], String, long, String) asynchronously} by letting the invoking code proceed its
 * execution in a fire and forget manner.
 * <p>
 * Stored files can be retrieved via the previously generated key of the message which needs to be
 * passed to {@link #getMessage(String)}. The returned input stream will allow to stream the bytes
 * of the message into memory (or any other target).
 */
public interface BlobStore {

  /**
   * Stores the bytes of a <em>file</em> to a remote store for a given <em>key</em>. The storage will happen in a
   * synchronous fashion and block until the message is either successfully uploaded or the upload failed.
   *
   * @param file      The byte of the message to store
   * @param uuid      The unique identifier of a message
   * @param timestamp A timestamp value similar to the one generated via {@link System#currentTimeMillis()}
   * @param traceId   Optional argument that may be used in order to keep track of the storage process
   * @return The unique key of the stored message
   */
  String syncStoreMessage(byte[] file, String uuid, long timestamp, String traceId);

  /**
   * Stores the bytes of a <em>file</em> to a remote store for a given <em>key</em>. The storage will happen
   * asynchronously and return immediately irrespective of the current storage action.
   *
   * @param file      The byte of the message to store
   * @param uuid      The unique identifier of a message
   * @param timestamp A timestamp value similar to the one generated via {@link System#currentTimeMillis()}
   * @param traceId   Optional argument that may be used in order to keep track of the storage process
   * @return The unique key of the stored message
   */
  String asyncStoreMessage(byte[] file, String uuid, long timestamp, String traceId);

  /**
   * Retrieves a file from the store by looking up the document with the previously generated and stored <em>key</em>.
   *
   * @param key The unique identifier of the stored message
   * @return The bytes of the message on S3 matching the provided key
   * @throws IOException If the document could not get accessed
   */
  byte[] getMessage(String key) throws IOException;
}
