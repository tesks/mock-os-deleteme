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
package jpl.gds.globallad;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.IGlobalLadDepthNotifiable;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.HostPortUtility;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * This class encapsulates the Global Lad configurations. 
 */
public class GlobalLadProperties extends GdsHierarchicalProperties {

	public enum DisruptorWaitStrategy {
		BLOCK, SLEEP, YIELD, SPIN
	}
	
	
	public static final int DEFAULT_DATA_DEPTH = 10;
	/**
	 * Name of the default properties file.
	 */
	public static final String PROPERTY_FILE = "globallad.properties";
	
	private static final String PROPERTY_PREFIX = "globallad.";
	
	private static final String CONTAINERS_BLOCK = PROPERTY_PREFIX + "containers.";

	public static final String GLAD_SKIP_ERT = PROPERTY_PREFIX + "containers.timeTypes.skip.ert";
	public static final String GLAD_SKIP_SCET = PROPERTY_PREFIX + "containers.timeTypes.skip.scet";
	public static final String GLAD_SKIP_EVENT = PROPERTY_PREFIX + "containers.timeTypes.skip.event";
	
	public static final String GLAD_DEBUG_PROPERTY = PROPERTY_PREFIX + "debug";
	public static final String GLAD_ENABLED_PROPERTY = PROPERTY_PREFIX + "enabled";
	public static final String DATA_FACTORY_PROPERTY = PROPERTY_PREFIX + "internal.dataFactoryClass";
	public static final String RING_BUFFER_PROPERTY = PROPERTY_PREFIX + "internal.containers.ringBuffer";
	public static final String CHILD_MAPPING_PROPERTY_BASE = PROPERTY_PREFIX + "containers.childContainers";
	
	public static final String DATA_DEPTH_PROPERTY = PROPERTY_PREFIX + "containers.depth.default";
	
	public static final String MAX_INSERTERS_PROPERTY = PROPERTY_PREFIX + "disruptor.globallad.inserters";
	
	public static final String SOCKET_SERVER_PORT_PROPERTY = PROPERTY_PREFIX + "socketServer.port";
	public static final String SOCKET_SERVER_HOST_PROPERTY = PROPERTY_PREFIX + "server.host";
	public static final String SOCKET_SERVER_CONNECT_RETRY_COUNT = PROPERTY_PREFIX + "server.retrycount";
	public static final String SOCKET_SERVER_CONNECT_RETRY_DELAY_MILLIS = PROPERTY_PREFIX + "server.retrydelay";
	public static final String REST_SERVER_PORT_PROPERTY = PROPERTY_PREFIX + "rest.port";
	
	public static final String DOWNLINK_RING_BUFFER_SIZE_PROPERTY = PROPERTY_PREFIX + "disruptor.downlink.ringBufferSize";
	public static final String CLIENT_RING_BUFFER_SIZE_PROPERTY = PROPERTY_PREFIX + "disruptor.client.ringBufferSize";
	public static final String INSERTER_RING_BUFFER_SIZE_PROPERTY = PROPERTY_PREFIX + "disruptor.globallad.ringBufferSize";

	public static final String JMS_SERVER_ROOT_TOPICS = PROPERTY_PREFIX + "jmsServer.rootTopics";
	public static final String JMS_HOST_NAME = PROPERTY_PREFIX + "jmsServer.hostName";
	public static final String JMS_DEFAULT_TOPICS = "DEFAULT";
	public static final String DATA_SOURCE = PROPERTY_PREFIX + "dataSource";
	public static final String DATA_SOURCE_DEFAULT = IGlobalLadDataSource.DataSourceType.SOCKET.name();

	/**
	 * The performance thresholds for the deal.  This should be from 1 to 100.
	 */
	private static final String DOWNLINK_PERFORMANCE_YELLOW_PROPERTY = PROPERTY_PREFIX + "disruptor.downlink.threshold.yellow";
	private static final String DOWNLINK_PERFORMANCE_RED_PROPERTY = PROPERTY_PREFIX + "disruptor.downlink.threshold.red";
	
	/**
	 * The wait strategy should be one of the following: [block, sleep, yield, spin].  Check the LMAX disruptor
	 * on the wiki for more details.
	 */
	public static final String DOWNLINK_WAIT_STRATEGY_PROPERTY = PROPERTY_PREFIX + "disruptor.wait.downlink";  // Multiple producer, single consumer.
	public static final String CLIENT_WAIT_STRATEGY_PROPERTY = PROPERTY_PREFIX + "disruptor.wait.client"; // Single producer, single consumer.
	public static final String INSERTER_WAIT_STRATEGY_PROPERTY = PROPERTY_PREFIX + "disruptor.wait.globallad"; // Multiple producer, multiple consumer.
	
