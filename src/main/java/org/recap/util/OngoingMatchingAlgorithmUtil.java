package org.recap.util;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.matchingalgorithm.MatchingAlgorithmCGDProcessor;
import org.recap.matchingalgorithm.service.OngoingMatchingReportsService;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.matchingReports.MatchingSummaryReport;
import org.recap.model.jpa.ReportEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.solr.BibItem;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.ItemChangeLogDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;


/**
 * Created by angelind on 2/2/17.
 */
@Component
public class OngoingMatchingAlgorithmUtil {

    private static final Logger logger = LoggerFactory.getLogger(OngoingMatchingAlgorithmUtil.class);

    @Autowired
    private SolrQueryBuilder solrQueryBuilder;

    @Resource(name = "recapSolrTemplate")
    private SolrTemplate solrTemplate;

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private MatchingAlgorithmUtil matchingAlgorithmUtil;

    @Autowired
    private UpdateCgdUtil updateCgdUtil;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private OngoingMatchingReportsService ongoingMatchingReportsService;

    private Map collectionGroupMap;
    private Map institutionMap;

    /**
     * Fetch updated records and start process string.
     *
     * @param date the date
     * @param rows the rows
     * @return the string
     * @throws IOException         the io exception
     * @throws SolrServerException the solr server exception
     */
    public String fetchUpdatedRecordsAndStartProcess(Date date, Integer rows) throws IOException, SolrServerException {
        String status;
        matchingAlgorithmUtil.populateMatchingCounter();
        List<MatchingSummaryReport> matchingSummaryReports = ongoingMatchingReportsService.populateSummaryReport();
        List<Integer> serialMvmBibIds = new ArrayList<>();
        String formattedDate = getFormattedDateString(date);
        Integer start = 0;
        QueryResponse queryResponse = fetchDataForOngoingMatchingBasedOnDate(formattedDate, rows, start);
        Integer totalNumFound = Math.toIntExact(queryResponse.getResults().getNumFound());
        int totalPages = Math.toIntExact(totalNumFound / rows);
        SolrDocumentList solrDocumentList = queryResponse.getResults();
        status = processOngoingMatchingAlgorithm(solrDocumentList, serialMvmBibIds);

        for(int pageNum=1; pageNum<totalPages; pageNum++) {
            start = pageNum * rows;
            queryResponse = fetchDataForOngoingMatchingBasedOnDate(formattedDate, rows, start);
            solrDocumentList = queryResponse.getResults();
            status = processOngoingMatchingAlgorithm(solrDocumentList, serialMvmBibIds);
        }
        if(CollectionUtils.isNotEmpty(serialMvmBibIds)) {
            ongoingMatchingReportsService.generateSerialAndMVMsReport(serialMvmBibIds);
        }
        ongoingMatchingReportsService.generateTitleExceptionReport(date, rows);

        ongoingMatchingReportsService.generateSummaryReport(matchingSummaryReports);
        return status;
    }

    /**
     * This method fetches data for ongoing matching based on date.
     *
     * @param date      the date
     * @param batchSize the batch size
     * @param start     the start
     * @return the solr document list
     */
    public QueryResponse fetchDataForOngoingMatchingBasedOnDate(String date, Integer batchSize, Integer start) {
        try {
            String query = solrQueryBuilder.fetchCreatedOrUpdatedBibs(date);
            SolrQuery solrQuery = new SolrQuery(query);
            solrQuery.setStart(start);
            solrQuery.setRows(batchSize);
            return solrTemplate.getSolrClient().query(solrQuery);

        } catch (SolrServerException | IOException e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
        return null;
    }

    /**
     * This method is used to process ongoing matching algorithm based on the given bibs in solrDocumentList and updates the CGD and generates report in solr and database.
     *
     * @param solrDocumentList the solr document list
     * @param serialMvmBibIds  the serial mvm bib ids
     * @return the string
     */
    public String processOngoingMatchingAlgorithm(SolrDocumentList solrDocumentList, List<Integer> serialMvmBibIds) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String status = RecapCommonConstants.SUCCESS;
        if(CollectionUtils.isNotEmpty(solrDocumentList)) {
            for (Iterator<SolrDocument> iterator = solrDocumentList.iterator(); iterator.hasNext(); ) {
                SolrDocument solrDocument = iterator.next();
                status = processMatchingForBib(solrDocument, serialMvmBibIds);
            }
        }
        stopWatch.stop();
        logger.info("Total Time taken to execute matching algorithm only : {}" , stopWatch.getTotalTimeSeconds());
        return status;
    }

