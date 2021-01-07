package org.recap.service.deaccession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.common.SolrInputDocument;
import org.recap.RecapCommonConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.util.BibJSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by angelind on 10/11/16.
 */
@Component
public class DeAccessSolrDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DeAccessSolrDocumentService.class);

    @Value("${solr.parent.core}")
    private String solrCore;

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    private HoldingsDetailsRepository holdingDetailRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Resource
    private SolrTemplate solrTemplate;

    /**
     * Gets bib json util.
     *
     * @return the bib json util
     */
    public BibJSONUtil getBibJSONUtil(){
        return new BibJSONUtil();
    }

    /**
     * Gets bibliographic details repository.
     *
     * @return the bibliographic details repository
     */
    public BibliographicDetailsRepository getBibliographicDetailsRepository() {
        return bibliographicDetailsRepository;
    }

    /**
     * Gets holding detail repository.
     *
     * @return the holding detail repository
     */
    public HoldingsDetailsRepository getHoldingDetailRepository() {
        return holdingDetailRepository;
    }

    /**
     * Gets item details repository.
     *
     * @return the item details repository
     */
    public ItemDetailsRepository getItemDetailsRepository() {
        return itemDetailsRepository;
    }

    /**
     * Gets solr template.
     *
     * @return the solr template
     */
    public SolrTemplate getSolrTemplate() {
        return solrTemplate;
    }

    /**
     * This method is used to update the documents for IsDeletedBib by using bib id in solr.
     *
     * @param bibIds the bib ids
     * @return the string
     */
    public String updateIsDeletedBibByBibId(@RequestBody List<Integer> bibIds){
        try{
            for(Integer bibId : bibIds){
                BibliographicEntity bibEntity = getBibliographicDetailsRepository().findByBibliographicId(bibId);
                updateBibSolrDocument(bibEntity);
            }
            return "Bib documents updated successfully.";
        }catch(Exception ex){
            logger.error(RecapCommonConstants.LOG_ERROR,ex);
            return "Bib documents failed to update.";
        }
    }

    private void updateBibSolrDocument(BibliographicEntity bibEntity) {
        SolrInputDocument bibSolrInputDocument = getBibJSONUtil().generateBibAndItemsForIndex(bibEntity, getSolrTemplate(), getBibliographicDetailsRepository(), getHoldingDetailRepository());
        StopWatch stopWatchIndexDocument = new StopWatch();
        stopWatchIndexDocument.start();
        getSolrTemplate().saveDocument(solrCore, bibSolrInputDocument);
        getSolrTemplate().commit(solrCore);
        stopWatchIndexDocument.stop();
        logger.info("Time taken to index the doc for Bib Solr Document from Deaccession Service --->{}sec", stopWatchIndexDocument.getTotalTimeSeconds());
    }

    /**
     * This method is used to update IsDeletedHoldings using holdings id in the solr.
     *
     * @param holdingsIds the holdings ids
     * @return the string
     */
    public String updateIsDeletedHoldingsByHoldingsId(@RequestBody  List<Integer> holdingsIds){
        try{
            for(Integer holdingsId : holdingsIds){
                HoldingsEntity holdingsEntity = getHoldingDetailRepository().findByHoldingsId(holdingsId);
                if(holdingsEntity != null && CollectionUtils.isNotEmpty(holdingsEntity.getBibliographicEntities())) {
                    for(BibliographicEntity bibliographicEntity : holdingsEntity.getBibliographicEntities()) {
                        updateBibSolrDocument(bibliographicEntity);
                    }
                }
            }
            return "Holdings documents updated successfully.";
        }catch(Exception ex){
            logger.error(RecapCommonConstants.LOG_ERROR,ex);
            return "Holdings documents failed to update.";
        }
    }

    /**
     * This method is used to update IsDeletedItem by using item id in the solr.
     *
     * @param itemIds the item ids
     * @return the string
     */
    public String updateIsDeletedItemByItemIds(@RequestBody  List<Integer> itemIds){
        try{
            for(Integer itemId : itemIds){
                ItemEntity itemEntity = getItemDetailsRepository().findByItemId(itemId);
                if(itemEntity != null && CollectionUtils.isNotEmpty(itemEntity.getBibliographicEntities())) {
                    for(BibliographicEntity bibliographicEntity : itemEntity.getBibliographicEntities()) {
                        updateBibSolrDocument(bibliographicEntity);
                    }
                }
            }
            return "Item documents updated successfully.";
        }catch(Exception ex){
            logger.error(RecapCommonConstants.LOG_ERROR,ex);
            return "Item documents failed to update.";
        }
    }
}