	/**
	 * Reaping configurations for time to live.  Gets the values after reaping and builds a lookup.  
	 */
	public static final String REAPING_PROPERTY_BASE = PROPERTY_PREFIX + "containers.reaping";
	
	/**
	 * Added properties to disable reaping and a new one
	 * to define the reaping levels.  Added getters for each of these.
	 */

	/**
	 * Defines the container type that the actual reaping will take place, ie where containers will be deleted.
	 */
	public static final String REAPING_LEVEL_PROPERTY = PROPERTY_PREFIX + "containers.reaping.level";
 
	public static final String REAPING_ENABLED_PROPERTY = PROPERTY_PREFIX + "containers.reaping.enabled";
	public static final String DATA_DEPTH_PROPERTY_BASE = PROPERTY_PREFIX + "containers.depth";

	public static final String REAPING_INTERVAL_PROPERTY = "globallad.containers.reaping.interval";
	public static final String REAPING_MEMORY_PROPERTY = "globallad.containers.reaping.memory.threshold";
	
	/**
	 * Used to tell if a time type is enabled for rest queries.
	 */
	public static final String REST_TIME_TYPE_PROPERTY_BASE = PROPERTY_PREFIX + "rest.timetypes.enable";
	
    public static final String BASE_URI_PROPERTY = PROPERTY_PREFIX + "rest.uri";
    public static final String BASE_URI_DEFAULT = "http://%s:%d/globallad/";
    private static final String                                                  BASE_URI_SSL                                          = "https://%s:%d/globallad/";

	private static final String EHA_COLUMN_PROPERTY = PROPERTY_PREFIX + "output.csv.columns.eha";
	private static final String EVR_COLUMN_PROPERTY = PROPERTY_PREFIX + "output.csv.columns.evr";
	private static final String LM_EHA_COLUMN_PROPERTY = "globallad.output.csv.columns.lm_eha";

	private static final String PERSISTER_ENABLED_PROPERTY = PROPERTY_PREFIX + "persistence.backup.doBackup";
	private static final String PERSISTER_DIRECTORY_PROPERTY = PROPERTY_PREFIX + "persistence.backup.directory";
	private static final String PERSISTER_MAX_BACKUPS_PROPERTY = PROPERTY_PREFIX + "persistence.backup.maxBackups";
	private static final String PERSISTER_MAX_SIZE_PROPERTY = PROPERTY_PREFIX + "persistence.backup.maxSize";
	private static final String PERSISTER_INTERVAL_PROPERTY = PROPERTY_PREFIX + "persistence.backup.interval";
	private static final String PERSISTER_BASENAME_PROPERTY = PROPERTY_PREFIX + "persistence.backup.basename";
	
	private static final String DOWNLINK_SINK_PUBLISH_RETRY_PROPERTY = PROPERTY_PREFIX + "downlink.sink.publishRetry";
	private static final String DOWNLINK_SINK_PUBLISH_RETRY_INTERVAL_PROPERTY = "globallad.downlink.sink.publishRetryInterval";
	private static final String DOWNLINK_SINK_PUBLISH_RETRY_LOGGING_INTERVAL_PROPERTY = "globallad.downlink.sink.publishRetryLoggingInterval";

	/**
	 * Test values that will put the lad into a state that will test the throughput of the data creation / insert
	 * up to a certain point.  
	 */
	private static final String TEST_DATA_CONSTRUCTION_ONLY = PROPERTY_PREFIX + "test.client.data.construction.only";
	private static final String TEST_RING_BUFFER_INSERT_ONLY = PROPERTY_PREFIX + "test.client.ringbuffer.insert.only";
	
	private static GlobalLadProperties globalInstance;
	
	private final Map<Byte,Integer> dataDepthMapping;
	private final Map<String, String> childMapping;
	private final Map<String, Map<String, Long>> reapTimeMapping;
	
	private final Set<Byte> scetSkip;
	private final Set<Byte> ertSkip;
	private final Set<Byte> eventSkip;
	
	private final Set<GlobalLadPrimaryTime> enabledTimeTypes;
	
	/**
	 * List of listeners to update when the depth changes.  Use a collection of weak references 
	 * so when objects are removed these references do not stop gc from removing them..
	 */
	private final Collection<ComparableWeakReference<IGlobalLadDepthNotifiable>> notifiables; 
	
	private final String dataFactoryClassName;
	private final String ringBufferClassName;
	private final boolean isEnabled;
	private final boolean isDebug;
	
	private String globalLadHost;
	private int globalLadSocketServerPort;
	private int globalLadRestServerPort;
	private final int globalLadSocketRetryDelayMillis;
	private final int globalLadSocketRetryCount;
	private final int globalLadSinkPublishRetry;
	// Units in milliseconds
	private final long globalLadSinkPublishRetryInterval;
	// Units in milliseconds
	private final long globalLadSinkPublishRetryLoggingInterval;
	
