package at.rovo.awsxray.config.settings;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
@Validated
public class AwsS3Settings {

  /** The AWS region to send requests to **/
  @NotNull
  private String region;
  /** The assigned access key **/
  @NotNull
  private String accessKey;
  /** The assigned secret key (should never be exposed to others) **/
  @NotNull
  private String secretKey;
  /** The key used for encrypting content uploaded to S3 **/
  @NotNull
  private String encryptionKey;
  /** The S3 bucket name to store files into **/
  @NotNull
  private String bucketName;
  /** The local error directory files get stored to on upload failures to S3 **/
  @NotNull
  private String remoteWriteUploadErrorPath;
  /** Specifies the file size, if exceeded, will result in compression before upload **/
  private Integer maxSupportedFileSize;
}
