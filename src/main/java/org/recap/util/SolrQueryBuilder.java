package org.recap.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.MatchingMatchPointsEntity;
import org.recap.model.reports.TitleMatchedReport;
import org.recap.model.search.SearchRecordsRequest;
import org.springframework.stereotype.Component;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by peris on 9/30/16.
 */
@Component
public class SolrQueryBuilder {

    private String and = " AND ";
    private String or = " OR ";
    private String coreParentFilterQuery = "{!parent which=\"ContentType:parent\"}";
    private String coreChildFilterQuery = "{!child of=\"ContentType:parent\"}";

    /**
     * Gets query string for item criteria for parent.
     *
     * @param searchRecordsRequest the search records request
     * @return the query string for item criteria for parent
     */
    public String getQueryStringForItemCriteriaForParent(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();

        List<String> availability = searchRecordsRequest.getAvailability();
        List<String> collectionGroupDesignations = searchRecordsRequest.getCollectionGroupDesignations();
        List<String> useRestrictions = searchRecordsRequest.getUseRestrictions();
        List<String> imsDepositoryCodes = searchRecordsRequest.getImsDepositoryCodes();

        if (CollectionUtils.isNotEmpty(availability)) {
            stringBuilder.append(buildQueryForFilterGivenChild(ScsbCommonConstants.AVAILABILITY, availability));
        }
        appendToQueryByFieldType(stringBuilder, collectionGroupDesignations, ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION);
        appendToQueryByFieldType(stringBuilder, useRestrictions, ScsbCommonConstants.USE_RESTRICTION);
        appendToQueryByFieldType(stringBuilder, imsDepositoryCodes, ScsbConstants.IMS_LOCATION_CODE);
        stringBuilder
                .append(and)
                .append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(searchRecordsRequest.isDeleted())
                .append(and)
                .append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(searchRecordsRequest.getCatalogingStatus());

        return coreParentFilterQuery + "(" + stringBuilder.toString() + ")";
    }

    private void appendToQueryByFieldType(StringBuilder stringBuilder, List<String> values, String fieldName) {
        if (StringUtils.isNotBlank(stringBuilder.toString()) && CollectionUtils.isNotEmpty(values)) {
            stringBuilder.append(and).append(buildQueryForFilterGivenChild(fieldName, values));
        } else if (CollectionUtils.isNotEmpty(values)) {
            stringBuilder.append(buildQueryForFilterGivenChild(fieldName, values));
        }
    }

