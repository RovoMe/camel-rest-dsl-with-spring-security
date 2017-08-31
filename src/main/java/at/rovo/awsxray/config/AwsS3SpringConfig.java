package at.rovo.awsxray.config;

import at.rovo.awsxray.config.settings.AwsS3Settings;
import at.rovo.awsxray.routes.S3FileUploadRoute;
import at.rovo.awsxray.routes.beans.UploadBlobToS3;
import at.rovo.awsxray.s3.BlobStore;
import at.rovo.awsxray.s3.CompressionService;
import at.rovo.awsxray.s3.Lz4Service;
import at.rovo.awsxray.s3.S3BlobStore;
import at.rovo.awsxray.s3.S3Util;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.google.common.base.Strings;
import java.lang.invoke.MethodHandles;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.annotation.Resource;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@EnableConfigurationProperties(AwsS3Settings.class)
public class AwsS3SpringConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private ProducerTemplate producerTemplate;
    @Resource
    private AwsS3Settings awsS3Settings;

    @Bean
    public AmazonS3EncryptionClient amazonS3EncryptionClient() {

        if (Strings.isNullOrEmpty(awsS3Settings.getAccessKey())) {
            throw new RuntimeException("Property s3.awsS3AccessKey not found. Cannot create s3 client");
        }
        if (Strings.isNullOrEmpty(awsS3Settings.getSecretKey())) {
            throw new RuntimeException("Property s3.awsS3SecretKey not found. Cannot create s3 client");
        }
        if (Strings.isNullOrEmpty(awsS3Settings.getEncryptionKey())) {
            throw new RuntimeException("The property s3.awsS3EncryptionKey could not be found. Cannot create s3 client");
        }
        if (Strings.isNullOrEmpty(awsS3Settings.getRegion())) {
            throw new RuntimeException("Property s3.region not set. Cannot create s3 client");
        }

        return createS3Client(awsS3Settings);
    }

    private AmazonS3EncryptionClient createS3Client(AwsS3Settings s3Settings) {

        final AWSCredentials credentials = new BasicAWSCredentials(s3Settings.getAccessKey(), s3Settings.getSecretKey());

        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withMaxErrorRetry(3) // matches the DEFAULT_RETRY_POLICY of the AWS client configuration which is set to 3 attempts before giving up
                .withConnectionTimeout(10_000) // 10 seconds max wait time for a connection to establish. Matches default value of 10 seconds
                .withSocketTimeout(50_000); // regard missing remote answers for more than 50 seconds as broken connections. Matches default value of 50 seconds

        try {
            byte[] key = s3Settings.getEncryptionKey().getBytes("UTF-8");
            final MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32); // use only first 256 bit

            final SecretKey encryptionKey = new SecretKeySpec(key, "AES");
            final EncryptionMaterials encryptionMaterials = new EncryptionMaterials(encryptionKey);
            final AmazonS3EncryptionClient client =
                    new AmazonS3EncryptionClient(credentials, encryptionMaterials, clientConfiguration, new CryptoConfiguration());

            LOG.debug("Setting region for Amazon S3 client to {}", s3Settings.getRegion());
            client.setEndpoint(s3Settings.getRegion());

            return client;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create S3 client",e);
        }
    }

    // This bean will use the AWS S3 Ireland client internally. Beware if you need to up- or download
    // files from S3 in Frankfurt!
    @Bean
    public S3Util s3Util() {
        S3Util s3Util = new S3Util();
        s3Util.setAwsS3Client(amazonS3EncryptionClient());
        return s3Util;
    }

    @Bean
    public CompressionService compressionService() {
        if (awsS3Settings.getMaxSupportedFileSize() != null) {
            int maxSupportedFileSize = awsS3Settings.getMaxSupportedFileSize();
            LOG.info("Initializing new LZ4 compression service configured for a maximum supported file size of {} bytes",
                     maxSupportedFileSize);
            return new Lz4Service(maxSupportedFileSize);
        } else {
            LOG.info("Initializing new default LZ4 compression service");
            return new Lz4Service();
        }
    }

    @Bean
    @DependsOn("producerTemplate")
    public BlobStore blobStore() throws Exception {
        return new S3BlobStore(amazonS3EncryptionClient(), producerTemplate, compressionService(), awsS3Settings);
    }

    @Bean(name = "uploadBlobToS3")
    public UploadBlobToS3 uploadBlobToS3() {
        return new UploadBlobToS3(amazonS3EncryptionClient(), awsS3Settings);
    }

    @Bean
    public S3FileUploadRoute remoteWriteStateUploadRoutes() {
        return new S3FileUploadRoute();
    }
}