    /**
     * This method processes matching for multiple match scenario and single match scenario.
     *
     * @param solrDocument    the solr document
     * @param serialMvmBibIds the serial mvm bib ids
     * @return the string
     */
    public String processMatchingForBib(SolrDocument solrDocument, List<Integer> serialMvmBibIds) {
        String status = RecapCommonConstants.SUCCESS;
        logger.info("Ongoing Matching Started");
        Map<Integer, BibItem> bibItemMap = new HashMap<>();
        List<Integer> itemIds = new ArrayList<>();
        Set<String> matchPointString = getMatchingBibsAndMatchPoints(solrDocument, bibItemMap);

        if(bibItemMap.size() > 0) {
            if(matchPointString.size() > 1) {
                // Multi Match
                logger.info("Multi Match Found.");
                try {
                    itemIds = saveReportAndUpdateCGDForMultiMatch(bibItemMap, serialMvmBibIds);
                } catch (IOException | SolrServerException e) {
                    logger.error(RecapCommonConstants.LOG_ERROR,e);
                    status = RecapCommonConstants.FAILURE;
                }
            } else if(matchPointString.size() == 1) {
                // Single Match
                logger.info("Single Match Found.");
                try {
                    itemIds = saveReportAndUpdateCGDForSingleMatch(bibItemMap, matchPointString.iterator().next(), serialMvmBibIds);
                } catch (Exception e) {
                    logger.error(RecapCommonConstants.LOG_ERROR,e);
                    status = RecapCommonConstants.FAILURE;
                }
            } else {
                logger.info("No Match Found.");
            }

            if(CollectionUtils.isNotEmpty(itemIds)) {
                updateCGDForItemInSolr(itemIds);
            }
        } else {
            logger.info("No Match Found.");
        }
        return status;
    }

    /**
     * This method is used to find the matching points.
     * @param solrDocument
     * @param bibItemMap
     * @return
     */
    private Set<String> getMatchingBibsAndMatchPoints(SolrDocument solrDocument, Map<Integer, BibItem> bibItemMap) {
        Set<String> matchPointString = new HashSet<>();
        addToBibItemMap(solrDocument, bibItemMap, matchPointString, RecapCommonConstants.OCLC_NUMBER);
        addToBibItemMap(solrDocument, bibItemMap, matchPointString, RecapCommonConstants.ISBN_CRITERIA);
        addToBibItemMap(solrDocument, bibItemMap, matchPointString, RecapCommonConstants.ISSN_CRITERIA);
        addToBibItemMap(solrDocument, bibItemMap, matchPointString, RecapCommonConstants.LCCN_CRITERIA);
        return matchPointString;
    }

    private void addToBibItemMap(SolrDocument solrDocument, Map<Integer, BibItem> bibItemMap, Set<String> matchPointString, String oclcNumber) {
        Map<Integer, BibItem> tempMap;
        tempMap = findMatchingBibs(solrDocument, matchPointString, oclcNumber);
        if (tempMap != null && tempMap.size() > 0)
            bibItemMap.putAll(tempMap);
    }

