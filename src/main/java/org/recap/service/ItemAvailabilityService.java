package org.recap.service;

import org.recap.RecapConstants;
import org.recap.controller.SharedCollectionRestController;
import org.recap.model.BibAvailabilityResponse;
import org.recap.model.BibItemAvailabityStatusRequest;
import org.recap.model.ItemAvailabilityResponse;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by premkb on 10/11/16.
 */
@Service
public class ItemAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(SharedCollectionRestController.class);

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    /**
     * This method gets item status by item's barcode and isDeleted field which is false.
     *
     * @param barcode the barcode
     * @return the item status by barcode and is deleted false
     */
    public String getItemStatusByBarcodeAndIsDeletedFalse(String barcode) {
        return itemDetailsRepository.getItemStatusByBarcodeAndIsDeletedFalse(barcode);
    }

    /**
     * This method gets item status by item's barcode and isDeleted field which is false.
     *
     * @param barcodeList the barcode list
     * @return the item status by barcode and is deleted false list
     */
    public List<ItemAvailabilityResponse> getItemStatusByBarcodeAndIsDeletedFalseList(List<String> barcodeList) {
        List<String> barcodes = new ArrayList<>();
        for (String barcode : barcodeList) {
            barcodes.add(barcode.trim());
        }
        Map<String, String> barcodeStatusMap = new HashMap<>();
        List<ItemAvailabilityResponse> itemAvailabilityResponses = new ArrayList<>();
        List<ItemEntity> itemEntityList = itemDetailsRepository.getItemStatusByBarcodeAndIsDeletedFalseList(barcodes);
        for (ItemEntity itemEntity : itemEntityList) {
            barcodeStatusMap.put(itemEntity.getBarcode(), itemEntity.getItemStatusEntity().getStatusDescription());
        }
        for (String requestedBarcode : barcodes) {
            ItemAvailabilityResponse itemAvailabilityResponse = new ItemAvailabilityResponse();
            if (barcodeStatusMap.containsKey(requestedBarcode)) {
                itemAvailabilityResponse.setItemBarcode(requestedBarcode);
                itemAvailabilityResponse.setItemAvailabilityStatus(barcodeStatusMap.get(requestedBarcode));
            } else {
                itemAvailabilityResponse.setItemBarcode(requestedBarcode);
                itemAvailabilityResponse.setItemAvailabilityStatus(RecapConstants.ITEM_BARCDE_DOESNOT_EXIST);
            }
            itemAvailabilityResponses.add(itemAvailabilityResponse);
        }
        return itemAvailabilityResponses;
    }

    /**
     * This method gets bibItem avaiablity status based on the bib item availabity status request.
     *
     * @param bibItemAvailabityStatusRequest the bib item availabity status request
     * @return the item avaiablity status
     */
    public List<BibAvailabilityResponse> getbibItemAvaiablityStatus(BibItemAvailabityStatusRequest bibItemAvailabityStatusRequest) {
        List<BibAvailabilityResponse> bibAvailabilityResponses = new ArrayList<>();
        BibliographicEntity bibliographicEntity;
        try {
            if (bibItemAvailabityStatusRequest.getInstitutionId().equalsIgnoreCase(RecapConstants.SCSB)) {
                bibliographicEntity = bibliographicDetailsRepository.findById(Integer.parseInt(bibItemAvailabityStatusRequest.getBibliographicId())).orElse(null);
            } else {
                InstitutionEntity institutionEntity = institutionDetailsRepository.findByInstitutionCode(bibItemAvailabityStatusRequest.getInstitutionId());
                bibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibId(institutionEntity.getId(), bibItemAvailabityStatusRequest.getBibliographicId());
            }
            if (bibliographicEntity != null) {
                for (ItemEntity itemEntity : bibliographicEntity.getItemEntities()) {
                    if(!itemEntity.isDeleted()) {
                        BibAvailabilityResponse bibAvailabilityResponse = new BibAvailabilityResponse();
                        bibAvailabilityResponse.setItemBarcode(itemEntity.getBarcode());
                        bibAvailabilityResponse.setItemAvailabilityStatus(itemEntity.getItemStatusEntity().getStatusCode());
                        CollectionGroupEntity collectionGroupEntity = itemEntity.getCollectionGroupEntity();
                        if(collectionGroupEntity != null && !StringUtils.isEmpty(collectionGroupEntity.getCollectionGroupDescription())){
                            bibAvailabilityResponse.setCollectionGroupDesignation(collectionGroupEntity.getCollectionGroupDescription());
                        }
                        bibAvailabilityResponses.add(bibAvailabilityResponse);
                    }
                }
            }else{
                BibAvailabilityResponse bibAvailabilityResponse = new BibAvailabilityResponse();
                bibAvailabilityResponse.setItemBarcode("");
                bibAvailabilityResponse.setErrorMessage(RecapConstants.BIB_ITEM_DOESNOT_EXIST);
                bibAvailabilityResponses.add(bibAvailabilityResponse);
            }
        } catch (Exception e) {
            logger.error(RecapConstants.EXCEPTION,e);
        }
        return bibAvailabilityResponses;
    }
}
