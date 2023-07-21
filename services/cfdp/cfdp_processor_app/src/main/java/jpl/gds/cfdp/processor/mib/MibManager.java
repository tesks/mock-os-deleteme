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
package jpl.gds.cfdp.processor.mib;

import cfdp.engine.*;
import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.common.mib.ELocalEntityMibPropertyKey;
import jpl.gds.cfdp.common.mib.ERemoteEntityMibPropertyKey;
import jpl.gds.cfdp.processor.ampcs.properties.CfdpProcessorAmpcsProperties;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.error.MissingRequiredPropertiesException;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

@Service
@DependsOn("configurationManager")
public class MibManager implements MIB {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    public static final String LOCAL_ENTITY_MIB_PROPERTY_PREFIX = "cfdp.processor.mib.local.";
    public static final String REMOTE_ENTITY_MIB_PROPERTY_PREFIX = "cfdp.processor.mib.remote.";

    @Autowired
    private ConfigurationManager configurationManager;

    private String mibFile;
    private Map<String, Properties> mibLocal;
    private Map<String, Properties> mibRemote;

    private DateFormat dateFormatter;

    private StringBuilder sb;

    private Object saveLock;

    @Autowired
    private GsfcUtil gsfcUtil;

    private String localEntityId;

    private CfdpProcessorAmpcsProperties ampcsProperties;

    @Autowired
    private CfdpFileUtil cfdpFileUtil;

    @PostConstruct
    public void init() throws MissingRequiredPropertiesException {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        mibFile = configurationManager.getWritableMibFile();
        log.info("CFDP Processor MIB file: " + mibFile);

        // MPCS-10233 - 10/1/2018 - If MIB file doesn't exist, create it
        final java.io.File f = new File(mibFile);

        if (!f.exists() || !f.isFile()) {
            log.warn("MIB file " + mibFile + " doesn't exist");

            try {
                cfdpFileUtil.createParentDirectoriesIfNotExist(mibFile);
                f.createNewFile();
                log.info("Created a new MIB file " + mibFile);
            } catch (final IOException ie) {
                log.error("Failed to create new MIB file: "
                        + ExceptionTools.getMessage(ie), ie);
            }

        }

        // Load all properties from the writable MIB file

        final MibLoader mibLoader = new MibLoader(appContext).load(mibFile);

        mibLocal = mibLoader.getMibLocal();
        mibRemote = mibLoader.getMibRemote();

        dateFormatter = TimeUtility.getFormatterFromPool();

        sb = new StringBuilder(256);

        saveLock = new Object();

        ampcsProperties = new CfdpProcessorAmpcsProperties();
    }

    @PreDestroy
    public void release() {
        TimeUtility.releaseFormatterToPool(dateFormatter);
    }

    private void saveMibToFile() {
        final Properties all = new OrderedProperties();

        sb.setLength(0);
        sb.append(LOCAL_ENTITY_MIB_PROPERTY_PREFIX);
        int firstStemOffset = sb.length();

        for (final Entry<String, Properties> entry : mibLocal.entrySet()) {
            sb.setLength(firstStemOffset);
            sb.append(entry.getKey());
            sb.append(".");
            final int secondStemOffset = sb.length();

            for (final String subKey : entry.getValue().stringPropertyNames()) {
                sb.setLength(secondStemOffset);
                sb.append(subKey);
                all.setProperty(sb.toString(), entry.getValue().getProperty(subKey));
            }

        }

        sb.setLength(0);
        sb.append(REMOTE_ENTITY_MIB_PROPERTY_PREFIX);
        firstStemOffset = sb.length();

        for (final Entry<String, Properties> entry : mibRemote.entrySet()) {
            sb.setLength(firstStemOffset);
            sb.append(entry.getKey());
            sb.append(".");
            final int secondStemOffset = sb.length();

            for (final String subKey : entry.getValue().stringPropertyNames()) {
                sb.setLength(secondStemOffset);
                sb.append(subKey);
                all.setProperty(sb.toString(), entry.getValue().getProperty(subKey));
            }

        }

        synchronized (saveLock) {

            try (FileOutputStream fos = new FileOutputStream(mibFile)) {
                all.store(fos, "Autosaved " + dateFormatter.format(new Date()));
            } catch (final IOException ie) {
                log.error("Exception while saving MIB properties file: " + ExceptionTools.getMessage(ie), ie);
            }

        }

    }

