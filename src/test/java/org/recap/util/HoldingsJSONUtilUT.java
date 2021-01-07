package org.recap.util;

import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.recap.BaseTestCaseUT;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.solr.Holdings;

import java.util.Date;

import static org.junit.Assert.assertNull;

public class HoldingsJSONUtilUT extends BaseTestCaseUT {

    @InjectMocks
    HoldingsJSONUtil holdingsJSONUtil;

    @Mock
    private ProducerTemplate producerTemplate;

    @Test
    public void generateHoldingsForIndex(){
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setHoldingsId(1);
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(1567));
        holdingsJSONUtil.getProducerTemplate();
        Holdings holdings=holdingsJSONUtil.generateHoldingsForIndex(holdingsEntity);
        assertNull(holdings);
    }

}
