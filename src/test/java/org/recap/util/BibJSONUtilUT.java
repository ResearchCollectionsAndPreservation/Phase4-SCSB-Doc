package org.recap.util;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.marc4j.marc.Record;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.recap.BaseTestCaseUT;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.solr.Bib;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.springframework.data.solr.core.SolrTemplate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by premkb on 1/8/16.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SolrTemplate.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class BibJSONUtilUT extends BaseTestCaseUT {

    @InjectMocks
    BibJSONUtil bibJSONUtil;

    @Mock
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Mock
    HoldingsDetailsRepository holdingsDetailsRepository;

    @Mock
    private ProducerTemplate producerTemplate;

    private String holdingContent = "<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "            <record>\n" +
            "              <datafield tag=\"852\" ind1=\"0\" ind2=\"1\">\n" +
            "                <subfield code=\"b\">off,che</subfield>\n" +
            "                <subfield code=\"h\">TA434 .S15</subfield>\n" +
            "              </datafield>\n" +
            "              <datafield tag=\"866\" ind1=\"0\" ind2=\"0\">\n" +
            "                <subfield code=\"a\">v.1-16         </subfield>\n" +
            "              </datafield>\n" +
            "            </record>\n" +
            "          </collection>";

    private String bibContent = "<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "          <record>\n" +
            "            <controlfield tag=\"001\">NYPG001000011-B</controlfield>\n" +
            "            <controlfield tag=\"005\">20001116192418.8</controlfield>\n" +
            "            <controlfield tag=\"008\">841106s1976    le       b    000 0 arax </controlfield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "              <subfield code=\"a\">79971032</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "              <subfield code=\"a\">NNSZ00100011</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "              <subfield code=\"a\">(OCoLC)ocm004417290 </subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "              <subfield code=\"c\">NN</subfield>\n" +
            "              <subfield code=\"d\">NN</subfield>\n" +
            "              <subfield code=\"d\">WaOLN</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "              <subfield code=\"a\">a-ba---</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "              <subfield code=\"a\">DS247.B28</subfield>\n" +
            "              <subfield code=\"b\">R85</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "              <subfield code=\"a\">Rumayḥī, Muḥammad Ghānim.</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\" \" tag=\"110\">\n" +
            "              <subfield code=\"a\">Rumayḥī, Muḥammad</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\" \" tag=\"111\">\n" +
            "              <subfield code=\"a\">Rumayḥī</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\" \" tag=\"700\">\n" +
            "              <subfield code=\"a\">Yūsuf, Ṣābir.</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\" \" tag=\"710\">\n" +
            "              <subfield code=\"a\">Yūsuf,</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\" \" tag=\"711\">\n" +
            "              <subfield code=\"a\">YūsufṢābir.</subfield>\n" +
            "            </datafield>\t\n" +
            "            <datafield ind1=\"1\" ind2=\"3\" tag=\"245\">\n" +
            "              <subfield code=\"a\">al-Baḥrayn :</subfield>\n" +
            "              <subfield code=\"b\">mushkilāt al-taghyīr al-siyāsī wa-al-ijtimāʻī /</subfield>\n" +
            "              <subfield code=\"c\">Muḥammad al-Rumayḥī.</subfield>\n" +
            "            </datafield>\n" +
            "    <datafield ind1=\"1\" ind2=\"3\" tag=\"130\">\n" +
            "              <subfield code=\"1\">al-Baḥrayn :</subfield>\n" +
            "            </datafield>\n" +
            "   <datafield ind1=\"1\" ind2=\"3\" tag=\"730\">\n" +
            "              <subfield code=\"a\">Muḥammad al-Rumayḥī.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"250\">\n" +
            "              <subfield code=\"a\">al-Ṭabʻah 1.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "              <subfield code=\"a\">[Bayrūt] :</subfield>\n" +
            "              <subfield code=\"b\">Dār Ibn Khaldūn,</subfield>\n" +
            "              <subfield code=\"c\">1976.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "              <subfield code=\"a\">264 p. ;</subfield>\n" +
            "              <subfield code=\"c\">24 cm.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "              <subfield code=\"a\">Includes bibliographies.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"546\">\n" +
            "              <subfield code=\"a\">In Arabic.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">History</subfield>\n" +
            "              <subfield code=\"y\">20th century.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">Economic conditions.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">Social conditions.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"907\">\n" +
            "              <subfield code=\"a\">.b100000241</subfield>\n" +
            "              <subfield code=\"c\">m</subfield>\n" +
            "              <subfield code=\"d\">a</subfield>\n" +
            "              <subfield code=\"e\">-</subfield>\n" +
            "              <subfield code=\"f\">ara</subfield>\n" +
            "              <subfield code=\"g\">le </subfield>\n" +
            "              <subfield code=\"h\">3</subfield>\n" +
            "              <subfield code=\"i\">1</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"952\">\n" +
            "              <subfield code=\"h\">*OFK 84-1944</subfield>\n" +
            "            </datafield>\n" +
            "          </record>\n" +
            "        </collection>";
    @Test
    public void generateBibAndItemsForIndex()throws Exception {
        BibliographicEntity bibliographicEntity = getBibliographicEntity();
        SolrTemplate mocksolrTemplate1 = PowerMockito.mock(SolrTemplate.class);
        SolrInputDocument solrInputDocument=new SolrInputDocument();
        Mockito.when(mocksolrTemplate1.convertBeanToSolrInputDocument(Mockito.any())).thenReturn(solrInputDocument);
        SolrInputDocument solrInputDocument1 = bibJSONUtil.generateBibAndItemsForIndex(bibliographicEntity, mocksolrTemplate1, bibliographicDetailsRepository, holdingsDetailsRepository);
        assertNotNull(solrInputDocument1);

    }

    @Test
    public void generateBibForIndex()throws Exception {
        BibliographicEntity bibliographicEntity = getBibliographicEntity();
        Bib generateBibForIndex = bibJSONUtil.generateBibForIndex(bibliographicEntity, bibliographicDetailsRepository, holdingsDetailsRepository);
        assertNotNull(generateBibForIndex);
        BibliographicEntity bibliographicEntity1= new BibliographicEntity();
        bibliographicEntity1.setId(1);
        bibliographicEntity1.setOwningInstitutionBibId("1");
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(3);
        institutionEntity.setInstitutionName("NYPL");
        institutionEntity.setInstitutionCode("NYPL");
        bibliographicEntity1.setInstitutionEntity(institutionEntity);
        Bib generateBibForIndex1 = bibJSONUtil.generateBibForIndex(bibliographicEntity1, bibliographicDetailsRepository, holdingsDetailsRepository);
        assertNull(generateBibForIndex1);

    }

    private BibliographicEntity getBibliographicEntity() {
        List<BibliographicEntity> bibliographicEntities = new ArrayList<>();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(bibContent.getBytes());
        bibliographicEntity.setOwningInstitutionId(1);
        Random random = new Random();
        String owningInstitutionBibId = String.valueOf(random.nextInt());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setLastUpdatedBy("tst");
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(3);
        institutionEntity.setInstitutionName("NYPL");
        institutionEntity.setInstitutionCode("NYPL");
        bibliographicEntity.setInstitutionEntity(institutionEntity);
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(holdingContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setCreatedBy("etl");
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setLastUpdatedBy("etl");
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("etl");
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setLastUpdatedBy("etl");
        String barcode = "1234";
        itemEntity.setBarcode(barcode);
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("1");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntities.add(bibliographicEntity);
        itemEntity.setBibliographicEntities(bibliographicEntities);
        return bibliographicEntity;
    }


    @Test
    public void testLccnTrimValue() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        String lccnValue = bibJSONUtil.getLCCNValue(marcRecord);
        assertEquals("79971032",lccnValue );
    }

    @Test
    public void testTitleDisplayValue() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        String titleDisplay = bibJSONUtil.getTitleDisplay(marcRecord);
        assertEquals( "al-Baḥrayn : mushkilāt al-taghyīr al-siyāsī wa-al-ijtimāʻī / Muḥammad al-Rumayḥī.",titleDisplay);
    }

    @Test
    public void testAuthorDisplayValue() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        String authorDisplayValue = bibJSONUtil.getAuthorDisplayValue(marcRecord);
        assertEquals( "Rumayḥī, Muḥammad Ghānim. Rumayḥī, Muḥammad Rumayḥī ",authorDisplayValue);
    }

    @Test
    public void testAuthorSearchValue() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        List<String> authorSearchValue = bibJSONUtil.getAuthorSearchValue(marcRecord);
        assertNotNull(authorSearchValue);
        assertEquals(6,authorSearchValue.size());
        assertTrue(authorSearchValue.contains("Rumayḥī, Muḥammad Ghānim."));
        assertTrue(authorSearchValue.contains("Rumayḥī, Muḥammad"));
        assertTrue(authorSearchValue.contains("Rumayḥī"));
        assertTrue(authorSearchValue.contains("Yūsuf, Ṣābir."));
        assertTrue(authorSearchValue.contains("Yūsuf,"));
        assertTrue(authorSearchValue.contains("YūsufṢābir."));
    }

    @Test
    public void testTitles() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        String titles = bibJSONUtil.getTitle(marcRecord);
        assertNotNull(titles);
        assertEquals("al-Baḥrayn : mushkilāt al-taghyīr al-siyāsī wa-al-ijtimāʻī /   Muḥammad al-Rumayḥī.   ",titles);
    }

    @Test
    public void testTitleSort() throws Exception {
        List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
        Record marcRecord = records.get(0);
        String titleSort = bibJSONUtil.getTitleSort(marcRecord, bibJSONUtil.getTitleDisplay(marcRecord));
        assertNotNull(titleSort);
        assertEquals("Baḥrayn : mushkilāt al-taghyīr al-siyāsī wa-al-ijtimāʻī / Muḥammad al-Rumayḥī.",titleSort);
    }

    @Test
    public void testStripStart() throws Exception {
        String number = "0023450";
        number = StringUtils.stripStart(number, "0");
        assertEquals("23450", number);
    }

}
