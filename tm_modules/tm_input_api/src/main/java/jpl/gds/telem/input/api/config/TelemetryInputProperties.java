/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.telem.input.api.config;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.shared.annotation.Singleton;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.input.api.InternalTmInputMessageType;


/**
 * TelemetryInputConfig manages configuration properties specific to classes in
 * the tm_input projects. The property file is loaded using the standard
 * configuration search.
 * 
 *
 * MPCS-7677 - 9/16/15. Removed 4 session-config-related methods
 *          from this class that had nothing to do with this class and
 *          introduced unnecessary coupling.
 *
 * MPCS-7449 - 9/29/15. Changed class to extend from
 *          GdsHierarchicalProperties. New Properties for BufferedRawInputStream
 *          and RawInputStreamConnection added to the new rawinput.properties
 *          file.
 * 
 * MPCS-7766 - 12/29/15. Added buffer mode items.
 * MPCS-8608 - 07/05/17. Changed name to
 *          TelemetryInputProperties, renamed property file to
 *          telem_input.properties, changed PROPERTY_PREFIX to telemInput.
 */
@Singleton
public final class TelemetryInputProperties extends GdsHierarchicalProperties
{

	/** Default property file name */
	public static final String PROPERTY_FILE = "telem_input.properties";

	private static final String PROPERTY_PREFIX = "telemInput.";

	private static final String BUFFER_BLOCK = PROPERTY_PREFIX + "buffer.";

	private static final String MESSAGE_PROPERTY_PREFIX = PROPERTY_PREFIX + "internal.message.";
	private static final String SOCKET_RETRIES_PROPERTY = PROPERTY_PREFIX + "socket.reconnectTries";
	private static final String RAW_SUMMARY_INTERVAL_PROPERTY = PROPERTY_PREFIX + "summaryInterval";
	private static final String SOCKET_CONNECT_INTERVAL_PROPERTY = PROPERTY_PREFIX + "socket.reconnectInterval";
	private static final String READ_BUFFER_SIZE_PROPERTY = PROPERTY_PREFIX + "readBufferSize";
	private static final String INPUT_METER_PROPERTY = PROPERTY_PREFIX + "inputMeterInterval";
	private static final String DISCARDED_BYTES_THRESHOLD_PROPERTY = PROPERTY_PREFIX + "discardedBytesReportThreshold";
	private static final String NEN_STATUS_CLASS_PROPERTY = PROPERTY_PREFIX + "nenStatus.dataClass";
	// MPCS-7610 - 11/10/16 Added property names for TDS heartbeat configuration
	private static final String SFDU_HEARTBEAT_PERIOD_MS = PROPERTY_PREFIX + "tdsHeartbeat.period";
	private static final String SFDU_HEARTBEAT_RECONNECT_ENABLED = PROPERTY_PREFIX + "tdsHeartbeat.reconnect";
	private static final String DATAFLOW_PREFIX					= "dataTimeout.";
	// MPCS-7766 12/21/15 - Added for DiskBackedBufferedInputStream command line option validation
    private static final String BUFFERED_INPUT_MODE_BLOCK = BUFFER_BLOCK + "mode.";
    private static final String ALLOWED_BUFFERED_INPUT_MODE_PROPERTY = BUFFERED_INPUT_MODE_BLOCK + "allowed";
    private static final String DEFAULT_BUFFERED_INPUT_MODE_PROPERTY = BUFFERED_INPUT_MODE_BLOCK + "default";

	private final int BUFFER_ITEM_SIZE	 		=	128000;
	private final int BUFFER_ITEM_COUNT	 		=	128000;
	private final int BUFFER_WINDOW_SIZE		=	10;
	private final int BUFFER_MAINTENANCE_DELAY	=	200;
	private final int BUFFER_FILE_SIZE			=	10000000;
	private final int BUFFER_FILE_COUNT 		=	65536;
	private final int BUFFER_YELLOW 			=	80;
	private final int BUFFER_RED 				=	100;
    
