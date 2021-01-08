package org.recap;

/**
 * Created by SheikS on 6/20/2016.
 */
public final class RecapConstants {
    public static final String MONOGRAPHIC_SET = "MonographicSet";
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String CGD_CHANGE_LOG = "CGDChangeLog";
    public static final String CGD_CHANGE_LOG_SHARED_TO_PRIVATE = "SharedToPrivate";
    public static final String CGD_CHANGE_LOG_OPEN_TO_PRIVATE = "OpenToPrivate";
    public static final String TITLE_SUBFIELD_A = "Title_subfield_a";
    public static final String USE_RESTRICTION_DISPLAY = "UseRestriction_display";

    //Camel Queues Constants
    public static final String FS_SUBMIT_COLLECTION_EXCEPTION_REPORT_Q = "scsbactivemq:queue:fsSubmitCollectionExceptionReportQ";
    public static final String FTP_SUBMIT_COLLECTION_EXCEPTION_REPORT_Q = "scsbactivemq:queue:ftpSubmitCollectionExceptionReportQ";
    public static final String FTP_SUBMIT_COLLECTION_SUMMARY_REPORT_Q = "scsbactivemq:queue:ftpSubmitCollectionSummaryReportQ";
    public static final String FS_SUBMIT_COLLECTION_SUMMARY_REPORT_Q = "scsbactivemq:queue:fsSubmitCollectionSummaryReportQ";
    public static final String FTP_SUBMIT_COLLECTION_SUCCESS_REPORT_Q = "scsbactivemq:queue:ftpSubmitCollectionSuccessReportQ";
    public static final String FS_SUBMIT_COLLECTION_SUCCESS_REPORT_Q = "scsbactivemq:queue:fsSubmitCollectionSuccessReportQ";
    public static final String FTP_SUBMIT_COLLECTION_FAILURE_REPORT_Q = "scsbactivemq:queue:ftpSubmitCollectionFailureReportQ";
    public static final String FS_SUBMIT_COLLECTION_FAILURE_REPORT_Q = "scsbactivemq:queue:fsSubmitCollectionFailureReportQ";
    public static final String FTP_ONGOING_ACCESSON_REPORT_Q = "scsbactivemq:queue:ftpOngoingAccessionReportQ";
    public static final String FS_ONGOING_ACCESSION_REPORT_Q = "scsbactivemq:queue:fsOngoingAccessionReportQ";
    public static final String FTP_SUBMIT_COLLECTION_REPORT_Q = "scsbactivemq:queue:ftpSubmitCollectionReportQ";
    public static final String FTP_DE_ACCESSION_SUMMARY_REPORT_Q = "scsbactivemq:queue:ftpDeAccessionSummaryReportQ";
    public static final String FS_ACCESSION_SUMMARY_REPORT_Q = "scsbactivemq:queue:fsAccessionSummaryReportQ";
    public static final String EMAIL_Q = "scsbactivemq:queue:solrClientEmailQ";
    public static final String FTP_SERIAL_MVM_REPORT_Q = "scsbactivemq:queue:ftpSerialMvmReportsQ";
    public static final String FTP_MATCHING_SUMMARY_REPORT_Q = "scsbactivemq:queue:ftpMatchingSummaryReportQ";

    //Camel Route Id Constants
    public static final String FS_SUBMIT_COLLECTION_REJECTION_REPORT_ID = "fsSubmitCollectionRejectionReport";
    public static final String FTP_SUBMIT_COLLECTION_REJECTION_REPORT_ID = "ftpSubmitCollectionRejectionReport";
    public static final String FS_SUBMIT_COLLECTION_EXCEPTION_REPORT_ID = "fsSubmitCollectionExceptionReport";
    public static final String FTP_SUBMIT_COLLECTION_EXCEPTION_REPORT_ID = "ftpSubmitCollectionExceptionReport";
    public static final String FTP_SUBMIT_COLLECTION_SUMMARY_REPORT_ID = "ftpSubmitCollectionSummaryReport";
    public static final String FS_SUBMIT_COLLECTION_SUMMARY_REPORT_ID = "fsSubmitCollectionSummaryReport";
    public static final String FTP_SUBMIT_COLLECTION_SUCCESS_REPORT_ID = "ftpSubmitCollectionSuccessReport";
    public static final String FS_SUBMIT_COLLECTION_SUCCESS_REPORT_ID = "fsSubmitCollectionSuccessReport";
    public static final String FTP_SUBMIT_COLLECTION_FAILURE_REPORT_ID = "ftpSubmitCollectionFailureReport";
    public static final String FS_SUBMIT_COLLECTION_FAILURE_REPORT_ID = "fsSubmitCollectionFailureReport";
    public static final String FTP_SUBMIT_COLLECTION_REPORT_ID = "ftpSubmitCollectionReportRoute";
    public static final String EMAIL_ROUTE_ID = "solrClientEmailQ";
    public static final String FTP_TITLE_EXCEPTION_REPORT_ROUTE_ID = "ftpTitleExceptionReportsRoute";
    public static final String FTP_SERIAL_MVM_REPORT_ROUTE_ID = "ftpSerialMvmReportsRoute";
    public static final String FTP_MATCHING_SUMMARY_REPORT_ROUTE_ID = "ftpMatchingSummaryReportRoute";

