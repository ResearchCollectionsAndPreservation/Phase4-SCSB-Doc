package org.recap.report;

import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.ReportEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Created by angelind on 30/9/16.
 */
@Component
public class S3SolrExceptionReportGenerator extends CommonReportGenerator implements ReportGeneratorInterface{

    @Override
    public boolean isInterested(String reportType) {
        return reportType.equalsIgnoreCase(ScsbCommonConstants.SOLR_INDEX_EXCEPTION);
    }

    @Override
    public boolean isTransmitted(String transmissionType) {
        return transmissionType.equalsIgnoreCase(ScsbCommonConstants.FTP);
    }

    @Override
    public String generateReport(String fileName, List<ReportEntity> reportEntityList) {
        return generateReportForSolrExceptionCsvRecords(fileName, ScsbCommonConstants.FTP_SOLR_EXCEPTION_REPORT_Q, reportEntityList);
    }
}
