package org.recap.controller.swagger;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.recap.controller.BaseControllerUT;
import org.recap.controller.SharedCollectionRestController;
import org.recap.model.BibItemAvailabityStatusRequest;
import org.recap.model.ItemAvailabityStatusRequest;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.AccessionDetailsRepository;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.solr.main.BibSolrCrudRepository;
import org.recap.service.ItemAvailabilityService;
import org.recap.service.accession.SolrIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by chenchulakshmig on 14/10/16.
 */
public class SharedCollectionRestControllerUT extends BaseControllerUT {

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Mock
    private SolrIndexService solrIndexService;

    @Mock
    private BibSolrCrudRepository bibSolrCrudRepository;

    @Mock
    private SharedCollectionRestController mockedSharedCollectionRestController;

    @Autowired
    private SharedCollectionRestController sharedCollectionRestController;

    @Mock
    private ItemAvailabilityService itemAvailabilityService;

    @Mock
    private AccessionDetailsRepository accessionDetailsRepository;

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private Exchange exchange;

    @Before
    public void setup()throws Exception{
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void itemAvailabilityStatus() throws Exception {
        String itemBarcode = "32101056185125";
        BibliographicEntity bibliographicEntity = saveBibEntityWithHoldingsAndItem(itemBarcode);
        ItemAvailabityStatusRequest itemAvailabityStatusRequest = new ItemAvailabityStatusRequest();
        String barcode = null;
        List<String>  barcodeList = new ArrayList<>();
        barcodeList.add(barcode);
        itemAvailabityStatusRequest.setBarcodes(barcodeList);
        Mockito.when(mockedSharedCollectionRestController.getItemAvailabilityService()).thenReturn(itemAvailabilityService);
        Mockito.when(mockedSharedCollectionRestController.getItemAvailabilityService().getItemStatusByBarcodeAndIsDeletedFalse(itemBarcode)).thenCallRealMethod();
        Mockito.when(mockedSharedCollectionRestController.itemAvailabilityStatus(itemAvailabityStatusRequest)).thenCallRealMethod();
        ResponseEntity responseEntity = mockedSharedCollectionRestController.itemAvailabilityStatus(itemAvailabityStatusRequest);
        assertNotNull(responseEntity);
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    }



    private BibliographicEntity saveBibEntityWithHoldingsAndItem(String itemBarcode) throws Exception {
        Random random = new Random();
        File bibContentFile = getBibContentFile();
        File holdingsContentFile = getHoldingsContentFile();
        String sourceBibContent = FileUtils.readFileToString(bibContentFile, "UTF-8");
        String sourceHoldingsContent = FileUtils.readFileToString(holdingsContentFile, "UTF-8");

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(sourceBibContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity.setDeleted(false);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(sourceHoldingsContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));
        holdingsEntity.setDeleted(false);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode(itemBarcode);
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setDeleted(false);

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));
        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        return savedBibliographicEntity;
    }

    private File getBibContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("BibContent.xml");
        return new File(resource.toURI());
    }

    private File getHoldingsContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("HoldingsContent.xml");
        return new File(resource.toURI());
    }

    @Test
    public void testBibAvailabilityStatus() throws Exception {
        BibItemAvailabityStatusRequest bibItemAvailabityStatusRequest = new BibItemAvailabityStatusRequest();
        bibItemAvailabityStatusRequest.setBibliographicId("93540");
        bibItemAvailabityStatusRequest.setInstitutionId("PUL");
        ResponseEntity responseEntity = sharedCollectionRestController.bibAvailabilityStatus(bibItemAvailabityStatusRequest);
        assertNotNull(responseEntity);

        bibItemAvailabityStatusRequest.setBibliographicId("66056");
        bibItemAvailabityStatusRequest.setInstitutionId("CUL");
        responseEntity = sharedCollectionRestController.bibAvailabilityStatus(bibItemAvailabityStatusRequest);
        assertNotNull(responseEntity);

        bibItemAvailabityStatusRequest.setBibliographicId("59321");
        bibItemAvailabityStatusRequest.setInstitutionId("SCSB");
        responseEntity = sharedCollectionRestController.bibAvailabilityStatus(bibItemAvailabityStatusRequest);
        assertNotNull(responseEntity);

        bibItemAvailabityStatusRequest.setBibliographicId("0000");
        bibItemAvailabityStatusRequest.setInstitutionId("PUL");
        responseEntity = sharedCollectionRestController.bibAvailabilityStatus(bibItemAvailabityStatusRequest);
        assertNotNull(responseEntity);

        bibItemAvailabityStatusRequest.setBibliographicId("0000");
        bibItemAvailabityStatusRequest.setInstitutionId("PU");
        responseEntity = sharedCollectionRestController.bibAvailabilityStatus(bibItemAvailabityStatusRequest);
        assertNotNull(responseEntity);
    }
}