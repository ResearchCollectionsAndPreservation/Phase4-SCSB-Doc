package org.recap.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.admin.SolrAdmin;
import org.recap.executors.BibItemIndexExecutorService;
import org.recap.model.solr.SolrIndexRequest;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.solr.main.BibSolrCrudRepository;
import org.recap.repository.solr.main.HoldingsSolrCrudRepository;
import org.recap.repository.solr.main.ItemCrudRepository;
import org.recap.service.accession.SolrIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Sheik on 6/18/2016.
 */
@Controller
public class SolrIndexController {

    private static final Logger logger = LoggerFactory.getLogger(SolrIndexController.class);

    @Autowired
    private BibItemIndexExecutorService bibItemIndexExecutorService;

    @Autowired
    private BibSolrCrudRepository bibSolrCrudRepository;

    @Autowired
    private ItemCrudRepository itemCrudRepository;

    @Autowired
    private HoldingsSolrCrudRepository holdingsSolrCrudRepository;

    @Autowired
    private SolrAdmin solrAdmin;

    @Value("${commit.indexes.interval}")
    private Integer commitIndexesInterval;

    @Autowired
    private SolrIndexService solrIndexService;

    @Value("${solr.parent.core}")
    private String solrCore;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    /**
     * To initialize solr indexing ui page.
     *
     * @param model the model
     * @return the string
     */
    @RequestMapping("/")
    public String solrIndexer(Model model){
        model.addAttribute("solrIndexRequest",new SolrIndexRequest());
        model.addAttribute("matchingAlgoDate", "");
        return "solrIndexer";
    }

    /**
     * This method is used to perform full index and incremental indexing through ui.
     *
     * @param solrIndexRequest the solr index request
     * @param result           the result
     * @param model            the model
     * @return the string
     * @throws Exception the exception
     */
    @ResponseBody
    @PostMapping(value = "/solrIndexer/fullIndex")
    public String fullIndex(@Valid @ModelAttribute("solrIndexRequest") SolrIndexRequest solrIndexRequest,
                            BindingResult result,
                            Model model) throws Exception {
        String docType = solrIndexRequest.getDocType();
        Integer numberOfThread = solrIndexRequest.getNumberOfThreads();
        Integer numberOfDoc = solrIndexRequest.getNumberOfDocs();
        String owningInstitutionCode = solrIndexRequest.getOwningInstitutionCode();
        if (solrIndexRequest.getCommitInterval() == null) {
            solrIndexRequest.setCommitInterval(commitIndexesInterval);
        }
        Integer commitInterval = solrIndexRequest.getCommitInterval();

        logger.info("Document Type : {} Number of Threads : {} Number of Docs : {} Commit Interval : {} From Date : {}",docType,numberOfThread,numberOfDoc,commitInterval,solrIndexRequest.getDateFrom());

        if (solrIndexRequest.isDoClean()) {
            if(StringUtils.isNotBlank(owningInstitutionCode)) {
                bibSolrCrudRepository.deleteByOwningInstitution(owningInstitutionCode);
                holdingsSolrCrudRepository.deleteByOwningInstitution(owningInstitutionCode);
                itemCrudRepository.deleteByOwningInstitution(owningInstitutionCode);
            } else {
                bibSolrCrudRepository.deleteAll();
                holdingsSolrCrudRepository.deleteAll();
                itemCrudRepository.deleteAll();
            }
            try {
                solrAdmin.unloadTempCores();
            } catch (IOException | SolrServerException e) {
                logger.error(RecapCommonConstants.LOG_ERROR,e);
            }
        }

        Integer totalProcessedRecords = bibItemIndexExecutorService.index(solrIndexRequest);
        String status = "Total number of records processed : " + totalProcessedRecords;

        return report(status);
    }

