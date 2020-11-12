package org.recap.report;

import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.repository.jpa.ReportDetailRepository;
import org.recap.util.DateUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by hemalathas on 24/1/17.
 */
public class SubmitCollectionExceptionReportGeneratorUT extends BaseTestCaseUT {

    @InjectMocks
    ReportGenerator reportGenerator;

    @Mock
    ReportDetailRepository reportDetailRepository;

    @Mock
    DateUtil dateUtil;

    @InjectMocks
    FSSubmitCollectionExceptionReportGenerator FSSubmitCollectionExceptionReportGenerator;

    @InjectMocks
    FTPSubmitCollectionExceptionReportGenerator ftpSubmitCollectionExceptionReportGenerator;

    @InjectMocks
    FTPSubmitCollectionSuccessReportGenerator FTPSubmitCollectionSuccessReportGenerator;

    @InjectMocks
    FSSubmitCollectionSuccessReportGenerator FSSubmitCollectionSuccessReportGenerator;

    @InjectMocks
    FTPSubmitCollectionReportGenerator ftpSubmitCollectionReportGenerator;

    @Mock
    ProducerTemplate producerTemplate;

    @Test
    public void testFSSubmitCollectionExceptionReport() throws InterruptedException {
        List<ReportEntity> reportEntityList = saveSubmitCollectionExceptionReport();
        Date createdDate = reportEntityList.get(0).getCreatedDate();
        Mockito.when(reportDetailRepository.findByFileLikeAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(saveSubmitCollectionExceptionReport());
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=FSSubmitCollectionExceptionReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        String generatedReportFileName = reportGenerator.generateReport(RecapCommonConstants.SUBMIT_COLLECTION_REPORT,"PUL", RecapCommonConstants.SUBMIT_COLLECTION_EXCEPTION_REPORT,RecapCommonConstants.FILE_SYSTEM,getFromDate(createdDate), getToDate(createdDate));
        assertNotNull(generatedReportFileName);
    }

    @Test
    public void testFSSubmitCollectionSuccessReportGenerator() throws InterruptedException {
        List<ReportEntity> reportEntityList = saveSubmitCollectionExceptionReport();
        Date createdDate = reportEntityList.get(0).getCreatedDate();
        Mockito.when(reportDetailRepository.findByFileLikeAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(saveSubmitCollectionExceptionReport());
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=FSSubmitCollectionSuccessReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        String generatedReportFileName = reportGenerator.generateReport(RecapCommonConstants.SUBMIT_COLLECTION_REPORT,"PUL", RecapCommonConstants.SUBMIT_COLLECTION_SUCCESS_REPORT,RecapCommonConstants.FILE_SYSTEM,getFromDate(createdDate), getToDate(createdDate));
        assertNotNull(generatedReportFileName);
    }

    @Test
    public void testFTPSubmitCollectionSuccessReportGenerator() throws InterruptedException {
        List<ReportEntity> reportEntityList = saveSubmitCollectionExceptionReport();
        Date createdDate = reportEntityList.get(0).getCreatedDate();
        Mockito.when(reportDetailRepository.findByFileLikeAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(saveSubmitCollectionExceptionReport());
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=FTPSubmitCollectionSuccessReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        String generatedReportFileName = reportGenerator.generateReport(RecapCommonConstants.SUBMIT_COLLECTION_REPORT,"PUL", RecapCommonConstants.SUBMIT_COLLECTION_SUCCESS_REPORT,RecapCommonConstants.FTP,getFromDate(createdDate), getToDate(createdDate));
        assertNotNull(generatedReportFileName);
    }

    @Test
    public void generateReportBasedOnReportRecordNum() throws Exception {
        Mockito.when(reportDetailRepository.findByIdIn(Mockito.anyList())).thenReturn(saveSubmitCollectionExceptionReport());

        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=ftpSubmitCollectionReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        String response=reportGenerator.generateReportBasedOnReportRecordNum(Arrays.asList(1), RecapConstants.SUBMIT_COLLECTION,RecapCommonConstants.FTP);
        assertNotNull(response);
    }

    @Test
    public void testFTPSubmitCollectionExceptionReport() throws InterruptedException {
        List<ReportEntity> reportEntityList = saveSubmitCollectionExceptionReport();
        Date createdDate = reportEntityList.get(0).getCreatedDate();
        Mockito.when(reportDetailRepository.findByFileLikeAndInstitutionAndTypeAndDateRange(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.any(),Mockito.any())).thenReturn(saveSubmitCollectionExceptionReport());
        List<ReportGeneratorInterface> reportGenerators=new ArrayList<>();
        ReportGeneratorInterface reportGeneratorInterface=ftpSubmitCollectionExceptionReportGenerator;
        reportGenerators.add(reportGeneratorInterface);
        ReflectionTestUtils.setField(reportGenerator,"reportGenerators",reportGenerators);
        String generatedReportFileName = reportGenerator.generateReport(RecapCommonConstants.SUBMIT_COLLECTION_REPORT,"PUL", RecapCommonConstants.SUBMIT_COLLECTION_EXCEPTION_REPORT,RecapCommonConstants.FTP, getFromDate(createdDate), getToDate(createdDate));
        assertNotNull(generatedReportFileName);
    }

    public Date getFromDate(Date createdDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(createdDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return  cal.getTime();
    }

    public Date getToDate(Date createdDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(createdDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    private List<ReportEntity> saveSubmitCollectionExceptionReport(){
        List<ReportEntity> reportEntityList = new ArrayList<>();
        List<ReportDataEntity> reportDataEntities = new ArrayList<>();
        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setFileName(RecapCommonConstants.SUBMIT_COLLECTION_REPORT);
        reportEntity.setType(RecapCommonConstants.SUBMIT_COLLECTION_EXCEPTION_REPORT);
        reportEntity.setCreatedDate(new Date());
        reportEntity.setInstitutionName("PUL");

        ReportDataEntity itemBarcodeReportDataEntity = new ReportDataEntity();
        itemBarcodeReportDataEntity.setHeaderName(RecapCommonConstants.SUBMIT_COLLECTION_ITEM_BARCODE);
        itemBarcodeReportDataEntity.setHeaderValue("123");
        reportDataEntities.add(itemBarcodeReportDataEntity);

        ReportDataEntity customerCodeReportDataEntity = new ReportDataEntity();
        customerCodeReportDataEntity.setHeaderName(RecapCommonConstants.SUBMIT_COLLECTION_CUSTOMER_CODE);
        customerCodeReportDataEntity.setHeaderValue("PB");
        reportDataEntities.add(customerCodeReportDataEntity);

        ReportDataEntity owningInstitutionReportDataEntity = new ReportDataEntity();
        owningInstitutionReportDataEntity.setHeaderName(RecapCommonConstants.OWNING_INSTITUTION);
        owningInstitutionReportDataEntity.setHeaderValue("1");
        reportDataEntities.add(owningInstitutionReportDataEntity);

        ReportDataEntity message = new ReportDataEntity();
        message.setHeaderName(RecapCommonConstants.MESSAGE);
        message.setHeaderValue("1");
        reportDataEntities.add(message);

        reportEntity.setReportDataEntities(reportDataEntities);
        reportEntityList.add(reportEntity);
        return reportEntityList;

    }

}