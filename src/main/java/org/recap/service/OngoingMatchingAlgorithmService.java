package org.recap.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.recap.RecapConstants;
import org.recap.exception.CGDRoundTripReportException;
import org.recap.model.jpa.ItemChangeLogEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.matchingreports.OngoingMatchingCGDReport;
import org.recap.repository.jpa.ItemChangeLogDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OngoingMatchingAlgorithmService {

    @Autowired
    ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    ProducerTemplate producerTemplate;

    @Autowired
    CamelContext camelContext;

    @Autowired
    ItemDetailsRepository itemDetailsRepository;

    public String generateCGDRoundTripReport() throws Exception {
        List<ItemChangeLogEntity> itemChangeLogEntityList = itemChangeLogDetailsRepository.findByUpdatedDateAndOperationType(LocalDate.now().toString(), RecapConstants.ONGOING_MATCHING_OPERATION_TYPE);
        if (!itemChangeLogEntityList.isEmpty()) {
            try {
                Map<String, List<OngoingMatchingCGDReport>> institutionWiseCgdRoundTripReports = itemChangeLogEntityList.stream()
                        .map(this::getOngoingMatchingCGDReport)
                        .collect(Collectors.groupingBy(OngoingMatchingCGDReport::getInstitution));
                camelContext.getRouteController().startRoute(RecapConstants.S3_ONGOING_MATCHING_CGD_REPORT_ROUTE_ID);
                institutionWiseCgdRoundTripReports.forEach(this::sendCgdReportsToQueue);
            }
            catch (CGDRoundTripReportException cgdRoundTripReportException){
                throw new CGDRoundTripReportException(RecapConstants.CGD_ROUND_TRIP_EXCEPTION_MESSAGE);
            }
            catch (CamelExecutionException camelExecutionException){
                camelExecutionException.printStackTrace();
                throw new CGDRoundTripReportException("Exception occured in camel executing while trying to generate CGD Round Trip report",camelExecutionException.getCause());
            }
            catch (NoSuchElementException elementException){
                throw new CGDRoundTripReportException(RecapConstants.CGD_ROUND_TRIP_EXCEPTION_MESSAGE,elementException.getCause());
            }
            catch (Exception exception) {
                throw new CGDRoundTripReportException("Exception occured while genenarting CGD Round-Trip report",exception.getCause());
            }
        } else {
            return "No records found to generate CGD Round-Trip report";
        }
        return "CGD Round-Trip report generated successfully";
    }


    @SneakyThrows
    private OngoingMatchingCGDReport getOngoingMatchingCGDReport(ItemChangeLogEntity itemChangeLogEntity) {
        Optional<ItemEntity> itemEntity = itemDetailsRepository.findById(itemChangeLogEntity.getRecordId());
        OngoingMatchingCGDReport ongoingMatchingCGDReport = new OngoingMatchingCGDReport();
        if(itemEntity.isPresent()) {
            ongoingMatchingCGDReport.setItemBarcode(itemEntity.get().getBarcode());
            ongoingMatchingCGDReport.setOldCgd(RecapConstants.SHARED);
            ongoingMatchingCGDReport.setNewCgd(RecapConstants.OPEN);
            ongoingMatchingCGDReport.setDate(String.valueOf(itemChangeLogEntity.getUpdatedDate()));
            ongoingMatchingCGDReport.setInstitution(itemEntity.get().getInstitutionEntity().getInstitutionCode());
        }
        else {
            throw new CGDRoundTripReportException(RecapConstants.CGD_ROUND_TRIP_EXCEPTION_MESSAGE);
        }
        return ongoingMatchingCGDReport;
    }

    private void sendCgdReportsToQueue(String key, List<OngoingMatchingCGDReport> value) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(RecapConstants.INSTITUTION, key);
        headers.put(RecapConstants.FILE_NAME, RecapConstants.CGD_ROUND_TRIP_REPORT);
        producerTemplate.sendBodyAndHeaders(RecapConstants.S3_ONGOING_MATCHING_CGD_REPORT_Q, value, headers);
    }
}
