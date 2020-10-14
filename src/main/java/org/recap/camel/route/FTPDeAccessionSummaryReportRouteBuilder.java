package org.recap.camel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.csv.DeAccessionSummaryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by chenchulakshmig on 13/10/16.
 */
@Component
public class FTPDeAccessionSummaryReportRouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FTPDeAccessionSummaryReportRouteBuilder.class);

    /**
     * This method instantiates a new route builder to generate deaccession summary report file to the FTP.
     *
     * @param context         the context
     * @param ftpUserName     the ftp user name
     * @param ftpRemoteServer the ftp remote server
     * @param ftpKnownHost    the ftp known host
     * @param ftpPrivateKey   the ftp private key
     */
    @Autowired
    public FTPDeAccessionSummaryReportRouteBuilder(CamelContext context,
                                                   @Value("${ftp.server.userName}") String ftpUserName, @Value("${ftp.etl.reports.dir}") String ftpRemoteServer,
                                                   @Value("${ftp.server.knownHost}") String ftpKnownHost, @Value("${ftp.server.privateKey}") String ftpPrivateKey) {
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(RecapConstants.FTP_DE_ACCESSION_SUMMARY_REPORT_Q)
                            .routeId(RecapConstants.FTP_DE_ACCESSION_SUMMARY_REPORT_ID)
                            .marshal().bindy(BindyType.Csv, DeAccessionSummaryRecord.class)
                            .to("sftp://" + ftpUserName + "@" + ftpRemoteServer + "?privateKeyFile=" + ftpPrivateKey + "&knownHostsFile=" + ftpKnownHost + "&fileName=${in.header.fileName}-${date:now:ddMMMyyyyHHmmss}.csv&fileExist=append");
                }
            });
        } catch (Exception e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
    }
}