    /**
     * @return the mibLocal
     */
    public Map<String, Properties> getMibLocal() {
        return mibLocal;
    }

    /**
     * @return the mibRemote
     */
    public Map<String, Properties> getMibRemote() {
        return mibRemote;
    }

    public Properties getMibLocal(final String id) {
        return mibLocal.get(id);
    }

    public Properties getMibRemote(final String id) {
        return mibRemote.get(id);
    }

    public String getMibLocal(final String id, final ELocalEntityMibPropertyKey propertyKey) {
        final Properties props = mibLocal.get(id);

        if (props == null) {
            return null;
        } else {
            return props.getProperty(propertyKey.toString());
        }

    }

    public String getMibRemote(final String id, final ERemoteEntityMibPropertyKey propertyKey) {
        final Properties props = mibRemote.get(id);

        if (props == null) {
            return null;
        } else {
            return props.getProperty(propertyKey.toString());
        }

    }

    public Properties updateMibLocal(final String id, final Properties propertiesToUpdate) {
        final Properties updatedProperties = new Properties();

        if (!mibLocal.containsKey(id)) {
            mibLocal.put(id, new Properties());
        }

        final Properties props = mibLocal.get(id);

        for (final String key : propertiesToUpdate.stringPropertyNames()) {

            if (ELocalEntityMibPropertyKey.getAllKeyStrings().contains(key)) {
                props.setProperty(key, propertiesToUpdate.getProperty(key));
                updatedProperties.setProperty(key, propertiesToUpdate.getProperty(key));
            } else {
                log.warn("Local entity " + id + " MIB update skipping invalid key " + key);
            }

        }

        if (updatedProperties.size() > 0 && !configurationManager.mibAutosaveDisabled()) {
            saveMibToFile();
        }

        return updatedProperties;
    }

    public Properties updateMibRemote(final String id, final Properties propertiesToUpdate) {
        final Properties updatedProperties = new Properties();

        if (!mibRemote.containsKey(id)) {
            mibRemote.put(id, new Properties());
        }

        final Properties props = mibRemote.get(id);

        for (final String key : propertiesToUpdate.stringPropertyNames()) {

            if (ERemoteEntityMibPropertyKey.getAllKeyStrings().contains(key)) {
                props.setProperty(key, propertiesToUpdate.getProperty(key));
                updatedProperties.setProperty(key, propertiesToUpdate.getProperty(key));
            } else {
                log.warn("Remote entity " + id + " MIB update skipping invalid key " + key);
            }

        }

        if (updatedProperties.size() > 0 && !configurationManager.mibAutosaveDisabled()) {
            saveMibToFile();
        }

        return updatedProperties;
    }

    public boolean deleteMibLocal(final String id) {

        if (!mibLocal.containsKey(id)) {
            return false;
        } else {
            mibLocal.remove(id);

            if (!configurationManager.mibAutosaveDisabled()) {
                saveMibToFile();
            }

            return true;
        }

    }

    public boolean deleteMibRemote(final String id) {

        if (!mibRemote.containsKey(id)) {
            return false;
        } else {
            mibRemote.remove(id);

            if (!configurationManager.mibAutosaveDisabled()) {
                saveMibToFile();
            }

            return true;
        }

    }

    public boolean deleteMibLocal(final String id, final String key) {

        if (!mibLocal.containsKey(id) || !mibLocal.get(id).containsKey(key)) {
            return false;
        } else {
            mibLocal.get(id).remove(key);

            if (!configurationManager.mibAutosaveDisabled()) {
                saveMibToFile();
            }

            return true;
        }

    }

    public boolean deleteMibRemote(final String id, final String key) {

        if (!mibRemote.containsKey(id) || !mibRemote.get(id).containsKey(key)) {
            return false;
        } else {
            mibRemote.get(id).remove(key);

            if (!configurationManager.mibAutosaveDisabled()) {
                saveMibToFile();
            }

            return true;
        }

    }

