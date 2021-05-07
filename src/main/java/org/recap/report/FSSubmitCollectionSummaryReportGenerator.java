package org.recap.report;

import org.apache.camel.ProducerTemplate;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.ReportEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Created by rajeshbabuk on 20/7/17.
 */
@Component
public class FSSubmitCollectionSummaryReportGenerator extends CommonReportGenerator implements ReportGeneratorInterface {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public boolean isInterested(String reportType) {
        return reportType.equalsIgnoreCase(ScsbCommonConstants.SUBMIT_COLLECTION_SUMMARY);
    }

    @Override
    public boolean isTransmitted(String transmissionType) {
        return transmissionType.equalsIgnoreCase(ScsbCommonConstants.FILE_SYSTEM);
    }

    @Override
    public String generateReport(String fileName, List<ReportEntity> reportEntityList) {
        return generateSubmitCollectionReportFile(fileName, reportEntityList, ScsbConstants.FS_SUBMIT_COLLECTION_SUMMARY_REPORT_Q);
    }
}