	private int defaultDepth;
	
	private boolean persistenceEnabled;
	private File persistenceDirectory;
	private double persistenceMaxSize;
	private final int persistenceMaxNumberBackups;
	private final int persistenceInterval;

	private final String persistenceBackupBaseName;
	
	// Adding the reaping level.
	private String reapingLevel;

	// Adding interval and memory threshold.
	private final int reapingInterval;
    private int                                                                  reapingMemoryThreshold;
	
	private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private boolean isHttpsEnabled;
    private String uriBase = null;

	private IGlobalLadDataSource.DataSourceType dataSource;
	private List<String>                        jmsRootTopics;
	private VenueType                           venueType;
	private DownlinkStreamType                  downlinkStreamType;
	private String                              testbedName;
	private String                              jmsHostName;

	/**
	 * Creating the logger like this and not going through the trace manager because we want
	 * to use the same format as spring.  Also, if we use the other tracer it will double print things because  
	 * there is a log interceptor.  Don't have time to track all this down so going with the path of least resistence.
	 * 
	 * This ended up causing issues so going to use this. At this point log messages inside of the spring
	 * app will get double logged, but that is going to have to be fixed later.
	 */
    private static final Tracer                                                  gladTracer                               = TraceManager
            .getTracer(Loggers.GLAD);

	public static Tracer getTracer() {
		return gladTracer;
	}
	
	
    /**
     * @return GlobalLadConfiguration instance
     */
	public static synchronized GlobalLadProperties getGlobalInstance() {
		if (globalInstance == null) {
            globalInstance = new GlobalLadProperties();
		}
		
		return globalInstance;
	}
	
