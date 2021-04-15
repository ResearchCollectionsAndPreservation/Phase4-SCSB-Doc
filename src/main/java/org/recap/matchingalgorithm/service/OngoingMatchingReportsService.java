package org.recap.matchingalgorithm.service;

import com.google.common.collect.Lists;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.matchingalgorithm.MatchingCounter;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.model.matchingreports.MatchingSerialAndMVMReports;
import org.recap.model.matchingreports.MatchingSummaryReport;
import org.recap.model.matchingreports.TitleExceptionReport;
import org.recap.model.search.SearchRecordsRequest;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ReportDetailRepository;
import org.recap.util.CsvUtil;
import org.recap.util.DateUtil;
import org.recap.util.OngoingMatchingAlgorithmReportGenerator;
import org.recap.util.PropertyUtil;
import org.recap.util.SolrQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.recap.RecapConstants.MATCHING_COUNTER_OPEN;
import static org.recap.RecapConstants.MATCHING_COUNTER_SHARED;

/**
 * Created by angelind on 21/6/17.
 */
@Service
public class OngoingMatchingReportsService {

    private static final Logger logger= LoggerFactory.getLogger(OngoingMatchingReportsService.class);

    @Autowired
    private ReportDetailRepository reportDetailRepository;

    @Autowired
    private DateUtil dateUtil;

    @Autowired
    private CsvUtil csvUtil;

    @Value("${ongoing.matching.report.directory}")
    private String matchingReportsDirectory;

    @Resource(name = "recapSolrTemplate")
    private SolrTemplate solrTemplate;

    @Autowired
    private SolrQueryBuilder solrQueryBuilder;

    /**
     * The Camel context.
     */
    @Autowired
    CamelContext camelContext;

    /**
     * The Institution details repository.
     */
    @Autowired
    InstitutionDetailsRepository institutionDetailsRepository;

    /**
     * The Producer template.
     */
    @Autowired
    ProducerTemplate producerTemplate;

    @Autowired
    PropertyUtil propertyUtil;

    public static Logger getLogger() {
        return logger;
    }

    public ReportDetailRepository getReportDetailRepository() {
        return reportDetailRepository;
    }

    public DateUtil getDateUtil() {
        return dateUtil;
    }

    public CsvUtil getCsvUtil() {
        return csvUtil;
    }

    public String getMatchingReportsDirectory() {
        return matchingReportsDirectory;
    }

    public SolrTemplate getSolrTemplate() {
        return solrTemplate;
    }

    public SolrQueryBuilder getSolrQueryBuilder() {
        return solrQueryBuilder;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public InstitutionDetailsRepository getInstitutionDetailsRepository() {
        return institutionDetailsRepository;
    }

    public ProducerTemplate getProducerTemplate() {
        return producerTemplate;
    }

    /**
     * Generate title exception report string.
     *
     * @param createdDate the created date
     * @param batchSize   the batch size
     * @return the string
     */
    public String generateTitleExceptionReport(Date createdDate, Integer batchSize) {
        Page<ReportEntity> reportEntityPage = getReportDetailRepository().findByFileAndTypeAndDateRangeWithPaging(PageRequest.of(0, batchSize), RecapCommonConstants.ONGOING_MATCHING_ALGORITHM, RecapConstants.TITLE_EXCEPTION_TYPE,
                getDateUtil().getFromDate(createdDate), getDateUtil().getToDate(createdDate));
        int totalPages = reportEntityPage.getTotalPages();
        List<TitleExceptionReport> titleExceptionReports = new ArrayList<>();
        int maxTitleCount = 0;
        maxTitleCount = getTitleExceptionReport(reportEntityPage.getContent(), titleExceptionReports, maxTitleCount);
        for(int pageNum=1; pageNum<totalPages; pageNum++) {
            reportEntityPage = getReportDetailRepository().findByFileAndTypeAndDateRangeWithPaging(PageRequest.of(pageNum, batchSize), RecapCommonConstants.ONGOING_MATCHING_ALGORITHM, RecapConstants.TITLE_EXCEPTION_TYPE,
                    getDateUtil().getFromDate(createdDate), getDateUtil().getToDate(createdDate));
            maxTitleCount = getTitleExceptionReport(reportEntityPage.getContent(), titleExceptionReports, maxTitleCount);
        }
        File file = null;
        if(CollectionUtils.isNotEmpty(titleExceptionReports)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(RecapConstants.DATE_FORMAT_FOR_REPORTS);
                String formattedDate = sdf.format(new Date());
                String fileNameWithExtension = getMatchingReportsDirectory() + File.separator + RecapConstants.TITLE_EXCEPTION_REPORT + RecapConstants.UNDER_SCORE + formattedDate + RecapConstants.CSV_EXTENSION;
                file = getCsvUtil().createTitleExceptionReportFile(fileNameWithExtension, maxTitleCount, titleExceptionReports);
                getCamelContext().getRouteController().startRoute(RecapConstants.FTP_TITLE_EXCEPTION_REPORT_ROUTE_ID);
            } catch (Exception e) {
                getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
            }
        }
        return file != null ? file.getName() : null;
    }


