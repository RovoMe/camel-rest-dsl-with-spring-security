package at.rovo.awsxray.routes.beans;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.config.settings.AwsS3Settings;
import at.rovo.awsxray.s3.S3Util;
import com.amazonaws.services.s3.AmazonS3;
import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.component.aws.xray.XRayTrace;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Uploads the bytes of the file to S3.
 */
@Component
@XRayTrace
public class UploadBlobToS3 {

  private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private S3Util s3Util;
  @Resource
  private AmazonS3 amazonS3Client;
  @Resource
  private AwsS3Settings awsS3Settings;

  // On using the Camel way of using .bean(UploadBlobToS3.class), Camel tries to initialize an
  // object of that class using the default constructor which is not available on using Spring's
  // constructor injection mechanism

  public UploadBlobToS3() {
    this.s3Util = new S3Util();
    this.s3Util.setAwsS3Client(amazonS3Client);
  }

  @Autowired
  public UploadBlobToS3(AmazonS3 amazonS3Client, AwsS3Settings awsS3Settings) {
    this.s3Util = new S3Util();
    this.s3Util.setAwsS3Client(amazonS3Client);

    this.awsS3Settings = awsS3Settings;
  }

  /**
   * Uploads the message body contained in the exchange to S3
   *
   * @param exchange The exchange containing the message bytes to upload
   */
  @Handler
  public void remoteWriteStateUpload(Exchange exchange) {
    if (s3Util == null) {
      throw new IllegalStateException("No S3Util instance found");
    }
    String key = exchange.getIn().getHeader(HeaderConstants.FILE_BLOB_S3KEY, String.class);
    LOG.debug("Attempting to upload file with key {} to S3 bucket: {}",
              key, awsS3Settings.getBucketName());
    StopWatch uploadWatch = new StopWatch();
    uploadWatch.start();
    byte[] content = exchange.getIn().getBody(byte[].class);
    s3Util.upload(awsS3Settings.getBucketName(), key, content);
    uploadWatch.stop();
    LOG.info("Upload of message with key {} to S3 bucket {} succeeded. Time needed to upload file containing {} bytes: {} ms",
              key, awsS3Settings.getBucketName(), content.length, uploadWatch.getTime());
  }
}
