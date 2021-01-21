package org.recap.executors;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.recap.RecapConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by chenchulakshmig on 21/6/16.
 */
public class BibItemIndexCallable extends CommonCallable implements Callable {

    private static final Logger logger = LoggerFactory.getLogger(BibItemIndexCallable.class);

    private final int pageNum;
    private final int docsPerPage;
    private String coreName;
    private String solrURL;
    private Integer owningInstitutionId;
    private Date fromDate;
    private BibliographicDetailsRepository bibliographicDetailsRepository;
    private HoldingsDetailsRepository holdingsDetailsRepository;
    private ProducerTemplate producerTemplate;
    private SolrTemplate solrTemplate;
    private String partialIndexType;
    private Map<String, Object> partialIndexMap;
    private List<String> nonHoldingInstitutionList;

    /**
     * This method instantiates a new bib item index callable.
     *
     * @param solrURL                        the solr url
     * @param coreName                       the core name
     * @param pageNum                        the page num
     * @param docsPerPage                    the docs per page
     * @param bibliographicDetailsRepository the bibliographic details repository
     * @param holdingsDetailsRepository      the holdings details repository
     * @param owningInstitutionId            the owning institution id
     * @param fromDate                       the from date
     * @param producerTemplate               the producer template
     * @param solrTemplate                   the solr template
     * @param partialIndexType               the partial index type
     * @param partialIndexMap                the partial index map
     */
    public BibItemIndexCallable(String solrURL, String coreName, int pageNum, int docsPerPage, BibliographicDetailsRepository bibliographicDetailsRepository, HoldingsDetailsRepository holdingsDetailsRepository, Integer owningInstitutionId,
                                Date fromDate, ProducerTemplate producerTemplate, SolrTemplate solrTemplate, String partialIndexType, Map<String, Object> partialIndexMap,List<String> nonHoldingInstitutionList) {
        this.coreName = coreName;
        this.solrURL = solrURL;
        this.pageNum = pageNum;
        this.docsPerPage = docsPerPage;
        this.bibliographicDetailsRepository = bibliographicDetailsRepository;
        this.holdingsDetailsRepository = holdingsDetailsRepository;
        this.owningInstitutionId = owningInstitutionId;
        this.fromDate = fromDate;
        this.producerTemplate = producerTemplate;
        this.solrTemplate = solrTemplate;
        this.partialIndexType = partialIndexType;
        this.partialIndexMap = partialIndexMap;
        this.nonHoldingInstitutionList = nonHoldingInstitutionList;
    }

    /**
     * This method is processed by thread to generate solr input documents and index to solr.
     * @return
     * @throws Exception
     */
    @Override
    public Object call() throws Exception {

        Page<BibliographicEntity> bibliographicEntities = null;
        if(StringUtils.isNotBlank(partialIndexType) && partialIndexMap != null) {
            if(partialIndexType.equalsIgnoreCase(RecapConstants.BIB_ID_LIST)) {
                List<Integer> bibIdList = (List<Integer>) partialIndexMap.get(RecapConstants.BIB_ID_LIST);
                bibliographicEntities = bibliographicDetailsRepository.getBibsBasedOnBibIds(PageRequest.of(pageNum, docsPerPage), bibIdList);
            } else if(partialIndexType.equalsIgnoreCase(RecapConstants.BIB_ID_RANGE)) {
                String bibIdFrom = (String) partialIndexMap.get(RecapConstants.BIB_ID_RANGE_FROM);
                String bibIdTo = (String) partialIndexMap.get(RecapConstants.BIB_ID_RANGE_TO);
                bibliographicEntities = bibliographicDetailsRepository.getBibsBasedOnBibIdRange(PageRequest.of(pageNum, docsPerPage), Integer.valueOf(bibIdFrom), Integer.valueOf(bibIdTo));
            } else if(partialIndexType.equalsIgnoreCase(RecapConstants.DATE_RANGE)) {
                Date dateFrom = (Date) partialIndexMap.get(RecapConstants.DATE_RANGE_FROM);
                Date dateTo = (Date) partialIndexMap.get(RecapConstants.DATE_RANGE_TO);
                bibliographicEntities = bibliographicDetailsRepository.getBibsBasedOnDateRange(PageRequest.of(pageNum, docsPerPage), dateFrom, dateTo);
            }
         } else {
            if (null == owningInstitutionId && null == fromDate) {
                bibliographicEntities = bibliographicDetailsRepository.findAll(PageRequest.of(pageNum, docsPerPage));
            } else if (null != owningInstitutionId && null == fromDate) {
                bibliographicEntities = bibliographicDetailsRepository.findByOwningInstitutionId(PageRequest.of(pageNum, docsPerPage), owningInstitutionId);
            } else if (null == owningInstitutionId && null != fromDate) {
                bibliographicEntities = bibliographicDetailsRepository.findByLastUpdatedDateAfter(PageRequest.of(pageNum, docsPerPage), fromDate);
            } else if (null != owningInstitutionId && null != fromDate) {
                bibliographicEntities = bibliographicDetailsRepository.findByOwningInstitutionIdAndLastUpdatedDateAfter(PageRequest.of(pageNum, docsPerPage), owningInstitutionId, fromDate);
            }
        }

        List<SolrInputDocument> solrInputDocumentsToIndex = setSolrInputDocuments(bibliographicEntities, solrTemplate, bibliographicDetailsRepository, holdingsDetailsRepository, producerTemplate, logger,nonHoldingInstitutionList);
        if (!CollectionUtils.isEmpty(solrInputDocumentsToIndex)) {
            SolrTemplate templateForSolr = new SolrTemplate(new HttpSolrClient.Builder(solrURL + File.separator).build());
            //templateForSolr.setSolrCore(coreName);
            templateForSolr.saveDocuments(coreName, solrInputDocumentsToIndex);
            templateForSolr.commit(coreName);
        }
        return solrInputDocumentsToIndex.size();
    }
}
