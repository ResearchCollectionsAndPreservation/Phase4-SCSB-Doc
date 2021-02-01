package org.recap.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.matchingalgorithm.service.MatchingBibInfoDetailService;
import org.recap.model.solr.SolrIndexRequest;
import org.recap.util.DateUtil;
import org.recap.util.OngoingMatchingAlgorithmUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by rajeshbabuk on 20/4/17.
 */
public class OngoingMatchingAlgorithmJobRestControllerUT extends BaseTestCaseUT {

    @InjectMocks
    OngoingMatchingAlgorithmJobRestController ongoingMatchingAlgorithmJobRestController;

    @Mock
    OngoingMatchingAlgorithmUtil ongoingMatchingAlgorithmUtil;

    @Mock
    MatchingBibInfoDetailService matchingBibInfoDetailService;

    @Mock
    DateUtil dateUtil;

    @Value("${matching.algorithm.bibinfo.batchsize}")
    private String batchSize;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(ongoingMatchingAlgorithmJobRestController,"batchSize",batchSize);
        Mockito.when(dateUtil.getFromDate(Mockito.any())).thenCallRealMethod();
        Mockito.when(dateUtil.getToDate(Mockito.any())).thenCallRealMethod();
    }

    @Test
    public void testStartMatchingAlgorithmJob() throws Exception {
        Mockito.when(ongoingMatchingAlgorithmUtil.fetchUpdatedRecordsAndStartProcess(Mockito.any(),Mockito.anyInt())).thenReturn(RecapCommonConstants.SUCCESS);
        Mockito.when(matchingBibInfoDetailService.populateMatchingBibInfo(Mockito.any(),Mockito.any())).thenReturn(RecapCommonConstants.SUCCESS);
        String status=ongoingMatchingAlgorithmJobRestController.startMatchingAlgorithmJob(getSolrIndexRequest());
        assertEquals(RecapCommonConstants.SUCCESS,status);
    }

    @Test
    public void testStartMatchingAlgorithmJobException() throws Exception {
        Mockito.when(ongoingMatchingAlgorithmUtil.fetchUpdatedRecordsAndStartProcess(Mockito.any(),Mockito.anyInt())).thenThrow(NullPointerException.class);
        String status=ongoingMatchingAlgorithmJobRestController.startMatchingAlgorithmJob(getSolrIndexRequest());
        assertNotNull(status);
    }

    private SolrIndexRequest getSolrIndexRequest() {
        SolrIndexRequest solrIndexRequest = new SolrIndexRequest();
        solrIndexRequest.setProcessType(RecapCommonConstants.ONGOING_MATCHING_ALGORITHM_JOB);
        solrIndexRequest.setCreatedDate(new Date());
        return solrIndexRequest;
    }
}
