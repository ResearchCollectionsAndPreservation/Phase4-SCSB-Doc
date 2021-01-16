package org.recap.util;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.recap.RecapCommonConstants;
import org.recap.model.jpa.*;
import org.recap.model.solr.Item;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by angelind on 16/6/16.
 */
public class ItemJSONUtil extends MarcUtil{

    private ProducerTemplate producerTemplate;

    /**
     * This method instantiates a new item json util.
     */
    public ItemJSONUtil() {
        //Do Nothing
    }

    /**
     * This method generates item document to index in solr.
     *
     * @param itemEntity the item entity
     * @return the item
     */
    public Item generateItemForIndex(ItemEntity itemEntity) {
        try {
            Item item = new Item();
            Integer itemId = itemEntity.getId();
            item.setId(itemEntity.getOwningInstitutionId()+itemEntity.getOwningInstitutionItemId());
            item.setItemId(itemId);
            item.setOwningInstitutionItemId(itemEntity.getOwningInstitutionItemId());
            item.setBarcode(itemEntity.getBarcode());
            item.setDocType(RecapCommonConstants.ITEM);
            item.setCustomerCode(itemEntity.getCustomerCode());
            String useRestriction = StringUtils.isNotBlank(itemEntity.getUseRestrictions()) ? itemEntity.getUseRestrictions() : RecapCommonConstants.NO_RESTRICTIONS;
            item.setUseRestriction(useRestriction.replaceAll(" ", ""));
            item.setUseRestrictionDisplay(useRestriction);
            item.setVolumePartYear(itemEntity.getVolumePartYear());
            item.setCallNumberSearch(itemEntity.getCallNumber().replaceAll(" ", ""));
            item.setCallNumberDisplay(itemEntity.getCallNumber());
            item.setItemCreatedBy(itemEntity.getCreatedBy());
            item.setItemCreatedDate(itemEntity.getCreatedDate());
            item.setItemLastUpdatedBy(itemEntity.getLastUpdatedBy());
            item.setItemLastUpdatedDate(itemEntity.getLastUpdatedDate());
            item.setDeletedItem(itemEntity.isDeleted());
            item.setItemCatalogingStatus(itemEntity.getCatalogingStatus() != null ? itemEntity.getCatalogingStatus():"");
            item.setCgdChangeLog(itemEntity.getCgdChangeLog());

            List<Integer> bibIdList = new ArrayList<>();
            List<BibliographicEntity> bibliographicEntities = itemEntity.getBibliographicEntities();
            for (BibliographicEntity bibliographicEntity : bibliographicEntities){
                bibIdList.add(bibliographicEntity.getId());
            }
            item.setItemBibIdList(bibIdList);

            InstitutionEntity institutionEntity = itemEntity.getInstitutionEntity();
            String institutionCode = null != institutionEntity ? institutionEntity.getInstitutionCode() : "";
            item.setOwningInstitution(institutionCode);

            ItemStatusEntity itemStatusEntity = itemEntity.getItemStatusEntity();
            if (itemStatusEntity != null) {
                String statusCode = itemStatusEntity.getStatusCode();
                item.setAvailability(statusCode.replaceAll(" ", ""));
                item.setAvailabilityDisplay(statusCode);
            }
            CollectionGroupEntity collectionGroupEntity = itemEntity.getCollectionGroupEntity();
            if (collectionGroupEntity != null) {
                item.setCollectionGroupDesignation(collectionGroupEntity.getCollectionGroupCode());
            }

            ImsLocationEntity imsLocationEntity = itemEntity.getImsLocationEntity();
            if (imsLocationEntity != null) {
                item.setImsLocation(imsLocationEntity.getImsLocationCode());
            }

            List<Integer> holdingsIds = new ArrayList<>();
            List<HoldingsEntity> holdingsEntities = itemEntity.getHoldingsEntities();
            if (!CollectionUtils.isEmpty(holdingsEntities)) {
                for (HoldingsEntity holdingsEntity : holdingsEntities) {
                    holdingsIds.add(holdingsEntity.getId());
                    item.setHoldingsIdList(holdingsIds);
                }
            }
            return item;
        } catch (Exception e) {
            saveExceptionReportForItem(itemEntity, e);
        }
        return null;
    }

    private void saveExceptionReportForItem(ItemEntity itemEntity, Exception e) {
        List<ReportDataEntity> reportDataEntities = new ArrayList<>();

        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setCreatedDate(new Date());
        reportEntity.setType(RecapCommonConstants.SOLR_INDEX_EXCEPTION);
        reportEntity.setFileName(RecapCommonConstants.SOLR_INDEX_FAILURE_REPORT);
        InstitutionEntity institutionEntity = null != itemEntity ? itemEntity.getInstitutionEntity() : null;
        String institutionCode = null != institutionEntity ? institutionEntity.getInstitutionCode() : RecapCommonConstants.NA;
        reportEntity.setInstitutionName(institutionCode);

        ReportDataEntity docTypeDataEntity = new ReportDataEntity();
        docTypeDataEntity.setHeaderName(RecapCommonConstants.DOCTYPE);
        docTypeDataEntity.setHeaderValue(RecapCommonConstants.ITEM);
        reportDataEntities.add(docTypeDataEntity);

        ReportDataEntity owningInstDataEntity = new ReportDataEntity();
        owningInstDataEntity.setHeaderName(RecapCommonConstants.OWNING_INSTITUTION);
        owningInstDataEntity.setHeaderValue(institutionCode);
        reportDataEntities.add(owningInstDataEntity);

        ReportDataEntity exceptionMsgDataEntity = new ReportDataEntity();
        exceptionMsgDataEntity.setHeaderName(RecapCommonConstants.EXCEPTION_MSG);
        exceptionMsgDataEntity.setHeaderValue(StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.toString());
        reportDataEntities.add(exceptionMsgDataEntity);

        if(itemEntity != null && itemEntity.getId() != null) {
            ReportDataEntity itemIdDataEntity = new ReportDataEntity();
            itemIdDataEntity.setHeaderName(RecapCommonConstants.ITEM_ID);
            itemIdDataEntity.setHeaderValue(String.valueOf(itemEntity.getId()));
            reportDataEntities.add(itemIdDataEntity);
        }

        reportEntity.addAll(reportDataEntities);
        getProducerTemplate().sendBody(RecapCommonConstants.REPORT_Q, reportEntity);
    }

    /**
     * This method gets producer template.
     *
     * @return the producer template
     */
    public ProducerTemplate getProducerTemplate() {
        return producerTemplate;
    }

    /**
     * This method sets producer template.
     *
     * @param producerTemplate the producer template
     */
    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }
}