	/**
	 * Default constructor.
	 */
	public GlobalLadProperties() {
        super(PROPERTY_FILE, true);
		
		defaultDepth = getIntProperty(DATA_DEPTH_PROPERTY, DEFAULT_DATA_DEPTH);
		
		dataFactoryClassName = getProperty(DATA_FACTORY_PROPERTY);
		ringBufferClassName = getProperty(RING_BUFFER_PROPERTY);
		
		globalLadHost = getProperty(SOCKET_SERVER_HOST_PROPERTY, "localhost");
		globalLadSocketServerPort = getIntProperty(SOCKET_SERVER_PORT_PROPERTY, 8900);
		globalLadRestServerPort = getIntProperty(REST_SERVER_PORT_PROPERTY, 8887);
		globalLadSocketRetryCount = getIntProperty(SOCKET_SERVER_CONNECT_RETRY_COUNT, 10);
		globalLadSocketRetryDelayMillis = getIntProperty(SOCKET_SERVER_CONNECT_RETRY_DELAY_MILLIS, 2000);

		globalLadSinkPublishRetry = getIntProperty(DOWNLINK_SINK_PUBLISH_RETRY_PROPERTY, 600);
        globalLadSinkPublishRetryInterval = getLongProperty(DOWNLINK_SINK_PUBLISH_RETRY_INTERVAL_PROPERTY, 500);
		globalLadSinkPublishRetryLoggingInterval = getLongProperty(DOWNLINK_SINK_PUBLISH_RETRY_LOGGING_INTERVAL_PROPERTY, 1000L);
		
		dataDepthMapping = new HashMap<Byte, Integer>();
		childMapping = new HashMap<String, String>();
		reapTimeMapping = new HashMap<String, Map<String, Long>>();

		notifiables = new ConcurrentSkipListSet<ComparableWeakReference<IGlobalLadDepthNotifiable>>();
		enabledTimeTypes = new HashSet<GlobalLadPrimaryTime>();
		
		scetSkip = new HashSet<Byte>();
		ertSkip = new HashSet<Byte>();
		eventSkip = new HashSet<Byte>();
	
		final String skipValues = StringUtils.join(new String[]{"description", "validValues", "formatHint", "behavioralNotes"}, "|");
		final Pattern pattern = Pattern.compile(String.format("^.+(%s)$", skipValues));
		for (final String propertyName : properties.stringPropertyNames()) {
			// Skill all property file des
			if (pattern.matcher(propertyName).matches()) {
				continue;
			}
			
			final String[] propertyChunks = StringUtils.split(propertyName, ".");
			final String lastChunk = propertyChunks[propertyChunks.length-1];
			final String value = StringUtils.strip(getProperty(propertyName));

			if (propertyName.startsWith(CHILD_MAPPING_PROPERTY_BASE)) {
				/**
				 * Child mappings.
				 * Must assume the last part of the property is the parent.
				 */
				childMapping.put(lastChunk, value);
			} else if (propertyName.startsWith(DATA_DEPTH_PROPERTY_BASE)) {
				/**
				 * Time to live mappings.
				 */
				final String dataTypeByte = lastChunk;
				final String dataValue = value;
				
				if (GDR.isIntString(dataTypeByte) && GDR.isIntString(dataValue)) {
					
					final byte dt = Byte.valueOf(dataTypeByte);
					
					dataDepthMapping.put(dt, Integer.valueOf(dataValue));
				}
			} else if (propertyName.equals(REAPING_LEVEL_PROPERTY)) {
				// If this is not set, set to something that will not match and not cause any NPE's.
				reapingLevel = value == null ? "NO_REAPING_LEVEL" : value;
			} else if (propertyName.startsWith(REAPING_PROPERTY_BASE)) {
				final String containerType = propertyChunks[propertyChunks.length-2];
				final String containerIdentifier = lastChunk;
				
				if (GDR.isIntString(value)) {
					final long reapingTimeMinutes = Long.valueOf(value);
					setReapingTimeMinutes(containerType, containerIdentifier, reapingTimeMinutes);
				}
			} else if (propertyName.startsWith(GLAD_SKIP_SCET)) {
				addBytesToCollectionFromCSV(value, scetSkip);
			} else if (propertyName.startsWith(GLAD_SKIP_ERT)) {
				addBytesToCollectionFromCSV(value, ertSkip);
			} else if (propertyName.startsWith(GLAD_SKIP_EVENT)) {
				addBytesToCollectionFromCSV(value, eventSkip);
			} else if (propertyName.startsWith(REST_TIME_TYPE_PROPERTY_BASE)) {
				try {
					if (getBooleanProperty(propertyName, false)) {
						final String tt = propertyChunks[propertyChunks.length-1].toUpperCase();
						enabledTimeTypes.add(GlobalLadPrimaryTime.valueOf(tt));
					}
				} catch (final Exception e) {
					getTracer().error("Unsupported time type from properties file: " + propertyChunks[propertyChunks.length-1]);
				}
			}
		}
		
		this.isEnabled = getBooleanProperty(GLAD_ENABLED_PROPERTY, false);
		this.isDebug = getBooleanProperty(GLAD_DEBUG_PROPERTY, false);
		this.reapingInterval = getIntProperty(REAPING_INTERVAL_PROPERTY, 10);
        this.reapingMemoryThreshold = getIntProperty(REAPING_MEMORY_PROPERTY, 95);

        /**
         * Setting a range on the memory level. Must be between 80 and 99.
         */
        this.reapingMemoryThreshold = reapingMemoryThreshold >= 80 && reapingMemoryThreshold < 100
                ? reapingMemoryThreshold : 95;
		
		/**
		 * Set up the persistence stuff.
		 */
		this.persistenceEnabled = getBooleanProperty(PERSISTER_ENABLED_PROPERTY, false);
		
		/**
		 * If not set there is no limit.
		 */
		try {
			this.persistenceMaxSize = Double.valueOf(getProperty(PERSISTER_MAX_SIZE_PROPERTY, "0"));
		} catch (final NumberFormatException e) {
			this.persistenceMaxSize = 0;
		}
		this.persistenceMaxNumberBackups = getIntProperty(PERSISTER_MAX_BACKUPS_PROPERTY, -1);
		
		/**
		 * Defaults to every five minutes.
		 */
		this.persistenceInterval = getIntProperty(PERSISTER_INTERVAL_PROPERTY, 30);
		
		this.persistenceBackupBaseName = getProperty(PERSISTER_BASENAME_PROPERTY, "backup_file");
		
		final String backupDir = getProperty(PERSISTER_DIRECTORY_PROPERTY);
		
		if (backupDir != null) {
			this.persistenceDirectory = new File(backupDir);
		}

        this.uriBase = getProperty(BASE_URI_PROPERTY, BASE_URI_DEFAULT);
        this.isHttpsEnabled = uriBase.toLowerCase().startsWith(HTTPS);

        String dataSourceFromConfig = getProperty(DATA_SOURCE, DATA_SOURCE_DEFAULT);

		IGlobalLadDataSource.DataSourceType type = IGlobalLadDataSource.DataSourceType.fromString(dataSourceFromConfig);
        if (type == null) {
        	this.dataSource = IGlobalLadDataSource.DataSourceType.SOCKET;
		} else {
        	this.dataSource = type;
		}

		this.jmsRootTopics = getListProperty(JMS_SERVER_ROOT_TOPICS, null, ",");
		this.jmsHostName = getProperty(JMS_HOST_NAME, HostPortUtility.getLocalHostName());
	}
	
	/**
	 * @param csv
	 * @param collection
	 */
	private void addBytesToCollectionFromCSV(final String csv, final Collection<Byte> collection) {
		for (final String chunk : StringUtils.split(csv == null ? "" : csv, ",")) {
			final String chunk_ = StringUtils.strip(chunk);
			if (GDR.isIntString(chunk_)) {
				collection.add(Byte.valueOf(chunk_));
			}
		}
	}
	/**
	 * This is being used so the notifiers collection can be a concurrent skip list.  The comparable will always
	 * return -1 so it will get added to the beginning of the collections and since we are not trying to actually sort the data.
	 *
	 * @param <T>
	 */
	private class ComparableWeakReference<T> extends WeakReference<T> implements Comparable<Object> {

