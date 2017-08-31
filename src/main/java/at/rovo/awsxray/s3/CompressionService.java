package at.rovo.awsxray.s3;

/**
 * Common interface for {@link #compress(byte[]) compressing} and {@link #decompress(byte[])
 * decormpessing} bytes.
 */
public interface CompressionService
{

  /**
   * Compresses the given byte content.
   *
   * @param content The byte content which needs to be compressed
   * @return The compressed representation of the provided byte content
   */
  byte[] compress(byte[] content);

  /**
   * Decompresses a previously compressed payload.
   * <p>
   * This method differs from {@link #decompress(byte[], int)} by not having to provide the original
   * size of the payload before compression. This method attempts to guess the original message size
   * though is therefore also a bit slower then the other method.
   *
   * @param compressed The compressed byte representation of some content
   * @return The decompressed or raw content of the provided bytes
   */
  byte[] decompress(byte[] compressed);

  /**
   * Decompresses a previously compressed payload.
   * <p>
   * This method differs from {@link #decompress(byte[])} by not trying to guess the total size
   * of the original payload. It is therefore slightly faster than then the other method.
   *
   * @param compressed   The compressed byte representation of some content
   * @param originalSize The original size of the payload before it got compressed
   * @return The decompressed or raw content of the provided bytes
   */
  byte[] decompress(byte[] compressed, int originalSize);
}
