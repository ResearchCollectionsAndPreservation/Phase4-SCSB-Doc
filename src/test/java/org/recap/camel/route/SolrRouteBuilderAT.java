/*
package org.recap.camel.route;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.recap.BaseTestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.solr.core.query.SimpleQuery;


*/
/**
 * Created by rajeshbabuk on 29/9/16.
 *//*

@Ignore
public class SolrRouteBuilderAT extends BaseTestCase {

    @Autowired
    ProducerTemplate producerTemplate;

    @Value("${solr.parent.core}")
    String solrCore;

    @Value("${solr.url}") String solrUri;

    @Value("${solr.router.uri.type}") String solrRouterURI;

    @Test
    public void testSolrRouteBuilder() throws Exception {

        solrTemplate.delete(solrCore, new SimpleQuery("*:*"));
        solrTemplate.commit(solrCore);

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.setField("id", "123");
        solrInputDocument.setField("Title_search", "Title1");
        solrInputDocument.setField("Author_search", "Author1");

       // producerTemplate.sendBodyAndHeader(RecapCommonConstants.SOLR_QUEUE, solrInputDocument, RecapCommonConstants.SOLR_CORE, solrCore);
        Thread.sleep(1000);
        producerTemplate.asyncRequestBodyAndHeader(solrRouterURI + "://" + solrUri + "/" + solrCore, "", SolrConstants.OPERATION, SolrConstants.OPERATION_COMMIT);
        Thread.sleep(1000);
    }

}*/