    public static final String SUBMIT_COLLECTION_SUMMARY_Q_SUFFIX = "SubmitCollectionSummaryReportQ";
    public static final String SUMMARY_REPORT_FILE_NAME = "MatchingCGDSummaryReport";
    public static final String MATCHING_SUMMARY_MONOGRAPH = "MatchingMonographCGDSummary";
    public static final String MATCHING_SUMMARY_SERIAL = "MatchingSerialCGDSummary";
    public static final String MATCHING_SUMMARY_MVM = "MatchingMVMCGDSummary";
    public static final String TITLE_EXCEPTION_REPORT = "TitleExceptionReport";
    public static final String MATCHING_SUMMARY_REPORT = "MatchingSummaryReport";
    public static final String MATCHING_SERIAL_MVM_REPORT = "MatchingSerialMvmReport";
    public static final String UNDER_SCORE = "_";
    public static final String CSV_EXTENSION = ".csv";
    public static final String MATCHING_BIB_IDS = "matchingBibIds";
    public static final String MATCHING_REPORTS_SEND_EMAIL = "sendEmailForMatchingReports";
    public static final String ACCESSION_REPORTS_SEND_EMAIL = "sendEmailForAccessionReports";

    public static final String DATE_FORMAT_FOR_FILE_NAME = "yyyyMMdd_HHmmss";
    public static final String DATE_FORMAT_FOR_REPORT_FILE_NAME = "ddMMMyyyyHHmmss";

    public static final String VOLUME_PART_YEAR = "VolumePartYear";
    public static final String INSTITUTION_ID = "InstitutionId";
    public static final String OCLC = "Oclc";
    public static final String ISBN = "Isbn";
    public static final String ISSN = "Issn";
    public static final String LCCN = "Lccn";
    public static final String USE_RESTRICTIONS = "UseRestrictions";
    public static final String SUMMARY_HOLDINGS = "SummaryHoldings";
    public static final String MATERIAL_TYPE = "MaterialType";
    public static final String INITIAL_MATCHING_OPERATION_TYPE = "InitialMatchingAlgorithm";
    public static final String ONGOING_MATCHING_OPERATION_TYPE = "OngoingMatchingAlgorithm";

    public static final String MATCHING_PENDING_BIBS = "PendingBibMatches";

    //Report Types For Matching Algorithm
    public static final String SINGLE_MATCH = "SingleMatch";
    public static final String MULTI_MATCH = "MultiMatch";

    //Transmission Types

    public static final String MATERIAL_TYPE_EXCEPTION = "MaterialTypeException";
    public static final String TITLE_EXCEPTION_TYPE = "TitleException";
    public static final String CRITERIA_VALUES = "CriteriaValues";
    public static final String MATCH_POINT = "MatchPoint";

    public static final String IMS_LOCATION_CODE = "ImsLocation";

    //Error Message
    public static final String RECORD_NOT_AVAILABLE = "Database may be empty or bib table does not contain this record";
    public static final String SERVER_ERROR_MSG = "Server is down for maintenance. Please try again later.";
    public static final String EMPTY_FACET_ERROR_MSG = "Check facets. At least one Bib Facet and one Item Facet must be checked to get results.";
    public static final String INVALID_BARCODE_LENGTH="Barcode length should not exceed 45 characters";

    public static final String BARCODE_LIMIT_ERROR = "A maximum of only 10 items can be retrieved. Ignored barcode(s)";
    public static final String ACCESSION_SUCCESS = "One or more records were successfully accessioned.";
    public static final String ACCESSION_DUMMY_RECORD = "Dummy record created";

    //Request
    public static final String ITEM_BARCODE_EMPTY = "Item Barcode is Blank.";
    public static final String CUSTOMER_CODE_EMPTY = "Customer Code is Blank.";
    public static final String OWNING_INST_EMPTY = "Owning Institution is Blank.";

    public static final String UNKNOWN_INSTITUTION = "UN";
    public static final String SCSB = "SCSB";

