package org.recap.report;

import org.apache.camel.ProducerTemplate;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.csv.DeAccessionSummaryRecord;
import org.recap.model.jpa.ReportEntity;
import org.recap.util.DeAccessionSummaryRecordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by chenchulakshmig on 13/10/16.
 */
@Component
public class FSDeAccessionReportGenerator implements ReportGeneratorInterface {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public boolean isInterested(String reportType) {
        return reportType.equalsIgnoreCase(ScsbCommonConstants.DEACCESSION_SUMMARY_REPORT);
    }

    @Override
    public boolean isTransmitted(String transmissionType) {
        return transmissionType.equalsIgnoreCase(ScsbCommonConstants.FILE_SYSTEM);
    }

    @Override
    public String generateReport(String fileName, List<ReportEntity> reportEntityList) {
        String generatedFileName;
        List<DeAccessionSummaryRecord> deAccessionSummaryRecordList = new ArrayList<>();
        DeAccessionSummaryRecordGenerator deAccessionSummaryRecordGenerator = new DeAccessionSummaryRecordGenerator();

        for(ReportEntity reportEntity : reportEntityList) {
            DeAccessionSummaryRecord deAccessionSummaryRecord = deAccessionSummaryRecordGenerator.prepareDeAccessionSummaryReportRecord(reportEntity);
            deAccessionSummaryRecordList.add(deAccessionSummaryRecord);
        }

        producerTemplate.sendBodyAndHeader(ScsbCommonConstants.FS_DE_ACCESSION_SUMMARY_REPORT_Q, deAccessionSummaryRecordList, "fileName", fileName);

        DateFormat df = new SimpleDateFormat(ScsbConstants.DATE_FORMAT_FOR_FILE_NAME);
        generatedFileName = fileName + "-" + df.format(new Date()) + ".csv";
        return generatedFileName;
    }
}