		/**
		 * @param referent
		 * @param q
		 */
		public ComparableWeakReference(final T referent, final ReferenceQueue<? super T> q) {
			super(referent, q);
		}

		/**
		 * @param referent
		 */
		public ComparableWeakReference(final T referent) {
			super(referent);
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(final Object o) {
			/**
			 * Always return -1 so the data will get added to the beginning of the collection.
			 */
			return -1;
		}
		
	}
	
	

	/**
	 * @return
	 */
	public boolean isTestMode() {
		return isDebug;
	}
	
	/**
	 * @param val
	 * @return
	 */
	public void setPersistenceEnabled(final boolean val) {
		this.persistenceEnabled = val;
	}
	
	/**
	 * @return
	 */
	public boolean isPersistenceEnabled() {
		return persistenceEnabled;
	}

	/**
	 * @return number of times to retry publish to downlink ring buffer.
	 */
	public int getDownlinkSinkPublishRetry() {
		return globalLadSinkPublishRetry;
	}

	/**
	 * @return the number of milliseconds to wait in between downlink ring buffer publication attempts.`
	 */
	public long getDownlinkSinkPublishInterval() {
		return globalLadSinkPublishRetryInterval;
	}
	
	/**
	 * @return the number of milliseconds to wait in between logging downlink ring buffer publication retries.
	 */
	public long getDownlinkSinkPublishLoggingInterval() {
		return globalLadSinkPublishRetryLoggingInterval;
	}

	/**
	 * @return
	 */
	public File getPersistenceDirectory() {
		return persistenceDirectory;
	}

	
	/**
	 * @return
	 */
	public int getPersistenceMaxNumberBackups() {
		return persistenceMaxNumberBackups;
	}

	/**
	 * @return
	 */
	public int getPersistenceIntervalSeconds() {
		return persistenceInterval;
	}

	/**
	 * @return
	 */
	public int getMaxSocketRetryCount() {
		return globalLadSocketRetryCount;
	}
	
	public int getMaxSocketRetryDelyMillis() {
		return globalLadSocketRetryDelayMillis;
	}

	/**
	 * @return
	 */
	public double getPersistenceMaxSize() {
		return persistenceMaxSize;
	}

	/**
	 * @return
	 */
	public String getPersistenceBackupBaseName() {
		return persistenceBackupBaseName;
	}

	/**
	 * Looks up the configured value for the yellow value.  If not set default is 90.
	 * 
	 * @return
	 */
	public long getDisruptorYellowThreshold() {
		return getLongProperty(DOWNLINK_PERFORMANCE_YELLOW_PROPERTY, 90);
	}
	
	/**
	 * Looks up the configured value for the red value.  If not set default is 100.
	 * 
	 * @return
	 */
	public long getDisruptorRedThreshold() {
		return getLongProperty(DOWNLINK_PERFORMANCE_RED_PROPERTY, 100);
	}
	
	/**
	 * Looks up a property that is expected to be a csv list and splits it and returns a list.  If not found return an empty list.
	 * 
	 * @param property
	 * @return
	 */
	private Collection<String> getCsvProperty(final String property) {
		return getListProperty(property, null, ",");
	}

	/**
	 * Returns the configured csv columns for eha csv output.
	 * 
	 * @return
	 */
	public Collection<String> getCsvColumnNamesEha() {
		return getCsvProperty(EHA_COLUMN_PROPERTY);
	}

	/**
	 * Returns the configured csv columns for the LM csv.
	 *
	 * @return
	 */
	public Collection<String> getLMCsvColumnNamesEha() {
		return getCsvProperty(LM_EHA_COLUMN_PROPERTY);
	}

	/**
	 * Returns the configured csv columns for eha csv output.
	 * 
	 * @return
	 */
	public Collection<String> getCsvColumnNamesEvr() {
		return getCsvProperty(EVR_COLUMN_PROPERTY);
	}

	/**
	 * Returns the URI string from the config.  Defaults to http://%s:%d/globallad.
	 * @return
	 */
    public String getRestURIBase() {
        return uriBase;
	}
	
	/**
	 * Creates a URI for the rest server using the base REST URI and the rest port number.  This expects that the 
	 * 
	 * @return Formatted URl using the base URI REST value and adding the host and port.
	 */
	public String getFormattedRestURI() {
		return String.format(getRestURIBase(), getServerHost(), getRestPort());
	}
	
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean storeErt(final byte userDataType) {
		return !ertSkip.contains(userDataType);
	}