    private String buildQueryForFilterGivenChild(String fieldName, List<String> values) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            String value = iterator.next();
            stringBuilder.append(fieldName).append(":").append(value);
            if (iterator.hasNext()) {
                stringBuilder.append(or);
            }
        }
        return "(" + stringBuilder.toString() + ")";
    }

    /**
     * Gets query string for parent criteria for child.
     *
     * @param searchRecordsRequest the search records request
     * @return the query string for parent criteria for child
     */
    public String getQueryStringForParentCriteriaForChild(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(buildQueryForBibFacetCriteria(searchRecordsRequest));
        stringBuilder
                .append(and).append(coreChildFilterQuery)
                .append(ScsbCommonConstants.IS_DELETED_BIB).append(":").append(searchRecordsRequest.isDeleted())
                .append(and).append(coreChildFilterQuery)
                .append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(searchRecordsRequest.getCatalogingStatus());
        return stringBuilder.toString();
    }

    private String buildQueryForBibFacetCriteria(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> owningInstitutions = searchRecordsRequest.getOwningInstitutions();
        List<String> materialTypes = searchRecordsRequest.getMaterialTypes();

        if (CollectionUtils.isNotEmpty(owningInstitutions)) {
            stringBuilder.append(buildQueryForParentGivenChild(ScsbCommonConstants.BIB_OWNING_INSTITUTION, owningInstitutions, coreChildFilterQuery));
        }
        if (StringUtils.isNotBlank(stringBuilder.toString()) && CollectionUtils.isNotEmpty(materialTypes)) {
            stringBuilder.append(and).append(buildQueryForParentGivenChild(ScsbCommonConstants.LEADER_MATERIAL_TYPE, materialTypes, coreChildFilterQuery));
        } else if (CollectionUtils.isNotEmpty(materialTypes)) {
            stringBuilder.append(buildQueryForParentGivenChild(ScsbCommonConstants.LEADER_MATERIAL_TYPE, materialTypes, coreChildFilterQuery));
        }
        return stringBuilder.toString();
    }

    /**
     * Gets query string for match child return parent.
     *
     * @param searchRecordsRequest the search records request
     * @return the query string for match child return parent
     */
    public String getQueryStringForMatchChildReturnParent(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> owningInstitutions = searchRecordsRequest.getOwningInstitutions();
        if (CollectionUtils.isNotEmpty(owningInstitutions)) {
            stringBuilder.append(buildQueryForMatchChildReturnParent(ScsbCommonConstants.BIB_OWNING_INSTITUTION, owningInstitutions));
        }

        List<String> materialTypes = searchRecordsRequest.getMaterialTypes();
        return buildQueryByFieldType(stringBuilder, materialTypes, ScsbCommonConstants.LEADER_MATERIAL_TYPE);
    }

    private String buildQueryByFieldType(StringBuilder stringBuilder, List<String> materialTypes, String leaderMaterialType) {
        if (StringUtils.isNotBlank(stringBuilder.toString()) && CollectionUtils.isNotEmpty(materialTypes)) {
            stringBuilder.append(and).append(buildQueryForMatchChildReturnParent(leaderMaterialType, materialTypes));
        } else if (CollectionUtils.isNotEmpty(materialTypes)) {
            stringBuilder.append(buildQueryForMatchChildReturnParent(leaderMaterialType, materialTypes));
        }
        return stringBuilder.toString();
    }

    /**
     * Gets query string for match parent return child.
     *
     * @param searchRecordsRequest the search records request
     * @return the query string for match parent return child
     */
    public String getQueryStringForMatchParentReturnChild(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> availability = searchRecordsRequest.getAvailability();
        if (CollectionUtils.isNotEmpty(availability)) {
            stringBuilder.append(buildQueryForMatchChildReturnParent(ScsbCommonConstants.AVAILABILITY, availability));
        }
        buildQueryByFieldType(stringBuilder, searchRecordsRequest.getCollectionGroupDesignations(), ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION);
        buildQueryByFieldType(stringBuilder, searchRecordsRequest.getUseRestrictions(), ScsbCommonConstants.USE_RESTRICTION);
        return buildQueryByFieldType(stringBuilder, searchRecordsRequest.getImsDepositoryCodes(), ScsbConstants.IMS_LOCATION_CODE);
    }

    /**
     * Gets query string for match parent return child for deleted data dump cgd to private.
     *
     * @return the query string for match parent return child for deleted data dump cgd to private
     */
    public String getQueryStringForMatchParentReturnChildForDeletedDataDumpCGDToPrivate() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(buildQueryForMatchChildReturnParent(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION, Arrays.asList(ScsbCommonConstants.PRIVATE)));
        return stringBuilder.toString();
    }

    /**
     * This method is used to build query for parent using the given field name,list of values and child query.
     * @param fieldName
     * @param values
     * @param parentQuery
     * @return
     */
    private String buildQueryForParentGivenChild(String fieldName, List<String> values, String parentQuery) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            String value = iterator.next();
            stringBuilder.append(parentQuery).append(fieldName).append(":").append(value);
            if (iterator.hasNext()) {
                stringBuilder.append(or);
            }
        }
        return "(" + stringBuilder.toString() + ")";
    }

    /**
     * This method is used to build query for the given fieldName and list of values.
     * @param fieldName
     * @param values
     * @return
     */
    private String buildQueryForMatchChildReturnParent(String fieldName, List<String> values) {
        List<String> modifiedValues = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(values)) {
            for (String value : values) {
                modifiedValues.add("\"" + value.trim() + "\"");
            }
        }
        return fieldName + ":" + "(" + StringUtils.join(modifiedValues, " ") + ")";
    }

    /**
     * IF the getQueryForFieldCriteria() is called with Item field/value combination, the query would still return
     * only Bib Criteria. You will need to call getItemSolrQueryForCriteria()
     *
     * @param searchRecordsRequest the search records request
     * @return the query for field criteria
     * @throws Exception
     */
    public String getQueryForFieldCriteria(SearchRecordsRequest searchRecordsRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        String fieldValue = parseSearchRequest(searchRecordsRequest.getFieldValue().trim());
        String fieldName = searchRecordsRequest.getFieldName();

        if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            //The following "if" condition is for exact match (i.e string data type fields in Solr)
            //Author, Title, Publisher, Publication Place, Publication date, Subjet & Notes.
            if(!(fieldName.equalsIgnoreCase(ScsbCommonConstants.BARCODE) || fieldName.equalsIgnoreCase(ScsbCommonConstants.CALL_NUMBER) || fieldName.equalsIgnoreCase(ScsbCommonConstants.CUSTOMER_CODE)
                    || fieldName.equalsIgnoreCase(ScsbCommonConstants.ISBN_CRITERIA) || fieldName.equalsIgnoreCase(ScsbCommonConstants.OCLC_NUMBER) || fieldName.equalsIgnoreCase(ScsbCommonConstants.ISSN_CRITERIA))) {

                if(fieldName.contains(ScsbConstants.DATE) && !fieldName.equalsIgnoreCase(ScsbCommonConstants.PUBLICATION_DATE)){
                    stringBuilder.append(fieldName).append(":").append("[");
                    stringBuilder.append(fieldValue).append("]");
                    return stringBuilder.toString();
                }

                String[] fieldValues = fieldValue.split("\\s+");

                if(fieldName.equalsIgnoreCase(ScsbCommonConstants.TITLE_STARTS_WITH)) {
                    stringBuilder.append(fieldName).append(":").append("(");
                    stringBuilder.append("\"").append(fieldValues[0]).append("\"").append(")");
                } else {
                    if(fieldValues.length > 1) {
                        List<String> fieldValuesList = Arrays.asList(fieldValues);
                        for (Iterator<String> iterator = fieldValuesList.iterator(); iterator.hasNext(); ) {
                            String value = iterator.next();
                            stringBuilder.append(fieldName).append(":").append("(").append("\"");
                            stringBuilder.append(value).append("\"").append(")");
                            if (iterator.hasNext()) {
                                stringBuilder.append(and);
                            }
                        }
                    } else {
                        stringBuilder.append(fieldName).append(":").append("(");
                        stringBuilder.append("\"").append(fieldValue).append("\"").append(")");
                    }
                }
            } else {
                //Check for item fields.
                if(fieldName.equalsIgnoreCase(ScsbCommonConstants.CALL_NUMBER)){
                    fieldValue = fieldValue.replace(" ", "");
                }
                if (fieldName.equalsIgnoreCase(ScsbCommonConstants.BARCODE)) {
                    String[] fieldValues = fieldValue.split(",");
                    if (ArrayUtils.isNotEmpty(fieldValues)) {
                        stringBuilder.append(buildQueryForMatchChildReturnParent(fieldName, Arrays.asList(fieldValues)));
                    }
                }
                //Check for Bib fields.
                else {
                    stringBuilder.append(fieldName).append(":").append("(");
                    stringBuilder.append("\"").append(fieldValue).append("\"").append(")");
                }
            }
            return stringBuilder.toString();
        }
        return "";
    }

    /**
     * Gets count query for field criteria.
     *
     * @param searchRecordsRequest the search records request
     * @param parentQuery          the parent query
     * @return the count query for field criteria
     */
    public String getCountQueryForFieldCriteria(SearchRecordsRequest searchRecordsRequest, String parentQuery) {
        StringBuilder stringBuilder = new StringBuilder();
        String fieldValue = parseSearchRequest(searchRecordsRequest.getFieldValue().trim());
        String fieldName = searchRecordsRequest.getFieldName();
        if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            if(!(fieldName.equalsIgnoreCase(ScsbCommonConstants.BARCODE) || fieldName.equalsIgnoreCase(ScsbCommonConstants.CALL_NUMBER) || fieldName.equalsIgnoreCase(ScsbCommonConstants.CUSTOMER_CODE)
                    || fieldName.equalsIgnoreCase(ScsbCommonConstants.ISBN_CRITERIA) || fieldName.equalsIgnoreCase(ScsbCommonConstants.OCLC_NUMBER) || fieldName.equalsIgnoreCase(ScsbCommonConstants.ISSN_CRITERIA))) {
                String[] fieldValues = fieldValue.split("\\s+");
                if(fieldName.equalsIgnoreCase(ScsbCommonConstants.TITLE_STARTS_WITH)) {
                    stringBuilder.append(parentQuery).append(fieldName).append(":").append(fieldValues[0]);
                } else {
                    if(fieldValues.length > 1) {
                        List<String> fieldValuesList = Arrays.asList(fieldValues);
                        stringBuilder.append(parentQuery);
                        for (Iterator<String> iterator = fieldValuesList.iterator(); iterator.hasNext(); ) {
                            String value = iterator.next();
                            stringBuilder.append(fieldName).append(":").append(value);
                            if (iterator.hasNext()) {
                                stringBuilder.append(and);
                            }
                        }
                    } else {
                        stringBuilder.append(parentQuery).append(fieldName).append(":").append(fieldValue);
                    }
                }
            } else {
                if(fieldName.equalsIgnoreCase(ScsbCommonConstants.CALL_NUMBER)) {
                    fieldValue = fieldValue.replace(" ", "");
                }
                stringBuilder.append(parentQuery).append(fieldName).append(":").append(fieldValue);
            }
            return stringBuilder.toString();
        }
        return "";
    }

    /**
     * Gets solr query for bib item.
     *
     * @param parentQueryString the parent query string
     * @return the solr query for bib item
     */
    public SolrQuery getSolrQueryForBibItem(String parentQueryString) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(parentQueryString);
        return new SolrQuery(stringBuilder.toString());
    }

    /**
     * Gets query for parent and child criteria.
     *
     * @param searchRecordsRequest the search records request
     * @return the query for parent and child criteria
     */
    public SolrQuery getQueryForParentAndChildCriteria(SearchRecordsRequest searchRecordsRequest) {
        String queryForFieldCriteria = getQueryForFieldCriteria(searchRecordsRequest);
        String queryStringForBibCriteria = getQueryStringForMatchChildReturnParent(searchRecordsRequest);
        String queryStringForItemCriteriaForParent = getQueryStringForItemCriteriaForParent(searchRecordsRequest);
        SolrQuery solrQuery = getFullQueryForBibCriteria(searchRecordsRequest, queryForFieldCriteria, queryStringForBibCriteria, searchRecordsRequest.getCatalogingStatus());
        solrQuery.setFilterQueries(queryStringForItemCriteriaForParent);
        return solrQuery;
    }

    /**
     * Gets query for parent and child criteria for data dump.
     *
     * @param searchRecordsRequest the search records request
     * @return the query for parent and child criteria for data dump
     */
    public SolrQuery getQueryForParentAndChildCriteriaForDataDump(SearchRecordsRequest searchRecordsRequest) {
        String queryForFieldCriteria = getQueryForFieldCriteria(searchRecordsRequest);
        String queryStringForBibCriteria = getQueryStringForMatchChildReturnParent(searchRecordsRequest);
        String queryStringForItemCriteriaForParent = getQueryStringForItemCriteriaForParent(searchRecordsRequest);
        SolrQuery solrQuery = getFullQueryForBibCriteria(searchRecordsRequest, queryForFieldCriteria, queryStringForBibCriteria, ScsbCommonConstants.COMPLETE_STATUS);
        solrQuery.setFilterQueries(queryStringForItemCriteriaForParent);
        return solrQuery;
    }

    private SolrQuery getFullQueryForBibCriteria(SearchRecordsRequest searchRecordsRequest, String queryForFieldCriteria, String queryStringForBibCriteria, String completeStatus) {
        return new SolrQuery(queryStringForBibCriteria
                + and + ScsbCommonConstants.IS_DELETED_BIB + ":" + searchRecordsRequest.isDeleted()
                + and + ScsbConstants.BIB_CATALOGING_STATUS + ":" + completeStatus
                + (StringUtils.isNotBlank(queryForFieldCriteria) ? and + queryForFieldCriteria : ""));
    }

    public SolrQuery getQueryForParentAndChildCriteriaForDeletedDataDump(SearchRecordsRequest searchRecordsRequest) {
        String queryForFieldCriteria = getQueryForFieldCriteria(searchRecordsRequest);
        String queryStringForBibCriteria = getQueryStringForMatchChildReturnParent(searchRecordsRequest);
        return getFullQueryForBibCriteria(searchRecordsRequest, queryForFieldCriteria, queryStringForBibCriteria, ScsbCommonConstants.COMPLETE_STATUS);
    }

    /**
     * Gets query for child and parent criteria.
     *
     * @param searchRecordsRequest the search records request
     * @return the query for child and parent criteria
     */
    public SolrQuery getQueryForChildAndParentCriteria(SearchRecordsRequest searchRecordsRequest) {
        String queryForFieldCriteria = getQueryForFieldCriteria(searchRecordsRequest);
        String queryStringForItemCriteria = getQueryStringForMatchParentReturnChild(searchRecordsRequest);
        String queryStringForParentCriteriaForChild = getQueryStringForParentCriteriaForChild(searchRecordsRequest);
        return new SolrQuery(queryStringForItemCriteria
                + and + ScsbCommonConstants.IS_DELETED_ITEM + ":" + searchRecordsRequest.isDeleted()
                + and + ScsbConstants.ITEM_CATALOGING_STATUS + ":" + searchRecordsRequest.getCatalogingStatus()
                + and + queryForFieldCriteria + queryStringForParentCriteriaForChild);
    }

    /**
     * Gets deleted query for data dump.
     *
     * @param searchRecordsRequest  the search records request
     * @param isCGDChangedToPrivate the is cgd changed to private
     * @return the deleted query for data dump
     */
    public SolrQuery getDeletedQueryForDataDump(SearchRecordsRequest searchRecordsRequest,boolean isCGDChangedToPrivate) {
        String queryForFieldCriteria = getQueryForFieldCriteria(searchRecordsRequest);
        String queryForBibCriteria = buildQueryForBibFacetCriteria(searchRecordsRequest);
        String queryForImsLocation = buildQueryForFilterGivenChild(ScsbConstants.IMS_LOCATION_CODE,searchRecordsRequest.getImsDepositoryCodes());
        String queryStringForItemCriteria;
        SolrQuery solrQuery;
        if (isCGDChangedToPrivate) {
            queryStringForItemCriteria = getQueryStringForMatchParentReturnChildForDeletedDataDumpCGDToPrivate();
            solrQuery = new SolrQuery(queryStringForItemCriteria + and+queryForImsLocation+and+"("+ ("("+ScsbCommonConstants.IS_DELETED_ITEM + ":" + false + and + ScsbConstants.CGD_CHANGE_LOG + ":" + "\"" + ScsbConstants.CGD_CHANGE_LOG_SHARED_TO_PRIVATE + "\"" +")")
                    + or + ("("+ScsbCommonConstants.IS_DELETED_ITEM + ":" + false + and + ScsbConstants.CGD_CHANGE_LOG + ":" + "\"" + ScsbConstants.CGD_CHANGE_LOG_OPEN_TO_PRIVATE +"\"" +")") +")"
                    + (StringUtils.isNotBlank(queryForFieldCriteria) ? and + queryForFieldCriteria : "")
                    + (StringUtils.isNotBlank(queryForBibCriteria) ? and + queryForBibCriteria : ""));//to include items that got changed from shared to private, open to private for deleted export
        } else{
            queryStringForItemCriteria = getQueryStringForMatchParentReturnChild(searchRecordsRequest);
            solrQuery = new SolrQuery(queryStringForItemCriteria + and +queryForImsLocation+and+ ScsbCommonConstants.IS_DELETED_ITEM + ":" + searchRecordsRequest.isDeleted()
                    + (StringUtils.isNotBlank(queryForFieldCriteria) ? and + queryForFieldCriteria : "")
                    + (StringUtils.isNotBlank(queryForBibCriteria) ? and + queryForBibCriteria : ""));
        }
        return solrQuery;
    }

    /**
     * Gets count query for parent and child criteria.
     *
     * @param searchRecordsRequest the search records request
     * @return the count query for parent and child criteria
     */
    public SolrQuery getCountQueryForParentAndChildCriteria(SearchRecordsRequest searchRecordsRequest) {
        String countQueryForFieldCriteria = getCountQueryForFieldCriteria(searchRecordsRequest, coreParentFilterQuery);
        String queryStringForBibCriteria = getQueryStringForMatchChildReturnParent(searchRecordsRequest);
        String queryStringForItemCriteriaForParent = getQueryStringForItemCriteriaForParent(searchRecordsRequest);
        SolrQuery solrQuery = new SolrQuery(queryStringForBibCriteria
                + and + ScsbCommonConstants.IS_DELETED_BIB + ":" + searchRecordsRequest.isDeleted()
                + and + ScsbConstants.BIB_CATALOGING_STATUS + ":" + searchRecordsRequest.getCatalogingStatus());
        solrQuery.setFilterQueries(queryStringForItemCriteriaForParent, countQueryForFieldCriteria);
        return solrQuery;
    }

    /**
     * Gets count query for child and parent criteria.
     *
     * @param searchRecordsRequest the search records request
     * @return the count query for child and parent criteria
     */
    public SolrQuery getCountQueryForChildAndParentCriteria(SearchRecordsRequest searchRecordsRequest) {
        String countQueryForFieldCriteria = getCountQueryForFieldCriteria(searchRecordsRequest, coreChildFilterQuery);
        String queryStringForItemCriteria = getQueryStringForMatchParentReturnChild(searchRecordsRequest);
        String queryStringForParentCriteriaForChild = getQueryStringForParentCriteriaForChild(searchRecordsRequest);
        SolrQuery solrQuery = new SolrQuery(queryStringForItemCriteria
                + and + ScsbCommonConstants.IS_DELETED_ITEM + ":" + searchRecordsRequest.isDeleted()
                + and + ScsbConstants.ITEM_CATALOGING_STATUS + ":" + searchRecordsRequest.getCatalogingStatus()
                + and + queryStringForParentCriteriaForChild);
        solrQuery.setFilterQueries(countQueryForFieldCriteria);
        return solrQuery;
    }

    /**
     * This method escapes the special characters.
     *
     * @param searchText the search text
     * @return string
     */
    public String parseSearchRequest(String searchText) {
        StringBuilder modifiedText = new StringBuilder();
        StringCharacterIterator stringCharacterIterator = new StringCharacterIterator(searchText);
        char character = stringCharacterIterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '\\') {
                modifiedText.append("\\\\");
            } else if (character == '?') {
                modifiedText.append("\\?");
            } else if (character == '*') {
                modifiedText.append("\\*");
            } else if (character == '+') {
                modifiedText.append("\\+");
            } else if (character == ':') {
                modifiedText.append("\\:");
            } else if (character == '{') {
                modifiedText.append("\\{");
            } else if (character == '}') {
                modifiedText.append("\\}");
            } else if (character == '[') {
                modifiedText.append("\\[");
            } else if (character == ']') {
                modifiedText.append("\\]");
            } else if (character == '(') {
                modifiedText.append("\\(");
            } else if (character == ')') {
                modifiedText.append("\\)");
            } else if (character == '^') {
                modifiedText.append("\\^");
            } else if (character == '~') {
                modifiedText.append("\\~");
            } else if (character == '-') {
                modifiedText.append("\\-");
            } else if (character == '!') {
                modifiedText.append("\\!");
            } else if (character == '\'') {
                modifiedText.append("\\'");
            } else if (character == '@') {
                modifiedText.append("\\@");
            } else if (character == '#') {
                modifiedText.append("\\#");
            } else if (character == '$') {
                modifiedText.append("\\$");
            } else if (character == '%') {
                modifiedText.append("\\%");
            } else if (character == '/') {
                modifiedText.append("\\/");
            } else if (character == '"') {
                modifiedText.append("\\\"");
            } else if (character == '.') {
                modifiedText.append("\\.");
            }
            else {
                modifiedText.append(character);
            }
            character = stringCharacterIterator.next();
        }
        return modifiedText.toString();
    }

    /**
     * Solr query to fetch bib details.
     *
     * @param matchingMatchPointsEntities the matching match points entities
     * @param matchCriteriaValues         the match criteria values
     * @param matchingCriteria            the matching criteria
     * @return the solr query
     */
    public SolrQuery solrQueryToFetchBibDetails(List<MatchingMatchPointsEntity> matchingMatchPointsEntities, List<String> matchCriteriaValues, String matchingCriteria) {
        Integer rows = 0;
        for (MatchingMatchPointsEntity matchingMatchPointsEntity : matchingMatchPointsEntities) {
            String criteriaValue = matchingMatchPointsEntity.getCriteriaValue();
            if(criteriaValue.contains("\\")) {
                criteriaValue = criteriaValue.replaceAll("\\\\", "\\\\\\\\");
            }
            matchCriteriaValues.add(criteriaValue);
            rows = rows + matchingMatchPointsEntity.getCriteriaValueCount();
        }
        StringBuilder query = new StringBuilder();
        if (CollectionUtils.isNotEmpty(matchCriteriaValues)) {
            query.append(buildQueryForMatchChildReturnParent(matchingCriteria, matchCriteriaValues));
        }
        SolrQuery solrQuery = new SolrQuery(getAllCGDBibliographicFilterSolrQuery(query));
        solrQuery.setRows(rows);
        return solrQuery;
    }

    /**
     * Solr query for initial matching.
     *
     * @param fieldName           the field name
     * @param matchingPointValues the matching point values
     * @return the string
     */
    public String solrQueryForInitialMatching(String fieldName, List<String> matchingPointValues) {
        StringBuilder query = new StringBuilder();
        query.append(buildQueryForMatchChildReturnParent(fieldName, matchingPointValues));
        return getAllCGDBibliographicFilterSolrQuery(query);  // initial MA
    }

    /**
     * Solr query for ongoing matching.
     *
     * @param fieldName           the field name
     * @param matchingPointValues the matching point values
     * @return the string
     */
    public String solrQueryForOngoingMatching(String fieldName, List<String> matchingPointValues) {
        StringBuilder query = new StringBuilder();
        query.append(buildQueryForMatchChildReturnParent(fieldName, matchingPointValues));
        return getBibliographicFilterSolrQueryForOngoingMA(query);  // Ongoing MA
    }

    /**
     * Solr query for initial matching.
     *
     * @param fieldName          the field name
     * @param matchingPointValue the matching point value
     * @return the string
     */
    public String solrQueryForInitialMatching(String fieldName, String matchingPointValue) {
        StringBuilder query = new StringBuilder();
        if(matchingPointValue.contains("\\")) {
            matchingPointValue = matchingPointValue.replaceAll("\\\\", "\\\\\\\\");
        }
        query.append(fieldName).append(":").append("\"").append(matchingPointValue).append("\"");
        return getBibliographicFilterSolrQuery(query);
    }


    /**
     * Solr query for ongoing matching.
     *
     * @param fieldName          the field name
     * @param matchingPointValue the matching point value
     * @return the string
     */
    public String solrQueryForOngoingMatching(String fieldName, String matchingPointValue) {
        StringBuilder query = new StringBuilder();
        if(matchingPointValue.contains("\\")) {
            matchingPointValue = matchingPointValue.replaceAll("\\\\", "\\\\\\\\");
        }
        query.append(fieldName).append(":").append("\"").append(matchingPointValue).append("\"");
        return getBibliographicFilterSolrQueryForOngoingMA(query);
    }

    private String getBibliographicFilterSolrQueryForOngoingMA(StringBuilder query) {
        query.append(and).append(ScsbCommonConstants.IS_DELETED_BIB).append(":").append(ScsbConstants.FALSE)
                .append(and).append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS)
                .append(and)
                .append("(").append("(")
                .append(coreParentFilterQuery).append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(ScsbCommonConstants.SHARED_CGD)
                .append(")")
                .append(or)
                .append("(")
                .append(coreParentFilterQuery).append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(ScsbConstants.COMMITTED)
                .append(")")
                .append(")")
                .append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE)
                .append(and).append(coreParentFilterQuery).append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return query.toString();
    }

    private String getBibliographicFilterSolrQuery(StringBuilder query) {
        query.append(and).append(ScsbCommonConstants.IS_DELETED_BIB).append(":").append(ScsbConstants.FALSE)
                .append(and).append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS)
                .append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(ScsbCommonConstants.SHARED_CGD)
                .append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE)
                .append(and).append(coreParentFilterQuery).append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return query.toString();
    }

    private String getAllCGDBibliographicFilterSolrQuery(StringBuilder query) {
        query.append(and).append(ScsbCommonConstants.IS_DELETED_BIB).append(":").append(ScsbConstants.FALSE)
                .append(and).append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS)
                .append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE)
                .append(and).append(coreParentFilterQuery).append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return query.toString();
    }

    /**
     * This query is used to Fetch created or updated bibs based on the date.
     *
     * @param date the date
     * @return the string
     */
    public String fetchCreatedOrUpdatedBibs(String date) {
        StringBuilder query = new StringBuilder();
        query.append("(").append(ScsbConstants.BIB_CREATED_DATE).append(":").append("[").append(date).append("]")
                .append(or).append(ScsbConstants.BIB_LAST_UPDATED_DATE).append(":").append("[").append(date).append("]").append(")")
                .append(and).append(ScsbCommonConstants.IS_DELETED_BIB).append(":").append(ScsbConstants.FALSE)
                .append(and).append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        query.append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(ScsbCommonConstants.SHARED_CGD)
                .append(and).append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE)
                .append(and).append(coreParentFilterQuery).append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return query.toString();
    }

    /**
     * Build solr query for accession reports.
     *
     * @param date                       the date
     * @param owningInstitution          the owning institution
     * @param isDeleted                  the is deleted
     * @param collectionGroupDesignation the collection group designation
     * @return the solr query
     */
    public SolrQuery buildSolrQueryForAccessionReports(String date, String owningInstitution, boolean isDeleted, String collectionGroupDesignation) {
        return getSolrQueryOnDateType(date, owningInstitution, isDeleted, collectionGroupDesignation, ScsbConstants.ITEM_CREATED_DATE);
    }

    private SolrQuery getSolrQueryOnDateType(String date, String owningInstitution, boolean isDeleted, String collectionGroupDesignation, String itemDate) {
        StringBuilder query = new StringBuilder();
        query.append(ScsbCommonConstants.DOCTYPE).append(":").append(ScsbCommonConstants.ITEM).append(and);
        query.append(itemDate).append(":").append("[").append(date).append("]").append(and);
        query.append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(isDeleted).append(and);
        query.append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS).append(and);
        query.append(ScsbCommonConstants.ITEM_OWNING_INSTITUTION).append(":").append(owningInstitution).append(and);
        query.append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(collectionGroupDesignation);
        return new SolrQuery(query.toString());
    }

    /**
     * Build solr query for deaccession reports.
     *
     * @param date                       the date
     * @param owningInstitution          the owning institution
     * @param isDeleted                  the is deleted
     * @param collectionGroupDesignation the collection group designation
     * @return the solr query
     */
    public SolrQuery buildSolrQueryForDeaccessionReports(String date, String owningInstitution, boolean isDeleted, String collectionGroupDesignation) {
        return getSolrQueryOnDateType(date, owningInstitution, isDeleted, collectionGroupDesignation, ScsbConstants.ITEM_LASTUPDATED_DATE);
    }


    /**
     * Build solr query for cgd reports.
     *
     * @param owningInstitution          the owning institution
     * @param collectionGroupDesignation the collection group designation
     * @return the solr query
     */
    public SolrQuery buildSolrQueryForCGDReports(String owningInstitution , String collectionGroupDesignation){
        StringBuilder query = new StringBuilder();
        query.append(ScsbCommonConstants.DOCTYPE).append(":").append(ScsbCommonConstants.ITEM).append(and);
        query.append(ScsbCommonConstants.ITEM_OWNING_INSTITUTION).append(":").append(owningInstitution).append(and);
        query.append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(collectionGroupDesignation).append(and);
        query.append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE).append(and);
        query.append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return new SolrQuery(query.toString());
    }


    /**
     * Build solr query for deaccesion report information.
     *
     * @param date              the date
     * @param owningInstitution the owning institution
     * @param isDeleted         the is deleted
     * @return the solr query
     */
    public SolrQuery buildSolrQueryForDeaccesionReportInformation(String date, String owningInstitution, boolean isDeleted) {
        StringBuilder query = new StringBuilder();
        query.append(ScsbCommonConstants.DOCTYPE).append(":").append(ScsbCommonConstants.ITEM).append(and);
        query.append(ScsbConstants.ITEM_LASTUPDATED_DATE).append(":").append("[").append(date).append("]").append(and);
        query.append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(isDeleted).append(and);
        query.append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS).append(and);
        query.append(ScsbCommonConstants.ITEM_OWNING_INSTITUTION).append(":").append(owningInstitution);
        return new SolrQuery(query.toString());
    }


    public SolrQuery buildSolrQueryForIncompleteReports(String owningInstitution){
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(ScsbConstants.ITEM_STATUS_INCOMPLETE+or+"("+ ScsbConstants.ITEM_STATUS_COMPLETE+and+coreChildFilterQuery+ ScsbConstants.BIB_STATUS_INCOMPLETE+")");
        solrQuery.addFilterQuery(ScsbCommonConstants.ITEM_OWNING_INSTITUTION+":"+owningInstitution);
        solrQuery.addFilterQuery(ScsbConstants.IS_DELETED_ITEM_FALSE);
        solrQuery.setFields(ScsbCommonConstants.ITEM_ID,ScsbCommonConstants.BARCODE,ScsbCommonConstants.CUSTOMER_CODE, ScsbConstants.ITEM_CREATED_DATE, ScsbConstants.ITEM_CATALOGING_STATUS, ScsbConstants.ITEM_BIB_ID,ScsbCommonConstants.ITEM_OWNING_INSTITUTION);
        return solrQuery;
    }

    public SolrQuery buildSolrQueryToGetBibDetails(List<Integer> bibIdList,int rows){
        String bibIds = StringUtils.join(bibIdList, ",");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(ScsbConstants.BIB_DOC_TYPE);
        solrQuery.setRows(rows);
        solrQuery.addFilterQuery(ScsbConstants.SOLR_BIB_ID+StringEscapeUtils.escapeJava(bibIds).replace(",","\" \""));
        solrQuery.setFields(ScsbCommonConstants.BIB_ID, ScsbConstants.TITLE_DISPLAY,ScsbCommonConstants.AUTHOR_SEARCH, ScsbConstants.AUTHOR_DISPLAY);
        return solrQuery;
    }
    public SolrQuery buildQueryTitleMatchCount( String date, String owningInst, String cgd, String matchingIdentifier) {
        StringBuilder query = new StringBuilder();
        query.append(ScsbCommonConstants.DOCTYPE).append(":").append(ScsbCommonConstants.BIB).append(
                and).append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS).append(
                and).append(ScsbCommonConstants.BIB_OWNING_INSTITUTION).append(":").append(owningInst).append(
                and).append(ScsbConstants.BIB_CREATED_DATE).append(":[").append(date).append("]").append(
                and).append(matchingIdentifier).append(ScsbConstants.MATCHING_IDENTIFIER).append(":").append("*").append(
                and).append(coreParentFilterQuery).append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":").append(cgd).append(
                and).append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE);
        return new SolrQuery(query.toString());
    }
    public SolrQuery buildQueryTitleMatchedReport( String date, StringBuilder owningInst, StringBuilder cgd, String matchingIdentifier) {
        StringBuilder query = new StringBuilder();
        query.append(ScsbCommonConstants.DOCTYPE).append(":").append(ScsbCommonConstants.BIB).append(and)
                .append(ScsbConstants.BIB_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS).append(and)
                .append(ScsbCommonConstants.BIB_OWNING_INSTITUTION).append(":(").append(owningInst).append(")").append(and)
                .append(ScsbConstants.BIB_CREATED_DATE).append(":[").append(date).append("]").append(and).append(matchingIdentifier)
                .append(ScsbConstants.MATCHING_IDENTIFIER).append(":").append("*").append(coreParentFilterQuery).append(" ")
                .append(ScsbCommonConstants.COLLECTION_GROUP_DESIGNATION).append(":(").append(cgd).append(")").append(" ")
                .append(coreParentFilterQuery).append(ScsbCommonConstants.IS_DELETED_ITEM).append(":").append(ScsbConstants.FALSE).
                append(coreChildFilterQuery).append(ScsbConstants.ITEM_CATALOGING_STATUS).append(":").append(ScsbCommonConstants.COMPLETE_STATUS);
        return new SolrQuery(query.toString());
    }
}
