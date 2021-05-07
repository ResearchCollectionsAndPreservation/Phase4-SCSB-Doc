package org.recap.util;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.matchingreports.TitleExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by angelind on 16/6/17.
 */
public class OngoingMatchingAlgorithmReportGenerator {

    private static final Logger logger= LoggerFactory.getLogger(OngoingMatchingAlgorithmReportGenerator.class);

    /**
     * This method prepares TitleExceptionReport which is used to generate title exception report based on the given report entity
     *
     * @param reportDataEntities                      the list of report data entity
     * @return the title exception report
     */
    public TitleExceptionReport prepareTitleExceptionReportRecord(List<ReportDataEntity> reportDataEntities) {

        TitleExceptionReport titleExceptionReport = new TitleExceptionReport();
        String headerName = null;
        Predicate<String> checkForMatchPoints = p -> ScsbCommonConstants.MATCH_POINT_FIELD_OCLC.contains(p) || p.equals(ScsbCommonConstants.MATCH_POINT_FIELD_ISBN) ||
                p.equals(ScsbCommonConstants.MATCH_POINT_FIELD_ISSN) || p.equals(ScsbCommonConstants.MATCH_POINT_FIELD_LCCN);

        for (Iterator<ReportDataEntity> iterator = reportDataEntities.iterator(); iterator.hasNext(); ) {
            ReportDataEntity report =  iterator.next();
            if(checkForMatchPoints.test(report.getHeaderName())){
                headerName = report.getHeaderName().toLowerCase();
            }else{
                headerName = report.getHeaderName();
            }
            String headerValue = report.getHeaderValue();
            Method setterMethod = getSetterMethod(headerName);
            if(null != setterMethod){
                try {
                    setterMethod.invoke(titleExceptionReport, headerValue);
                } catch (Exception e) {
                    logger.error(ScsbCommonConstants.LOG_ERROR,e.getMessage());
                }
            }
        }
        return titleExceptionReport;
    }

    /**
     * This method is used to get the setter method for the given one of the instance variable name in TitleExceptionReport class.
     *
     * @param propertyName the property name
     * @return the setter method
     */
    public Method getSetterMethod(String propertyName) {
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
        try {
            return propertyUtilsBean.getWriteMethod(new PropertyDescriptor(propertyName, TitleExceptionReport.class));
        } catch (IntrospectionException e) {
            logger.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return null;
    }
}