    /**
     * Partial index string.
     *
     * @param solrIndexRequest the solr index request
     * @param result           the result
     * @param model            the model
     * @return the string
     * @throws Exception the exception
     */
    @ResponseBody
    @PostMapping(value = "/solrIndexer/partialIndex")
    public String partialIndex(@Valid @ModelAttribute("solrIndexRequest") SolrIndexRequest solrIndexRequest,
                            BindingResult result,
                            Model model) throws Exception {
        Integer numberOfThread = solrIndexRequest.getNumberOfThreads();
        Integer numberOfDoc = solrIndexRequest.getNumberOfDocs();
        if (solrIndexRequest.getCommitInterval() == null) {
            solrIndexRequest.setCommitInterval(commitIndexesInterval);
        }
        Integer commitInterval = solrIndexRequest.getCommitInterval();

        logger.info("Number of Threads : {} Number of Docs : {} Commit Interval : {} From Date : {}",numberOfThread,numberOfDoc,commitInterval,solrIndexRequest.getDateFrom());

        Integer totalProcessedRecords = bibItemIndexExecutorService.partialIndex(solrIndexRequest);
        String status = "Total number of records processed : " + totalProcessedRecords;

        return report(status);
    }

    /**
     * This method is used to get the status of the report.
     *
     * @param status the status
     * @return the string
     */
    @ResponseBody
    @GetMapping(value = "/solrIndexer/report")
    public String report(String status) {
        return StringUtils.isBlank(status) ? "Index process initiated!" : status;
    }

