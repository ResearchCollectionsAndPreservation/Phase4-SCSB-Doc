package org.recap.repository.solr.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.search.SearchRecordsRequest;
import org.recap.model.search.resolver.HoldingsValueResolver;
import org.recap.model.solr.BibItem;
import org.recap.model.solr.Holdings;
import org.recap.model.solr.Item;
import org.recap.repository.solr.main.CustomDocumentRepository;
import org.recap.util.CommonUtil;
import org.recap.util.SolrQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by rajeshbabuk on 8/7/16.
 */
@Repository
public class BibSolrDocumentRepositoryImpl implements CustomDocumentRepository {

    private static final Logger logger = LoggerFactory.getLogger(BibSolrDocumentRepositoryImpl.class);

    @Resource(name = "recapSolrTemplate")
    private SolrTemplate solrTemplate;

    @Autowired
    private SolrQueryBuilder solrQueryBuilder;

    @Autowired
    private CommonUtil commonUtil;

    @Override
    public Map<String,Object> search(SearchRecordsRequest searchRecordsRequest) {
        List<BibItem> bibItems = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        try {
            if (isEmptyField(searchRecordsRequest)) {
                searchRecordsRequest.setShowTotalCount(true);
                searchRecordsRequest.setFieldName(RecapCommonConstants.ALL_FIELDS);
                bibItems = searchByBib(searchRecordsRequest);
                if(CollectionUtils.isEmpty(bibItems)) {
                    bibItems = searchByItem(searchRecordsRequest);
                }
                searchRecordsRequest.setFieldName("");
            } else if (isItemField(searchRecordsRequest)) {
                bibItems = searchByItem(searchRecordsRequest);
            } else {
                bibItems = searchByBib(searchRecordsRequest);
            }
            response.put(RecapCommonConstants.SEARCH_SUCCESS_RESPONSE, bibItems);
        } catch (IOException|SolrServerException e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
            response.put(RecapCommonConstants.SEARCH_ERROR_RESPONSE, e.getMessage());
        }
        return response;
    }

    private boolean isEmptyField(SearchRecordsRequest searchRecordsRequest) {
        return StringUtils.isBlank(searchRecordsRequest.getFieldName()) && StringUtils.isNotBlank(searchRecordsRequest.getFieldValue());
    }

    private List<BibItem> searchByItem(SearchRecordsRequest searchRecordsRequest) throws SolrServerException, IOException {
        List<BibItem> bibItems = new ArrayList<>();
        SolrQuery queryForChildAndParentCriteria = solrQueryBuilder.getQueryForChildAndParentCriteria(searchRecordsRequest);
        queryForChildAndParentCriteria.setStart(searchRecordsRequest.getPageNumber() * searchRecordsRequest.getPageSize());
        queryForChildAndParentCriteria.setRows(searchRecordsRequest.getPageSize());
        if (searchRecordsRequest.isSortIncompleteRecords()){
            queryForChildAndParentCriteria.setSort(RecapConstants.ITEM_CREATED_DATE, SolrQuery.ORDER.desc);
        }
        else {
            queryForChildAndParentCriteria.setSort(RecapCommonConstants.TITLE_SORT, SolrQuery.ORDER.asc);
        }
        QueryResponse queryResponse = solrTemplate.getSolrClient().query(queryForChildAndParentCriteria);
        SolrDocumentList itemSolrDocumentList = queryResponse.getResults();
        if (CollectionUtils.isNotEmpty(itemSolrDocumentList)) {
            setCountsByItem(searchRecordsRequest, itemSolrDocumentList);
            for (SolrDocument itemSolrDocument : itemSolrDocumentList) {
                Item item = commonUtil.getItem(itemSolrDocument);
                bibItems.addAll(getBibItemsAndHoldings(item, searchRecordsRequest.isDeleted(), searchRecordsRequest.getCatalogingStatus()));
            }
        }
        return bibItems;
    }

    private List<BibItem> searchByBib(SearchRecordsRequest searchRecordsRequest) throws SolrServerException, IOException {
        List<BibItem> bibItems = new ArrayList<>();
        SolrQuery queryForParentAndChildCriteria = solrQueryBuilder.getQueryForParentAndChildCriteria(searchRecordsRequest);
        queryForParentAndChildCriteria.setStart(searchRecordsRequest.getPageNumber() * searchRecordsRequest.getPageSize());
        queryForParentAndChildCriteria.setRows(searchRecordsRequest.getPageSize());
        queryForParentAndChildCriteria.setSort(RecapCommonConstants.TITLE_SORT, SolrQuery.ORDER.asc);
        QueryResponse queryResponse = solrTemplate.getSolrClient().query(queryForParentAndChildCriteria);
        SolrDocumentList bibSolrDocumentList = queryResponse.getResults();
        if(CollectionUtils.isNotEmpty(bibSolrDocumentList)) {
            setCountsByBib(searchRecordsRequest, bibSolrDocumentList);
            for (SolrDocument bibSolrDocument : bibSolrDocumentList) {
                BibItem bibItem = new BibItem();
                populateBibItem(bibSolrDocument, bibItem);
                populateItemHoldingsInfo(bibItem, searchRecordsRequest.isDeleted(), searchRecordsRequest.getCatalogingStatus());
                bibItems.add(bibItem);
            }
        }
        return bibItems;
    }

