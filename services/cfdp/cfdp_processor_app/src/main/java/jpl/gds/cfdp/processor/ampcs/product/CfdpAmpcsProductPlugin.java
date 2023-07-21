/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.processor.ampcs.product;

import cfdp.engine.DeliveryCode;
import cfdp.engine.TransStatus;
import cfdp.engine.ampcs.ICfdpAmpcsProductPlugin;
import cfdp.engine.ampcs.PduLog;
import cfdp.engine.ampcs.TransIdUtil;
import jpl.gds.cfdp.processor.ampcs.session.CfdpAmpcsSessionManager;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.message.disruptor.MessageDisruptorManager;
import jpl.gds.cfdp.processor.message.disruptor.MessageEvent;
import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.api.*;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductOutputDirectoryUtil;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.message.*;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.ProductTemplateManager;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.*;
import org.apache.velocity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jpl.gds.product.api.builder.AssemblyTrigger.CFDP_AMPCS_PRODUCT_PLUGIN;

/**
 * Class {@code CfdpAmpcsProductPlugin} is used by CFDP Processor to produce all AMPCS Products-related artifacts.
 *
 * @since CFDP R3
 */
@Service
public class CfdpAmpcsProductPlugin implements ICfdpAmpcsProductPlugin {

    protected Tracer log;

    static final String EMD_VERSION = "1";

    private static final String COMPLETE_DATA_FILE_EXTENSION = ".dat";
    private static final String COMPLETE_DATA_EMD_FILE_EXTENSION = ".emd";
    private static final String PARTIAL_DATA_FILE_EXTENSION = ".pdat";
    private static final String PARTIAL_DATA_EMD_FILE_EXTENSION = ".pemd";

    /* Below is a list of Velocity template variable name definitions. Whatever variable name that emd_cfdp_1.vm declares,
    it should be declared here as well, by convention and to avoid mistakenly leaving out any variables from being defined. */
    public static final String SESSION_ID_TEMPLATE_VARNAME = "sessionId";
    public static final String SESSION_NAME_TEMPLATE_VARNAME = "sessionName";
    public static final String FSW_DICTIONARY_DIR_TEMPLATE_VARNAME = "fswDictionaryDir";
    public static final String FSW_VERSION_TEMPLATE_VARNAME = "fswVersion";
    public static final String VENUE_TYPE_TEMPLATE_VARNAME = "venueType";
    public static final String TESTBED_NAME_TEMPLATE_VARNAME = "testbedName";
    public static final String USER_TEMPLATE_VARNAME = "user";
    public static final String HOST_TEMPLATE_VARNAME = "host";
    public static final String OUTPUT_DIR_TEMPLATE_VARNAME = "outputDir";
    public static final String PRODUCT_CREATION_TIME_STR_TEMPLATE_VARNAME = "productCreationTimeStr";
    public static final String SCID_TEMPLATE_VARNAME = "scid";
    public static final String APID_TEMPLATE_VARNAME = "apid";
    public static final String PRODUCT_TYPE_TEMPLATE_VARNAME = "productType";
    public static final String VCID_TEMPLATE_VARNAME = "vcid";
    public static final String GROUND_STATUS_TEMPLATE_VARNAME = "groundStatus";
    public static final String FULL_PATH_TEMPLATE_VARNAME = "fullPath";
    public static final String SEQUENCE_ID_TEMPLATE_VARNAME = "sequenceId";
    public static final String SEQUENCE_VERSION_TEMPLATE_VARNAME = "sequenceVersion";
    public static final String COMMAND_NUMBER_TEMPLATE_VARNAME = "commandNumber";
    public static final String DVT_COARSE_TEMPLATE_VARNAME = "dvtCoarse";
    public static final String DVT_FINE_TEMPLATE_VARNAME = "dvtFine";
    public static final String FIRST_PART_SCLK_TEMPLATE_VARNAME = "firstPartSclk";
    public static final String FIRST_PART_SCET_TEMPLATE_VARNAME = "firstPartScet";
    public static final String FIRST_PART_SOL_TEMPLATE_VARNAME = "firstPartSol";
    public static final String FIRST_PART_ERT_TEMPLATE_VARNAME = "firstPartErt";
    public static final String EXPECTED_CHECKSUM_TEMPLATE_VARNAME = "expectedChecksum";
    public static final String ACTUAL_CHECKSUM_TEMPLATE_VARNAME = "actualChecksum";
    public static final String EXPECTED_FILE_SIZE_TEMPLATE_VARNAME = "expectedFileSize";
    public static final String ACTUAL_FILE_SIZE_TEMPLATE_VARNAME = "actualFileSize";
    public static final String CFDP_TRANSACTION_ID_TEMPLATE_VARNAME = "cfdpTransactionId";
    public static final String NUM_RECEIVED_PARTS_TEMPLATE_VARNAME = "numReceivedParts";
    public static final String MISSING_RANGE_LIST_TEMPLATE_VARNAME = "missingRangeList";
    public static final String PART_LIST_TEMPLATE_VARNAME = "partList";