    /**
     * This method updates cgd for item in solr.
     *
     * @param itemIds the item ids
     */
    public void updateCGDForItemInSolr(List<Integer> itemIds) {
        if (CollectionUtils.isNotEmpty(itemIds)) {
            List<ItemEntity> itemEntities = itemDetailsRepository.findByItemIdIn(itemIds);
            updateCgdUtil.updateCGDForItemInSolr(itemEntities);
        }
    }

    private List<Integer> saveReportAndUpdateCGDForSingleMatch(Map<Integer, BibItem> bibItemMap, String matchPointString, List<Integer> serialMvmBibIds) {
        List<ReportDataEntity> reportDataEntities = new ArrayList<>();
        Set<String> owningInstSet = new HashSet<>();
        Set<String> materialTypeSet = new HashSet<>();
        List<Integer> bibIds = new ArrayList<>();
        List<String> owningInstList = new ArrayList<>();
        List<String> materialTypeList = new ArrayList<>();
        Map<String,String> titleMap = new HashMap<>();
        List<String> owningInstBibIds = new ArrayList<>();
        List<Integer> itemIds = new ArrayList<>();
        List<String> criteriaValues = new ArrayList<>();
        List<ReportEntity> reportEntitiesToSave = new ArrayList<>();

        int index=0;
        for (Iterator<Integer> iterator = bibItemMap.keySet().iterator(); iterator.hasNext(); ) {
            Integer bibId = iterator.next();
            BibItem bibItem = bibItemMap.get(bibId);
            owningInstSet.add(bibItem.getOwningInstitution());
            owningInstList.add(bibItem.getOwningInstitution());
            owningInstBibIds.add(bibItem.getOwningInstitutionBibId());
            bibIds.add(bibId);
            materialTypeList.add(bibItem.getLeaderMaterialType());
            materialTypeSet.add(bibItem.getLeaderMaterialType());
            if(matchPointString.equalsIgnoreCase(RecapCommonConstants.OCLC_NUMBER)) {
                criteriaValues.addAll(bibItem.getOclcNumber());
            } else if(matchPointString.equalsIgnoreCase(RecapCommonConstants.ISBN_CRITERIA)) {
                criteriaValues.addAll(bibItem.getIsbn());
            } else if(matchPointString.equalsIgnoreCase(RecapCommonConstants.ISSN_CRITERIA)) {
                criteriaValues.addAll(bibItem.getIssn());
            } else if(matchPointString.equalsIgnoreCase(RecapCommonConstants.LCCN_CRITERIA)) {
                criteriaValues.add(bibItem.getLccn());
            }
            index = index + 1;
            if(StringUtils.isNotBlank(bibItem.getTitleSubFieldA())) {
                String titleHeader = RecapCommonConstants.TITLE + index;
                matchingAlgorithmUtil.getReportDataEntity(titleHeader, bibItem.getTitleSubFieldA(), reportDataEntities);
                titleMap.put(titleHeader, bibItem.getTitleSubFieldA());
            }
        }

        if(owningInstSet.size() > 1) {
            ReportEntity reportEntity = new ReportEntity();
            String fileName = RecapCommonConstants.ONGOING_MATCHING_ALGORITHM;
            reportEntity.setFileName(fileName);
            reportEntity.setInstitutionName(RecapCommonConstants.ALL_INST);
            reportEntity.setCreatedDate(new Date());
            String criteriaValueString = StringUtils.join(criteriaValues, ",");
            if(materialTypeSet.size() == 1) {
                Map parameterMap = new HashMap();
                parameterMap.put(RecapCommonConstants.BIB_ID, bibIds);
                parameterMap.put(RecapConstants.MATERIAL_TYPE, materialTypeList);
                parameterMap.put(RecapCommonConstants.OWNING_INST_BIB_ID, owningInstBibIds);
                parameterMap.put(RecapConstants.OWNING_INST, owningInstList);
                parameterMap.put(RecapConstants.CRITERIA_VALUES, criteriaValueString);
                parameterMap.put(RecapConstants.MATCH_POINT, matchPointString);
                try {
                    itemIds = updateCGDBasedOnMaterialTypes(reportEntity, materialTypeSet, serialMvmBibIds, RecapConstants.SINGLE_MATCH, parameterMap, reportEntitiesToSave, titleMap);
                    materialTypeList = (List<String>) parameterMap.get(RecapConstants.MATERIAL_TYPE);
                } catch (Exception e) {
                    logger.error(RecapCommonConstants.LOG_ERROR,e);
                }
            } else {
                reportEntity.setType(RecapConstants.MATERIAL_TYPE_EXCEPTION);
            }
            matchingAlgorithmUtil.getReportDataEntityList(reportDataEntities, owningInstList, bibIds, materialTypeList, owningInstBibIds);
            matchingAlgorithmUtil.getReportDataEntity(matchPointString.equalsIgnoreCase(RecapCommonConstants.OCLC_NUMBER) ? RecapCommonConstants.OCLC_CRITERIA : matchPointString, criteriaValueString, reportDataEntities);
            reportEntity.addAll(reportDataEntities);
            reportEntitiesToSave.add(reportEntity);
            producerTemplate.sendBody("scsbactivemq:queue:saveMatchingReportsQ", reportEntitiesToSave);
        }
        return itemIds;
    }

