package org.recap.matchingalgorithm;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.CollectionUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemChangeLogEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemChangeLogDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by angelind on 6/1/17.
 */
public class MatchingAlgorithmCGDProcessor {

    private BibliographicDetailsRepository bibliographicDetailsRepository;
    private ProducerTemplate producerTemplate;
    private Map collectionGroupMap;
    private Map institutionMap;
    private ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;
    private String matchingType;
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;
    private ItemDetailsRepository itemDetailsRepository;
    private InstitutionDetailsRepository institutionDetailsRepository;

    /**
     * This method instantiates a new matching algorithm cgd processor.
     *  @param bibliographicDetailsRepository   the bibliographic details repository
     * @param producerTemplate                 the producer template
     * @param collectionGroupMap               the collection group map
     * @param institutionMap                   the institution map
     * @param itemChangeLogDetailsRepository   the item change log details repository
     * @param matchingType                     the matching type
     * @param collectionGroupDetailsRepository the collection group details repository
     * @param itemDetailsRepository            the item details repository
     * @param institutionDetailsRepository
     */
    public MatchingAlgorithmCGDProcessor(BibliographicDetailsRepository bibliographicDetailsRepository, ProducerTemplate producerTemplate, Map collectionGroupMap, Map institutionMap,
                                         ItemChangeLogDetailsRepository itemChangeLogDetailsRepository, String matchingType, CollectionGroupDetailsRepository collectionGroupDetailsRepository,
                                         ItemDetailsRepository itemDetailsRepository, InstitutionDetailsRepository institutionDetailsRepository) {
        this.bibliographicDetailsRepository = bibliographicDetailsRepository;
        this.producerTemplate = producerTemplate;
        this.collectionGroupMap = collectionGroupMap;
        this.institutionMap = institutionMap;
        this.itemChangeLogDetailsRepository = itemChangeLogDetailsRepository;
        this.matchingType = matchingType;
        this.collectionGroupDetailsRepository = collectionGroupDetailsRepository;
        this.itemDetailsRepository = itemDetailsRepository;
        this.institutionDetailsRepository=institutionDetailsRepository;
    }

    /**
     * This method updates cgd based on the counter values of use restriction for each institution and saves them in the database.
     *
     * @param itemEntityMap     the item entity map
     */
    public void updateCGDProcess(Map<Integer, ItemEntity> itemEntityMap) {
        Map<Integer, List<ItemEntity>> owningInstitutionMap=itemEntityMap.values()
                .stream()
                .collect(Collectors.groupingBy(itemEntity -> itemEntity.getInstitutionEntity().getId()));
        if(matchingType.equalsIgnoreCase(ScsbConstants.INITIAL_MATCHING_OPERATION_TYPE)) {
            /* For Initial Matching algorithm if the use restriction are same,
             * then we need to check for counter and select the item which needs to be Shared
             */
            findItemToBeSharedBasedOnCounter(itemEntityMap, owningInstitutionMap);
        } else {
            /* For Ongoing Matching algorithm if the use restriction are same,
             * then we need to check for date of accession and select the item which needs to be Shared
             */
            findItemToBeSharedBasedOnDate(itemEntityMap);
        }
        updateItemsCGD(itemEntityMap);
    }

    private void findItemToBeSharedBasedOnDate(Map<Integer, ItemEntity> itemEntityMap) {
        List<ItemEntity> itemEntities = new ArrayList<>(itemEntityMap.values());
        ItemEntity itemEntityToBeShared = getItemToBeSharedBasedOnInitialMatchingDate(itemEntities);
        if(itemEntityToBeShared != null) {
            itemEntityMap.remove(itemEntityToBeShared.getId());
            MatchingCounter.updateCGDCounter(itemEntityToBeShared.getInstitutionEntity().getInstitutionCode(),false);
        } else {
            itemEntities.sort(Comparator.comparing(ItemEntity::getCreatedDate));
            findAndremoveSharedItem(itemEntityMap, itemEntities);
        }
    }

    private ItemEntity getItemToBeSharedBasedOnInitialMatchingDate (List<ItemEntity> itemEntities) {
        for(ItemEntity itemEntity : itemEntities) {
            if(itemEntity.getInitialMatchingDate() != null) {
                return itemEntity;
            }
        }
        return null;
    }