    /**
     * This method is used to perform indexing by using bibliographic id.
     *
     * @param bibliographicIdList the bibliographic id list
     * @return the string
     */
    @ResponseBody
    @PostMapping(value = "/solrIndexer/indexByBibliographicId")
    public String indexByBibliographicId(@RequestBody List<Integer> bibliographicIdList) {
        String response = null;
        try {
            for (Integer bibliographicId : bibliographicIdList) {
                getSolrIndexService().indexByBibliographicId(bibliographicId);
            }
            response = RecapCommonConstants.SUCCESS;
        } catch (Exception e) {
            response = RecapCommonConstants.FAILURE;
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
        return response;
    }

    @ResponseBody
    @PostMapping(value = "/solrIndexer/indexByOwningInstBibliographicIdList")
    public String indexByOwningInstBibliographicIdList(@RequestParam Map<String,Object> requestParameters) {
        String ownInstbibliographicIdListString = (String)requestParameters.get(RecapConstants.OWN_INST_BIBID_LIST);
        ownInstbibliographicIdListString = ownInstbibliographicIdListString.replace("[","");
        ownInstbibliographicIdListString = ownInstbibliographicIdListString.replace("]","");
        ownInstbibliographicIdListString = ownInstbibliographicIdListString.replace("\"","");
        logger.info("ownInstbibliographicIdListString--->{}",ownInstbibliographicIdListString);
        String[] ownInstbibliographicIdArray = ownInstbibliographicIdListString.split(",");
        List<String> ownInstbibliographicIdList = Arrays.asList(ownInstbibliographicIdArray);
        Integer owningInstId = Integer.valueOf((String) requestParameters.get(RecapCommonConstants.OWN_INSTITUTION_ID));
        String response = null;
        try {
            getSolrIndexService().indexByOwnInstBibId(ownInstbibliographicIdList,owningInstId);
            response = RecapCommonConstants.SUCCESS;
        } catch (Exception e) {
            response = RecapCommonConstants.FAILURE;
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
        return response;
    }

    /**
     * This method is used to delete records by bib,holding and item id.
     * Root value is passed to delete the associated holdings and item for that bib.
     *
     * @param idMapToRemoveIndexList the id list of map to remove index
     * @return the string
     */
    @ResponseBody
    @PostMapping(value = "/solrIndexer/deleteByBibHoldingItemId")
    public String deleteByBibHoldingItemId(@RequestBody List<Map<String,String>> idMapToRemoveIndexList) {
        String response = null;
        logger.info("idMapToRemoveIndexList size--->{}",idMapToRemoveIndexList.size());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Map<String,String> idMapToRemoveIndex : idMapToRemoveIndexList) {
            StopWatch stopWatchDeleteDummyRec = new StopWatch();
            stopWatchDeleteDummyRec.start();
            String bibliographicId = idMapToRemoveIndex.get("BibId");
            String holdingId = idMapToRemoveIndex.get("HoldingId");
            String itemId = idMapToRemoveIndex.get("ItemId");
            String root = idMapToRemoveIndex.get("_root_");
            // Scenario : Bound-with - when a bib is unlinked to an item, the relationship between the bib and its holdings and item should be removed from solr.
            // Root value is used to delete the associated holdings and item for that bib.
            try {
                if (StringUtils.isNotBlank(root)) {
                    logger.info("deleting unlinked holding and item record from solr holding id - {}, item id - {}, root - {}",holdingId,itemId,root);
                    getSolrIndexService().deleteBySolrQuery(RecapCommonConstants.HOLDING_ID + ":" + holdingId + " " + RecapCommonConstants.AND + " " + RecapCommonConstants.ROOT + ":" + root);
                    getSolrIndexService().deleteBySolrQuery(RecapCommonConstants.ITEM_ID + ":" + itemId + " " + RecapCommonConstants.AND + " " + RecapCommonConstants.ROOT + ":" + root);
                } else {
                    logger.info("deleting dummy record from solr bib id - {}, holding id - {}, item id - {}",bibliographicId,holdingId,itemId);
                    getSolrIndexService().deleteByDocId(RecapCommonConstants.BIB_ID, bibliographicId);
                    getSolrIndexService().deleteByDocId(RecapCommonConstants.HOLDING_ID, holdingId);
                    getSolrIndexService().deleteByDocId(RecapCommonConstants.ITEM_ID, itemId);
                }
                response = RecapCommonConstants.SUCCESS;
            } catch (Exception e) {
                response = RecapCommonConstants.FAILURE;
                logger.error(RecapCommonConstants.LOG_ERROR,e);
            }
            stopWatchDeleteDummyRec.stop();
            logger.info("Time taken to delete  bib id - {}, holding id - {}, item id {}--> is {} milli sec",bibliographicId,holdingId,itemId,stopWatchDeleteDummyRec.getTotalTimeMillis());
        }
        stopWatch.stop();
        logger.info("Total time to delete dummy record from solr--->{} milli sec",stopWatch.getTotalTimeMillis());
        return response;
    }

    /**
     * This method is used to delete records by bib id and is deleted flag.
     *
     * @param bibIdMapToRemoveIndexList
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/solrIndexer/deleteByBibIdAndIsDeletedFlag")
    public String deleteByBibIdAndIsDeletedFlag(@RequestBody List<Map<String,String>> bibIdMapToRemoveIndexList) {
        String response = null;
        logger.info("bibIdMapToRemoveIndexList size--->{}",bibIdMapToRemoveIndexList.size());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Map<String,String> bibIdMapToRemoveIndex : bibIdMapToRemoveIndexList) {
            StopWatch stopWatchDeleteRec = new StopWatch();
            stopWatchDeleteRec.start();
            String bibliographicId = bibIdMapToRemoveIndex.get(RecapCommonConstants.BIB_ID);
            String isDeletedBib = bibIdMapToRemoveIndex.get(RecapCommonConstants.IS_DELETED_BIB);
            try {
                logger.info("deleting linked existing bib record from solr bib id - {}, is Deleted Bib - {}", bibliographicId, isDeletedBib);
                getSolrIndexService().deleteBySolrQuery(RecapCommonConstants.BIB_ID + ":" + bibliographicId + " " + RecapCommonConstants.AND + " " + RecapCommonConstants.IS_DELETED_BIB + ":" + isDeletedBib);
                response = RecapCommonConstants.SUCCESS;
            } catch (Exception e) {
                response = RecapCommonConstants.FAILURE;
                logger.error(RecapCommonConstants.LOG_ERROR, e);
            }
            stopWatchDeleteRec.stop();
            logger.info("Time taken to delete  bib id - {} --> is {} milli sec", bibliographicId, stopWatchDeleteRec.getTotalTimeMillis());
        }
        stopWatch.stop();
        logger.info("Total time to delete bib record from solr--->{} milli sec", stopWatch.getTotalTimeMillis());
        return response;
    }

    @GetMapping(value = "/solrIndexer/institutions")
    @ResponseBody
    public List<String> getInstitution(){
        return  institutionDetailsRepository.findAllInstitutionCodeExceptHTC();
    }

    /**
     * This method gets solr index service.
     *
     * @return the solr index service
     */
    public SolrIndexService getSolrIndexService() {
        return solrIndexService;
    }

    /**
     * This method sets solr index service.
     *
     * @param solrIndexService the solr index service
     */
    public void setSolrIndexService(SolrIndexService solrIndexService) {
        this.solrIndexService = solrIndexService;
    }
}