    /**
     * This method processes cgd and reports for un matching titles report entity.
     *
     * @param fileName                 the file name
     * @param titleMap                 the title map
     * @param bibIds                   the bib ids
     * @param materialTypes            the material types
     * @param owningInstitutions       the owning institutions
     * @param owningInstBibIds         the owning inst bib ids
     * @param matchPointValue          the match point value
     * @param unMatchingTitleHeaderSet the un matching title header set
     * @param matchPointString         the match point string
     * @return the report entity
     */
    public ReportEntity processCGDAndReportsForUnMatchingTitles(String fileName, Map<String, String> titleMap, List<Integer> bibIds, List<String> materialTypes, List<String> owningInstitutions,
                                                                List<String> owningInstBibIds, String matchPointValue, Set<String> unMatchingTitleHeaderSet, String matchPointString) {
        ReportEntity unMatchReportEntity = matchingAlgorithmUtil.buildReportEntity(fileName);
        List<ReportDataEntity> reportDataEntityList = new ArrayList<>();
        List<String> bibIdList = new ArrayList<>();
        List<String> materialTypeList = new ArrayList<>();
        List<String> owningInstitutionList = new ArrayList<>();
        List<String> owningInstBibIdList = new ArrayList<>();

        matchingAlgorithmUtil.prepareReportForUnMatchingTitles(titleMap, bibIds, materialTypes, owningInstitutions, owningInstBibIds, unMatchingTitleHeaderSet, reportDataEntityList, bibIdList, materialTypeList, owningInstitutionList, owningInstBibIdList);

        List<Integer> bibliographicIds = new ArrayList<>();
        for (Iterator<String> iterator = bibIdList.iterator(); iterator.hasNext(); ) {
            String bibId = iterator.next();
            bibliographicIds.add(Integer.valueOf(bibId));
        }
        matchingAlgorithmUtil.getReportDataEntityList(reportDataEntityList, owningInstitutionList, bibIdList, materialTypeList, owningInstBibIdList);
        matchingAlgorithmUtil.getReportDataEntity(matchPointString.equalsIgnoreCase(RecapCommonConstants.OCLC_NUMBER) ? RecapCommonConstants.OCLC_CRITERIA : matchPointString, matchPointValue, reportDataEntityList);
        unMatchReportEntity.addAll(reportDataEntityList);
        return unMatchReportEntity;
    }

