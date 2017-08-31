package at.rovo.awsxray.s3;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.apache.commons.io.FileUtils;

/**
 * This {@link CompressionService} uses the LZ4 compression and decompression algorithm to compress
 * and/or decompress a byte representation of some arbitrary data.
 */
@Slf4j
@Getter
@Setter
public class Lz4Service implements CompressionService {

  private final static LZ4Factory factory = LZ4Factory.fastestInstance();

  /** Number of bytes contained in a single megabyte **/
  private final static int MB = 1_048_576; // 1 MB

  /** Defines the maximum file size supported by this service. Note underestimating this constant
   * may lead to data loss if a file exceeds this threshold as on decompressing the data there might
   * not be enough storage space to copy over all required data. Oversizing this value however will
   * allocate plenty more space for every decompressed file than eventually necessary **/
  private int maxSupportedFileSize = 10 * MB;

  /**
   * Initializes a LZ4 based compression and decompression service which supports a maximum file
   * size of up to 10 megabytes. If larger files need to be decompressed, please use
   * {@link #Lz4Service(int)} instead.
   */
  public Lz4Service() {

  }

  /**
   * Initializes a LZ4 based compression and decompression service which supports a custom maximum
   * file size.
   *
   * @param maxSupportedFileSize The maximum file size to decompress supported by this service.
   */
  public Lz4Service(int maxSupportedFileSize) {
    this.maxSupportedFileSize = maxSupportedFileSize;
  }

  @Override
  public byte[] compress(byte[] data) {
    LZ4Compressor compressor = factory.fastCompressor();
    // create a length value that for sure can hold the compressed data
    int maxCompressedLength = compressor.maxCompressedLength(data.length);
    // ... and create a target array of that size
    byte[] compressed = new byte[maxCompressedLength];
    // the compression algorithm will fill the oversized compress array without removing any
    // unneeded slot at the and
    int compressedLength =
        compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);
    // we have to copy over the actual compressed data into a new array so avoid writing even more
    // bytes then the actual original data payload
    byte[] onlyCompressed = new byte[compressedLength];
    System.arraycopy(compressed, 0, onlyCompressed, 0, compressedLength);

    log.info("Compressed bytes - Original size: {} ({} bytes), Compressed: {} ({} bytes)",
             FileUtils.byteCountToDisplaySize(data.length), data.length,
             FileUtils.byteCountToDisplaySize(compressedLength), compressedLength
    );
    return onlyCompressed;
  }

  @Override
  public byte[] decompress(byte[] compressed) {
    LZ4SafeDecompressor decompressor = factory.safeDecompressor();
    // as we do not know the actual uncompressed message size yet, we need to oversize the array to
    // guarantee that the uncompressed message can fit into the target array
    byte[] restored = new byte[ maxSupportedFileSize ];
    // revert the compression. This algorithm does not shrink the target array by itself, though
    // copy the uncompressed data into the array
    int decompressedLength = decompressor.decompress(compressed, 0, compressed.length, restored, 0);
    // as the target array now contains the uncompressed data though also plenty of eventual
    // unneeded bytes, we have to copy over the actual data to a new array
    byte[] bytes = new byte[decompressedLength];
    System.arraycopy(restored, 0, bytes, 0, decompressedLength);

    log.info("Decompressed bytes - Compressed: {} ({} bytes), Decompressed size: {} ({} bytes)",
             FileUtils.byteCountToDisplaySize(compressed.length), compressed.length,
             FileUtils.byteCountToDisplaySize(decompressedLength), decompressedLength
    );
    return bytes;
  }

  @Override
  public byte[] decompress(byte[] compressed, int originalSize) {
    LZ4FastDecompressor decompressor = factory.fastDecompressor();
    // we already know the concrete size of the compressed message in its original state so we
    // utilize this knowledge here
    byte[] restored = new byte[originalSize];
    // the algorithm writes the uncompressed data into the previously generated buffer. As the
    // buffer should fit the original size the buffer will hold all of the original data after the
    // method invocation. So no need to copy over any further data
    int compressedLength = decompressor.decompress(compressed, 0, restored, 0, originalSize);

    if (originalSize != restored.length) {
      log.error("Restored size of compressed bytes does not match given original size! Please check");
    }
    if (compressed.length != compressedLength) {
      log.error("Compressed data was not read fully! Please check");
    }
    log.info("Decompressed bytes - Compressed: {} ({} bytes), Original Size: {} ({} bytes), Restored size: {} ({} bytes)",
             FileUtils.byteCountToDisplaySize(compressedLength), compressedLength,
             FileUtils.byteCountToDisplaySize(originalSize), originalSize,
             FileUtils.byteCountToDisplaySize(restored.length), restored.length
    );
    return restored;
  }
}
