package org.recap.report;

import org.apache.camel.ProducerTemplate;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.jpa.ReportEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Created by hemalathas on 21/12/16.
 */
@Component
public class S3SubmitCollectionExceptionReportGenerator extends CommonReportGenerator implements ReportGeneratorInterface {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public boolean isInterested(String reportType) {
        return reportType.equalsIgnoreCase(RecapCommonConstants.SUBMIT_COLLECTION_EXCEPTION_REPORT);
    }

    @Override
    public boolean isTransmitted(String transmissionType) {
        return transmissionType.equalsIgnoreCase(RecapCommonConstants.FTP);
    }

    @Override
    public String generateReport(String fileName, List<ReportEntity> reportEntityList) {
        return generateSubmitCollectionReportFile(fileName, reportEntityList, RecapConstants.FTP_SUBMIT_COLLECTION_EXCEPTION_REPORT_Q);
    }
}
