package at.rovo.awsxray.s3;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.config.settings.AwsS3Settings;
import at.rovo.awsxray.routes.S3FileUploadRoute;
import com.amazonaws.services.s3.AmazonS3;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link BlobStore} implementation which stores file bytes to Amazon's Simple Storage Service (S3) and also retrieves
 * files from this service.
 */
@Service
public class S3BlobStore implements BlobStore {

  private static final Logger LOG = LoggerFactory.getLogger(S3BlobStore.class);

  private final S3Util s3Util;

  /** Camel's producer template to send data into a Camel route **/
  private final ProducerTemplate producerTemplate;
  /** The Camel managed endpoint to send data to **/
  private final Endpoint localQueueToRemoteWriteStateUpload;
  /** The compression service which takes care of compressing and decompressing the payload **/
  private final CompressionService compressionService;
  /** Settings for the file upload to and download from S3 **/
  private final AwsS3Settings awsS3Settings;

  /** Threshold that defines whether files need to be LZ4 zipped or not **/
  private final static int ZIP_THRESHOLD = 524_288; // 0.5 MB


  @Autowired
  public S3BlobStore(AmazonS3 s3Client, ProducerTemplate producerTemplate,
                     CompressionService compressionService, AwsS3Settings awsS3Settings)
      throws NoSuchAlgorithmException {

    this.s3Util = new S3Util();
    this.s3Util.setAwsS3Client(s3Client);

    this.producerTemplate = producerTemplate;
    this.compressionService = compressionService;
    this.awsS3Settings = awsS3Settings;
    this.localQueueToRemoteWriteStateUpload =
        this.producerTemplate.getCamelContext().getEndpoint(S3FileUploadRoute.S3_FILE_UPLOAD);

    LOG.info("S3 file blob store initialized");
  }

  private String generateKey(long timestamp, String uuid, int size, boolean needsZipping) {
    
    // according to http://docs.aws.amazon.com/AmazonS3/latest/dev/request-rate-perf-considerations.html
    // the random value will be generated using a MD5 hash of the actual key name
    StringBuilder sb = new StringBuilder(uuid).append("+").append(timestamp).append("+").append(size).append(".file");
    if (needsZipping) {
      sb.append(".lz4");
    }

    LOG.debug("Generated new key: {}", sb);
    return sb.toString();
  }

  @Override
  public String syncStoreMessage(byte[] file, String uuid, long timestamp) {
    throw new UnsupportedOperationException("Method not implemented yet");
  }

  @Override
  public String asyncStoreMessage(byte[] file, String uuid, long timestamp) {

    LOG.debug("S3 blob store async store for file {} invoked", uuid);
    String key;
    if (file.length >= ZIP_THRESHOLD) {
      // zip the bytes
      LOG.debug("File size exceeds raw threshold. Starting compression of payload");
      int originMessageSize = file.length;
      file = compressionService.compress(file);
      key = generateKey(timestamp, uuid, originMessageSize, true);
    } else {
      LOG.debug("File size within raw threshold. Uploading raw payload");
      key = generateKey(timestamp, uuid, file.length, false);
    }

    Exchange remoteWriteStateUpload =
        localQueueToRemoteWriteStateUpload.createExchange(ExchangePattern.InOnly);
    remoteWriteStateUpload.getIn().setBody(file);
    remoteWriteStateUpload.getIn().setHeader(HeaderConstants.FILE_BLOB_S3KEY, key);
    LOG.debug("Forwarding file {} for upload to S3: {}", uuid, key);
    producerTemplate.asyncSend(localQueueToRemoteWriteStateUpload, remoteWriteStateUpload);
    LOG.debug("Uploading file {} blob with key {} to S3 initiated", uuid, key);

    return key;
  }

  @Override
  public byte[] getMessage(String key) throws IOException {
    LOG.trace("Looking up file from S3: {}", key);
    byte[] bytes = IOUtils.toByteArray(s3Util.downloadStream(awsS3Settings.getBucketName(), key));
    if (key.endsWith(".lz4")) {
      // the key indicates that the payload was compressed before, so decompress it
      LOG.trace("Found key for compressed payload. Starting decompression");
      String sSize = key.substring(key.lastIndexOf("+") + 1, key.indexOf("."));
      try {
        int originSize = Integer.parseInt(sSize);
        LOG.trace("Decompressing file content with origin size of {} based of information parsed from the key", originSize);
        bytes = compressionService.decompress(bytes, originSize);
      } catch (NumberFormatException ex) {
        LOG.trace("Could not determine original file size from key. Falling back on probing the size based on the compressed data");
        bytes = compressionService.decompress(bytes);
      }
    }
    LOG.debug("Loaded {} blob file bytes for key {} from S3", bytes.length, key);
    return bytes;
  }
}
