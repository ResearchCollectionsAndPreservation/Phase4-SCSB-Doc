package org.recap.camel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.recap.RecapCommonConstants;
import org.recap.model.csv.SolrExceptionReportReCAPCSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Created by angelind on 30/9/16.
 */
@Component
public class CSVSolrExceptionRecordRouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CSVSolrExceptionRecordRouteBuilder.class);

    /**
     * This route builder is used to generate solr exception csv report to the file system.
     *
     * @param context                  the context
     * @param solrReportsDirectory the matching reports directory
     */
    @Autowired
    public CSVSolrExceptionRecordRouteBuilder(CamelContext context, @Value("${solr.exception.report.directory}") String solrReportsDirectory) {
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(RecapCommonConstants.CSV_SOLR_EXCEPTION_REPORT_Q)
                            .routeId(RecapCommonConstants.CSV_SOLR_EXCEPTION_REPORT_ROUTE_ID)
                            .marshal().bindy(BindyType.Csv, SolrExceptionReportReCAPCSVRecord.class)
                            .to("file:" + solrReportsDirectory + File.separator + "?fileName=${in.header.fileName}-${date:now:ddMMMyyyy}.csv")
                            .onCompletion().log("File has been created successfully.");
                }
            });
        } catch (Exception e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
    }
}
