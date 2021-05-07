package org.recap.service.accession;

import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ItemStatusEntity;
import org.recap.model.jpa.OwningInstitutionIDSequence;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.ItemStatusDetailsRepository;
import org.recap.repository.jpa.OwningInstitutionIDSequenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by premkb on 27/4/17.
 */
@Service
public class DummyDataService {

    private static final Logger logger = LoggerFactory.getLogger(DummyDataService.class);

    private Map<String,Integer> collectionGroupMap;

    private Map<String,Integer> itemStatusMap;

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    private ItemStatusDetailsRepository itemStatusDetailsRepository;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    private OwningInstitutionIDSequenceRepository owningInstitutionIDSequenceRepository;

    @Autowired
    private AccessionDAO accessionDAO;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * This method is used to create dummy record when item barcode is not found in ILS.
     * @param owningInstitutionId the owning institution id
     * @param itemBarcode         the item barcode
     * @param customerCode        the customer code
     * @return the bibliographic entity
     */
    public BibliographicEntity createDummyDataAsIncomplete(Integer owningInstitutionId, String itemBarcode, String customerCode) {
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        Date currentDate = new Date();
        try {
            updateBibWithDummyDetails(owningInstitutionId, bibliographicEntity, currentDate,ScsbCommonConstants.ACCESSION, getDummyOwningInstId());

            HoldingsEntity holdingsEntity = getHoldingsWithDummyDetails(owningInstitutionId, currentDate,ScsbCommonConstants.ACCESSION, getDummyOwningInstId());

            ItemEntity itemEntity = new ItemEntity();
            itemEntity.setCallNumberType(ScsbConstants.DUMMY_CALL_NUMBER_TYPE);
            itemEntity.setCallNumber(ScsbCommonConstants.DUMMYCALLNUMBER);
            itemEntity.setCreatedDate(currentDate);
            itemEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
            itemEntity.setLastUpdatedDate(currentDate);
            itemEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);
            itemEntity.setBarcode(itemBarcode);
            itemEntity.setOwningInstitutionItemId(getDummyOwningInstId());
            itemEntity.setOwningInstitutionId(owningInstitutionId);
            itemEntity.setCollectionGroupId((Integer) getCollectionGroupMap().get(ScsbCommonConstants.NOT_AVAILABLE_CGD));
            itemEntity.setCustomerCode(customerCode);
            itemEntity.setItemAvailabilityStatusId((Integer) getItemStatusMap().get(ScsbCommonConstants.NOT_AVAILABLE));
            itemEntity.setDeleted(false);
            itemEntity.setHoldingsEntities(Collections.singletonList(holdingsEntity));
            itemEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
            List<ItemEntity> itemEntityList = new ArrayList<>();
            itemEntityList.add(itemEntity);
            holdingsEntity.setItemEntities(itemEntityList);

            bibliographicEntity.setHoldingsEntities(Collections.singletonList(holdingsEntity));
            bibliographicEntity.setItemEntities(Collections.singletonList(itemEntity));
        } catch (Exception e) {
            logger.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return accessionDAO.saveBibRecord(bibliographicEntity);
    }

    public HoldingsEntity getHoldingsWithDummyDetails(Integer owningInstitutionId, Date currentDate, String createdBy, String owningInstitutionHoldingsId) {
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(getXmlContent(ScsbConstants.DUMMY_HOLDING_CONTENT_XML).getBytes());
        holdingsEntity.setCreatedDate(currentDate);
        holdingsEntity.setCreatedBy(createdBy);
        holdingsEntity.setLastUpdatedDate(currentDate);
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        holdingsEntity.setOwningInstitutionHoldingsId(owningInstitutionHoldingsId);
        holdingsEntity.setLastUpdatedBy(createdBy);
        return holdingsEntity;
    }

    public void updateBibWithDummyDetails(Integer owningInstitutionId, BibliographicEntity bibliographicEntity, Date currentDate,
                                          String createdBy, String owningInstitutionBibId
    ) {
        bibliographicEntity.setContent(getXmlContent(ScsbConstants.DUMMY_BIB_CONTENT_XML).getBytes());
        bibliographicEntity.setCreatedDate(currentDate);
        bibliographicEntity.setCreatedBy(createdBy);
        bibliographicEntity.setLastUpdatedBy(createdBy);
        bibliographicEntity.setLastUpdatedDate(currentDate);
        bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
    }

    private synchronized Map getCollectionGroupMap() {
        if (null == collectionGroupMap) {
            collectionGroupMap = new HashMap();
            try {
                Iterable<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
                for (Iterator iterator = collectionGroupEntities.iterator(); iterator.hasNext(); ) {
                    CollectionGroupEntity collectionGroupEntity = (CollectionGroupEntity) iterator.next();
                    collectionGroupMap.put(collectionGroupEntity.getCollectionGroupCode(), collectionGroupEntity.getId());
                }
            } catch (Exception e) {
                logger.error(ScsbConstants.EXCEPTION,e);
            }
        }
        return collectionGroupMap;
    }

    private synchronized Map getItemStatusMap() {
        if (null == itemStatusMap) {
            itemStatusMap = new HashMap();
            try {
                Iterable<ItemStatusEntity> itemStatusEntities = itemStatusDetailsRepository.findAll();
                for (ItemStatusEntity itemStatusEntity : itemStatusEntities) {
                    itemStatusMap.put(itemStatusEntity.getStatusCode(), itemStatusEntity.getId());
                }
            } catch (Exception e) {
                logger.error(ScsbConstants.EXCEPTION,e);
            }
        }
        return itemStatusMap;
    }

    private String getXmlContent(String filename) {
        InputStream inputStream = getClass().getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    out.append(line);
                }
                out.append("\n");
            }
        } catch (IOException e) {
            logger.error(ScsbConstants.EXCEPTION,e);
        }
        return out.toString();
    }

    private String getDummyOwningInstId(){
        OwningInstitutionIDSequence owningInstitutionIDSequence = new OwningInstitutionIDSequence();
        OwningInstitutionIDSequence savedOwningInstitutionIDSequence = owningInstitutionIDSequenceRepository.saveAndFlush(owningInstitutionIDSequence);
        logger.info("seq id---->{}",savedOwningInstitutionIDSequence.getId());
        return "d"+savedOwningInstitutionIDSequence.getId();
    }
}
