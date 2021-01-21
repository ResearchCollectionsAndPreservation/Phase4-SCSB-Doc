package org.recap.executors;

import org.apache.camel.ProducerTemplate;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.recap.util.BibJSONUtil;
import org.springframework.data.solr.core.SolrTemplate;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by chenchulakshmig on 21/6/16.
 */
public class BibItemRecordSetupCallable implements Callable {

    BibliographicEntity bibliographicEntity;
    private final SolrTemplate solrTemplate;
    BibliographicDetailsRepository bibliographicDetailsRepository;
    HoldingsDetailsRepository holdingsDetailsRepository;
    ProducerTemplate producerTemplate;
    private List<String> nonHoldingInstitutionList;


    public BibItemRecordSetupCallable(BibliographicEntity bibliographicEntity, SolrTemplate solrTemplate, BibliographicDetailsRepository bibliographicDetailsRepository,
                                      HoldingsDetailsRepository holdingsDetailsRepository, ProducerTemplate producerTemplate,List<String> nonHoldingInstitutionList) {
        this.bibliographicEntity = bibliographicEntity;
        this.solrTemplate = solrTemplate;
        this.bibliographicDetailsRepository = bibliographicDetailsRepository;
        this.holdingsDetailsRepository = holdingsDetailsRepository;
        this.producerTemplate = producerTemplate;
        this.nonHoldingInstitutionList = nonHoldingInstitutionList;
    }

    @Override
    public Object call() throws Exception {
        BibJSONUtil bibJSONUtil = new BibJSONUtil();
        bibJSONUtil.setProducerTemplate(producerTemplate);
        bibJSONUtil.setNonHoldingInstitutions(nonHoldingInstitutionList);
        return bibJSONUtil.generateBibAndItemsForIndex(bibliographicEntity, solrTemplate, bibliographicDetailsRepository, holdingsDetailsRepository);
    }
}