    /**
     * Update items cgd.
     *
     * @param itemEntityMap the item entity map
     */
    public void updateItemsCGD(Map<Integer, ItemEntity> itemEntityMap) {
        List<ItemEntity> itemEntitiesToUpdate = new ArrayList<>();
        List<ItemChangeLogEntity> itemChangeLogEntities = new ArrayList<>();
        CollectionGroupEntity collectionGroupEntity = null;
        Integer collectionGroupId = (Integer) collectionGroupMap.get(ScsbCommonConstants.REPORTS_OPEN);
        if(matchingType.equalsIgnoreCase(ScsbCommonConstants.ONGOING_MATCHING_ALGORITHM)) {
            collectionGroupEntity = collectionGroupDetailsRepository.findById(collectionGroupId).orElse(null);
        }
        for (Iterator<ItemEntity> iterator = itemEntityMap.values().iterator(); iterator.hasNext(); ) {
            // Items which needs to be changed to open status
            ItemEntity itemEntity = iterator.next();
            MatchingCounter.updateCGDCounter(itemEntity.getInstitutionEntity().getInstitutionCode(),true);
            Integer oldCgd = itemEntity.getCollectionGroupId();
            itemEntity.setLastUpdatedDate(new Date());
            itemEntity.setCollectionGroupId(collectionGroupId);
            if(matchingType.equalsIgnoreCase(ScsbCommonConstants.ONGOING_MATCHING_ALGORITHM)) {
                itemEntity.setCollectionGroupEntity(collectionGroupEntity);
                itemEntity.setLastUpdatedBy(ScsbCommonConstants.ONGOING_MATCHING_ALGORITHM);
            } else {
                itemEntity.setInitialMatchingDate(null);
                itemEntity.setLastUpdatedBy(ScsbConstants.INITIAL_MATCHING_OPERATION_TYPE);
            }
            itemEntitiesToUpdate.add(itemEntity);
            itemChangeLogEntities.add(getItemChangeLogEntity(oldCgd, itemEntity));
        }
        if(CollectionUtils.isNotEmpty(itemEntitiesToUpdate) && CollectionUtils.isNotEmpty(itemChangeLogEntities)) {
            if(matchingType.equalsIgnoreCase(ScsbCommonConstants.ONGOING_MATCHING_ALGORITHM)) {
                itemDetailsRepository.saveAll(itemEntitiesToUpdate);
            } else {
                producerTemplate.sendBody("scsbactivemq:queue:updateItemsQ", itemEntitiesToUpdate);
            }
            itemChangeLogDetailsRepository.saveAll(itemChangeLogEntities);
        }
    }

    private ItemChangeLogEntity getItemChangeLogEntity(Integer oldCgd, ItemEntity itemEntity) {
        ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
        itemChangeLogEntity.setOperationType(matchingType);
        itemChangeLogEntity.setUpdatedBy(ScsbCommonConstants.GUEST);
        itemChangeLogEntity.setUpdatedDate(new Date());
        itemChangeLogEntity.setRecordId(itemEntity.getId());
        itemChangeLogEntity.setNotes(oldCgd + " - " + itemEntity.getCollectionGroupId());
        return itemChangeLogEntity;
    }

    /**
     * This method checks whether the item is monograph or monographic set and populates value for monograph item.
     *
     * @param materialTypeSet   the material type set
     * @param itemEntityMap     the item entity map
     * @param bibIdList         the bib id list
     * @return the boolean
     */
    public boolean checkForMonographAndPopulateValues(Set<String> materialTypeSet,Map<Integer, ItemEntity> itemEntityMap, List<Integer> bibIdList) {
        boolean isMonograph = true;
        List<BibliographicEntity> bibliographicEntities = bibliographicDetailsRepository.findByIdIn(bibIdList);
        for(BibliographicEntity bibliographicEntity : bibliographicEntities) {
            List<ItemEntity> itemEntities = bibliographicEntity.getNonDeletedAndCompleteItemEntities();
            //Check for Monograph - (Single Bib & Single Item)
            if(itemEntities != null && itemEntities.size() == 1) {
                ItemEntity itemEntity = itemEntities.get(0);
                if(itemEntity.getCollectionGroupId().equals(collectionGroupMap.get(ScsbCommonConstants.SHARED_CGD))) {
                    populateValues(itemEntityMap, itemEntity);
                }
                materialTypeSet.add(ScsbCommonConstants.MONOGRAPH);
            } else {
                if(bibliographicEntity.getOwningInstitutionId().equals(institutionMap.get("NYPL"))) {
                    //NYPL
                    boolean isMultipleCopy = false;
                    for(ItemEntity itemEntity : itemEntities) {
                        if(itemEntity != null) {
                            if(itemEntity.getCopyNumber() != null && itemEntity.getCopyNumber() > 1) {
                                isMultipleCopy = true;
                            }
                            if(itemEntity.getCollectionGroupId().equals(collectionGroupMap.get(ScsbCommonConstants.SHARED_CGD))) {
                                populateValues(itemEntityMap, itemEntity);
                            }
                        }
                    }
                    if(!isMultipleCopy) {
                        isMonograph = false;
                        materialTypeSet.add(ScsbConstants.MONOGRAPHIC_SET);
                    } else {
                        materialTypeSet.add(ScsbCommonConstants.MONOGRAPH);
                    }
                } else {
                    //CUL & PUL
                    if(bibliographicEntity.getHoldingsEntities().size() > 1) {
                        for(ItemEntity itemEntity : itemEntities) {
                            if(itemEntity.getCollectionGroupId().equals(collectionGroupMap.get(ScsbCommonConstants.SHARED_CGD))) {
                                populateValues(itemEntityMap, itemEntity);
                            }
                        }
                        materialTypeSet.add(ScsbCommonConstants.MONOGRAPH);
                    } else {
                        for(ItemEntity itemEntity : itemEntities) {
                            if(itemEntity.getCollectionGroupId().equals(collectionGroupMap.get(ScsbCommonConstants.SHARED_CGD))) {
                                populateValues(itemEntityMap, itemEntity);
                            }
                        }
                        isMonograph = false;
                        materialTypeSet.add(ScsbConstants.MONOGRAPHIC_SET);
                    }
                }
            }
        }
        return isMonograph;
    }