    private int getTitleExceptionReport(List<ReportEntity> reportEntities, List<TitleExceptionReport> titleExceptionReports, int maxTitleCount) {
        if(CollectionUtils.isNotEmpty(reportEntities)) {
            for(ReportEntity reportEntity : reportEntities) {
                List<ReportDataEntity> reportDataEntities = new ArrayList<>();
                List<String> titleList = new ArrayList<>();
                for(ReportDataEntity reportDataEntity : reportEntity.getReportDataEntities()) {
                    String headerName = reportDataEntity.getHeaderName();
                    String headerValue = reportDataEntity.getHeaderValue();
                    if(headerName.contains("Title")) {
                        titleList.add(headerValue);
                    } else {
                        reportDataEntities.add(reportDataEntity);
                    }
                }
                int size = titleList.size();
                if(maxTitleCount < size) {
                    maxTitleCount = size;
                }
                OngoingMatchingAlgorithmReportGenerator ongoingMatchingAlgorithmReportGenerator = new OngoingMatchingAlgorithmReportGenerator();
                TitleExceptionReport titleExceptionReport = ongoingMatchingAlgorithmReportGenerator.prepareTitleExceptionReportRecord(reportDataEntities);
                titleExceptionReport.setTitleList(titleList);
                titleExceptionReports.add(titleExceptionReport);
            }
        }
        return maxTitleCount;
    }

