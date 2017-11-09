package at.rovo.awsxray.routes.api;

import org.apache.camel.builder.RouteBuilder;

public class SqlQueryRoute extends RouteBuilder {

    public static final String SQL_QUERY= "seda:sql-query";

    @Override
    public void configure() throws Exception {
        from(SQL_QUERY).routeId("sql-query")
                .log("Performing sample Camel-based SQL query")
                .to("sql:select * from audit?outputType=StreamList&outputClass=org.apache.camel.component.sql.ProjectModel&dataSource=#messageDataSource")
                .to("log:stream")
                .split(body())
                .streaming()
                .to("log:row");
    }
}
