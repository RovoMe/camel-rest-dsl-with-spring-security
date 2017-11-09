package at.rovo.awsxray.routes.api;

import at.rovo.awsxray.HeaderConstants;
import at.rovo.awsxray.domain.entities.mongo.FileEntity;
import at.rovo.awsxray.routes.api.beans.AnalysisResults;
import at.rovo.awsxray.routes.api.beans.DetermineFileName;
import at.rovo.awsxray.routes.api.beans.GetFile;
import at.rovo.awsxray.routes.api.beans.ListFiles;
import at.rovo.awsxray.routes.api.beans.StoreFile;
import at.rovo.awsxray.routes.beans.LogUserCompany;
import at.rovo.awsxray.security.SpringSecurityContextLoader;
import org.apache.camel.Exchange;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.http.MediaType;

import static at.rovo.awsxray.routes.api.SqlQueryRoute.SQL_QUERY;

public class SampleFileRoute extends BaseAPIRouteBuilder {

    @Override
    protected void defineRoute() throws Exception {

        rest("/files")

                .get("/")
                    .bindingMode(RestBindingMode.json)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .description("Sample service which lists uploaded files")

                    .param()
                        .name("limit")
                        .type(RestParamType.query)
                        .dataType("int")
                        .defaultValue("30")
                        .required(false)
                        .description("Optional parameter that specifies the maximum number of entries to return")
                    .endParam()
                    .param()
                        .name("offset")
                        .type(RestParamType.query)
                        .dataType("int")
                        .defaultValue("0")
                        .required(false)
                        .description("Optional parameter that specifies the starting position to return entries from")
                    .endParam()

                    .route().routeId("api-available-files")
                        .log("Processing list files request")
                        .bean(SpringSecurityContextLoader.class)
                        .policy("authenticated")
                        .bean(LogUserCompany.class)
                        .bean(ListFiles.class)
                        .log("List files request processed")
                .endRest()

                .get("/{file_uuid}")
                    .bindingMode(RestBindingMode.json)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .description("Sample external service invocation")

                    .param()
                        .name("file_uuid")
                        .type(RestParamType.path)
                        .dataType("string")
                        .defaultValue(null)
                        .required(true)
                        .description("Unique identifier of the file requested")
                    .endParam()

                    .route().routeId("api-retrieve-file")
                        .log("Processing get file request")
                        .bean(SpringSecurityContextLoader.class)
                        .policy("authenticated")
                        .bean(LogUserCompany.class)
                        .bean(GetFile.class)
                        .log("Get file request processed")
                .endRest()

                .post("/")
                    .consumes(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .description("Sample file upload endpoint")

                    .route().routeId("api-file-upload")
                        .log("Processing incoming file")
                        .inOnly(SQL_QUERY)
                        .bean(SpringSecurityContextLoader.class)
                        .policy("authenticated")
                        .bean(LogUserCompany.class)
                        .bean(DetermineFileName.class)
                        .enrich(FileProcessingRoute.PROCESS_FILE, (Exchange oldExchange, Exchange newExchange) -> {

                                String charset = (String)oldExchange.getIn().getHeader(Exchange.CHARSET_NAME);
                                String fileName = (String)oldExchange.getIn().getHeader(Exchange.FILE_NAME);
                                byte[] file = oldExchange.getIn().getBody(byte[].class);

                                FileEntity fileEntity = new FileEntity(fileName, charset, file.length);
                                oldExchange.getIn().setHeader(HeaderConstants.FILE_ID, fileEntity.getUuid());
                                fileEntity.setRawContent(file);

                                AnalysisResults results = newExchange.getIn().getBody(AnalysisResults.class);
                                fileEntity.setFileTerms(results.getTerms());
                                results.getSearchResults().forEach(data -> fileEntity.addSearchResult(data.getUrl(), data.getUrlDescription(), data.getCite(), data.getSubhead()));

                                oldExchange.getIn().setBody(fileEntity);
                                return oldExchange;
                        })
                        .bean(StoreFile.class)
                        .log("File upload completed")
                .endRest();
    }
}