    /*
     * MPCS-7930 2/1/2015
     * Relocated properties from GdsConfiguration to rawInput.properties
     */
    private final int DEFAULT_SOCKET_RETRY_COUNT 				= -1;
    private final int DEFAULT_READ_BUFFER_SIZE				    = 800;
    private final int DEFAULT_INPUT_METER_INTERVAL				= 0;
    private final long DEFAULT_SOCKET_RETRY_INTERVAL 			= 1000;
    private final long DEFAULT_DISCARDED_BYTES_THRESHOLD		= 1000000;
    private final long DEFAULT_SUMMARY_INTERVAL					= 60000;
    private final int DEFAULT_NEN_STATUS_CLASS                  = 2;    
    private final int DEFAULT_EMULATOR_CHAIN                    = -1;

    // MPCS-7610  - 11/10/16 - Added defaults for TDS heartbeat configuration
    private final int DEFAULT_SFDU_HEARTBEAT_PERIOD_MS             = 300000;
    private final boolean DEFAULT_SFDU_HEARTBEAT_RECONNECT_ENABLED = false;
    
	// MPCS-7766 12/21/15 - Added for DiskBackedBufferedInputStream command line option validation
    private static final BufferedInputModeType DEFAULT_BUFFER_MODE = BufferedInputModeType.NONE;
    private static final String LIST_DELIM = ",";
    
    private static BufferedInputModeType bufferedInputMode = DEFAULT_BUFFER_MODE;
    
    /**
     * Test constructor
     */
    public TelemetryInputProperties() {
        this(new SseContextFlag());
    }

    /**
     * Creates an instance of TelemetryInputConfig by loading the default property file.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public TelemetryInputProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }

    /**
     * Get socket retries count.
     *
     * @return the configured number of socket connection retries.
     */
    public int getSocketRetries() {
        return getIntProperty(SOCKET_RETRIES_PROPERTY, DEFAULT_SOCKET_RETRY_COUNT);
    }
    
    /**
     * Set socket retries count.
     *
     * @param retries the number of socket connection retries to set
     */
    public void setSocketRetries(final int retries) {
        asProperties().setProperty(SOCKET_RETRIES_PROPERTY, String.valueOf(retries));
    }

    /**
     * Get socket retry interval.
     *
     * @return the configured time interval between socket connection retries,
     *         in milliseconds.
     */
    public long getSocketRetryInterval() {
        return getLongProperty(SOCKET_CONNECT_INTERVAL_PROPERTY, DEFAULT_SOCKET_RETRY_INTERVAL);
    }

    /**
     * Get read buffer size. Used by telemetry input stream readers when reading
     * non-synchronized data, either via socket or from file.
     *
     * @return the configured buffer size for file reads.
     */
    public int getReadBufferSize() {
        return getIntProperty(READ_BUFFER_SIZE_PROPERTY, DEFAULT_READ_BUFFER_SIZE);
    }

    /**
     * Get meter interval.
     *
     * @return the configured time interval between reads, in milliseconds.
     */
    public long getMeterInterval() {
        return getIntProperty(INPUT_METER_PROPERTY, DEFAULT_INPUT_METER_INTERVAL);
    }
    
    /**
     * MPCS-7903 - 2/4/16 Removed setMeterInterval()
     * Functionality for meterInterval is preserved and maintained in AbstractRawStreamProcessor
     */

    /**
     * Get discarded bytes threshold.
     *
     * @return the configured threshold number of bytes between reporting
     *         "bytes discarded" messages.
     */
    public long getDiscardedBytesThreshold() {
        return getLongProperty(DISCARDED_BYTES_THRESHOLD_PROPERTY, DEFAULT_DISCARDED_BYTES_THRESHOLD);
    }
    
    /**
     * Get data-flow timeout.
     *
     * @param type DownlinkConnectionType
     * must be passed using SessionUtility or SessionConfiguration
     * 
     * @return Timeout
     */
    public long getDataFlowTimeout(final TelemetryConnectionType type) {
        return getLongProperty(PROPERTY_PREFIX + DATAFLOW_PREFIX + type.name(), 0);
    }


    /**
     * Get summary timer interval.
     *
     * @return Interval
     */
    public long getRawSummaryTimerInterval() {
        return getLongProperty(RAW_SUMMARY_INTERVAL_PROPERTY, DEFAULT_SUMMARY_INTERVAL);
    }

    /**
     * Get subscription name.
     *
     * @param type Stream type
     *
     * @return Name
     */
    public IMessageType getRawDataMessageSubscriptionType(final StreamType type) {
        final String temp = getProperty(MESSAGE_PROPERTY_PREFIX
                + "stream.subscriptionName." + type.name());
        return InternalTmInputMessageType.valueOf(temp);
    }
    