    /**
     * This method is used to generate reports and update CGD for multiple match scenario
     * @param bibItemMap
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    private List<Integer> saveReportAndUpdateCGDForMultiMatch(Map<Integer, BibItem> bibItemMap, List<Integer> serialMvmBibIds) throws IOException, SolrServerException {
        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setFileName(RecapCommonConstants.ONGOING_MATCHING_ALGORITHM);
        reportEntity.setCreatedDate(new Date());
        reportEntity.setInstitutionName(RecapCommonConstants.ALL_INST);
        List<ReportDataEntity> reportDataEntities = new ArrayList<>();
        Set<String> owningInstSet = new HashSet<>();
        List<String> owningInstList = new ArrayList<>();
        List<Integer> bibIdList = new ArrayList<>();
        List<String> materialTypeList = new ArrayList<>();
        Set<String> materialTypes = new HashSet<>();
        List<String> owningInstBibIds = new ArrayList<>();
        Set<String> oclcNumbers = new HashSet<>();
        Set<String> isbns = new HashSet<>();
        Set<String> issns = new HashSet<>();
        Set<String> lccns = new HashSet<>();
        List<Integer> itemIds = new ArrayList<>();

        for (Iterator<Integer> iterator = bibItemMap.keySet().iterator(); iterator.hasNext(); ) {
            Integer bibId = iterator.next();
            BibItem bibItem = bibItemMap.get(bibId);
            owningInstSet.add(bibItem.getOwningInstitution());
            owningInstList.add(bibItem.getOwningInstitution());
            bibIdList.add(bibItem.getBibId());
            materialTypes.add(bibItem.getLeaderMaterialType());
            materialTypeList.add(bibItem.getLeaderMaterialType());
            owningInstBibIds.add(bibItem.getOwningInstitutionBibId());
            if(CollectionUtils.isNotEmpty(bibItem.getOclcNumber()))
                oclcNumbers.addAll(bibItem.getOclcNumber());
            if(CollectionUtils.isNotEmpty(bibItem.getIsbn()))
                isbns.addAll(bibItem.getIsbn());
            if(CollectionUtils.isNotEmpty(bibItem.getIssn()))
                issns.addAll(bibItem.getIssn());
            if(StringUtils.isNotBlank(bibItem.getLccn()))
                lccns.add(bibItem.getLccn());
        }

        if(owningInstSet.size() > 1) {
            Map parameterMap = new HashMap();
            parameterMap.put(RecapCommonConstants.BIB_ID, bibIdList);
            parameterMap.put(RecapConstants.MATERIAL_TYPE, materialTypeList);
            itemIds = updateCGDBasedOnMaterialTypes(reportEntity, materialTypes, serialMvmBibIds, RecapConstants.MULTI_MATCH, parameterMap, new ArrayList<>(), null);
            materialTypeList = (List<String>) parameterMap.get(RecapConstants.MATERIAL_TYPE);
            matchingAlgorithmUtil.getReportDataEntityList(reportDataEntities, owningInstList, bibIdList, materialTypeList, owningInstBibIds);

            checkAndAddReportEntities(reportDataEntities, oclcNumbers, RecapCommonConstants.OCLC_CRITERIA);
            checkAndAddReportEntities(reportDataEntities, isbns, RecapCommonConstants.ISBN_CRITERIA);
            checkAndAddReportEntities(reportDataEntities, issns, RecapCommonConstants.ISSN_CRITERIA);
            checkAndAddReportEntities(reportDataEntities, lccns, RecapCommonConstants.LCCN_CRITERIA);
            reportEntity.addAll(reportDataEntities);
            producerTemplate.sendBody("scsbactivemq:queue:saveMatchingReportsQ", Arrays.asList(reportEntity));
        }
        return itemIds;
    }

    private void checkAndAddReportEntities(List<ReportDataEntity> reportDataEntities, Set<String> oclcNumbers, String oclcCriteria) {
        if (CollectionUtils.isNotEmpty(oclcNumbers)) {
            ReportDataEntity oclcNumberReportDataEntity = matchingAlgorithmUtil.getReportDataEntityForCollectionValues(oclcNumbers, oclcCriteria);
            reportDataEntities.add(oclcNumberReportDataEntity);
        }
    }

    /**
     * This method checks for monograph and updates the CGD
     *
     * @param reportEntity     the report entity
     * @param materialTypes    the material types
     * @param serialMvmBibIds  the serial mvm bib ids
     * @param matchType        the match type
     * @param parameterMap     the parameter map
     * @param reportEntityList the report entity list
     * @param titleMap         the title map
     * @return the list
     * @throws IOException         the io exception
     * @throws SolrServerException the solr server exception
     */
    private List<Integer> updateCGDBasedOnMaterialTypes(ReportEntity reportEntity, Set<String> materialTypes, List<Integer> serialMvmBibIds, String matchType, Map parameterMap,
                                                        List<ReportEntity> reportEntityList, Map<String,String> titleMap) throws IOException, SolrServerException {
        List<Integer> itemIds = new ArrayList<>();
        List<String> materialTypeList = (List<String>) parameterMap.get(RecapConstants.MATERIAL_TYPE);
        List<Integer> bibIdList = (List<Integer>) parameterMap.get(RecapCommonConstants.BIB_ID);
        MatchingAlgorithmCGDProcessor matchingAlgorithmCGDProcessor = new MatchingAlgorithmCGDProcessor(bibliographicDetailsRepository, producerTemplate,
                getCollectionGroupMap(), getInstitutionEntityMap(), itemChangeLogDetailsRepository, RecapConstants.ONGOING_MATCHING_OPERATION_TYPE, collectionGroupDetailsRepository, itemDetailsRepository);
        if(materialTypes.size() == 1) {
            reportEntity.setType(matchType);
            Map<Integer, Map<Integer, List<ItemEntity>>> useRestrictionMap = new HashMap<>();
            Map<Integer, ItemEntity> itemEntityMap = new HashMap<>();
            if(materialTypes.contains(RecapCommonConstants.MONOGRAPH)) {
                Set<String> materialTypeSet = new HashSet<>();
                boolean isMonograph = matchingAlgorithmCGDProcessor.checkForMonographAndPopulateValues(materialTypeSet, useRestrictionMap, itemEntityMap, bibIdList);
                if(isMonograph) {
                    if(matchType.equalsIgnoreCase(RecapConstants.SINGLE_MATCH)) {
                        ReportEntity reportEntityForTitleException = titleVerificationForSingleMatch(reportEntity.getFileName(), titleMap, bibIdList, materialTypeList, parameterMap);
                        if(reportEntityForTitleException != null) {
                            logger.info("updateCGDBasedOnMaterialTypes, Match type is Single Match, TileException Found");
                            reportEntityList.add(reportEntityForTitleException);
                        }

                    }
                    matchingAlgorithmCGDProcessor.updateCGDProcess(useRestrictionMap, itemEntityMap);
                    itemIds.addAll(itemEntityMap.keySet());
                } else {
                    if(materialTypeSet.size() > 1) {
                        reportEntity.setType(RecapConstants.MATERIAL_TYPE_EXCEPTION);
                    } else if(materialTypeSet.size() == 1){
                        reportEntity.setType(matchType);
                        if(matchType.equalsIgnoreCase(RecapConstants.SINGLE_MATCH)) {
                            ReportEntity reportEntityForTitleException = titleVerificationForSingleMatch(reportEntity.getFileName(), titleMap, bibIdList, materialTypeList, parameterMap);
                            if(reportEntityForTitleException != null) {
                                logger.info("updateCGDBasedOnMaterialTypes, Match type is Single Match, TileException Found");
                                reportEntityList.add(reportEntityForTitleException);
                            }
                        }
                        if(materialTypeSet.contains(RecapConstants.MONOGRAPHIC_SET)) {
                            int size = materialTypeList.size();
                            materialTypeList = new ArrayList<>();
                            for(int i = 0; i < size; i++) {
                                materialTypeList.add(RecapConstants.MONOGRAPHIC_SET);
                            }
                            matchingAlgorithmCGDProcessor.updateItemsCGD(itemEntityMap);
                            itemIds.addAll(itemEntityMap.keySet());
                            serialMvmBibIds.addAll(bibIdList);
                        }
                    }
                }
            } else if(materialTypes.contains(RecapCommonConstants.SERIAL)) {
                if(matchType.equalsIgnoreCase(RecapConstants.SINGLE_MATCH)) {
                    ReportEntity reportEntityForTitleException = titleVerificationForSingleMatch(reportEntity.getFileName(), titleMap, bibIdList, materialTypeList, parameterMap);
                    if(reportEntityForTitleException != null) {
                        logger.info("updateCGDBasedOnMaterialTypes, Match type is Single Match, TileException Found");
                        reportEntityList.add(reportEntityForTitleException);
                    }
                }
                matchingAlgorithmCGDProcessor.populateItemEntityMap(itemEntityMap, bibIdList);
                matchingAlgorithmCGDProcessor.updateItemsCGD(itemEntityMap);
                itemIds.addAll(itemEntityMap.keySet());
                serialMvmBibIds.addAll(bibIdList);
            }
        } else {
            reportEntity.setType(RecapConstants.MATERIAL_TYPE_EXCEPTION);
        }
        parameterMap.put(RecapConstants.MATERIAL_TYPE, materialTypeList);
        return itemIds;
    }