	/**
	 * @param userDataType
	 * @return
	 */
	public boolean storeScet(final byte userDataType) {
		return !scetSkip.contains(userDataType);
	}
	
	/**
	 * @param userDataType
	 * @return
	 */
	public boolean storeEvent(final byte userDataType) {
		return !eventSkip.contains(userDataType);
	}
	
	/**
	 * Added a programmatic way to enable / disable REST time types.
	 */

	/**
	 * Adds timeType to the enabled list.
	 * 
	 * @param timeType
	 */
	public void enableRestTimeType(final GlobalLadPrimaryTime timeType) {
		enabledTimeTypes.add(timeType);
	}
	
	/**
	 * Removes timeType from the enabled list..
	 * 
	 * @param timeType
	 */
	public void disableRestTimeType(final GlobalLadPrimaryTime timeType) {
		enabledTimeTypes.remove(timeType);
	}

	/**
	 * Checks to see if the given time type has been enabled for REST queries in the properties.
	 * 
	 * @param timeType
	 * @return
	 */
	public boolean isRestTimeTypeEnabled(final GlobalLadPrimaryTime timeType) {
		return enabledTimeTypes.contains(timeType);
	}
	
	/**
	 * Checks to see if the given time type has been enabled for REST queries in the properties.  Converts
	 * the timetype to the proper enum.  Fails 
	 * 
	 * @param timeType
	 * @return
	 */
	public boolean isRestTimeTypeEnabled(final String timeType) {
		if (timeType == null) {
			return true;
		} else {
			try {
				return isRestTimeTypeEnabled(GlobalLadPrimaryTime.valueOf(timeType));
			} catch (final Exception e) {
				return false;
			}
		}
	}
	
	public boolean isEnabled() {
		return this.isEnabled;
	}

	public boolean isDebug() {
		return this.isDebug;
	}
	
	/**
	 * Checks to see if the given value is a parent type in the mappings from the properties.
	 * 
	 * @param value
	 * @return
	 */
	public boolean isValueContainerType(final String value) {
		return childMapping.containsKey(value);
	}
	
	public Iterator<Entry<Byte, Integer>> getDepthSetIterator() {
		return dataDepthMapping.entrySet().iterator();
	}
	
	/**
	 * Returns the configured data depth as configured in the global lad config.  If not set
	 * returns the default max depth.
	 * 
	 * @return
	 */
	public int getDefaultDataDepth() {
		return defaultDepth;
	}
	
	/**
	 * Adds notifiable to the list of depth notifiables.  This value is stored as a weak 
	 * reference so there is no need to 
	 * 
	 * @param notifiable
	 */
	public void addDepthListener(final IGlobalLadDepthNotifiable notifiable) {
		notifiables.add(new ComparableWeakReference<IGlobalLadDepthNotifiable>(notifiable));
	}
	
	/**
	 * Cycles through the notifiables list and calls the depthUpdated method.
	 */
	public void notifyDepthListeners() {
		final Iterator<ComparableWeakReference<IGlobalLadDepthNotifiable>> iterator = notifiables.iterator();
		
		IGlobalLadDepthNotifiable notifiable;
		
		while (iterator.hasNext()) {
			notifiable = iterator.next().get();
			
			if (notifiable == null) {
				/**
				 * Been gc'd, remove from the collection.
				 */
				iterator.remove();
			} else {
				notifiable.depthUpdated();
			}
		}
	}
	
	/**
	 * Set a new default depth. 
	 * 
	 * @param newDepth
	 */
	public void setDefaultDepth(final int newDepth) {
		defaultDepth = newDepth;
		notifyDepthListeners();
	}
	
	/**
	 * Define a new depth for a data type. 
	 * 
	 * @param dataType
	 * @param newDepth
	 */
	public void setDataDepth(final byte dataType, final int newDepth) {
		dataDepthMapping.put(dataType, newDepth);
		notifyDepthListeners();
	}
	
	/**
	 * Looks up the data type to see if there is a special mapping for the data type.  If not set of the value is 
	 * less than or equal to zero returns the default value. 
	 * 
	 * @param dataType
	 * @return
	 */
	public int getDataDepth(final byte dataType) {
		final Integer depth = dataDepthMapping.get(dataType);
		
		return (depth == null || depth <= 0) ? 
			getDefaultDataDepth() :
			depth;
	}
	
	/**
	 * Looks up socket server port from the config.  Default is 8900.
	 * @return
	 */
	public int getSocketServerPort() {
		return globalLadSocketServerPort;
	}
	
	/**
	 * Looks up the configured global lad server host.  Default is "localhost".
	 * @return
	 */
	public String getServerHost() {
		return globalLadHost;
	}
	
	/**
	 * Looks up rest server port from the config.  Default is 8700.
	 * @return
	 */
	public int getRestPort() {
		return globalLadRestServerPort;
	}
	