    // MPCS-7449 10/05/15 Added all items utilizing properties in the BUFFER_BLOCK
	/**
	 * Get the buffer item size (in bytes) from the BUFFER_BLOCK section of the
	 * rawinput.properties file.
	 * 
	 * @return Integer value used to set the number of bytes in each
	 *         DiskMappedByteBufferItem
	 */
    public int getBufferItemSize(){
    	return getIntProperty(BUFFER_BLOCK + "bufferItemSize", BUFFER_ITEM_SIZE);
    }
    
	/**
	 * Get the number of buffer items from the BUFFER_BLOCK section of the
	 * rawinput.properties file.
	 * 
	 * @return Integer value used to set the number of DiskMappedByteBufferItems
	 *         in the BufferedRawInputStream
	 */
    public int getBufferItemCount(){
    	return getIntProperty(BUFFER_BLOCK + "bufferItemCount", BUFFER_ITEM_COUNT);
    }
    
	/**
	 * Get the consumer window size from the BUFFER_BLOCK section of the
	 * rawinput.properties file.
	 * 
	 * @return Integer value used to set the number of items including the
	 *         current read item to keep pulled from disk / loaded in memory for
	 *         the DiskMappedByteBufferItems in the BufferedRawInputStream
	 */
    public int getBufferWindowSize(){
    	return getIntProperty(BUFFER_BLOCK + "windowSize", BUFFER_WINDOW_SIZE);
    }
    
	/**
	 * Get the maintenance interval from the BUFFER_BLOCK section of the
	 * reawinput.properties file.
	 * 
	 * @return Integer value use to set the delay between each maintenance
	 *         execution in DiskMappedByteBuffer in the BufferedRawInputStream
	 */
    public int getBufferMaintenanceDelay(){
    	return getIntProperty(BUFFER_BLOCK + "maintenanceInterval", BUFFER_MAINTENANCE_DELAY);
    }
    
	/**
	 * Get the buffer file size (in bytes) from the BUFFER_BLOCK section of the
	 * rawinput.properties file.
	 * 
	 * @return Integer value used to set the maximum number of bytes in each
	 *         file on disk for the DiskMappedByteBuffer used by
	 *         BufferedRawInputStream
	 */
    public long getBufferFileSize(){
    	return getIntProperty(BUFFER_BLOCK + "fileSize", BUFFER_FILE_SIZE);
    }
    
	/**
	 * Get the number of buffer files from the BUFFER_BLOCK section of the
	 * rawinput.properties file.
	 * 
	 * @return Integer value used to set the number of files backing the
	 *         DiskMappedByteBuffer used by BufferedRawInputStream
	 */
    public int getBufferFileLimit(){
    	return getIntProperty(BUFFER_BLOCK + "fileLimit", BUFFER_FILE_COUNT);
    }
    
	/**
	 * Get whether or not files for BufferedRawInputStream are deleted after
	 * they are no longer in use from the BUFFER_BLOCK section of the
	 * rawinput.properties file
	 * 
	 * @return TRUE if files are to be deleted after they are no longer in use
	 *         FALSE if all files are to be kept
	 */
    public boolean getDeleteBufferFiles(){
    	return getBooleanProperty(BUFFER_BLOCK + "deleteFiles", true);
    }
    
	/**
	 * Get whether or not all data that passes through the
	 * BufferedRawInputStream is to be written to a file.
	 * 
	 * @return TRUE if all data is to be written to the disk mapped files FALSE
	 *         if data is only written to disk mapped files as necessary
	 */
    public boolean getBackupAll(){
    	return getBooleanProperty(BUFFER_BLOCK + "backupAll", false);
    }
    
	/**
	 * Get whether or not files are to be used to back the
	 * BufferedRawInputStream's DiskMappedByteBuffer
	 * 
	 * @return TRUE if the BufferedRawInputStream can push data to disk files
	 *         FALSE if all data is to be kept in memory
	 */
    public boolean getUseFiles(){
    	return getBooleanProperty(BUFFER_BLOCK + "useFiles", true);
    }
    
	/**
	 * Get the default location for data to be stored. for the
	 * BufferedRawInputStream
	 * 
	 * @return String representing the default path for BufferedRawInputStream
	 *         to utilize as a file storage location
	 */
    public String getBufferDir(){
    	return getProperty(BUFFER_BLOCK + "bufferDir");
    }
    