    /**
     * This method is used to find the matching bibs
     * @param solrDocument
     * @param matchPointString
     * @param fieldName
     * @return
     */
    private Map<Integer, BibItem> findMatchingBibs(SolrDocument solrDocument, Set<String> matchPointString, String fieldName) {
        Map<Integer, BibItem> bibItemMap = null;
        Object value = solrDocument.getFieldValue(fieldName);
        if(value != null) {
            if(value instanceof String) {
                String fieldValue = (String) value;
                if(StringUtils.isNotBlank(fieldValue)) {
                    String query = solrQueryBuilder.solrQueryForOngoingMatching(fieldName, fieldValue);
                    bibItemMap = getBibsFromSolr(matchPointString, fieldName, query);
                }
            } else if(value instanceof List) {
                List<String> fieldValues = (List<String>) value;
                if(CollectionUtils.isNotEmpty(fieldValues)) {
                    String query = solrQueryBuilder.solrQueryForOngoingMatching(fieldName, fieldValues);
                    bibItemMap = getBibsFromSolr(matchPointString, fieldName, query);
                }
            }
        }
        return bibItemMap;
    }

    /**
     * This method is used to get bibs from the solr
     *
     * @param matchPointString the match point string
     * @param fieldName        the field name
     * @param query            the query
     * @return bibs from solr
     */
    public Map<Integer, BibItem> getBibsFromSolr(Set<String> matchPointString, String fieldName, String query) {
        Map<Integer, BibItem> bibItemMap = new HashMap<>();
        SolrQuery solrQuery = new SolrQuery(query);
        try {
            QueryResponse queryResponse = solrTemplate.getSolrClient().query(solrQuery);
            SolrDocumentList solrDocumentList = queryResponse.getResults();
            long numFound = solrDocumentList.getNumFound();
            if(numFound > 1) {
                matchPointString.add(fieldName);
                if(numFound > solrDocumentList.size()) {
                    solrQuery.setRows((int) numFound);
                    queryResponse = solrTemplate.getSolrClient().query(solrQuery);
                    solrDocumentList = queryResponse.getResults();
                }
                for (Iterator<SolrDocument> iterator = solrDocumentList.iterator(); iterator.hasNext(); ) {
                    SolrDocument solrDocument = iterator.next();
                    BibItem bibItem = populateBibItem(solrDocument);
                    bibItemMap.put(bibItem.getBibId(), bibItem);
                }
            }
        } catch (IOException|SolrServerException e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
        return bibItemMap;
    }

    /**
     * Title verification for single match report entity.
     *
     * @param fileName         the file name
     * @param titleMap         the title map
     * @param bibIds           the bib ids
     * @param materialTypeList the material type list
     * @param parameterMap     the parameter map
     * @return the report entity
     */
    private ReportEntity titleVerificationForSingleMatch(String fileName, Map<String,String> titleMap, List<Integer> bibIds, List<String> materialTypeList, Map parameterMap) {
        ReportEntity reportEntity = null;
        List<String> owningInstBibIds = (List<String>) parameterMap.get(RecapCommonConstants.OWNING_INST_BIB_ID);
        List<String> owningInstList = (List<String>) parameterMap.get(RecapConstants.OWNING_INST);
        String criteriaValues = (String) parameterMap.get(RecapConstants.CRITERIA_VALUES);
        String matchPointString = (String) parameterMap.get(RecapConstants.MATCH_POINT);
        Set<String> unMatchingTitleHeaderSet = matchingAlgorithmUtil.getMatchingAndUnMatchingBibsOnTitleVerification(titleMap);
        if(CollectionUtils.isNotEmpty(unMatchingTitleHeaderSet)) {
            reportEntity = processCGDAndReportsForUnMatchingTitles(fileName, titleMap, bibIds,
                    materialTypeList, owningInstList, owningInstBibIds,
                    criteriaValues, unMatchingTitleHeaderSet, matchPointString);
            logger.info("titleVerificationForSingleMatch, Single Match, found unmatching titles");
        }
        return reportEntity;
    }

    /**
     * This method populates bib item.
     *
     * @param solrDocument the solr document
     * @return the bib item
     */
    public BibItem populateBibItem(SolrDocument solrDocument) {
        Collection<String> fieldNames = solrDocument.getFieldNames();
        BibItem bibItem = new BibItem();
        bibItem=commonUtil.getBibItemFromSolrFieldNames(solrDocument, fieldNames, bibItem);
        return bibItem;
    }

    /**
     * This method gets formatted date.
     *
     * @param inputDate the input date
     * @return the formatted date string
     */
    public String getFormattedDateString(Date inputDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(RecapCommonConstants.DATE_FORMAT_YYYYMMDDHHMM);
        String utcStr = null;
        try {
            String inputDateString = simpleDateFormat.format(inputDate);
            Date date = simpleDateFormat.parse(inputDateString);
            DateFormat format = new SimpleDateFormat(RecapCommonConstants.UTC_DATE_FORMAT);
            format.setTimeZone(TimeZone.getTimeZone(RecapCommonConstants.UTC));
            utcStr = format.format(date);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
        return utcStr + RecapCommonConstants.SOLR_DATE_RANGE_TO_NOW;
    }

    /**
     * This method gets collection group map.
     *
     * @return the collection group map
     */
    public Map getCollectionGroupMap() {
        if (null == collectionGroupMap) {
            collectionGroupMap = new HashMap();
            Iterable<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
            for (Iterator<CollectionGroupEntity> iterator = collectionGroupEntities.iterator(); iterator.hasNext(); ) {
                CollectionGroupEntity collectionGroupEntity = iterator.next();
                collectionGroupMap.put(collectionGroupEntity.getCollectionGroupCode(), collectionGroupEntity.getId());
            }
        }
        return collectionGroupMap;
    }

    /**
     * This method gets institution entity map.
     *
     * @return the institution entity map
     */
    public Map getInstitutionEntityMap() {
        if (null == institutionMap) {
            institutionMap = new HashMap();
            Iterable<InstitutionEntity> institutionEntities = institutionDetailsRepository.findAll();
            for (Iterator<InstitutionEntity> iterator = institutionEntities.iterator(); iterator.hasNext(); ) {
                InstitutionEntity institutionEntity = iterator.next();
                institutionMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getId());
            }
        }
        return institutionMap;
    }
}
