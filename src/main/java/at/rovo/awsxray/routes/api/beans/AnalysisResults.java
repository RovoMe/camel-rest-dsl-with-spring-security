package at.rovo.awsxray.routes.api.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.camel.component.aws.xray.XRayTrace;

@Getter
@XRayTrace
public class AnalysisResults {

    private final Map<String, Integer> terms;
    private final List<AnalyzedFileData> searchResults = new ArrayList<>();

    public AnalysisResults(Map<String, Integer> terms) {
        this.terms = Collections.unmodifiableMap(terms);
    }

    public void addSearchResult(String url, String urlDescription, String cite, String subhead) {
        searchResults.add(new AnalyzedFileData(url, urlDescription, cite, subhead));
    }

    @Getter
    public static class AnalyzedFileData {

        private final String url;
        private final String urlDescription;
        private final String cite;
        private final String subhead;

        private AnalyzedFileData(String url, String urlDescription, String cite, String subhead) {
            this.url = url;
            this.urlDescription = urlDescription;
            this.cite = cite;
            this.subhead = subhead;
        }
    }
}