    public static final String OWNING_INSTITUTION_ITEM_ID = "OwningInstitutionItemId";
    public static final String BIB_CREATED_DATE = "BibCreatedDate";
    public static final String BIB_LAST_UPDATED_DATE = "BibLastUpdatedDate";
    public static final String DATE = "Date";
    public static final String ITEM_BARCDE_DOESNOT_EXIST = "Item Barcode doesn't exist in SCSB database.";
    public static final String BIB_ITEM_DOESNOT_EXIST = "Bib Id doesn't exist in SCSB database.";

    public static final String FS_DE_ACCESSION_SUMMARY_REPORT_ID = "fsDeAccessionSummaryReportQ";
    public static final String FTP_DE_ACCESSION_SUMMARY_REPORT_ID = "ftpDeAccessionSummaryReportQ";

    public static final String FS_ONGOING_ACCESSION_REPORT_ID = "fsOngoingAccessionReportQ";
    public static final String FTP_ONGOING_ACCESSION_REPORT_ID = "ftpOngingAccessionReportQ";
    public static final String TRANSFER_REPORT = "Transfer_Report";
    public static final String FAILURE_BIB_REASON = "ReasonForFailureBib";
    public static final String FAILURE_ITEM_REASON = "ReasonForFailureItem";

    public static final String SUBMIT_COLLECTION_SUMMARY_REPORT = "SubmitCollectionSummary";
    public static final String REJECTION = "Rejection";
    public static final String SC_EXCEPTION = "Exception";
    public static final String ITEM_BARCODE_ALREADY_ACCESSIONED_MSG = "Unavailable barcode from partner is already accessioned";
    public static final String REACCESSION = "re-accession";
    public static final String ITEM_ISDELETED_TRUE_TO_FALSE = "Item isdeleted true to false";
    public static final String DUMMY_CALL_NUMBER_TYPE = "dummycallnumbertype";
    public static final String POPULATE_DATA_FOR_DATA_DUMP_JOB = "populateDataForDataDumpJob";

    //solr
    public static final String BIB_ID_LIST = "BibIdList";
    public static final String BIB_ID_RANGE = "BibIdRange";
    public static final String DATE_RANGE = "DateRange";
    public static final String BIB_ID_RANGE_FROM = "BibIdRangeFrom";
    public static final String BIB_ID_RANGE_TO = "BibIdRangeTo";
    public static final String DATE_RANGE_FROM = "DateRangeFrom";
    public static final String DATE_RANGE_TO = "DateRangeTo";

    //Reports
    public static final String TRANSMISSION_TYPE = "TransmissionType";
    public static final String REPORT_TYPE = "ReportType";
    public static final String JOB_PARAM_DATA_FILE_NAME = "FileName";
    public static final String DATE_FORMAT_FOR_REPORTS = "yyyyMMdd_HHmmss";


    public static final String  BIBITEM_LASTUPDATED_DATE = "BibItemLastUpdatedDate";
    public static final String  ITEM_LASTUPDATED_DATE = "ItemLastUpdatedDate";
    public static final String  ITEM_CREATED_DATE = "ItemCreatedDate";
    public static final String  BIB_LASTUPDATED_DATE = "BibLastUpdatedDate";

    public static final String OWNING_INST = "owningInstitution";
    public static final String TO = "To";
    public static final String ITEM_CATALOGING_STATUS = "ItemCatalogingStatus";
    public static final String BIB_CATALOGING_STATUS = "BibCatalogingStatus";
    public static final String ONGOING_ACCESSION_REPORT = "Ongoing_Accession_Report";
    public static final String ERROR = "error->";
    public static final String EXCEPTION = "exception->";
    public static final String ITEM_BARCODE_NOT_FOUND = "Item Barcode not found";
    public static final String MARC_FORMAT_PARSER_ERROR = "Unable to parse input";
    public static final String INVALID_MARC_XML_ERROR_MSG = "Unable to parse input, xml is having invalid marc tag";
    public static final String SERVICE_UNAVAILABLE =" Service is unavailable.";
    public static final String VERIFY_ONGOING_ACCESSION_REPORT_MSG = "Please verify ongoing accession report";
    public static final String DUMMY_BIB_CONTENT_XML = "dummybibcontent.xml";
    public static final String DUMMY_HOLDING_CONTENT_XML = "dummyholdingcontent.xml";
    public static final String ONGOING_ACCESSION_LIMIT_EXCEED_MESSAGE = "Input limit exceeded, maximum allowed input limit is ";
    public static final String ITEM_ALREADY_ACCESSIONED = "Item already accessioned - Existing item details : ";
    public static final String INVALID_BOUNDWITH_RECORD = "Bound-with item having invalid data";
    public static final String FAILED = "Failed";

