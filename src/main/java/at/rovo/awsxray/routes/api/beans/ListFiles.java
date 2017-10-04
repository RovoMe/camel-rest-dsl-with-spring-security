package at.rovo.awsxray.routes.api.beans;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.domain.FileService;
import at.rovo.awsxray.domain.dto.FileEntry;
import at.rovo.awsxray.domain.dto.ListFilesResponse;
import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import at.rovo.awsxray.utils.AuditLogUtils;
import at.rovo.awsxray.xray.Trace;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

@Component
@Trace
public class ListFiles {

    @Resource
    private FileService fileService;
    @Resource
    private AuditLogUtils auditLogUtils;

    @Handler
    public ListFilesResponse listFiles(Exchange exchange) {

        String userId = exchange.getIn().getHeader("userId", String.class);

        String contextPath = exchange.getIn().getHeader(Exchange.HTTP_URL, String.class);
        int limit = exchange.getIn().getHeader("limit", Integer.class);
        int offset = exchange.getIn().getHeader("offset", Integer.class);

        if (limit > 30){
            limit = 30;
        }
        if (limit < 1) {
            limit = 1;
        }
        if (offset < 0) {
            offset = 0;
        }

        long fileCount = fileService.countFiles();
        List<FileEntity> files = fileService.listFiles(limit, offset);

        List<FileEntry> dtos = new ArrayList<>(files.size());
        for (FileEntity entity : files) {
            FileEntry entry = new FileEntry(entity, contextPath);
            dtos.add(entry);
        }

        auditLogUtils.auditLog(userId, "Files for listing collected");

        return new ListFilesResponse(dtos, contextPath, limit, offset, fileCount);
    }
}