	/**
	 * @param globalLadHost the globalLadHost to set
	 */
	public void setGlobalLadHost(final String globalLadHost) {
		this.globalLadHost = globalLadHost;
	}

	/**
	 * @param globalLadSocketServerPort the globalLadSocketServerPort to set
	 */
	public void setGlobalLadSocketServerPort(final int globalLadSocketServerPort) {
		this.globalLadSocketServerPort = globalLadSocketServerPort;
	}

	/**
	 * @param globalLadRestServerPort the globalLadRestServerPort to set
	 */
	public void setGlobalLadRestServerPort(final int globalLadRestServerPort) {
		this.globalLadRestServerPort = globalLadRestServerPort;
	}

	/**
	 * @return
	 */
	public int getNumberInserters() {
		return getIntProperty(MAX_INSERTERS_PROPERTY, 2);
	}
	
	/**
	 * Looks up the set value for the downlink ring buffer size.  Default is 1024 if no value was set.
	 * @return
	 */
	public int getDownlinkRingBufferSize() {
		return getIntProperty(DOWNLINK_RING_BUFFER_SIZE_PROPERTY, 1024);
	}
	
	/**
	 * Looks up the set value for the client ring buffer size.  Default is 1024 if no value was set.
	 * @return
	 */
	public int getClientRingBufferSize() {
		return getIntProperty(CLIENT_RING_BUFFER_SIZE_PROPERTY, 1024);
	}
	
	/**
	 * Looks up the set value for the globallad ring buffer size.  Default is 1024 if no value was set.
	 * @return
	 */
	public int getGlobalLadRingBufferSize() {
		return getIntProperty(INSERTER_RING_BUFFER_SIZE_PROPERTY, 1024);
	}
	
	/**
	 * @return the childMapping
	 */
	public Map<String, String> getChildMapping() {
		return childMapping;
	}

	/**
	 * @return the dataFactoryClassName
	 */
	public String getDataFactoryClassName() {
		return dataFactoryClassName;
	}

	/**
	 * @return the ringBufferClassName
	 */
	public String getRingBufferClassName() {
		return ringBufferClassName;
	}

	/**
	 * Matches the name to the deal.  Default is BLOCK.
	 * @param propertyName
	 * @return
	 */
	private DisruptorWaitStrategy waitStrategy(final String propertyName) {
		try {
			return DisruptorWaitStrategy.valueOf(getProperty(propertyName, "").toUpperCase());
		} catch (final Exception e) {
			return DisruptorWaitStrategy.BLOCK;
		}
	}
	
	/**
	 * Reads the configuration and finds the wait strategy for the client
	 * disruptors.  The default value will be BLOCK
	 * 
	 * @return client disruptor wait strategy
	 */
	public DisruptorWaitStrategy getClientWaitStrategy() {
		return waitStrategy(CLIENT_WAIT_STRATEGY_PROPERTY);
	}
	
	/**
	 * Reads the configuration and finds the wait strategy for the downlink
	 * disruptors. The default value will be BLOCK
	 * 
	 * @return downlink disruptor wait strategy
	 */
	public DisruptorWaitStrategy getDownlinkWaitStrategy() {
		return waitStrategy(DOWNLINK_WAIT_STRATEGY_PROPERTY);
	}

	/**
	 * Reads the configuration and finds the wait strategy for the insert
	 * disruptors. The default value will be BLOCK
	 * 
	 * @return inserter disruptor wait strategy
	 */
	public DisruptorWaitStrategy getInserterWaitStrategy() {
		return waitStrategy(INSERTER_WAIT_STRATEGY_PROPERTY);
	}

	/**
	 * 
	 * @param containerType
	 * @return true if containerType is the configured container type to do reaping, false otherwise.
	 */
	public boolean isReapingLevel(final String containerType) {
		return reapingLevel.equals(containerType);
	}
	
	public int getReapingTimeInterval() {
		return this.reapingInterval;
	}
	
	public int getReapingMemoryThreshold() {
		return this.reapingMemoryThreshold;
	}

	public boolean isReapableContainer(final String containerType) {
		return reapTimeMapping.containsKey(containerType);
	}
	
	public boolean isReapable(final String containerType, final String containerIdentifier) {
		return reapTimeMapping.containsKey(containerType) && reapTimeMapping.get(containerType).containsKey(containerIdentifier);
	}
	
	/**
	 * Get the number of milliseconds for a given container before it should be reaped. 
	 * 
	 * @return the reapingMilliseconds
	 */
	public long getReapingMilliseconds(final String containerType, final String containerIdentifier) {
		return isReapable(containerType, containerIdentifier) ? 
				reapTimeMapping.get(containerType).get(containerIdentifier) :
				-1;
	}