    public static final String STATUS_DONE="Status  : Done";
    public static final String TOTAL_TIME_TAKEN="Total Time Taken ";
    public static final String STATUS_FAILED="Status : Failed";
    public static final String PUL_MATCHING_COUNT="pulMatchingCount";
    public static final String CUL_MATCHING_COUNT="culMatchingCount";
    public static final String NYPL_MATCHING_COUNT="nyplMatchingCount";
    public static final String GENERATED_REPORT_FILE_NAME="The Generated Report File Name : ";
    public static final String TOTAL_TIME_TAKEN_TO_GENERATE_FILE_NAME="Total time taken to generate File : ";
    public static final String ACCESSION_FAILURE_REPORT="Accession_Failure_Report";
    public static final String TOTAL_BIB_ID_PARTITION_LIST="Total Bib Id partition List : {}";
    public static final String NON_MONOGRAPH_RECORD_NUMS="NonMonographRecordNums";
    public static final String EXCEPTION_RECORD_NUMS="ExceptionRecordNums";

    //Logger
    public static final String SCSB_PERSISTENCE_SERVICE_IS_UNAVAILABLE = "Scsb Persistence Service is Unavailable.";
    public static final String CGD_NA="NA";

    public static final String NUMBER_PATTERN="[^0-9]";

    public static final String FILE = "file:";
    public static final String DELETE_FILE_OPTION = "?delete=true";
    public static final String FILE_NAME = "fileName";
    public static final String SUBMIT_COLLECTION_REPORT_SFTP_OPTIONS = "&fileName=${in.header.fileName}&fileExist=append";
    public static final String ITEM_LAST_UPDATED_DATE = "ItemLastUpdatedDate";

    //deaccession report
    public static final String BIB_DOC_TYPE = "DocType:Bib";
    public static final String SOLR_BIB_ID = "BibId:";
    public static final String IS_DELETED_BIB_TRUE = "IsDeletedBib:true";
    public static final String TITLE_DISPLAY = "Title_display";

    public static final String EMAIL_FOR = "emailFor";
    public static final String UPDATECGD = "updateCgd";
    public static final String BATCHJOB = "batchJob";
    public static final String PURGE_EXCEPTION_REQUESTS = "PurgeExceptionRequests";
    public static final String PENDING = "pending";
    public static final String ACCESSION_SAVE_SUCCESS_STATUS = "The accession request is successfully processed.";
    public static final String ACCESSION_SAVE_FAILURE_STATUS = "Failed to process accession request.";
    public static final String MATCHING_REPORTS = "matchingReports";
    public static final String ACCESSION_REPORTS = "accessionReports";
    public static final String MATCHING_ALGORITHM_REPORTS = "MatchingAlgorithm Reports";
    public static final String ACCESSION_BATCH_COMPLETE = "Accession Batch Complete";
    public static final String INSTITUTION_NAME = "institutionName";

    public static final String MIXED_STATUS = "MixedStatus";

    public static final String DOC_TYPE_ITEM = "DocType:Item";
    public static final String ITEM_STATUS_INCOMPLETE = "ItemCatalogingStatus:Incomplete";
    public static final String IS_DELETED_ITEM_FALSE = "IsDeletedItem:false";
    public static final String ITEM_BIB_ID = "ItemBibId";
    public static final String AUTHOR_DISPLAY = "Author_display";

    public static final String SUBMIT_COLLECTION = "SubmitCollectionReport";
    public static final String OWN_INST_BIB_ID = " OwningInstBibId-";
    public static final String OWN_INST_HOLDING_ID = " OwningInstHoldingId-";
    public static final String OWN_INST_ITEM_ID = " OwningInstItemId-";
    public static final String OWN_INST_BIBID_LIST = "owningInstBibIdList";


    public static final String BULK_ACCESSION_SUMMARY = "BULK_ACCESSION_SUMMARY";
    public static final String ACCESSION_SUMMARY = "ACCESSION_SUMMARY";
    public static final String ACCESSION_JOB_FAILURE = "Exception occurred in SCSB Ongoing Accession Job";
    public static final String IMS_LOCATION = "ImsLocation";

    public static class SERVICE_PATH {
        public static final String CHECKIN_ITEM = "requestItem/checkinItem";
        public static final String REFILE_ITEM_IN_ILS = "requestItem/refileItemInILS";
        public static final String REPLACE_REQUEST = "requestItem/replaceRequest";
    }

