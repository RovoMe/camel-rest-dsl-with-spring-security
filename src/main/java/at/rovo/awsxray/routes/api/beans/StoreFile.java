package at.rovo.awsxray.routes.api.beans;

import at.rovo.awsxray.domain.FileService;
import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import at.rovo.awsxray.s3.BlobStore;
import at.rovo.awsxray.xray.Trace;
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
    private BlobStore blobStore;

    @Handler
    public JSONObject storeFile(@Headers Map<String, Object> headers, @Body byte[] file) throws JSONException {

        String contentDisposition = (String)headers.get("Content-Disposition");
        String charset = (String)headers.get(Exchange.CHARSET_NAME);

        String fileName = "unknown";
        if (null != contentDisposition) {
            int startPos = contentDisposition.indexOf("filename=")+"filename=".length();
            if (startPos != -1) {
                int endPos = contentDisposition.indexOf(";", startPos);
                if (endPos == -1) {
                    fileName = contentDisposition.substring(startPos);
                } else {
                    fileName = contentDisposition.substring(startPos, endPos - 1);
                }
            }
            if (fileName.contains("\"")) {
                fileName = fileName.replace("\"", "");
            }
        }

        // Upload file to S3
        FileEntity fileEntity = new FileEntity(fileName, charset, file.length);
        String blobKey = blobStore.asyncStoreMessage(file, fileEntity.getUuid(), System.currentTimeMillis());
        fileEntity.setBlobKey(blobKey);

        fileService.persist(fileEntity);

        JSONObject response = new JSONObject();
        response.put("uuid", fileEntity.getUuid());
        return response;
    }
}