	/**
	 * @param containerType
	 * @param containerIdentifier
	 * @param reapingTimeMinutes
	 */
	public void setReapingTimeMinutes(final String containerType, final String containerIdentifier, final long reapingTimeMinutes) {
		if (reapingTimeMinutes > 0) {
			if (!reapTimeMapping.containsKey(containerType)) {
				reapTimeMapping.put(containerType, new HashMap<String, Long>());
			}
			
			reapTimeMapping.get(containerType).put(containerIdentifier, reapingTimeMinutes*60000);
		} else {
			getTracer().error("Refusing to change the reaping value for container type " + containerType + " and container identifier " + containerIdentifier + " because the reaping time was less than 0");
		}
	}
	
	/**
	 * If true the glad should only do data creation and stop before publishing 
	 * to the disruptor ring buffer.
	 * 
	 * @return
	 */
	public boolean isTestDataConstructionOnly() {
		return getBooleanProperty(TEST_DATA_CONSTRUCTION_ONLY, false);
	}
	
	/**
	 * If true glad should create the data objects, publish to the ring buffer and stop before
	 * inserting into the global lad.
	 * @return
	 */
	public boolean isTestRingBufferInsertOnly() {
		return getBooleanProperty(TEST_RING_BUFFER_INSERT_ONLY, false);
	}

	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}


	/**
	 * Sets the global LAD server host.
	 * @param ladHost host name to set
	 */
    public void setServerHost(final String ladHost) {
        this.globalLadHost = ladHost;
    }

    /**
     * @return if Https enabled on query
     */
    public boolean isHttpsEnabled() {
        return isHttpsEnabled;
    }

    /**
     * Set whether HTTPS is enabled or disabled
     */
    public synchronized void setHttpsEnabled(final boolean enabled) {
        isHttpsEnabled = enabled;
        if (isHttpsEnabled) {
            uriBase = uriBase.replaceFirst(HTTP, HTTPS);
        }
        else {
            uriBase = uriBase.replaceFirst(HTTPS, HTTP);
        }
    }

	/**
	 * Sets the data source
	 *
	 * @param dataSource
	 * @throws IllegalArgumentException
	 */
	public void setDataSource(final IGlobalLadDataSource.DataSourceType dataSource) throws IllegalArgumentException {
		this.dataSource = dataSource;
	}

	/**
	 * Gets the source of GLAD data
	 *
	 * @return SOCKET_SERVER or JMS
	 */
	public IGlobalLadDataSource.DataSourceType getDataSource() {
		return dataSource;
	}

	/**
	 * Sets the list of root topics for JMS data source subscription
	 *
	 * @param topics a comma delimited string list of JMS root topics
	 */
	public void setJmsRootTopics(final String topics) {
		List<String> topicList = Arrays.asList(topics.split(","));
		setJmsRootTopics(topicList);
	}

	/**
	 * Sets the list of root topics for JMS data source subscription
	 *
	 * @param topics a list of JMS root topics
	 */
	public void setJmsRootTopics(final List<String> topics) {
		Set<String> topicsSet = new LinkedHashSet<>(topics);
		this.jmsRootTopics = new ArrayList<>(topicsSet);
	}

	/**
	 * Gets a list of root topics for JMS data source subscription
	 *
	 * @return list of root JMS topics, EVR and EHA subtopics are inferred
	 */
	public List<String> getJmsRootTopics() {
		return jmsRootTopics;
	}

	/**
	 * Get the venue type for JMS
	 *
	 * @return venue type
	 */
	public VenueType getVenueType() {
		return venueType;
	}

	/**
	 * Set the venue type for JMS
	 *
	 * @param venueType
	 */
	public void setVenueType(final VenueType venueType) {
		this.venueType = venueType;
	}

	/**
	 * Get the downlink stream type for JMS
	 *
	 * @return downlink stream type
	 */
	public DownlinkStreamType getDownlinkStreamType() {
		return downlinkStreamType;
	}

	/**
	 * Set the downlink stream type for JMS
	 *
	 * @param downlinkStreamType
	 */
	public void setDownlinkStreamType(final DownlinkStreamType downlinkStreamType) {
		this.downlinkStreamType = downlinkStreamType;
	}

	/**
	 * Get testbed name for JMS
	 *
	 * @return testbed name
	 */
	public String getTestbedName() {
		return testbedName;
	}

	/**
	 * Set testbed name for JMS
	 *
	 * @param testbedName
	 */
	public void setTestbedName(final String testbedName) {
		this.testbedName = testbedName;
	}

	/**
	 * Set JMS hostname, for constructing JMS topics
	 * @param hostName
	 */
	public void setJmsHostName(final String hostName) {
		this.jmsHostName = hostName;
	}

	/**
	 * Get JMS hostname, for constructing JMS topics
	 * @return
	 */
	public String getJmsHostName() {
		return this.jmsHostName;
	}
}
