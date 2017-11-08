package at.rovo.awsxray.routes.api.beans;

import at.rovo.awsxray.domain.FileService;
import at.rovo.awsxray.domain.dto.FileEntry;
import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import at.rovo.awsxray.exceptions.FileNotFoundException;
import at.rovo.awsxray.s3.BlobStore;
import at.rovo.awsxray.utils.AuditLogUtils;

import java.io.IOException;
import javax.annotation.Resource;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

@Component
public class GetFile {

    @Resource
    private FileService fileService;
    @Resource
    private BlobStore blobStore;
    @Resource
    private AuditLogUtils auditLogUtils;

    @Handler
    public FileEntry getFile(Exchange exchange) throws IOException, FileNotFoundException {

        String userId = exchange.getIn().getHeader("userId", String.class);

        String contextPath = exchange.getIn().getHeader(Exchange.HTTP_URL, String.class);
        String uuid = exchange.getIn().getHeader("file_uuid", String.class);

        FileEntity file = fileService.get(uuid);
        if (null != file) {
            if (file.getBlobKey() != null) {
                auditLogUtils.auditLog(userId, "Preparing download of document content from S3 for key " + file.getBlobKey());
                byte[] content = blobStore.getMessage(file.getBlobKey());
                auditLogUtils.auditLog(userId, "Download complete. Downloaded " + content.length + " bytes from S3");
                return new FileEntry(file, contextPath, content);
            } else {
                return new FileEntry(file, contextPath);
            }
        } else {
            throw new FileNotFoundException("No file with uuid " + uuid + " available");
        }
    }
}
