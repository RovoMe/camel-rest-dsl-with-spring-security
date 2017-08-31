package at.rovo.awsxray.routes;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.config.settings.AwsS3Settings;
import at.rovo.awsxray.routes.beans.UploadBlobToS3;
import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3FileUploadRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String S3_FILE_UPLOAD = "seda:s3-file-upload";

    @Resource
    private AwsS3Settings awsS3Settings;

    public void setAwsS3Settings(AwsS3Settings awsS3Settings) {
        this.awsS3Settings = awsS3Settings;
    }

    @Override
    public void configure() throws Exception {
        from(S3_FILE_UPLOAD)
                .routeId("s3-blob-upload")

                // in case of any exceptions store file to local path for later retry
                .onException(Exception.class)
                    .maximumRedeliveries(2) // indefinite retry
                    .redeliveryDelay(2000L)
                    .onRedelivery((Exchange exchange) -> {
                        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                        LOG.info(">> Retrying seda upload to S3 due to " + ex.getLocalizedMessage());
                    })
                    .logExhausted(true)
                    .handled(true)
                    .logStackTrace(true)
                    .log(LoggingLevel.WARN, "Upload attempts exhausted. Storing file temporarily to the local file system. Reason: ${exception.message}")
                    .log("Writing message which failed to upload to S3 as file ${in.headers." + Exchange.FILE_NAME
                         + "} to local directory " + awsS3Settings.getRemoteWriteUploadErrorPath())
                    // write into temporary file and rename the file afterwards to prevent the consumer
                    // route from consuming the file to early
                    .to(awsS3Settings.getRemoteWriteUploadErrorPath()+"?tempFileName=inprogress-${file:name.noext}.tmp")
                .end()

                // rename the file to the s3Key otherwise we will lose that information on reading in the
                // file later on
                .setHeader(Exchange.FILE_NAME, header(HeaderConstants.FILE_BLOB_S3KEY))

                .log("Uploading file ${in.headers." + HeaderConstants.FILE_BLOB_S3KEY +"} to S3 bucket " + awsS3Settings.getBucketName())
                .bean(UploadBlobToS3.class)
                .log("Upload of ${in.headers." + HeaderConstants.FILE_BLOB_S3KEY + "} to S3 done");

        // retry in considerable (e.g., 10s) delay to upload the file again. Only process files that do
        // not end with *.tmp suffix
        from(awsS3Settings.getRemoteWriteUploadErrorPath() + "?delay=30000&delete=true&antExclude=*.tmp")
                .routeId("s3-blob-upload-retry")

                .onException(Exception.class)
                    .maximumRedeliveries(1) // indefinite retry
                    .redeliveryDelay(5000L)
                    .onRedelivery((Exchange exchange) -> {
                        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                        LOG.info(">> Retrying file system upload to S3 due to " + ex.getLocalizedMessage());
                    })
                    .logExhausted(true)
                    .handled(false)
                .end()

                // as we stored the file with the s3Key as its file name we need to read back the file name
                // and write it to the headers in order for the service to know the s3Key of the message to
                // persist
                .setHeader(HeaderConstants.FILE_BLOB_S3KEY, header(Exchange.FILE_NAME))
                .log("Consumed file from directory " + awsS3Settings.getRemoteWriteUploadErrorPath()
                     + " and restored S3Key ${in.headers." + HeaderConstants.FILE_BLOB_S3KEY + "}")

                .log("Uploading message ${in.headers." + HeaderConstants.FILE_BLOB_S3KEY +"} to S3 bucket " + awsS3Settings.getBucketName())
                .bean(UploadBlobToS3.class)
                .log("Upload of ${in.headers." + HeaderConstants.FILE_BLOB_S3KEY + "} to S3 done");
    }
}
