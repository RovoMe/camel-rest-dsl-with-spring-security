package at.rovo.awsxray.routes.api.beans;

import at.rovo.awsxray.HeaderConstants;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws.xray.XRayTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XRayTrace
public class ExtractSearchTerms {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private ProducerTemplate producerTemplate;

    private static List<String> stopWords = new ArrayList<>();

    @Handler
    public String extractSearchTermsFromFileContent(@Body String body, @Header(Exchange.FILE_NAME) String fileName, Exchange exchange) {

        if (null == body) {
            LOG.info("Not content to parse in file {}", fileName);
            return "";
        }

        if (stopWords.isEmpty()) {
            LOG.debug("Filling stop word list");
            stopWords.addAll(getStopWords());
            LOG.debug("Added {} new stop words", stopWords.size());
        }

        String[] tokens = parseContent(body);
        Map<String, Integer> countTokens = new HashMap<>();
        for (String token : tokens) {
            token = token.replaceAll("[^A-Za-z0-9]", "");
            token = token.toLowerCase();
            if (!stopWords.contains(token)) {
                Integer count = countTokens.get(token);
                countTokens.put(token, null == count ? 1 : ++count);
            }
        }

        Map<String, Integer> top5 = sortByValue(countTokens)
                                    .limit(5)
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        exchange.getIn().setHeader(HeaderConstants.SEARCH_TERMS, top5);
        LOG.info("Top 5 words of file {}: {}", fileName, top5);

        return top5.entrySet().stream().map(entry -> entry.getKey()).collect(Collectors.joining("+"));
    }

    private String[] parseContent(String content) {
        return content.split("\\s+");
    }

    private List<String> getStopWords() {
        List<String> stopWords = new ArrayList<>();
        Exchange exchange = producerTemplate.send("https://www.ranks.nl/stopwords",
                                                  (Exchange _exchange) -> { });
        String response = exchange.getOut().getBody(String.class);
        parseResponse(response);

        return stopWords;
    }

    private void parseResponse(String response) {
        int segmentStart = response.indexOf("Long Stopword List");
        if (segmentStart > -1) {
            int start = response.indexOf("<table", segmentStart);
            int stop = response.indexOf("</table>", segmentStart);
            String table = response.substring(start, stop);
            Pattern column = Pattern.compile("<td .*?>(.*?)</td>");
            Matcher matcher = column.matcher(table);
            while (matcher.find()) {
                String columnEntries = matcher.group(1);
                String[] _stopWords = columnEntries.split("(?:<br>|<br/>|<br />)");
                stopWords.addAll(Arrays.stream(_stopWords).map(String::toLowerCase).collect(Collectors.toList()));
            }
        }
    }

    private Stream<Map.Entry<String, Integer>> sortByValue(Map<String, Integer> map) {
//        List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
//        list.sort(Comparator.comparing(Map.Entry::getValue));
//
//        Map<String, Integer> results = new LinkedHashMap<>();
//        list.forEach(entry -> results.put(entry.getKey(), entry.getValue()));
//        return results;

        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    }
}