    /**
     * Generate serial and mvms report.
     *
     * @param serialMvmBibIds the serial mvm bib ids
     */
    public void generateSerialAndMVMsReport(List<Integer> serialMvmBibIds) {
        List<MatchingSerialAndMVMReports> matchingSerialAndMvmReports = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(serialMvmBibIds)) {
            List<List<Integer>> bibIdLists = Lists.partition(serialMvmBibIds, 100);
            for(List<Integer> bibIds : bibIdLists) {
                String bibIdQuery = RecapCommonConstants.BIB_ID + ":" + "(" + StringUtils.join(bibIds, " ") + ")";
                SolrQuery solrQuery = new SolrQuery(bibIdQuery);
                String[] fieldNameList = {RecapConstants.TITLE_SUBFIELD_A, RecapCommonConstants.BIB_ID, RecapCommonConstants.BIB_OWNING_INSTITUTION, RecapCommonConstants.OWNING_INST_BIB_ID, RecapCommonConstants.ROOT};
                solrQuery.setFields(fieldNameList);
                solrQuery.setRows(100);
                try {
                    QueryResponse queryResponse = getSolrTemplate().getSolrClient().query(solrQuery);
                    SolrDocumentList solrDocumentList = queryResponse.getResults();
                    for (Iterator<SolrDocument> iterator = solrDocumentList.iterator(); iterator.hasNext(); ) {
                        SolrDocument solrDocument = iterator.next();
                        matchingSerialAndMvmReports.addAll(getMatchingSerialAndMvmReports(solrDocument));
                    }
                } catch (Exception e) {
                    getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(matchingSerialAndMvmReports)) {
            try {
                getCamelContext().getRouteController().startRoute(RecapConstants.FTP_SERIAL_MVM_REPORT_ROUTE_ID);
                getProducerTemplate().sendBodyAndHeader(RecapConstants.FTP_SERIAL_MVM_REPORT_Q, matchingSerialAndMvmReports, RecapConstants.FILE_NAME, RecapConstants.MATCHING_SERIAL_MVM_REPORT);
            } catch (Exception e) {
                getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
            }
        }
    }

    private List<MatchingSerialAndMVMReports> getMatchingSerialAndMvmReports(SolrDocument solrDocument) {

        List<MatchingSerialAndMVMReports> matchingSerialAndMVMReportsList = new ArrayList<>();
        SolrQuery solrQueryForChildDocuments = getSolrQueryBuilder().getSolrQueryForBibItem(RecapCommonConstants.ROOT + ":" + solrDocument.getFieldValue(RecapCommonConstants.ROOT));
        solrQueryForChildDocuments.setFilterQueries(RecapCommonConstants.DOCTYPE + ":" + "(\"" + RecapCommonConstants.HOLDINGS + "\" \"" + RecapCommonConstants.ITEM + "\")");
        String[] fieldNameList = {RecapConstants.VOLUME_PART_YEAR, RecapCommonConstants.HOLDING_ID, RecapConstants.SUMMARY_HOLDINGS, RecapCommonConstants.BARCODE,
                RecapConstants.USE_RESTRICTION_DISPLAY, RecapCommonConstants.ITEM_ID, RecapCommonConstants.ROOT, RecapCommonConstants.DOCTYPE, RecapCommonConstants.HOLDINGS_ID,
                RecapCommonConstants.IS_DELETED_ITEM, RecapConstants.ITEM_CATALOGING_STATUS};
        solrQueryForChildDocuments.setFields(fieldNameList);
        solrQueryForChildDocuments.setSort(RecapCommonConstants.DOCTYPE, SolrQuery.ORDER.asc);
        QueryResponse queryResponse = null;
        try {
            queryResponse = getSolrTemplate().getSolrClient().query(solrQueryForChildDocuments);
            SolrDocumentList solrDocuments = queryResponse.getResults();
            if (solrDocuments.getNumFound() > 10) {
                solrQueryForChildDocuments.setRows((int) solrDocuments.getNumFound());
                queryResponse = getSolrTemplate().getSolrClient().query(solrQueryForChildDocuments);
                solrDocuments = queryResponse.getResults();
            }
            Map<Integer, String> holdingsMap = new HashMap<>();
            for (Iterator<SolrDocument> iterator = solrDocuments.iterator(); iterator.hasNext(); ) {
                SolrDocument solrChildDocument =  iterator.next();
                String docType = (String) solrChildDocument.getFieldValue(RecapCommonConstants.DOCTYPE);
                if(docType.equalsIgnoreCase(RecapCommonConstants.HOLDINGS)) {
                    holdingsMap.put((Integer) solrChildDocument.getFieldValue(RecapCommonConstants.HOLDING_ID),
                            String.valueOf(solrChildDocument.getFieldValue(RecapConstants.SUMMARY_HOLDINGS)));
                }
                if(docType.equalsIgnoreCase(RecapCommonConstants.ITEM)) {
                    boolean isDeletedItem = (boolean) solrChildDocument.getFieldValue(RecapCommonConstants.IS_DELETED_ITEM);
                    String itemCatalogingStatus = String.valueOf(solrChildDocument.getFieldValue(RecapConstants.ITEM_CATALOGING_STATUS));
                    if(!isDeletedItem && itemCatalogingStatus.equalsIgnoreCase(RecapCommonConstants.COMPLETE_STATUS)) {
                        MatchingSerialAndMVMReports matchingSerialAndMVMReports = new MatchingSerialAndMVMReports();
                        matchingSerialAndMVMReports.setTitle(String.valueOf(solrDocument.getFieldValue(RecapConstants.TITLE_SUBFIELD_A)));
                        matchingSerialAndMVMReports.setBibId(String.valueOf(solrDocument.getFieldValue(RecapCommonConstants.BIB_ID)));
                        matchingSerialAndMVMReports.setOwningInstitutionId(String.valueOf(solrDocument.getFieldValue(RecapCommonConstants.BIB_OWNING_INSTITUTION)));
                        matchingSerialAndMVMReports.setOwningInstitutionBibId(String.valueOf(solrDocument.getFieldValue(RecapCommonConstants.OWNING_INST_BIB_ID)));
                        matchingSerialAndMVMReports.setBarcode(String.valueOf(solrChildDocument.getFieldValue(RecapCommonConstants.BARCODE)));
                        matchingSerialAndMVMReports.setVolumePartYear(String.valueOf(solrChildDocument.getFieldValue(RecapConstants.VOLUME_PART_YEAR)));
                        matchingSerialAndMVMReports.setUseRestriction(String.valueOf(solrChildDocument.getFieldValue(RecapConstants.USE_RESTRICTION_DISPLAY)));
                        List<Integer> holdingsIds = (List<Integer>) solrChildDocument.getFieldValue(RecapCommonConstants.HOLDINGS_ID);
                        Integer holdingsId = holdingsIds.get(0);
                        matchingSerialAndMVMReports.setSummaryHoldings(holdingsMap.get(holdingsId));
                        matchingSerialAndMVMReportsList.add(matchingSerialAndMVMReports);
                    }
                }
            }
        }catch (Exception e) {
            getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
        }
        return matchingSerialAndMVMReportsList;
    }

    /**
     * Populate summary report list.
     *
     * @return the list
     */
    public List<MatchingSummaryReport> populateSummaryReportBeforeMatching() {
        List<MatchingSummaryReport> matchingSummaryReports = new ArrayList<>();
        List<String> allInstitutionCodeExceptHTC = institutionDetailsRepository.findAllInstitutionCodeExceptHTC();
        for (String institutionCode : allInstitutionCodeExceptHTC) {
            MatchingSummaryReport matchingSummaryReport = new MatchingSummaryReport();
            matchingSummaryReport.setInstitution(institutionCode);
            matchingSummaryReport.setOpenItemsBeforeMatching(String.valueOf(MatchingCounter.getSpecificInstitutionCounterMap(institutionCode).get(MATCHING_COUNTER_OPEN)));
            matchingSummaryReport.setSharedItemsBeforeMatching(String.valueOf(MatchingCounter.getSpecificInstitutionCounterMap(institutionCode).get(MATCHING_COUNTER_SHARED)));
            matchingSummaryReports.add(matchingSummaryReport);
        }
        return matchingSummaryReports;
    }

    /**
     * Generate summary report.
     *
     * @param matchingSummaryReports the matching summary reports
     */
    public void generateSummaryReport(List<MatchingSummaryReport> matchingSummaryReports) {
        SearchRecordsRequest searchRecordsRequest = new SearchRecordsRequest(propertyUtil.getAllInstitutions());
        Integer bibCount = 0;
        Integer itemCount = 0;
        SolrQuery bibCountQuery = getSolrQueryBuilder().getCountQueryForParentAndChildCriteria(searchRecordsRequest);
        SolrQuery itemCountQuery = getSolrQueryBuilder().getCountQueryForChildAndParentCriteria(searchRecordsRequest);
        bibCountQuery.setRows(0);
        itemCountQuery.setRows(0);
        try {
            QueryResponse queryResponseForBib = getSolrTemplate().getSolrClient().query(bibCountQuery);
            QueryResponse queryResponseForItem = getSolrTemplate().getSolrClient().query(itemCountQuery);
            bibCount = Math.toIntExact(queryResponseForBib.getResults().getNumFound());
            itemCount = Math.toIntExact(queryResponseForItem.getResults().getNumFound());
        } catch (Exception e) {
            getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
        }
        try {
            for(MatchingSummaryReport matchingSummaryReport : matchingSummaryReports) {
                matchingSummaryReport.setTotalBibs(String.valueOf(bibCount));
                matchingSummaryReport.setTotalItems(String.valueOf(itemCount));
                String openItemsAfterMatching = "";
                String sharedItemsAfterMatching = "";
                List<String> institutions = institutionDetailsRepository.findAllInstitutionCodeExceptHTC();
                for (String institution : institutions) {
                    if (matchingSummaryReport.getInstitution().equalsIgnoreCase(institution)) {
                        openItemsAfterMatching = String.valueOf(MatchingCounter.getSpecificInstitutionCounterMap(institution).get(MATCHING_COUNTER_OPEN));
                        sharedItemsAfterMatching = String.valueOf(MatchingCounter.getSpecificInstitutionCounterMap(institution).get(MATCHING_COUNTER_SHARED));
                    }
                }
                String openItemsDiff = String.valueOf(Integer.valueOf(openItemsAfterMatching) - Integer.valueOf(matchingSummaryReport.getOpenItemsBeforeMatching()));
                String sharedItemsDiff = String.valueOf(Integer.valueOf(sharedItemsAfterMatching) - Integer.valueOf(matchingSummaryReport.getSharedItemsBeforeMatching()));
                matchingSummaryReport.setOpenItemsDiff(openItemsDiff);
                matchingSummaryReport.setSharedItemsDiff(sharedItemsDiff);
                matchingSummaryReport.setOpenItemsAfterMatching(openItemsAfterMatching);
                matchingSummaryReport.setSharedItemsAfterMatching(sharedItemsAfterMatching);
            }
            getCamelContext().getRouteController().startRoute(RecapConstants.FTP_MATCHING_SUMMARY_REPORT_ROUTE_ID);
            getProducerTemplate().sendBodyAndHeader(RecapConstants.FTP_MATCHING_SUMMARY_REPORT_Q, matchingSummaryReports, RecapConstants.FILE_NAME, RecapConstants.MATCHING_SUMMARY_REPORT);
        } catch (Exception e) {
            getLogger().error(RecapConstants.EXCEPTION_TEXT + RecapConstants.LOGGER_MSG, e);
        }
    }
}
