package at.rovo.awsxray.routes.beans;

import org.apache.camel.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuditMapper {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Handler
    public List<String> readAudits(List<Map<String, Object>> dataList) {
        LOG.debug("data: {}", dataList);
        List<String> audits = new ArrayList<>();
        int entryNum = 1;
        for (Map<String, Object> data : dataList) {
            StringBuilder sb = new StringBuilder();
            sb.append("Entry #").append(entryNum).append(" [");
            int initialSize = sb.length();
            for (String key : data.keySet()) {
                LOG.debug("Entry #{} with key: {}, value: {}", entryNum, key, data.get(key));
                if (sb.length() > initialSize) {
                    sb.append(", ");
                }
                sb.append(key).append(": ").append(data.get(key));
            }
            sb.append("]");
            audits.add(sb.toString());
            entryNum++;
        }
        return audits;
    }
}