    private List<BibItem> getBibItemsAndHoldings(Item item, boolean isDeleted, String catalogingStatus) {
        List<BibItem> bibItems = new ArrayList<>();
        SolrQuery solrQueryForBib = solrQueryBuilder.getSolrQueryForBibItem("_root_:" + item.getRoot());
        try {
            SolrDocumentList solrDocuments = commonUtil.getSolrDocumentsByDocType(solrQueryForBib, solrTemplate);
            BibItem bibItem = new BibItem();
            for (Iterator<SolrDocument> iterator = solrDocuments.iterator(); iterator.hasNext(); ) {
                SolrDocument solrDocument = iterator.next();
                String docType = (String) solrDocument.getFieldValue(RecapCommonConstants.DOCTYPE);
                if (docType.equalsIgnoreCase(RecapCommonConstants.BIB)) {
                    boolean isDeletedBib = (boolean) solrDocument.getFieldValue(RecapCommonConstants.IS_DELETED_BIB);
                    String bibCatalogingStatus = (String) solrDocument.getFieldValue(RecapConstants.BIB_CATALOGING_STATUS);
                    if (isDeletedBib == isDeleted && catalogingStatus.equals(bibCatalogingStatus)) {
                        populateBibItem(solrDocument, bibItem);
                        bibItem.setItems(Collections.singletonList(item));
                    }
                }
                addHoldingsToBibItem(isDeleted, bibItem, solrDocument, docType);
            }
            bibItems.add(bibItem);
        } catch (IOException|SolrServerException e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
        return bibItems;
    }

    private void addHoldingsToBibItem(boolean isDeleted, BibItem bibItem, SolrDocument solrDocument, String docType) {
        if (docType.equalsIgnoreCase(RecapCommonConstants.HOLDINGS)) {
            boolean isDeletedHoldings = (boolean) solrDocument.getFieldValue(RecapCommonConstants.IS_DELETED_HOLDINGS);
            if (isDeletedHoldings == isDeleted) {
                Holdings holdings = getHoldings(solrDocument);
                bibItem.addHoldings(holdings);
            }
        }
    }

    /**
     * Populate item holdings info based on isdeleted flag and item cataloging status.
     *
     * @param bibItem          the bib item
     * @param isDeleted        the is deleted
     * @param catalogingStatus the cataloging status
     */
    public void populateItemHoldingsInfo(BibItem bibItem, boolean isDeleted, String catalogingStatus) {
        SolrQuery solrQueryForItem = solrQueryBuilder.getSolrQueryForBibItem("_root_:" + bibItem.getRoot());
        try {
            SolrDocumentList solrDocuments = commonUtil.getSolrDocumentsByDocType(solrQueryForItem, solrTemplate);
            for (Iterator<SolrDocument> iterator = solrDocuments.iterator(); iterator.hasNext(); ) {
                SolrDocument solrDocument = iterator.next();
                String docType = (String) solrDocument.getFieldValue(RecapCommonConstants.DOCTYPE);
                if(docType.equalsIgnoreCase(RecapCommonConstants.ITEM)) {
                    boolean isDeletedItem = (boolean) solrDocument.getFieldValue(RecapCommonConstants.IS_DELETED_ITEM);
                    String itemCatalogingStatus = (String) solrDocument.getFieldValue(RecapConstants.ITEM_CATALOGING_STATUS);
                    if (isDeletedItem == isDeleted && catalogingStatus.equals(itemCatalogingStatus)) {
                        Item item = commonUtil.getItem(solrDocument);
                        bibItem.addItem(item);
                    }
                }
                addHoldingsToBibItem(isDeleted, bibItem, solrDocument, docType);
            }
        } catch (IOException|SolrServerException e) {
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
    }

    private boolean isItemField(SearchRecordsRequest searchRecordsRequest) {
        return StringUtils.isNotBlank(searchRecordsRequest.getFieldName())
                && (searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapCommonConstants.BARCODE)
                || searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapCommonConstants.CALL_NUMBER)
                || searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapConstants.ITEM_CATALOGING_STATUS)
                || searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapCommonConstants.CUSTOMER_CODE)
                || searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapConstants.IMS_LOCATION_CODE));
    }

    private void setCountsByBib(SearchRecordsRequest searchRecordsRequest, SolrDocumentList bibSolrDocuments) throws IOException, SolrServerException {
        long numFound = bibSolrDocuments.getNumFound();
        String totalBibCount = NumberFormat.getNumberInstance().format(numFound);
        searchRecordsRequest.setTotalBibRecordsCount(totalBibCount);
        searchRecordsRequest.setTotalRecordsCount(totalBibCount);
        if(!searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapCommonConstants.ALL_FIELDS)) {
            String totalItemCount = NumberFormat.getNumberInstance().format(getItemCountsForBib(searchRecordsRequest));
            searchRecordsRequest.setTotalItemRecordsCount(totalItemCount);
        }
        int totalPagesCount = (int) Math.ceil((double) numFound / (double) searchRecordsRequest.getPageSize());
        searchRecordsRequest.setTotalPageCount(totalPagesCount);
    }

    private long getItemCountsForBib(SearchRecordsRequest searchRecordsRequest) throws IOException, SolrServerException {
        SolrQuery queryForChildAndParentCriteria = solrQueryBuilder.getCountQueryForChildAndParentCriteria(searchRecordsRequest);
        QueryResponse queryResponse = solrTemplate.getSolrClient().query(queryForChildAndParentCriteria);
        return queryResponse.getResults().getNumFound();
    }

    private void setCountsByItem(SearchRecordsRequest searchRecordsRequest, SolrDocumentList itemSolrDocuments) throws IOException, SolrServerException {
        long numFound = itemSolrDocuments.getNumFound();
        String totalItemCount = NumberFormat.getNumberInstance().format(numFound);
        searchRecordsRequest.setTotalItemRecordsCount(totalItemCount);
        searchRecordsRequest.setTotalRecordsCount(totalItemCount);
        if(!searchRecordsRequest.getFieldName().equalsIgnoreCase(RecapCommonConstants.ALL_FIELDS)) {
            String totalBibCount = NumberFormat.getNumberInstance().format(getBibCountsForItem(searchRecordsRequest));
            searchRecordsRequest.setTotalBibRecordsCount(totalBibCount);
        }
        int totalPagesCount = (int) Math.ceil((double) numFound / (double) searchRecordsRequest.getPageSize());
        searchRecordsRequest.setTotalPageCount(totalPagesCount);
    }

    private long getBibCountsForItem(SearchRecordsRequest searchRecordsRequest) throws IOException, SolrServerException {
        SolrQuery queryForParentAndChildCriteria = solrQueryBuilder.getCountQueryForParentAndChildCriteria(searchRecordsRequest);
        QueryResponse queryResponse = solrTemplate.getSolrClient().query(queryForParentAndChildCriteria);
        return queryResponse.getResults().getNumFound();
    }

    /**
     * Gets holdings for the give holdings solr document.
     *
     * @param holdingsSolrDocument the holdings solr document
     * @return the holdings
     */
    public Holdings getHoldings(SolrDocument holdingsSolrDocument) {
        Holdings holdings = new Holdings();
        Collection<String> fieldNames = holdingsSolrDocument.getFieldNames();
        List<HoldingsValueResolver> holdingsValueResolvers = commonUtil.getHoldingsValueResolvers();
        for (Iterator<String> iterator = fieldNames.iterator(); iterator.hasNext(); ) {
            String fieldName = iterator.next();
            Object fieldValue = holdingsSolrDocument.getFieldValue(fieldName);
            for (Iterator<HoldingsValueResolver> holdingsValueResolverIterator = holdingsValueResolvers.iterator(); holdingsValueResolverIterator.hasNext(); ) {
                HoldingsValueResolver holdingsValueResolver = holdingsValueResolverIterator.next();
                if(holdingsValueResolver.isInterested(fieldName)) {
                    holdingsValueResolver.setValue(holdings, fieldValue);
                }
            }
        }
        return holdings;
    }

    /**
     * Populate bib item based on input solr document.
     *
     * @param solrDocument the solr document
     * @param bibItem      the bib item
     */
    public void populateBibItem(SolrDocument solrDocument, BibItem bibItem) {
        Collection<String> fieldNames = solrDocument.getFieldNames();
        commonUtil.getBibItemFromSolrFieldNames(solrDocument, fieldNames, bibItem);
    }

    @Override
    public Integer getPageNumberOnPageSizeChange(SearchRecordsRequest searchRecordsRequest) {
        int totalRecordsCount;
        Integer pageNumber = searchRecordsRequest.getPageNumber();
        try {
            if (isEmptyField(searchRecordsRequest)) {
                totalRecordsCount = NumberFormat.getNumberInstance().parse(searchRecordsRequest.getTotalRecordsCount()).intValue();
            } else if (isItemField(searchRecordsRequest)) {
                totalRecordsCount = NumberFormat.getNumberInstance().parse(searchRecordsRequest.getTotalItemRecordsCount()).intValue();
            } else {
                totalRecordsCount = NumberFormat.getNumberInstance().parse(searchRecordsRequest.getTotalBibRecordsCount()).intValue();
            }
            int totalPagesCount = (int) Math.ceil((double) totalRecordsCount / (double) searchRecordsRequest.getPageSize());
            if (totalPagesCount > 0 && pageNumber >= totalPagesCount) {
                pageNumber = totalPagesCount - 1;
            }
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
        return pageNumber;
    }

}
