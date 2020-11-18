package org.recap.controller;

import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.model.camel.EmailPayLoad;

import static org.junit.Assert.assertEquals;

/**
 * Created by rajeshbabuk on 20/4/17.
 */
public class BatchJobEmailControllerUT extends BaseTestCaseUT {

    @InjectMocks
    BatchJobEmailController batchJobEmailController;

    @Mock
    EmailPayLoad emailPayLoad;

    @Mock
    ProducerTemplate producerTemplate;

    @Test
    public void testBatchJobSendEmail() throws Exception {
        String response=batchJobEmailController.batchJobSendEmail(emailPayLoad);
        assertEquals(RecapCommonConstants.SUCCESS,response);
    }
}