    public static final String SUCCESS_INCOMPLETE_RECORD = "Success - Incomplete record";
    public static final String INCOMPLETE_RESPONSE = "incompleteResponse";

    public static class TRANSFER {
        public static final String TRANSFER = "transfer";
        public static final String INSTITUION_EMPTY = "Institution is empty";
        public static final String UNKNOWN_INSTITUTION = "Unknow institution";
        public static final String SOURCE_DESTINATION_ITEM_IDS_NOT_MATCHING = "Source and Destination item ids are not matching";
        public static final String SOURCE_DESTINATION_HOLDINGS_IDS_NOT_MATCHING = "Source and Destination holdings ids are not matching";
        public static final String SOURCE_EMPTY = "Source is empty";
        public static final String DESTINATION_EMPTY = "Destination is empty";
        public static final String SOURCE_OWN_INST_BIB_ID_EMPTY = "Source owning institution bib id is empty";
        public static final String SOURCE_OWN_INST_ITEM_ID_EMPTY = "Source owning institution item id is empty";
        public static final String DEST_OWN_INST_BIB_ID_EMPTY = "Destination owning institution bib id is empty";
        public static final String SOURCE_OWN_INST_HOLDINGS_ID_EMPTY = "Source owning institution holdings id is empty";
        public static final String DEST_OWN_INST_HOLDINGS_ID_EMPTY = "Destination owning institution holdings id is empty";
        public static final String DEST_OWN_INST_ITEM_ID_EMPTY = "Destination owning institution item id is empty";
        public static final String SOURCE_BIB_NOT_EXIST = "Source bib does not exist";
        public static final String SOURCE_HOLDING_NOT_UNDER_SOURCE_BIB = "Source holdings is not under source bib";
        public static final String SOURCE_ITEM_NOT_UNDER_SOURCE_HOLDING = "Source item is not under source holding";
        public static final String DEST_HOLDINGS_ATTACHED_WITH_DIFF_BIB = "Destination holdings is linked with another bib and not under destination bib";
        public static final String DEST_HOLDING_DEACCESSIONED="Destination holding is a deaccessioned holding";
        public static final String SOURCE_ITEM_DEACCESSIONED="Source item is a deaccessioned item";
        public static final String DEST_BIB_DEACCESSIONED="Destination Bib is a deaccessioned bib";
        public static final String SOURCE_HOLDING_DEACCESSIONED="Source Holding is a deaccessioned holding";

        public static final String SUCCESSFULLY_RELINKED = "Successfully relinked";
        public static final String RELINKED_FAILED = "Relinked Failed";
        public static final String COMPLETED = "Success";
        public static final String FAILED = "Failed";
        public static final String PARTIALLY_SUCCESS = "Partially Success";
        public static final String REQUEST = "Request";
        public static final String RESPONSE = "Response";
        public static final String INSTITUTION = "Institution";
        public static final String TRANSFER_TYPE = "TransferType";
        public static class TRANSFER_TYPES {
            public static final String HOLDINGS_TRANSFER = "Holdings Transfer";
            public static final String ITEM_TRANSFER = "Item Transfer";
        }
        public static final String ROOT = "Root";
    }

    public static final String ITEM_STATUS_COMPLETE="ItemCatalogingStatus:Complete";
    public static final String BIB_STATUS_INCOMPLETE="BibCatalogingStatus:Incomplete";
    public static final String DISTINCT_VALUES_FALSE="{!distinctValues=false}Barcode";
    public static final String GROUP = "group";
    public static final String GROUP_FIELD = "group.field";
    public static final String INCREMENTAL_DUMP_TO_NOW = "TO NOW";

    public static final String ACCESSION_JOB_INITIATE_ROUTE_ID = "scsbactivemq:queue:accessionInitiateRoute";
    public static final String MATCHING_ALGORITHM_JOB_INITIATE_ROUTE_ID = "scsbactivemq:queue:matchingAlgorithmInitiateRoute";
    public static final String PROCESSING = "Processing";
    public static final String MATCHING_BATCH_JOB_DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";
    public static final String ACCESSION__CAUGHT_EXCEPTION_METHOD = "caughtException";

    public static final String ETL_DATA_LOAD_NAMESPACE = "http://www.loc.gov/MARC21/slim";
    public static final String SCSB_CAMEL_S3_TO_ENDPOINT = "aws-s3://{{scsbBucketName}}?autocloseBody=false&region={{awsRegion}}&accessKey=RAW({{awsAccessKey}})&secretKey=RAW({{awsAccessSecretKey}})";
    public static final String  SUBMIT_COLLECTION_BASE_PATH= "reports/collection/submitCollection/";

    private RecapConstants(){}
}