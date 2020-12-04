package org.recap.report;

import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.repository.jpa.ReportDetailRepository;
import org.recap.util.DateUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by hemalathas on 25/1/17.
 */

public class DeAccessionReportGeneratorUT extends BaseTestCaseUT {

    @InjectMocks
    ReportGenerator reportGenerator;

    @Mock
    ReportDetailRepository reportDetailRepository;

    @Mock
    DateUtil dateUtil;

    @InjectMocks
    FSDeAccessionReportGenerator FSDeAccessionReportGenerator;

    @InjectMocks
    S3DeAccessionReportGenerator S3DeAccessionReportGenerator;

    @Mock
    ProducerTemplate producerTemplate;

    @Test
    public void FSDeAccessionReportGenerator() throws InterruptedException {
        List<ReportEntity> reportEntities = getReportEntity();
        Date createdDate = reportEntities.get(0).getCreatedDate();
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=FSDeAccessionReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        Mockito.when(reportDetailRepository.findByFileAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(getReportEntity());
        String generatedReportFileNameInFileSyatem = reportGenerator.generateReport(RecapCommonConstants.DEACCESSION_REPORT, RecapCommonConstants.PRINCETON, RecapCommonConstants.DEACCESSION_SUMMARY_REPORT, RecapCommonConstants.FILE_SYSTEM, dateUtil.getFromDate(createdDate), dateUtil.getToDate(createdDate));
        assertNotNull(generatedReportFileNameInFileSyatem);
    }

    @Test
    public void FTPDeAccessionReportGenerator() throws InterruptedException {
        List<ReportEntity> reportEntities = getReportEntity();
        Date createdDate = reportEntities.get(0).getCreatedDate();
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface= S3DeAccessionReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        Mockito.when(reportDetailRepository.findByFileAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(getReportEntity());
        String generatedReportFileNameInFileSystem = reportGenerator.generateReport(RecapCommonConstants.DEACCESSION_REPORT, RecapCommonConstants.PRINCETON, RecapCommonConstants.DEACCESSION_SUMMARY_REPORT, RecapCommonConstants.FTP, dateUtil.getFromDate(createdDate), dateUtil.getToDate(createdDate));
        assertNotNull(generatedReportFileNameInFileSystem);
    }

    private List<ReportEntity> getReportEntity(){
        List<ReportEntity> reportEntities = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setFileName(RecapCommonConstants.DEACCESSION_REPORT);
        reportEntity.setType(RecapCommonConstants.DEACCESSION_SUMMARY_REPORT);
        reportEntity.setInstitutionName(RecapCommonConstants.PRINCETON);
        reportEntity.setCreatedDate(new Date());

        List<ReportDataEntity> reportDataEntities = new ArrayList<>();

        ReportDataEntity dateReportDataEntity = new ReportDataEntity();
        dateReportDataEntity.setHeaderName(RecapCommonConstants.DATE_OF_DEACCESSION);
        dateReportDataEntity.setHeaderValue(formatter.format(new Date()));
        reportDataEntities.add(dateReportDataEntity);

        ReportDataEntity owningInstitutionReportDataEntity = new ReportDataEntity();
        owningInstitutionReportDataEntity.setHeaderName(RecapCommonConstants.OWNING_INSTITUTION);
        owningInstitutionReportDataEntity.setHeaderValue(RecapCommonConstants.PRINCETON);
        reportDataEntities.add(owningInstitutionReportDataEntity);

        ReportDataEntity barcodeReportDataEntity = new ReportDataEntity();
        barcodeReportDataEntity.setHeaderName(RecapCommonConstants.BARCODE);
        barcodeReportDataEntity.setHeaderValue("123");
        reportDataEntities.add(barcodeReportDataEntity);

        ReportDataEntity owningInstitutionBibIdReportDataEntity = new ReportDataEntity();
        owningInstitutionBibIdReportDataEntity.setHeaderName(RecapCommonConstants.OWNING_INST_BIB_ID);
        owningInstitutionBibIdReportDataEntity.setHeaderValue("3456");
        reportDataEntities.add(owningInstitutionBibIdReportDataEntity);

        ReportDataEntity collectionGroupCodeReportDataEntity = new ReportDataEntity();
        collectionGroupCodeReportDataEntity.setHeaderName(RecapCommonConstants.COLLECTION_GROUP_CODE);
        collectionGroupCodeReportDataEntity.setHeaderValue("Private");
        reportDataEntities.add(collectionGroupCodeReportDataEntity);

        ReportDataEntity statusReportDataEntity = new ReportDataEntity();
        statusReportDataEntity.setHeaderName(RecapCommonConstants.STATUS);
        statusReportDataEntity.setHeaderValue("Success");
        reportDataEntities.add(statusReportDataEntity);

        reportEntity.setReportDataEntities(reportDataEntities);
        reportEntities.add(reportEntity);
        return reportEntities;
    }

}