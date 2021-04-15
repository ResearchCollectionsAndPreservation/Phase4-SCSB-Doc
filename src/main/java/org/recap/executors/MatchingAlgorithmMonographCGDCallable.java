package org.recap.executors;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.CollectionUtils;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.matchingalgorithm.MatchingAlgorithmCGDProcessor;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.ItemChangeLogDetailsRepository;
import org.recap.repository.jpa.ReportDataDetailsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by angelind on 6/1/17.
 */
public class MatchingAlgorithmMonographCGDCallable extends  CommonCallable implements Callable {

    private InstitutionDetailsRepository institutionDetailsRepository;
    private ReportDataDetailsRepository reportDataDetailsRepository;
    private BibliographicDetailsRepository bibliographicDetailsRepository;
    private int pageNum;
    private Integer batchSize;
    private ProducerTemplate producerTemplate;
    private Map collectionGroupMap;
    private Map institutionMap;
    private ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;
    private ItemDetailsRepository itemDetailsRepository;
    private boolean isPendingMatch;

    /**
     * This method instantiates a new matching algorithm cgd callable.
     *  @param reportDataDetailsRepository      the report data details repository
     * @param bibliographicDetailsRepository   the bibliographic details repository
     * @param pageNum                          the page num
     * @param batchSize                        the batch size
     * @param producerTemplate                 the producer template
     * @param collectionGroupMap               the collection group map
     * @param institutionMap                   the institution map
     * @param itemChangeLogDetailsRepository   the item change log details repository
     * @param collectionGroupDetailsRepository the collection group details repository
     * @param itemDetailsRepository            the item details repository
     * @param isPendingMatch                   the is pending match
     * @param institutionDetailsRepository
     */
    public MatchingAlgorithmMonographCGDCallable(ReportDataDetailsRepository reportDataDetailsRepository, BibliographicDetailsRepository bibliographicDetailsRepository,
                                                 int pageNum, Integer batchSize, ProducerTemplate producerTemplate, Map collectionGroupMap, Map institutionMap,
                                                 ItemChangeLogDetailsRepository itemChangeLogDetailsRepository, CollectionGroupDetailsRepository collectionGroupDetailsRepository,
                                                 ItemDetailsRepository itemDetailsRepository, boolean isPendingMatch, InstitutionDetailsRepository institutionDetailsRepository) {
        this.reportDataDetailsRepository = reportDataDetailsRepository;
        this.bibliographicDetailsRepository = bibliographicDetailsRepository;
        this.pageNum = pageNum;
        this.batchSize = batchSize;
        this.producerTemplate = producerTemplate;
        this.collectionGroupMap = collectionGroupMap;
        this.institutionMap = institutionMap;
        this.itemChangeLogDetailsRepository = itemChangeLogDetailsRepository;
        this.collectionGroupDetailsRepository = collectionGroupDetailsRepository;
        this.itemDetailsRepository = itemDetailsRepository;
        this.isPendingMatch = isPendingMatch;
        this.institutionDetailsRepository = institutionDetailsRepository;
    }

    /**
     * This method is used to check for monograph status of bib and updates the CGD .
     * @return
     * @throws Exception
     */
    @Override
    public Object call() throws Exception {

        long from = pageNum * Long.valueOf(batchSize);
        List<ReportDataEntity> reportDataEntities;
        if(isPendingMatch) {
            reportDataEntities = reportDataDetailsRepository.getReportDataEntityForPendingMatchingMonographs(RecapCommonConstants.BIB_ID, from, batchSize);
        } else {
            reportDataEntities =  reportDataDetailsRepository.getReportDataEntityForMatchingMonographs(RecapCommonConstants.BIB_ID, from, batchSize);
        }
        List<Integer> nonMonographRecordNums = new ArrayList<>();
        List<Integer> exceptionRecordNums = new ArrayList<>();
        Map<String, List<Integer>> unProcessedRecordNumMap = new HashMap<>();
        for(ReportDataEntity reportDataEntity : reportDataEntities) {
            Map<Integer, Map<Integer, List<ItemEntity>>> useRestrictionMap = new HashMap<>();
            Map<Integer, ItemEntity> itemEntityMap = new HashMap<>();
            List<Integer> bibIdList = getBibIdListFromString(reportDataEntity);
            Set<String> materialTypeSet = new HashSet<>();
            MatchingAlgorithmCGDProcessor matchingAlgorithmCGDProcessor = new MatchingAlgorithmCGDProcessor(bibliographicDetailsRepository, producerTemplate, collectionGroupMap,
                    institutionMap, itemChangeLogDetailsRepository, RecapConstants.INITIAL_MATCHING_OPERATION_TYPE, collectionGroupDetailsRepository, itemDetailsRepository,institutionDetailsRepository);
            boolean isMonograph = matchingAlgorithmCGDProcessor.checkForMonographAndPopulateValues(materialTypeSet,useRestrictionMap, itemEntityMap, bibIdList);
            if(isMonograph) {
                matchingAlgorithmCGDProcessor.updateCGDProcess(useRestrictionMap, itemEntityMap);
            } else {
                if(materialTypeSet.size() > 1) {
                    exceptionRecordNums.add(Integer.valueOf(reportDataEntity.getRecordNum()));
                } else {
                    if(materialTypeSet.contains(RecapConstants.MONOGRAPHIC_SET)) {
                        nonMonographRecordNums.add(Integer.valueOf(reportDataEntity.getRecordNum()));
                    }
                }
            }
        }
        if(CollectionUtils.isNotEmpty(nonMonographRecordNums)) {
            unProcessedRecordNumMap.put("NonMonographRecordNums", nonMonographRecordNums);
        }
        if(CollectionUtils.isNotEmpty(exceptionRecordNums)) {
            unProcessedRecordNumMap.put("ExceptionRecordNums", exceptionRecordNums);
        }
        return unProcessedRecordNumMap;
    }
}