	/**
	 * Gets the percentage level at which the BufferedRawInputStream should be
	 * considered in YELLOW health state. A value of 0 should disable YELLOW
	 * health checking.
	 * 
	 * @return percentage (0 - 100)
	 */
    public long getBufferYellowLevel() {
        long val = getLongProperty(BUFFER_BLOCK + "bufferYellow", BUFFER_YELLOW);
        if (val < 0 || val > 100) {
            log.warn("Value for " + BUFFER_BLOCK + "bufferYellow" + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }

	/**
	 * Gets the percentage level at which the BufferedRawInputStream should be
	 * considered in RED health state. A value of 0 should disable RED health
	 * checking.
	 * 
	 * @return percentage (0 - 100)
	 */
    public long getBufferRedLevel() {
        long val = getLongProperty(BUFFER_BLOCK + "bufferRed", BUFFER_RED);
        if (val < 0 || val > 100) {
            log.warn("Value for " + BUFFER_BLOCK + "bufferRed" + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }
    
	/**
	 * Gets the value at which BufferedRawInputStream file usage should be
	 * considered in YELLOW health state. A value of 0 should disable YELLOW
	 * health checking.
	 * 
	 * @return percentage (0 - 100), or file count (0+)
	 */
    public long getBufferFileYellowLevel() {
        long val = getLongProperty(BUFFER_BLOCK + "fileYellow", BUFFER_YELLOW);
        if (val < 0) {
            log.warn("Value for " + BUFFER_BLOCK + "fileYellow" + " in the " + PROPERTY_FILE + " file is a negative value; setting to 0");
            val = 0;
        }
        if (val > 100){
        	log.warn("Value for " + BUFFER_BLOCK + "fileYellow" + " in the " + PROPERTY_FILE + " file is not a valid percentage; use as a file count");
        }
        return val;
    }

	/**
	 * Gets the value at which BufferedRawInputStream file usage should be
	 * considered in RED health state. A value of 0 should disable RED health
	 * checking.
	 * 
	 * @return percentage (0 - 100), or file count (0+)
	 */
    public long getBufferFileRedLevel() {
        long val = getLongProperty(BUFFER_BLOCK + "fileRed", BUFFER_RED);
        if (val < 0) {
            log.warn("Value for " + BUFFER_BLOCK + "fileRed" + " in the " + PROPERTY_FILE + " file is a negative value; setting to 0");
            val = 0;
        }
        if (val > 100){
        	log.warn("Value for " + BUFFER_BLOCK + "fileRed" + " in the " + PROPERTY_FILE + " file is not a valid percentage; use as a file count");
        }
        return val;
    }
    
    // MPCS-7766 12/21/15 added for bufferedInputMode support
	/**
	 * Gets the default buffered input mode from the session.properties file. If
	 * the value does not exist in the file, or is invalid, no buffer will be
	 * used.
	 * 
	 * @return BufferedInputModeType corresponding to one of the enumerated
	 *         values (NONE, FSW, SSE, or BOTH)
	 */
    public BufferedInputModeType getDefaultBufferedInputMode(){
    	final String temp = getProperty(DEFAULT_BUFFERED_INPUT_MODE_PROPERTY, DEFAULT_BUFFER_MODE.toString());
    	try {
            return BufferedInputModeType.valueOf(temp.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            reportError(DEFAULT_BUFFERED_INPUT_MODE_PROPERTY, temp,
                    DEFAULT_BUFFER_MODE.toString());
            return DEFAULT_BUFFER_MODE;
        }
    }
    
    // MPCS-7766 12/21/15 added for bufferedInputMode support
	/**
	 * Gets the list of allowed values for the buffered input mode property from
	 * the session.properties file. If no values exist, only the default mode
	 * will be allowed. If any input values are invalid they will be discarded
	 * and only valid values will be used. The buffered input mode corresponds
	 * to which downlink types can use the DiskBackedBufferedInputStream when a
	 * supported input connection is used.
	 * 
	 * @return Set of the allowed buffered input modes
	 */
    public Set<BufferedInputModeType> getAllowedBufferedInputModes(){
    	final List<String> temp = getListProperty(ALLOWED_BUFFERED_INPUT_MODE_PROPERTY, null, LIST_DELIM);
    	final SortedSet<BufferedInputModeType> result = new TreeSet<BufferedInputModeType>();
    	for(final String mode : temp){
    		try{
    			result.add(BufferedInputModeType.valueOf(mode.trim().toUpperCase()));
    		} catch (final IllegalArgumentException e){
    			reportError(ALLOWED_BUFFERED_INPUT_MODE_PROPERTY, mode, null);
    			log.error("Value will be omitted from the configured list");
    		}
    	}
    	//always allow no buffer
    	if(!result.contains(DEFAULT_BUFFER_MODE)){
    		result.add(DEFAULT_BUFFER_MODE);
    	}
    	return result;
    	
    }
    
    // MPCS-7766 12/22/15 - added get and set for bufferedInputMode
	/**
	 * Gets the bufferedInputMode variable, which indicates which downlink
	 * mode(s) will use the DiskBackedBufferedInputStream when a supported input
	 * connection is used.
	 * 
	 * @return BufferedInputModeType corresponding to one of the enumerated
	 *         values (NONE, FSW, SSE, or BOTH)
	 */
    public BufferedInputModeType getBufferedInputMode(){
    	return bufferedInputMode;
    }
    
	/**
	 * Sets the bufferedInputMode variable, which indicates which downlink
	 * mode(s) will use the DiskBackedBufferedInputStream when a supported input
	 * connection is used.
	 * 
	 * @param newVal
	 *            BufferedInputModeType corresponding to one of the enumerated
	 *            values (NONE, FSW, SSE, or BOTH)
	 */
    public void setBufferedInputMode(final BufferedInputModeType newVal){
    	if(getAllowedBufferedInputModes().contains(newVal)){
    		bufferedInputMode = newVal;
    	}
    	else{
    		log.warn(newVal + " is not an accepted bufferedInput mode. Current mode of " + bufferedInputMode + " will continue to be used. Allowed modes are " + getAllowedBufferedInputModes());
    	}
    }
    
	/**
     * States if the DiskBackedBufferedInputStream can be utilized in the
     * current downlink application.
     * 
     * @param type
     *            DownlinkConnectionType
     *            Must be passed in from SessionConfiguration or SessionUtility
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return TRUE if DiskBackedBufferedInputStream can be utilized. FALSE if
     *         DiskBackedBufferedInputStream cannot be utilized, RawInputStream
     *         should be used instead.
     * 
     */
    public boolean isBufferedInputAllowed(final TelemetryConnectionType type, final SseContextFlag sseFlag) {
    	// MPCS-7832 01/11/15 - Added check of downlink connection type.
    	boolean retVal = false;
    	
    	switch(type){
    		case CLIENT_SOCKET:
    		case SERVER_SOCKET:
    		case TDS:
                retVal = bufferedInputMode.isAllowed(sseFlag.isApplicationSse());
    			break;
    		default:
    			retVal = false;
    	}

    	return retVal;
    }
    
	/**
	 * Get the SFDU/TDS heartbeat interval in milliseconds
	 * 
	 * @return milliseconds between TDS heartbeats
	 */
    public int getSfduHeartbeatPeriod(){
    	return getIntProperty(SFDU_HEARTBEAT_PERIOD_MS, DEFAULT_SFDU_HEARTBEAT_PERIOD_MS);	// 5 minutes
    }
    
	/**
	 * Get if the lack of a SFDU/TDS heartbeat message after the interval will
	 * disconnect and allow the handler to reconnect
	 * 
	 * @return TRUE if lack of a heartbeat will encourage a reconnect, false if
	 *         not
	 */
    public boolean isSfduHeartbeatReconnectEnabled(){
    	return getBooleanProperty(SFDU_HEARTBEAT_RECONNECT_ENABLED, DEFAULT_SFDU_HEARTBEAT_RECONNECT_ENABLED);
    }
    
	/**
	 * Gets the NEN status data class for NEN status packets. This value is in the LEOT frame header 
	 * and is used to distinguish flight frames from NEN status packets.
	 * 
	 * @return NEN data class value
	 */
	public int getNenStatusDataClass() {
        return getIntProperty(NEN_STATUS_CLASS_PROPERTY, DEFAULT_NEN_STATUS_CLASS);
    }

	
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
