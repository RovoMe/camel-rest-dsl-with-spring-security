package at.rovo.awsxray.routes;

import at.rovo.awsxray.routes.beans.AuditMapper;
import org.apache.camel.builder.RouteBuilder;

public class SqlQueryRoute extends RouteBuilder {

    public static final String SQL_QUERY= "seda:sql-query";

    @Override
    public void configure() throws Exception {
        from(SQL_QUERY).routeId("sql-query")
                .log("Performing sample Camel-based SQL query")
                .to("sql:SELECT *  FROM audit")
                .bean(AuditMapper.class)
                .log("${body}");
    }
}