    private DateFormat dateFormatter;

    @Autowired
    private IProductPropertiesProvider productPropertiesProvider;

    @Autowired
    private ProductTemplateManager productTemplateManager;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    protected IProductBuilderObjectFactory productBuilderObjectFactory;

    @Autowired
    private IProductMessageFactory productMessageFactory;

    @Autowired
    private MessageDisruptorManager messageDisruptorManager;

    // Key is TransID converted to string; also the concurrency lock
    private Map<String, IProductMetadataProvider> productMetadataCache;
    // Key is TransID converted to string
    private Map<String, Long> productMetadataLastUpdated;

    ScheduledExecutorService scheduledThreadPool;

    @Autowired
    protected ICfdpDvtExtractor dvtExtractor;

    @Autowired
    private IVenueConfiguration venueConfiguration;

    @Autowired
    private IApidDefinitionProvider apidDefinitionProvider;

    @Autowired
    private IProductOutputDirectoryUtil productOutputDirectoryUtil;

    @Autowired
    protected IContextIdentification contextId;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private CfdpAmpcsSessionManager cfdpAmpcsSessionManager;

    @Autowired
    @Qualifier(PdppApiBeans.AUTOMATION_FEATURE_MANAGER)
    private ITelemetryFeatureManager tfm;

    // MPCS-11572 prevent PDPP Adder start twice
    private static boolean pdppAdderStarted = false;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        dateFormatter = TimeUtility.getFormatterFromPool();
        productMetadataCache = new HashMap<>();
        productMetadataLastUpdated = new HashMap<>();
        scheduledThreadPool = Executors.newScheduledThreadPool(1);
        scheduledThreadPool.scheduleWithFixedDelay(new PeriodicProductMetadataCachePurgeTask(), 10, 5, SECONDS);

        if(configurationManager.isPdppEnabled()){
            startPdppAdderService();
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduledThreadPool.shutdown();
        tfm.shutdown();
        TimeUtility.releaseFormatterToPool(dateFormatter);
    }

    int getNextCompleteVersionNumber(final Path productOutputDirectory, final String filename) throws IOException {
        int largestVersionNumber = 0;

        final Pattern p = Pattern.compile(filename + "-(\\d+).dat");

        final File[] files = productOutputDirectory.toFile().listFiles();

        for (int i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                final Matcher m = p.matcher(files[i].getName());

                if (m.find()) {
                    final int foundVersionNumber = Integer.parseInt(m.group(1));
                    largestVersionNumber = foundVersionNumber > largestVersionNumber
                            ? foundVersionNumber
                            : largestVersionNumber;
                }

            }

        }