    /**
     * Populate item entity map.
     *
     * @param itemEntityMap the item entity map
     * @param bibIdList     the bib id list
     */
    public void populateItemEntityMap(Map<Integer, ItemEntity> itemEntityMap, List<Integer> bibIdList) {
        List<BibliographicEntity> bibliographicEntities = bibliographicDetailsRepository.findByIdIn(bibIdList);
        for(BibliographicEntity bibliographicEntity : bibliographicEntities) {
            List<ItemEntity> itemEntities = bibliographicEntity.getNonDeletedAndCompleteItemEntities();
            for(ItemEntity itemEntity : itemEntities) {
                if(itemEntity.getCollectionGroupId().equals(collectionGroupMap.get(ScsbCommonConstants.SHARED_CGD))) {
                    itemEntityMap.put(itemEntity.getId(), itemEntity);
                }
            }
        }
    }

    /**
     * This method is used to populate values for the monograph based on the given materialTypeSet, itemEntityMap and itemEntity.
     * @param itemEntityMap
     * @param itemEntity
     */
    private void populateValues(Map<Integer, ItemEntity> itemEntityMap, ItemEntity itemEntity) {
        itemEntityMap.put(itemEntity.getId(), itemEntity);
    }

    private void findItemToBeSharedBasedOnCounter(Map<Integer, ItemEntity> itemEntityMap, Map<Integer, List<ItemEntity>> institutionMap) {
        Set<Integer> owningInstitutions = institutionMap.keySet();
        Map<Integer, List<Integer>> counterMap = new HashMap<>();
        for (Iterator<Integer> iterator = owningInstitutions.iterator(); iterator.hasNext();) {
            Integer institution = iterator.next();
            populateCounterMap(counterMap, institution);
        }
        if(counterMap.size() > 1) {
            // Different Counter Values
            Integer count = Collections.min(counterMap.keySet());
            List<Integer> institutionList = counterMap.get(count);
            if(CollectionUtils.isNotEmpty(institutionList)) {
                // Institution to which item to be remained Shared
                Integer institution = institutionList.get(0);
                List<ItemEntity> itemEntities = institutionMap.get(institution);
                findAndremoveSharedItem(itemEntityMap, itemEntities);
            }
        } else {
            // The counter values are same across one or more institutions
            for (Iterator<List<Integer>> iterator = counterMap.values().iterator(); iterator.hasNext(); ) {
                List<Integer> institutions =  iterator.next();
                // Institution to which item to be remained Shared
                Integer institution = institutions.get(0);
                List<ItemEntity> itemEntities = institutionMap.get(institution);
                findAndremoveSharedItem(itemEntityMap, itemEntities);
            }
        }
    }

    private void findAndremoveSharedItem(Map<Integer, ItemEntity> itemEntityMap, List<ItemEntity> itemEntities) {
        // Item which needs to remain in Shared status and increment the institution's counter
        ItemEntity itemEntity = itemEntities.get(0);
        itemEntityMap.remove(itemEntity.getId());
        if(matchingType.equalsIgnoreCase(ScsbConstants.INITIAL_MATCHING_OPERATION_TYPE)) {
            MatchingCounter.updateCGDCounter(itemEntity.getInstitutionEntity().getInstitutionCode(),false);
            itemEntity.setInitialMatchingDate(new Date());
            producerTemplate.sendBody("scsbactivemq:queue:updateItemsQ", itemEntity);
        }
        else{
            MatchingCounter.updateCGDCounter(itemEntity.getInstitutionEntity().getInstitutionCode(),false);
        }
    }

    private void populateCounterMap(Map<Integer, List<Integer>> counterMap, Integer institution) {
        Optional<InstitutionEntity> institutionEntity = institutionDetailsRepository.findById(institution);
        if(institutionEntity.isPresent()) {
            Integer counter = getCounterForGivenInst(institutionEntity.get().getInstitutionCode());
            if(counterMap.containsKey(counter)) {
                List<Integer> institutions = new ArrayList<>(counterMap.get(counter));
                institutions.add(institution);
                counterMap.put(counter, institutions);
            } else {
                counterMap.put(counter, Arrays.asList(institution));
            }
        }
    }

    private Integer getCounterForGivenInst(String institution) {
        return MatchingCounter.getSpecificInstitutionCounterMap(institution).get(ScsbConstants.MATCHING_COUNTER_SHARED);
    }
}
