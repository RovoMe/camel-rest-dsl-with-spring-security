package at.rovo.awsxray.routes.api;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.routes.api.beans.AnalysisResults;
import at.rovo.awsxray.routes.api.beans.ExtractSearchTerms;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileProcessingRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PROCESS_FILE = "seda:process-file";

    @Override
    public void configure() throws Exception {

        from(PROCESS_FILE).routeId("process-file")

                .removeHeader(Exchange.HTTP_URL)
                .removeHeader(Exchange.HTTP_URI)
                .removeHeader(Exchange.HTTP_PATH)

                .bean(ExtractSearchTerms.class)

                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("User-Agent", constant("Mozilla/5.0"))
                .recipientList(simple("https://www.google.at/search?q=${body}"))
                .choice()
                    .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
                        .process((Exchange exchange) -> {

                            Map<String, Integer> terms =
                                    (Map<String, Integer>)exchange.getIn().getHeader(HeaderConstants.SEARCH_TERMS);
                            AnalysisResults result = new AnalysisResults(terms);

                            String response = exchange.getIn().getBody(String.class);
                            Pattern numResults = Pattern.compile("<div.*?id=\"resultStats\".*?>(.*?)</div>");
                            Matcher matcher = numResults.matcher(response);
                            if (matcher.find()) {
                                LOG.info("{} available", matcher.group(1));
                            }

                            // group1 link data
                            // group2 link name
                            // group3 date
                            // group4 article short info
                            String response2 = response.replaceAll("<script.*?</script>", "");
                            response2 = response2.replaceAll("\\s+", " ");
                            Pattern results = Pattern.compile("<div class=\"g\">.*?<h3 class=\"r\"><a href=\"/url\\?q=(.*?)\">(.*?)</a>.*?</h3>.*?<div class=\"s\">.*?<cite>(.*?)</cite>.*?<span class=\"st\">(.*?)</span>");
                            Matcher resultMatcher = results.matcher(response2);

                            while (resultMatcher.find()) {
                                String url = resultMatcher.group(1);
                                String urlDesc = resultMatcher.group(2).replaceAll("<.*?>", "");
                                String cite = resultMatcher.group(3).replaceAll("<.*?>", "");
                                String subhead = resultMatcher.group(4).replaceAll("<.*?>", "");

                                result.addSearchResult(url, urlDesc, cite, subhead);
                            }

                            exchange.getIn().setBody(result);
                        })
                    .otherwise()
                        .log("Retrieving search results failed due to: ${exception.message}")
                .end()

                .log("Processing file ${header.CamelFileName} done");
    }
}
