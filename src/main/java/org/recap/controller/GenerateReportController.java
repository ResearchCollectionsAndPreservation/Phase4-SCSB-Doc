package org.recap.controller;

import org.codehaus.plexus.util.StringUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.solr.SolrIndexRequest;
import org.recap.report.ReportGenerator;
import org.recap.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Date;

/**
 * Created by angelind on 11/11/16.
 */
@Controller
public class GenerateReportController {

    private static final Logger logger = LoggerFactory.getLogger(GenerateReportController.class);

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private DateUtil dateUtil;

    /**
     * This method is used to generate reports appropriately depending on the report type selected in UI.
     *
     * @param solrIndexRequest the solr index request
     * @param result           the result
     * @param model            the model
     * @return the string
     */
    @ResponseBody
    @PostMapping(value = "/reportGeneration/generateReports")
    public String generateReports(@Valid @ModelAttribute("solrIndexRequest") SolrIndexRequest solrIndexRequest,
                                  BindingResult result,
                                  Model model) {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Date createdDate = solrIndexRequest.getCreatedDate();
        if (createdDate == null) {
            createdDate = new Date();
        }
        Date toDate = solrIndexRequest.getToDate();
        if (toDate == null) {
            toDate = new Date();
        }
        String reportType = solrIndexRequest.getReportType();
        String generatedReportFileName;
        String owningInstitutionCode = solrIndexRequest.getOwningInstitutionCode();
        String status;
        String fileName;
        if (reportType.equalsIgnoreCase(ScsbCommonConstants.DEACCESSION_SUMMARY_REPORT)) {
            fileName = ScsbCommonConstants.DEACCESSION_REPORT;
        } else if (reportType.equalsIgnoreCase(ScsbCommonConstants.ACCESSION_SUMMARY_REPORT) || reportType.equalsIgnoreCase(ScsbConstants.ONGOING_ACCESSION_REPORT)) {
            fileName = ScsbCommonConstants.ACCESSION_REPORT;
        } else if (reportType.equalsIgnoreCase(ScsbCommonConstants.SUBMIT_COLLECTION_REJECTION_REPORT)
                || reportType.equalsIgnoreCase(ScsbCommonConstants.SUBMIT_COLLECTION_EXCEPTION_REPORT)
                || reportType.equalsIgnoreCase(ScsbCommonConstants.SUBMIT_COLLECTION_SUCCESS_REPORT)
                || reportType.equalsIgnoreCase(ScsbCommonConstants.SUBMIT_COLLECTION_FAILURE_REPORT)
                || reportType.equalsIgnoreCase(ScsbConstants.SUBMIT_COLLECTION_SUMMARY_REPORT)) {
            fileName = ScsbCommonConstants.SUBMIT_COLLECTION_REPORT;
        } else {
            fileName = ScsbCommonConstants.SOLR_INDEX_FAILURE_REPORT;
        }
        generatedReportFileName = reportGenerator.generateReport(fileName, owningInstitutionCode, reportType, solrIndexRequest.getTransmissionType(), dateUtil.getFromDate(createdDate), dateUtil.getToDate(toDate));
        if (StringUtils.isEmpty(generatedReportFileName)) {
            status = "Report wasn't generated! Please contact help desk!";
        } else {
            status = "The Generated Report File Name : " + generatedReportFileName;
        }
        stopWatch.stop();
        logger.info("Total time taken to generate File : {}" , stopWatch.getTotalTimeSeconds());
        return status;
    }

}