        return ++largestVersionNumber;
    }

    int[] getNextIdentifiedPartialVersionNumber(final Path productOutputDirectory, final String filename) throws IOException {
        final int myCompleteVersionNumber = getNextCompleteVersionNumber(productOutputDirectory, filename);

        int largestVersionNumber = 0;

        final Pattern p = Pattern.compile(filename + "_Partial-" + myCompleteVersionNumber + "\\.(\\d+).pdat");

        final File[] files = productOutputDirectory.toFile().listFiles();

        for (int i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                final Matcher m = p.matcher(files[i].getName());

                if (m.find()) {
                    final int foundVersionNumber = Integer.parseInt(m.group(1));
                    largestVersionNumber = foundVersionNumber > largestVersionNumber
                            ? foundVersionNumber
                            : largestVersionNumber;
                }

            }

        }

        return new int[]{myCompleteVersionNumber, ++largestVersionNumber};
    }

    /**
     *
     * Create the metadata that will be sent along with the product part
     *
     * @param status
     * @param pduLog
     * @param sessionId
     * @param sessionHost
     * @return IReferenceProductMetadataUpdater
     * @throws ParseException
     * @throws SAXException
     */
    protected IReferenceProductMetadataUpdater createPopulatedProductMetadataForProductPartMessage(final TransStatus status,
                                                                                                 final PduLog pduLog,
                                                                                                 final long sessionId,
                                                                                                 final String sessionHost)
            throws ParseException, SAXException {
        final IReferenceProductMetadataUpdater productMetadataUpdater =
                (IReferenceProductMetadataUpdater) productBuilderObjectFactory.createMetadataUpdater();

        if (sessionId >= 0) {
            productMetadataUpdater.setSessionId(sessionId);
        }

        productMetadataUpdater.setSessionHost(sessionHost);
        productMetadataUpdater.setVcid(pduLog.getVcid());
        productMetadataUpdater.setApid(pduLog.getApid());

        if (pduLog.getProductType() != null) {
            productMetadataUpdater.setProductType(pduLog.getProductType());
        }

        productMetadataUpdater.setScid(pduLog.getScid());

        if (pduLog.getSclkStr() != null) {
            productMetadataUpdater.setSclk(pduLog.getSclkStr());
        }

        if (pduLog.getErtStr() != null) {
            productMetadataUpdater.setErt(new AccurateDateTime(pduLog.getErtStr()));
        }

        if (pduLog.getScetStr() != null) {
            productMetadataUpdater.setScet(pduLog.getScetStr());
        }

        if (pduLog.getSolStr() != null) {
            productMetadataUpdater.setSol(new LocalSolarTime(pduLog.getScid(), pduLog.getSolStr()));
        }

        productMetadataUpdater.setCfdpTransactionId(pduLog.getCfdpTransactionSequenceNumber());

        try {
            productMetadataUpdater.setDvtCoarse(dvtExtractor.extractDvtCoarse(status));
            productMetadataUpdater.setDvtFine(dvtExtractor.extractDvtFine(status));
        } catch (final IllegalArgumentException iae) {
            log.warn("Unable to set DVT on the ProductPart message for transaction " + status.getTransID() + ": " + ExceptionTools.getMessage(iae));
        }

        IContextConfiguration contextConfiguration = cfdpAmpcsSessionManager.getSession(status.getTransID());
        if(contextConfiguration != null) {
            productMetadataUpdater.setFswDictionaryDir(contextConfiguration.getDictionaryConfig().getFswDictionaryDir());
            productMetadataUpdater.setFswDictionaryVersion(contextConfiguration.getDictionaryConfig().getFswVersion());
        }

        productMetadataUpdater.setCommandNumber(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setSequenceId(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setSequenceVersion(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setTotalParts(0); // CFDP PDUs don't specify total parts
        return productMetadataUpdater;
    }

    /**
     * Create the metadata that will be sent along with the product
     *
     * @param status
     * @param dataFilePath
     * @return IReferenceProductMetadataUpdater
     */
    protected IReferenceProductMetadataUpdater createPopulatedProductMetadataForProductFileMessage(final TransStatus status,
                                                                                                 final String dataFilePath) {
        final IReferenceProductMetadataUpdater productMetadataUpdater =
                (IReferenceProductMetadataUpdater) productBuilderObjectFactory.createMetadataUpdater();
        productMetadataUpdater.setSessionId(status.getSessionId());
        productMetadataUpdater.setSessionHost(status.getHost());
        productMetadataUpdater.setFullPath(dataFilePath);
        productMetadataUpdater.setVcid(status.getVcid());
        productMetadataUpdater.setApid(status.getApid());

        productMetadataUpdater.setSessionHostId(contextId.getContextKey().getHostId());

        if (status.getProductType() != null) {
            productMetadataUpdater.setProductType(status.getProductType());
        }

        productMetadataUpdater.setScid(status.getScid());

        // Use last part's times
        final List<PduLog> pduLogValues = new ArrayList<>(status.getPduLog().values());

        if (pduLogValues.isEmpty()) {
            log.error("No part found in transaction - cannot populate time fields in the product metadata");
        } else {

            boolean foundLastReceivedPart = false;
            // Get last element and work our way back, looking for the last received part
            for (int i = pduLogValues.size() - 1; !foundLastReceivedPart && i >= 0; i--) {

                if (pduLogValues.get(i).isReceived()) {
                    foundLastReceivedPart = true;

                    try {

                        if (pduLogValues.get(i).getSclkStr() != null) {
                            productMetadataUpdater.setSclk(pduLogValues.get(i).getSclkStr());
                        }

                        if (pduLogValues.get(i).getErtStr() != null) {
                            productMetadataUpdater.setErt(new AccurateDateTime(pduLogValues.get(i).getErtStr()));
                        }

                        if (pduLogValues.get(i).getScetStr() != null) {
                            productMetadataUpdater.setScet(pduLogValues.get(i).getScetStr());
                        }

                        if (pduLogValues.get(i).getSolStr() != null) {
                            productMetadataUpdater.setSol(new LocalSolarTime(pduLogValues.get(i).getScid(), pduLogValues.get(i).getSolStr()));
                        }

                        try {
                            productMetadataUpdater.setDvtCoarse(dvtExtractor.extractDvtCoarse(status));
                            productMetadataUpdater.setDvtFine(dvtExtractor.extractDvtFine(status));
                        } catch (final IllegalArgumentException iae) {
                            // If DVT can't be extracted, set them to 0
                            productMetadataUpdater.setDvtCoarse(0);
                            productMetadataUpdater.setDvtFine(0);
                        }

                    } catch (SAXException | ParseException e) {
                        log.error("Exception thrown while doing time (SCLK, ERT, SCET, SOL, DVT) setting on product metadata: "
                                + ExceptionTools.getMessage(e), e);
                    }

                }

            }

        }

        productMetadataUpdater.setCfdpTransactionId(status.getTransID().getNumber());

        productMetadataUpdater.setCommandNumber(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setSequenceId(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setSequenceVersion(0); // Not provided by CFDP PDU message
        productMetadataUpdater.setTotalParts(0); // CFDP PDUs don't specify total parts so hint this to the users

        IContextConfiguration contextConfiguration = cfdpAmpcsSessionManager.getSession(status.getTransID());
        if(contextConfiguration != null) {
            productMetadataUpdater.setFswDictionaryDir(contextConfiguration.getDictionaryConfig().getFswDictionaryDir());
            productMetadataUpdater.setFswDictionaryVersion(contextConfiguration.getDictionaryConfig().getFswVersion());
        }

        productMetadataUpdater.setGroundStatus(getGroundStatus(status));
        productMetadataUpdater.setProductCreationTime(new AccurateDateTime(status.getDeliveryFileGenerationTime()));

        productMetadataUpdater.setChecksum(status.getFileChecksumInEof() != null ? status.getFileChecksumInEof() : 0);
        productMetadataUpdater.setFileSize(status.getFileSize());
        return productMetadataUpdater;
    }

    /**
     * Publish a ProductPart message for the incoming PDU.
     *
     * @param status      transaction status that contains filename and transaction sequence number for extracting the DVT
     * @param pduLog      PDU metadata
     * @param sessionId   AMPCS session key to associate the product part message with
     * @param sessionHost AMPCS session host to associate the product part message with
     */
    public void publishProductPartMessage(final TransStatus status, final PduLog pduLog, final long sessionId, final String sessionHost) {

        try {
            final IProductPartUpdater part = productBuilderObjectFactory.createPartUpdater();
            // part.setData() doesn't seem to be needed for messages
            // part.setGroupingFlags() doesn't seem to be needed for messages
            part.setMetadata(createPopulatedProductMetadataForProductPartMessage(status, pduLog, sessionId, sessionHost));
            part.setVcid(pduLog.getVcid());
            part.setApid(pduLog.getApid());
            part.setPacketSequenceNumber(part.getPacketSequenceNumber());
            part.setRelayScid(pduLog.getRelayScid());
            part.setPartLength(pduLog.getLength());
            part.setPartNumber(0); // CFDP PDUs don't have part numbers
            part.setPartOffset(pduLog.getOffset());
            part.setPartPduType(new CfdpPduType(pduLog.getPduType()));
            final IPartReceivedMessage m = productMessageFactory.createPartReceivedMessage(part);
            messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
        } catch (final ClassCastException cce) {
            log.error("IProductBuilderObjectFactory returned an object that can't be casted to IReferenceProductMetadataUpdater. Check mission or Spring configuration settings: "
                    + ExceptionTools.getMessage(cce), cce);
        } catch (final Exception e) {
            log.warn("Exception caught in publishProductPartMessage: " + ExceptionTools.getMessage(e));
            log.debug(e);
        }

    }

    @Override
    public void publishProductStartedMessage(final TransStatus status) {
        // Create a metadata object so that we can use its getDirectoryName() utility method
        final IReferenceProductMetadataUpdater metadata = (IReferenceProductMetadataUpdater) productBuilderObjectFactory.createMetadataUpdater();
        metadata.setApid(status.getApid());
        metadata.setProductType(status.getProductType());
        metadata.setCfdpTransactionId(status.getTransID().getNumber());

        final IProductStartedMessage m = productMessageFactory.
                createProductStartedMessage(status.getProductType(), status.getApid(), status.getVcid(),
                        metadata.getDirectoryName()
                                + "/" + Long.toUnsignedString(metadata.getCfdpTransactionId()), 0);
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
    }

    private void productGenerationCompleteProduct(final TransStatus status, final Path sourceDownlinkFilePath,
                                                  final Path productOutputDirectory) throws IOException, TemplateException {

        // MPCS-10526 - If destination filename is a path of directory(-ies), just get the filename at the
        // end. AMPCS legacy products don't support such paths.
        String destinationFileName = Paths.get(status.getDestinationFile()).getFileName().toString();

        // Determine complete version
        final int completeVersion = getNextCompleteVersionNumber(productOutputDirectory, destinationFileName);

        // Determine product copy destination path (minus extension)
        final String productDestinationPathStemStr = destinationFileName + "-" + completeVersion;
        final Path productCopyDestinationPath = productOutputDirectory.resolve(productDestinationPathStemStr + COMPLETE_DATA_FILE_EXTENSION);
        final Path productEmdPath = productOutputDirectory.resolve(productDestinationPathStemStr + COMPLETE_DATA_EMD_FILE_EXTENSION);

        // Make sure that the product destination doesn't exist yet
        if (Files.exists(productCopyDestinationPath)) {
            throw new IOException("Product file " + productCopyDestinationPath + " already exists");
        }

        // Create any intermediate directories
        if (Files.notExists(productCopyDestinationPath.getParent())) {
            Files.createDirectories(productCopyDestinationPath.getParent());
        }

        // Now copy
        Files.copy(sourceDownlinkFilePath, productCopyDestinationPath);

        generateEmdFile(status, productCopyDestinationPath, productEmdPath);

        // Publish ProductAssembled message
        final IReferenceProductMetadataUpdater metadata = createPopulatedProductMetadataForProductFileMessage(status,
                productCopyDestinationPath.toString());
        final IProductAssembledMessage m = productMessageFactory.createProductAssembledMessage(metadata,
                metadata.getDirectoryName()
                        + "/"
                        + Long.toUnsignedString(metadata.getCfdpTransactionId()));
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
    }


    private void productGenerationPartialProduct(final TransStatus status, final Path sourceDownlinkFilePath,
                                                 final Path productOutputDirectory, final String effectiveMetadataFilename) throws IOException, TemplateException {

        // MPCS-10526  - If destination filename is a path of directory(-ies), just get the filename at the
        // end. AMPCS legacy products don't support such paths.
        String destinationFileName = Paths.get(status.getDestinationFile()).getFileName().toString();

        // Determine complete version
        final int[] completePartialVersions = getNextIdentifiedPartialVersionNumber(productOutputDirectory,
                destinationFileName);

        // <filename>_Partial-<complete-version>.<partial-version>.pdat

        // Determine product copy destination path (minus extension)
        final String productDestinationPathStemStr = destinationFileName
                + "_Partial-" + completePartialVersions[0] + "." + completePartialVersions[1];
        final Path productCopyDestinationPath = productOutputDirectory.resolve(productDestinationPathStemStr + PARTIAL_DATA_FILE_EXTENSION);
        final Path productEmdPath = productOutputDirectory.resolve(productDestinationPathStemStr + PARTIAL_DATA_EMD_FILE_EXTENSION);

        // Make sure that the product destination doesn't exist yet
        if (Files.exists(productCopyDestinationPath)) {
            throw new IOException("Product file " + productCopyDestinationPath + " already exists");
        }

        // Create any intermediate directories
        if (Files.notExists(productCopyDestinationPath.getParent())) {
            Files.createDirectories(productCopyDestinationPath.getParent());
        }

        // Now copy
        Files.copy(sourceDownlinkFilePath, productCopyDestinationPath);

        generateEmdFile(status, productCopyDestinationPath, productEmdPath);

        // Publish PartialProduct message
        final IReferenceProductMetadataUpdater metadata = createPopulatedProductMetadataForProductFileMessage(status,
                productCopyDestinationPath.toString());
        final IPartialProductMessage m = productMessageFactory.createPartialProductMessage(metadata.getDirectoryName()
                        + "/" + Long.toUnsignedString(metadata.getCfdpTransactionId()),
                effectiveMetadataFilename, CFDP_AMPCS_PRODUCT_PLUGIN, metadata);
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
    }

    private void productGenerationUnknownFilenamePartialProduct(final TransStatus status,
                                                                final Path sourceDownlinkFilePath,
                                                                final Path productOutputDirectory,
                                                                final String effectiveMetadataFilename) throws IOException, TemplateException {
        String firstPartErtStr = null;

        // As long as JavaCFDP implements Machine's pdu_log as LinkedHashMap, the insertion order is maintained and we
        // can simply grab the first entry for the first part information
        final Map.Entry<String, PduLog> firstPdu = status.getPduLog().entrySet().iterator().next();

        if (firstPdu != null && firstPdu.getValue() != null && firstPdu.getValue().getErtStr() != null) {
            firstPartErtStr = firstPdu.getValue().getErtStr();
        } else {
            firstPartErtStr = "FIRST_PART_ERT_UNKNOWN";
        }

        // <source-entity-id>-<CFDP-transaction-id>-<time_at_part_receipt>.pdat
        final String productDestinationPathStemStr = Long.toUnsignedString(TransIdUtil.INSTANCE.convertEntity(status.getTransID().getSource()))
                + "-" + Long.toUnsignedString(status.getTransID().getNumber())
                + "-" + firstPartErtStr;

        // Determine product copy destination path
        final Path productCopyDestinationPath = productOutputDirectory.resolve(productDestinationPathStemStr + PARTIAL_DATA_FILE_EXTENSION);
        final Path productEmdPath = productOutputDirectory.resolve(productDestinationPathStemStr + PARTIAL_DATA_EMD_FILE_EXTENSION);

        // Create any intermediate directories
        if (Files.notExists(productCopyDestinationPath.getParent())) {
            Files.createDirectories(productCopyDestinationPath.getParent());
        }

        // Now copy
        Files.copy(sourceDownlinkFilePath, productCopyDestinationPath);

        generateEmdFile(status, productCopyDestinationPath, productEmdPath);

        // Publish PartialProduct message
        final IReferenceProductMetadataUpdater metadata = createPopulatedProductMetadataForProductFileMessage(status,
                productCopyDestinationPath.toString());
        final IPartialProductMessage m = productMessageFactory.createPartialProductMessage(metadata.getDirectoryName()
                        + "/" + Long.toUnsignedString(metadata.getCfdpTransactionId()),
                effectiveMetadataFilename, CFDP_AMPCS_PRODUCT_PLUGIN, metadata);
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);

    }

    private void generateEmdFile(final TransStatus status, final Path dataFilePath, final Path emdFilePath) throws
            TemplateException, IOException {
        final Template template = productTemplateManager.getTemplateForType("emd_cfdp", EMD_VERSION);
        final Map<String, Object> map = new HashMap<String, Object>();

        /* Instead of using IProductMetadataUpdater and its numerous required sub-objects just to populate the template,
        just opt to create a flat map (it's simpler and clearer) */
        map.put(SESSION_ID_TEMPLATE_VARNAME, status.getSessionId());
        map.put(SESSION_NAME_TEMPLATE_VARNAME, status.getSessionName());
        map.put(FSW_DICTIONARY_DIR_TEMPLATE_VARNAME, status.getFswDictionaryDir());
        map.put(FSW_VERSION_TEMPLATE_VARNAME, status.getFswVersion());
        map.put(VENUE_TYPE_TEMPLATE_VARNAME, status.getVenueType());
        map.put(TESTBED_NAME_TEMPLATE_VARNAME, status.getTestbedName());
        map.put(USER_TEMPLATE_VARNAME, status.getUser());
        map.put(HOST_TEMPLATE_VARNAME, status.getHost());
        map.put(OUTPUT_DIR_TEMPLATE_VARNAME, status.getAmpcsOutputDirectory());
        map.put(PRODUCT_CREATION_TIME_STR_TEMPLATE_VARNAME, status.getDeliveryFileGenerationTime() > 0
                ? dateFormatter.format(new AccurateDateTime(status.getDeliveryFileGenerationTime())) : "");
        map.put(SCID_TEMPLATE_VARNAME, status.getScid());
        map.put(APID_TEMPLATE_VARNAME, status.getApid());
        map.put(PRODUCT_TYPE_TEMPLATE_VARNAME, status.getProductType());
        map.put(VCID_TEMPLATE_VARNAME, status.getVcid());

        map.put(GROUND_STATUS_TEMPLATE_VARNAME, getGroundStatus(status));

        map.put(FULL_PATH_TEMPLATE_VARNAME, dataFilePath);
        map.put(SEQUENCE_ID_TEMPLATE_VARNAME, status.getSequenceId());
        map.put(SEQUENCE_VERSION_TEMPLATE_VARNAME, status.getSequenceVersion());
        map.put(COMMAND_NUMBER_TEMPLATE_VARNAME, status.getCommandNumber());

        try {
            map.put(DVT_COARSE_TEMPLATE_VARNAME, dvtExtractor.extractDvtCoarse(status));
            map.put(DVT_FINE_TEMPLATE_VARNAME, dvtExtractor.extractDvtFine(status));
        } catch (final IllegalArgumentException iae) {
            // If DVT can't be extracted, set them to 0
            map.put(DVT_COARSE_TEMPLATE_VARNAME, 0);
            map.put(DVT_FINE_TEMPLATE_VARNAME, 0);
        }

        // Obtain the list of PDUs in this transaction, which are in received/sent order
        final List<PduLog> receivedPdus = new LinkedList<>();

        for (final PduLog candidatePdu : status.getPduLog().values()) {

            if (candidatePdu.isReceived() && candidatePdu.isFileDataPdu()) {
                receivedPdus.add(candidatePdu);
            }

        }

        map.put(FIRST_PART_SCLK_TEMPLATE_VARNAME, receivedPdus.size() > 0 ? receivedPdus.get(0).getSclkStr() : "");
        map.put(FIRST_PART_SCET_TEMPLATE_VARNAME, receivedPdus.size() > 0 ? receivedPdus.get(0).getScetStr() : "");
        map.put(FIRST_PART_SOL_TEMPLATE_VARNAME, receivedPdus.size() > 0 ? receivedPdus.get(0).getSolStr() : "");
        map.put(FIRST_PART_ERT_TEMPLATE_VARNAME, receivedPdus.size() > 0 ? receivedPdus.get(0).getErtStr() : "");

        map.put(EXPECTED_CHECKSUM_TEMPLATE_VARNAME, status.getFileChecksumInEof());
        map.put(ACTUAL_CHECKSUM_TEMPLATE_VARNAME, status.getFileChecksumAsCalculated());
        map.put(EXPECTED_FILE_SIZE_TEMPLATE_VARNAME, status.getFileSize());
        map.put(ACTUAL_FILE_SIZE_TEMPLATE_VARNAME, status.getReceivedFileSize());

        // ReferenceProductMetadata only expects the transaction sequence number, not the source entity ID
        map.put(CFDP_TRANSACTION_ID_TEMPLATE_VARNAME, Long.toUnsignedString(status.getTransID().getNumber()));
        map.put(NUM_RECEIVED_PARTS_TEMPLATE_VARNAME, receivedPdus.size());

        map.put(MISSING_RANGE_LIST_TEMPLATE_VARNAME, status.getListOfGaps());
        map.put(PART_LIST_TEMPLATE_VARNAME, receivedPdus);

        final String xmlText = TemplateManager.createText(template, map);
        Files.write(emdFilePath, xmlText.getBytes());
    }

    /**
     * Get the ProductStatusType based on the TransStatus. Includes a checksum check.
     *
     * @param status of the product on the ground for the transaction
     * @return ProductStatusType
     */
    protected ProductStatusType getGroundStatus(final TransStatus status) {

        if (status.getDeliveryCode() == DeliveryCode.DATA_COMPLETE) {
            // MCSECLIV-1075 -> MPCS-12562: - Also check for checksum failure on complete products
            if (status.isChecksumFailed()) {
                return ProductStatusType.COMPLETE; // COMPLETE_CHECKSUM_FAIL is deprecated
            } else {
                return ProductStatusType.COMPLETE_CHECKSUM_PASS;
            }
        } else {
            if (status.isChecksumFailed()) {
                return ProductStatusType.PARTIAL_CHECKSUM_FAIL;
            } else {
                return ProductStatusType.PARTIAL;
            }
        }
    }

    @Override
    public void productGeneration(final TransStatus status, final String effectiveMetadataFilename) {
        final String sourceDownlinkFile = status.getEffectiveDestinationPath();

        try {
            final Path sourceDownlinkFilePath = Paths.get(sourceDownlinkFile);

            if (Files.exists(sourceDownlinkFilePath, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {

                // Handle various scenarios
                if (status.getDeliveryCode() == DeliveryCode.DATA_COMPLETE) {
                    productGenerationCompleteProduct(status, sourceDownlinkFilePath, getDirectoryToWriteProduct(status, true));
                } else if (status.getDestinationFile() == null || status.getDestinationFile().equalsIgnoreCase("")) {
                    productGenerationUnknownFilenamePartialProduct(status, sourceDownlinkFilePath,
                            getDirectoryToWriteProduct(status, false), effectiveMetadataFilename);
                } else {
                    productGenerationPartialProduct(status, sourceDownlinkFilePath, getDirectoryToWriteProduct(status, true),
                            effectiveMetadataFilename);
                }

            } else {
                log.warn("File downlinked but cannot produce AMPCS Product due to " + sourceDownlinkFilePath + " not being found");
            }

        } catch (final Exception e) {
            log.error("Exception caught in productGeneration (were PDUs ingested from file or another CFDP Processor?): " + ExceptionTools.getMessage(e));
            log.debug(e);
        }

    }

    /**
     * Gets the product directory for an operational venue
     *
     * @return operational product directory
     */
    protected String getProductOperationsDirectory() {
        return productPropertiesProvider.getOverrideProductDir() != null ?
                productPropertiesProvider.getOverrideProductDir() : productPropertiesProvider.getOpsStorageDir();
    }

    private Path getDirectoryToWriteProduct(final TransStatus status, final boolean filenameKnown) throws FileSystemException {

        /*

        If ops venue:
          Configured product output directory or overridden directory
            |- SCET DOY or <SCET month>/<SCET day>
                 |- APID number or product type
                      |- product file
                      |- partial product file
            |- UNIDENTIFIED_PARTIALS
                 |- product file with unknown filename

        If test venue:
          AMPCS session directory or overridden directory
            |- products
                 |- APID number or product type
                      |- product file
                      |- partial product file
                      |- product file with unknown filename

         */

        Path directoryToWriteProduct = null;
        String apidSubDirectory = null;

        if (productPropertiesProvider.productDirNameUsesApid()) {
            apidSubDirectory = String.format("%04d", status.getApid());
        } else {
            apidSubDirectory = apidDefinitionProvider.getApidDefinition(status.getApid()).getName();
        }

        if (venueConfiguration.getVenueType().isOpsVenue()) {
            // 6/2022 MCSECLIV-993 -> MPCS-12391: Default to configured product OPS directory if no override
            final String productDir = getProductOperationsDirectory();

            if (filenameKnown) {
                // Determine DVT SCET
                final DataValidityTime dvt = new DataValidityTime(dvtExtractor.extractDvtCoarse(status), dvtExtractor.extractDvtFine(status));
                final IAccurateDateTime dvtScet = SclkScetUtility.getScet(dvt, null, status.getScid());

                directoryToWriteProduct = Paths.get(productOutputDirectoryUtil.getProductOutputDir(productDir, dvtScet, apidSubDirectory));
            } else {
                // Filename is unknown
                directoryToWriteProduct = Paths.get(productOutputDirectoryUtil.getProductOutputDir(productDir, null, apidSubDirectory));
            }

        } else {
            // Test venue

            if (productPropertiesProvider.getOverrideProductDir() != null
                    && !"".equals(productPropertiesProvider.getOverrideProductDir())) {
                // Use overridden directory
                directoryToWriteProduct = Paths.get(productPropertiesProvider.getOverrideProductDir(), apidSubDirectory);
            } else {
                // Use AMPCS session directory

                if (status.getAmpcsOutputDirectory() == null) {
                    throw new IllegalArgumentException("TransStatus AMPCS output directory is null");
                }

                directoryToWriteProduct = Paths.get(status.getAmpcsOutputDirectory(), productPropertiesProvider.getStorageSubdir(), apidSubDirectory);
            }

        }

        if (Files.notExists(directoryToWriteProduct)) {

            if (!directoryToWriteProduct.toFile().mkdirs())
                throw new FileSystemException("Cannot write AMPCS Product: Could not create directory " + directoryToWriteProduct);
        }

        return directoryToWriteProduct;
    }

    public static class CfdpPduType implements IPduType {

        final int pduType;

        public CfdpPduType(final int pduType) {
            this.pduType = pduType;
        }

        /**
         * @return true if the pdu type is a metadata type.
         */
        @Override
        public boolean isMetadata() {
            return pduType == 0;
        }

        /**
         * @return true if the pdu type is an end of data type.
         */
        @Override
        public boolean isEndOfData() {
            return pduType == 1;
        }

        /**
         * @return true if the pdu type is a data type.
         */
        @Override
        public boolean isData() {
            return pduType == 2;
        }

        /**
         * @return true if the pdu type is an end type.
         */
        @Override
        public boolean isEnd() {
            return pduType == 1;
        }

    }

    private class PeriodicProductMetadataCachePurgeTask implements Runnable {

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {

            final long purgeCutOff = System.currentTimeMillis() - 5000;

            synchronized (productMetadataCache) {
                final List<String> keysToPurge = new ArrayList<>();

                productMetadataLastUpdated.forEach((k, v) -> {
                    if (v < purgeCutOff) keysToPurge.add(k);
                });

                keysToPurge.forEach(k -> {
                    productMetadataCache.remove(k);
                    productMetadataLastUpdated.remove(k);
                });

            }

        }

    }

    // MPCS-11572: Moved start of PDPP adder service here
    private void startPdppAdderService() {
        if(pdppAdderStarted){
            return;
        }
        String className = "";
        try {
            className = tfm.getClass().getName();
            log.trace("Successfully instantiated downlink feature " + className);

            boolean ok = tfm.init(appContext);
            if (!ok) {
                log.error("Startup of PDPP service " + tfm + " failed");
            } else {
                log.debug("Started feature manager: " + tfm);
                pdppAdderStarted = true;
            }
        } catch (Exception e) {
            log.error("PDPP service " + className + " could not be instantiated: " + e.getMessage(), e);
        }
    }
}