    int gsfcLocalMibLookupInt(final ELocalEntityMibPropertyKey propertyKey) {
        final String valStr = getMibLocal(getLocalEntityId(), propertyKey);

        if (valStr != null) {
            return Integer.parseInt(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitLocalProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final int appDefault = Integer.parseInt(ampcsProperties.getMibFileInitLocalProperty(propertyKey));
            log.warn("MIB for local entity " + getLocalEntityId() + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault + " and saving to MIB");
            updateMibLocal(getLocalEntityId(), propertiesToUpdate);
            return appDefault;
        }

    }

    boolean gsfcLocalMibLookupBoolean(final ELocalEntityMibPropertyKey propertyKey) {
        final String valStr = getMibLocal(getLocalEntityId(), propertyKey);

        if (valStr != null) {
            return Boolean.parseBoolean(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitLocalProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final boolean appDefault = Boolean.parseBoolean(ampcsProperties.getMibFileInitLocalProperty(propertyKey));
            log.warn("MIB for local entity " + getLocalEntityId() + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault + " and saving to MIB");
            updateMibLocal(getLocalEntityId(), propertiesToUpdate);
            return appDefault;
        }

    }

    Response gsfcLocalMibLookupResponse(final ELocalEntityMibPropertyKey propertyKey) {
        final String valStr = getMibLocal(getLocalEntityId(), propertyKey);

        if (valStr != null) {
            return Response.valueOf(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitLocalProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final Response appDefault = Response.valueOf(ampcsProperties.getMibFileInitLocalProperty(propertyKey));
            log.warn("MIB for local entity " + getLocalEntityId() + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault.name() + " and saving to MIB");
            updateMibLocal(getLocalEntityId(), propertiesToUpdate);
            return appDefault;
        }

    }

    CycleMode gsfcLocalMibLookupCycleMode(final ELocalEntityMibPropertyKey propertyKey) {
        final String valStr = getMibLocal(getLocalEntityId(), propertyKey);

        if (valStr != null) {
            return CycleMode.valueOf(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitLocalProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final CycleMode appDefault = CycleMode.valueOf(ampcsProperties.getMibFileInitLocalProperty(propertyKey));
            log.warn("MIB for local entity " + getLocalEntityId() + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault.name() + " and saving to MIB");
            updateMibLocal(getLocalEntityId(), propertiesToUpdate);
            return appDefault;
        }

    }

    int gsfcRemoteMibLookupInt(final ID nodeID, final ERemoteEntityMibPropertyKey propertyKey) {
        final String remoteEntityStr = Long.toUnsignedString(gsfcUtil.convertEntityId(nodeID));
        final String valStr = getMibRemote(remoteEntityStr, propertyKey);

        if (valStr != null) {
            return Integer.parseUnsignedInt(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitRemoteProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final int appDefault = Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(propertyKey));
            log.warn("MIB for remote entity " + remoteEntityStr + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault + " and saving to MIB");
            updateMibRemote(remoteEntityStr, propertiesToUpdate);
            return appDefault;
        }

    }

    boolean gsfcRemoteMibLookupBoolean(final ID nodeID, final ERemoteEntityMibPropertyKey propertyKey) {
        final String remoteEntityStr = Long.toUnsignedString(gsfcUtil.convertEntityId(nodeID));
        final String valStr = getMibRemote(remoteEntityStr, propertyKey);

        if (valStr != null) {
            return Boolean.parseBoolean(valStr);
        } else {
            final String appDefaultStr = ampcsProperties.getMibFileInitRemoteProperty(propertyKey);
            final Properties propertiesToUpdate = new Properties();
            propertiesToUpdate.setProperty(propertyKey.getKeyStr(), appDefaultStr);
            final boolean appDefault = Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(propertyKey));
            log.warn("MIB for remote entity " + remoteEntityStr + " property " + propertyKey.toString()
                    + " is undefined: Using application default " + appDefault + " and saving to MIB");
            updateMibRemote(remoteEntityStr, propertiesToUpdate);
            return appDefault;
        }

    }

    // Begin MIB interface method implementations

    @Override
    public int ackLimit(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.POSITIVE_ACK_TIMER_EXPIRATION_LIMIT_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.POSITIVE_ACK_TIMER_EXPIRATION_LIMIT_PROPERTY);
    }

    @Override
    public int ackTimeout(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.POSITIVE_ACK_TIMEOUT_INTERVAL_SECONDS_PROPERTY))
                    + 2 * Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY))
                    + Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.POSITIVE_ACK_TIMEOUT_INTERVAL_SECONDS_PROPERTY)
                + 2 * gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY)
                + gsfcRemoteMibLookupInt(nodeID,
                ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY);
    }

    @Override
    public int inactivityTimeout(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.TRANSACTION_INACTIVITY_TIMEOUT_SECONDS_PROPERTY))
                    + 2 * Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY))
                    + Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.TRANSACTION_INACTIVITY_TIMEOUT_SECONDS_PROPERTY)
                + 2 * gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY)
                + gsfcRemoteMibLookupInt(nodeID,
                ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY);
    }

    @Override
    public boolean issueEofRecv() {
        return gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.EOF_RECV_INDICATION_PROPERTY);
    }

    @Override
    public boolean issueEofSent() {
        return gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.EOF_SENT_INDICATION_PROPERTY);
    }

    @Override
    public boolean issueFileSegmentRecv() {
        return gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.FILE_SEGMENT_RECV_INDICATION_PROPERTY);
    }

    @Override
    public boolean issueFileSegmentSent() {
        return gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.FILE_SEGMENT_SENT_INDICATION_PROPERTY);
    }

    @Override
    public int outgoingFileChunkSize(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.MAXIMUM_FILE_DATA_PER_OUTGOING_PDU_BYTES_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID,
                ERemoteEntityMibPropertyKey.MAXIMUM_FILE_DATA_PER_OUTGOING_PDU_BYTES_PROPERTY);
    }

    @Override
    public boolean saveIncompleteFiles(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.SAVE_INCOMPLETE_FILES_PROPERTY));
        }

        return gsfcRemoteMibLookupBoolean(nodeID, ERemoteEntityMibPropertyKey.SAVE_INCOMPLETE_FILES_PROPERTY);
    }

    @Override
    public int nakLimit(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.NAK_TIMER_EXPIRATION_LIMIT_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.NAK_TIMER_EXPIRATION_LIMIT_PROPERTY);
    }

    @Override
    public int nakTimeout(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.NAK_TIMEOUT_INTERVAL_SECONDS_PROPERTY))
                    + 2 * Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY))
                    + Integer.parseInt(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY));
        }

        return gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.NAK_TIMEOUT_INTERVAL_SECONDS_PROPERTY)
                + 2 * gsfcRemoteMibLookupInt(nodeID, ERemoteEntityMibPropertyKey.ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY)
                + gsfcRemoteMibLookupInt(nodeID,
                ERemoteEntityMibPropertyKey.TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY);
    }

    @Override
    public Response response(final ConditionCode conditionCode) {

        switch (conditionCode) {
            case NO_ERROR:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_NO_ERROR_PROPERTY);
            case POSITIVE_ACK_LIMIT_REACHED:
                return gsfcLocalMibLookupResponse(
                        ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_POSITIVE_ACK_LIMIT_REACHED_PROPERTY);
            case KEEP_ALIVE_LIMIT_REACHED:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_KEEP_ALIVE_LIMIT_REACHED_PROPERTY);
            case INVALID_TRANSMISSION_MODE:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_INVALID_TRANSMISSION_MODE_PROPERTY);
            case FILESTORE_REJECTION:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_FILESTORE_REJECTION_PROPERTY);
            case FILE_CHECKSUM_FAILURE:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_FILE_CHECKSUM_FAILURE_PROPERTY);
            case FILE_SIZE_ERROR:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_FILE_SIZE_ERROR_PROPERTY);
            case NAK_LIMIT_REACHED:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_NAK_LIMIT_REACHED_PROPERTY);
            case INACTIVITY_DETECTED:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_INACTIVITY_DETECTED_PROPERTY);
            case INVALID_FILE_STRUCTURE:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_INVALID_FILE_STRUCTURE_PROPERTY);
            case SUSPEND_REQUEST_RECEIVED:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_SUSPEND_REQUEST_RECEIVED_PROPERTY);
            case CANCEL_REQUEST_RECEIVED:
                return gsfcLocalMibLookupResponse(ELocalEntityMibPropertyKey.DEFAULT_HANDLER_FOR_CANCEL_REQUEST_RECEIVED_PROPERTY);
            default:
                log.warn("ConditionCode." + conditionCode.name() + " is unsupported - Ignoring");
                return Response.RESPONSE_IGNORE;
        }

    }

    @Override
    public int maxConcurrentTransactions() {
        return gsfcLocalMibLookupInt(ELocalEntityMibPropertyKey.MAXIMUM_CONCURRENT_TRANSACTIONS_PROPERTY);
    }

    @Override
    public int maxFileChunkLength() {
        return gsfcLocalMibLookupInt(ELocalEntityMibPropertyKey.MAXIMUM_FILE_DATA_PER_INCOMING_PDU_BYTES_PROPERTY);
    }

    @Override
    public int maxGapsPerNakPDU() {
        return gsfcLocalMibLookupInt(ELocalEntityMibPropertyKey.MAXIMUM_GAPS_PER_NAK_PDU_PROPERTY);
    }

    @Override
    public int maxFilenameLength() {
        return gsfcLocalMibLookupInt(ELocalEntityMibPropertyKey.MAXIMUM_INCOMING_METADATA_PDU_FILENAME_LENGTH_PROPERTY);
    }

    @Override
    public boolean uplinkOn(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.TIMERS_ENABLED_PROPERTY));
        }

        return gsfcRemoteMibLookupBoolean(nodeID, ERemoteEntityMibPropertyKey.TIMERS_ENABLED_PROPERTY);
    }

    @Override
    public boolean reportUnknownTransaction() {
        return gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.REPORT_UNKNOWN_TRANSACTIONS_PROPERTY);
    }

    @Override
    public boolean class1TimersRunning() {
        return !gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.FREEZE_TIMERS_ON_NEW_CLASS_1_TRANSACTIONS_PROPERTY);
    }

    @Override
    public boolean class2TimersRunning() {
        return !gsfcLocalMibLookupBoolean(ELocalEntityMibPropertyKey.FREEZE_TIMERS_ON_NEW_CLASS_2_TRANSACTIONS_PROPERTY);
    }

    /**
     * @return the localEntityId
     */
    public String getLocalEntityId() {
        return localEntityId;
    }

    /**
     * @param localEntityId the localEntityId to set
     */
    public void setLocalEntityId(final String localEntityId) {
        this.localEntityId = localEntityId;
    }


    // MPCS-9750 - 5/17/2018
    // Add new local and remote entity MIB items introduced in JavaCFDP v1.2.1-crc

    @Override
    public int genTransSeqNumLength() {
        return gsfcLocalMibLookupInt(ELocalEntityMibPropertyKey.TRANSACTION_SEQUENCE_NUMBER_LENGTH_PROPERTY);
    }

    @Override
    public CycleMode getCycleMode() {
        return gsfcLocalMibLookupCycleMode(ELocalEntityMibPropertyKey.CYCLE_MODE_PROPERTY);
    }

    @Override
    public boolean ackRequired(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.REQUIRE_ACKNOWLEDGMENT_PROPERTY));
        }

        return gsfcRemoteMibLookupBoolean(nodeID, ERemoteEntityMibPropertyKey.REQUIRE_ACKNOWLEDGMENT_PROPERTY);
    }

    @Override
    public boolean applyCRC(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.ADD_PDU_CRC_PROPERTY));
        }

        return gsfcRemoteMibLookupBoolean(nodeID, ERemoteEntityMibPropertyKey.ADD_PDU_CRC_PROPERTY);
    }

    @Override
    public boolean checkCRC(final ID nodeID) {

        if (nodeID == null) {
            log.warn("Null remote entity ID was used to look up the MIB");
            return Boolean.parseBoolean(ampcsProperties.getMibFileInitRemoteProperty(ERemoteEntityMibPropertyKey.CHECK_PDU_CRC_PROPERTY));
        }

        return gsfcRemoteMibLookupBoolean(nodeID, ERemoteEntityMibPropertyKey.CHECK_PDU_CRC_PROPERTY);
    }

}