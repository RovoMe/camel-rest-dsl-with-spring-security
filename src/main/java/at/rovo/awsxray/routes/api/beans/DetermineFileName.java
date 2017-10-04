package at.rovo.awsxray.routes.api.beans;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Headers;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetermineFileName {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Handler
    public void determineFileName(@Headers Map<String, Object> headers) throws JSONException {

        String contentDisposition = (String) headers.get("Content-Disposition");

        String fileName = "unknown";
        if (null != contentDisposition) {
            int startPos = contentDisposition.indexOf("filename=") + "filename=".length();
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

        LOG.info("Received file name determined as {}", fileName);
        headers.put(Exchange.FILE_NAME, fileName);
    }
}
