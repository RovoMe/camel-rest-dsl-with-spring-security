package at.rovo.awsxray.routes.api.beans;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.domain.FileService;
import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import at.rovo.awsxray.s3.BlobStore;
import at.rovo.awsxray.utils.AuditLogUtils;
import at.rovo.awsxray.xray.Trace;
import com.amazonaws.xray.AWSXRay;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Headers;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@Trace(metricName = "StoreFile")
public class StoreFile {

    @Resource
    private FileService fileService;
    @Resource
    private AuditLogUtils auditLogUtils;
    @Resource
    private BlobStore blobStore;

    @Handler
    public JSONObject storeFile(@Headers Map<String, Object> headers, @Body FileEntity fileEntity) throws JSONException {
        String userId = (String)headers.get("userId");
        String fileName = (String)headers.get(Exchange.FILE_NAME);
        String traceId = (String)headers.get(HeaderConstants.XRAY_TRACE_ID);

        // Upload file to S3
        byte[] rawContent = fileEntity.getRawContent();
        String blobKey = blobStore.asyncStoreMessage(rawContent, fileEntity.getUuid(), System.currentTimeMillis(), traceId);

        fileEntity.setBlobKey(blobKey);

        fileService.persist(fileEntity);

        AWSXRay.getCurrentSegment().putAnnotation("documentUuid", fileEntity.getUuid());
        auditLogUtils.auditLog(userId, "Metadata of file " + fileName + " persisted for UUID " + fileEntity.getUuid());

        JSONObject response = new JSONObject();
        response.put("uuid", fileEntity.getUuid());
        return response;
    }
}
