package at.rovo.awsxray.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Util {

  private static final Logger LOG = LoggerFactory.getLogger(S3Util.class);

  private AmazonS3 awsS3Client;

  protected PutObjectRequest createPutObjectRequest(String bucket, String key, byte[] body) {
    if (body == null) {
      throw new IllegalArgumentException("Body to upload must not be null");
    }
    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentLength(body.length);
    return new PutObjectRequest(bucket,
                                key, new ByteArrayInputStream(body), objectMetadata);

  }

  public String upload(String bucket, String key, byte[] body) {
    PutObjectRequest request = createPutObjectRequest(bucket, key, body);
    PutObjectResult result = awsS3Client.putObject(request);
    LOG.debug("Received ETag {} for file with key {}", result.getETag(), key);
    return result.getETag();
  }

  public Upload uploadWithTransferManager(String bucket, String key, byte[] body) {
    PutObjectRequest request = createPutObjectRequest(bucket, key, body);
    TransferManager txManager = new TransferManager(awsS3Client);
    Upload upload = txManager.upload(request);
    LOG.debug("Started upload for file with key {}. The current state is {}", key,
              upload.getState());
    return upload;
  }

  public InputStream downloadStream(String bucket, String key) {
    S3Object s3Object = awsS3Client.getObject(bucket, key);
    return s3Object.getObjectContent();
  }

  public byte[] download(String bucket, String key) throws IOException {
    S3Object s3Object = awsS3Client.getObject(bucket, key);
    return IOUtils.toByteArray(s3Object.getObjectContent());
  }

  public String testBucketAvailability(String bucket) throws IOException {

    String bucketLocation;
    try {
      bucketLocation = awsS3Client.getBucketLocation(bucket);
      if (Strings.isNullOrEmpty(bucketLocation)) {
        throw new IOException("Could not access bucket " + bucket);
      }
    } catch (Exception e) {
      throw new IOException(e);
    }

    return bucketLocation;
  }

  public void setAwsS3Client(AmazonS3 awsS3Client) {
    this.awsS3Client = awsS3Client;
  }

  public static String createUniqueKey(String identifier, String filename) {

    DateTime now = DateTime.now();

    return new StringBuffer()
        .append(now.getYear())
        .append("/")
        .append(now.getMonthOfYear())
        .append("/")
        .append(now.getDayOfMonth())
        .append("/")
        .append(identifier)
        .append("/")
        .append(now.getMillis())
        .append("/")
        .append(filename)
        .toString();
  }
}
