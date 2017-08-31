package at.rovo.awsxray.routes.api;

import at.rovo.awsxray.routes.api.beans.GetFile;
import at.rovo.awsxray.routes.api.beans.ListFiles;
import at.rovo.awsxray.routes.api.beans.StoreFile;
import at.rovo.awsxray.security.SpringSecurityContextLoader;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.http.MediaType;

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
                        .bean(GetFile.class)
                        .log("Get file request processed")
                .endRest()

                .post("/")
                    .consumes(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .description("Sample file upload endpoint")

                    .route().routeId("api-file-upload")
                        .log("Processing incoming file")
                        .bean(SpringSecurityContextLoader.class)
                        .policy("authenticated")
                        .bean(StoreFile.class)
                        .log("File upload completed")
                .endRest();
    }
}
