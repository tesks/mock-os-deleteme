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
package jpl.gds.dictionary.api.client.alarm;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmOffControl;
import jpl.gds.dictionary.api.alarm.IAlarmReloadListener;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.DirectoryChangeEvent;
import jpl.gds.shared.file.DirectoryChangeListener;
import jpl.gds.shared.file.DirectoryChangeMonitor;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.Pair;

/**
 * This class stores all single-channel alarm calculation objects, as created
 * from multiple alarm files. It will parse alarm dictionary files, get the
 * alarm definitions, and create alarm objects from those definitions. It keeps
 * maps of alarm objects by both channel ID and alarm ID. Combination source and
 * target proxy alarms, which also apply to a single channel, are also kept
 * here. The combination alarms themselves are kept in the
 * CombinationAlarmTable, which interacts with this AlarmTable to manage
 * proxy alarms.  This class also works relies upon a Channel Definition Map
 * which must contain the definitions for channels to be alarmed before any
 * alarm file is parsed by this class.
 * <p>
 * This is a singleton class. Only one instance is ever created.
 * 
 *
 * 
 *
 */
public class AlarmDictionaryClientUtility implements DirectoryChangeListener, IAlarmDictionaryManager {
    
    private final Tracer                                              log;
    
	/** Loaded alarm files, or files being looked for periodically **/
	private final List<Pair<String, Map<String, IChannelDefinition>>> alarmFilePaths = new CopyOnWriteArrayList<Pair<String, Map<String, IChannelDefinition>>>();

	/** Active directory monitors **/
	private final List<DirectoryChangeMonitor> fswMonitors = new CopyOnWriteArrayList<DirectoryChangeMonitor>();
	private final List<DirectoryChangeMonitor> sseMonitors = new CopyOnWriteArrayList<DirectoryChangeMonitor>();
	
    private final AtomicBoolean fswChannelDictionaryLoadFlag = new AtomicBoolean(false); 
    private final AtomicBoolean sseChannelDictionaryLoadFlag = new AtomicBoolean(false); 

    private final Map<String, List<IAlarmDefinition>> alarmsForFswChannels = new ConcurrentHashMap<String, List<IAlarmDefinition>>();
    private final Map<String, ICombinationAlarmDefinition> comboAlarmsForFswChannels = new ConcurrentHashMap<String, ICombinationAlarmDefinition>();
    private final Map<String, List<IAlarmDefinition>> alarmsForSseChannels = new ConcurrentHashMap<String, List<IAlarmDefinition>>();
    private final Map<String, ICombinationAlarmDefinition> comboAlarmsForSseChannels = new ConcurrentHashMap<String, ICombinationAlarmDefinition>();

    private DictionaryProperties dictConfig;
    private boolean doAutoReload;
    
    private final List<IAlarmReloadListener> listeners = new CopyOnWriteArrayList<IAlarmReloadListener>();
    
    private final IDictionaryCache cache;
    
	private final FlightDictionaryLoadingStrategy fsw;
	private final SseDictionaryLoadingStrategy sse;
    private final SseContextFlag                                      sseFlag;


	/**
     * Constructor. Uses the dictionary properties and dictionary factory from the supplied
     * application context.
     * 
     * @param appContext
     *            the current application context
     * @param autoReload
     *            true to enable auto-reload of changed dictionary files, false
     *            to disable auto reload
     */
    public AlarmDictionaryClientUtility(final ApplicationContext appContext, final boolean autoReload) {
        dictConfig = appContext.getBean(DictionaryProperties.class);
        cache = appContext.getBean(IDictionaryCache.class);
        log = TraceManager.getTracer(appContext, Loggers.DICTIONARY);
        sseFlag = appContext.getBean(SseContextFlag.class);

        fsw = appContext.getBean(FlightDictionaryLoadingStrategy.class);
        sse = appContext.getBean(SseDictionaryLoadingStrategy.class);
        this.doAutoReload = autoReload;
	}


    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#getSingleChannelAlarmDefinitions(java.lang.String)
	 */
	@Override
    public List<IAlarmDefinition> getSingleChannelAlarmDefinitions(final String id) {
        List<IAlarmDefinition> alarmList = alarmsForFswChannels.get(id);
        if (alarmList == null) {
            alarmList = alarmsForSseChannels.get(id);
        }
        if (alarmList != null && alarmList.isEmpty()) {
            alarmList = null;
        }
        return alarmList;
    }

    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#getCombinationAlarmMap()
	 */
	@Override
    public Map<String, ICombinationAlarmDefinition> getCombinationAlarmMap() {
        final Map<String, ICombinationAlarmDefinition> comboAlarms = new HashMap<String, ICombinationAlarmDefinition>(comboAlarmsForFswChannels);
        comboAlarms.putAll(comboAlarmsForSseChannels);
        return comboAlarms;
    }

    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#getSingleChannelAlarmMapByChannel()
	 */
	@Override
    public Map<String, List<IAlarmDefinition>> getSingleChannelAlarmMapByChannel() {
        final Map<String, List<IAlarmDefinition>> alarmMap = new TreeMap<String, List<IAlarmDefinition>>(alarmsForFswChannels);
        alarmMap.putAll(alarmsForSseChannels);
        return alarmMap;
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addSimpleFswAlarmDefinition(jpl.gds.dictionary.api.alarm.IAlarmDefinition)
	 */
    @Override
	public void addSimpleFswAlarmDefinition(final IAlarmDefinition ad) {
        addFromSimpleAlarmDefinition(alarmsForFswChannels, ad);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addSimpleSseAlarmDefinition(jpl.gds.dictionary.api.alarm.IAlarmDefinition)
	 */
    @Override
	public void addSimpleSseAlarmDefinition(final IAlarmDefinition ad) {
        addFromSimpleAlarmDefinition(alarmsForSseChannels, ad);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addFswCombinationAlarm(jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition)
	 */
    @Override
	public void addFswCombinationAlarm(final ICombinationAlarmDefinition ad) {
        comboAlarmsForFswChannels.put(ad.getAlarmId(), ad);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addSseCombinationAlarm(jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition)
	 */
    @Override
	public void addSseCombinationAlarm(final ICombinationAlarmDefinition ad) {
        comboAlarmsForSseChannels.put(ad.getAlarmId(), ad);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addReloadListener(jpl.gds.dictionary.api.alarm.IAlarmReloadListener)
	 */
	@Override
    public void addReloadListener(final IAlarmReloadListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#removeReloadListener(jpl.gds.dictionary.api.alarm.IAlarmReloadListener)
	 */
	@Override
    public void removeReloadListener(final IAlarmReloadListener l) {
        listeners.remove(l);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#loadAll(boolean, java.util.Map)
	 */
    @Override
	public synchronized void loadAll(final Map<String, IChannelDefinition> chanMap) throws DictionaryException {
        loadFsw(chanMap);
        loadSse(chanMap);
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#loadAll(jpl.gds.dictionary.api.config.DictionaryConfiguration, boolean, java.util.Map)
	 */
    @Override
	public synchronized void loadAll(final DictionaryProperties config, final Map<String, IChannelDefinition> chanMap) throws DictionaryException {
    	this.dictConfig = config;
    	loadAll(chanMap);
    }

    
    @Override
    public synchronized void clearAll() {
        for (final DirectoryChangeMonitor m: fswMonitors) {
            m.stop();
        }
        fswMonitors.clear();
        clearFsw();
        for (final DirectoryChangeMonitor m: sseMonitors) {
            m.stop();
        }
        sseMonitors.clear();
        clearSse();
        alarmFilePaths.clear();
    }
    
    @Override
    public synchronized void clearFsw() {
        alarmsForFswChannels.clear();
        comboAlarmsForFswChannels.clear();
        fswChannelDictionaryLoadFlag.set(false);
    }
    
    @Override
    public synchronized void clearSse() {
        alarmsForSseChannels.clear();
        comboAlarmsForSseChannels.clear();
        sseChannelDictionaryLoadFlag.set(false);
    }


    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addFswAlarmsFromFile(java.util.Map, java.lang.String)
	 */
    @Override
	public synchronized void addFswAlarmsFromFile(final Map<String, IChannelDefinition> chanMap, final String filename) throws DictionaryException {
        if (filename != null && new File(filename).exists()) {
            processAlarmDictionary(
            		cache.getAlarmDictionary(dictConfig, chanMap, filename, false),
                    alarmsForFswChannels, comboAlarmsForFswChannels);
            addToMonitorList(filename, chanMap, fswMonitors);
        }
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#addSseAlarmsFromFile(java.util.Map, java.lang.String)
	 */
    @Override
	public synchronized void addSseAlarmsFromFile(final Map<String, IChannelDefinition> chanMap, final String filename) throws DictionaryException {
        if (filename != null && new File(filename).exists()) {
            processAlarmDictionary(
            		cache.getAlarmDictionary(dictConfig, chanMap, filename, true),
                    alarmsForSseChannels, comboAlarmsForSseChannels);
            addToMonitorList(filename, chanMap, sseMonitors);
        }
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#loadFsw(java.util.Map)
	 */
    @Override
	public synchronized void loadFsw(final Map<String, IChannelDefinition> chanMap) throws DictionaryException {

        if (fsw.isAlarmEnabled() && !fswChannelDictionaryLoadFlag.getAndSet(true)) {
            clearFsw();

            String systemFile = null;
            try {
                systemFile = dictConfig.findFileForSystemMission(DictionaryType.ALARM);
            } catch (final DictionaryException e) {
                // ignore. ok not to have alarm file.
            }
            if (systemFile != null && new File(systemFile).exists()) {
                processAlarmDictionary(
                		cache.getAlarmDictionary(dictConfig, chanMap, systemFile, false),
                        alarmsForFswChannels, comboAlarmsForFswChannels);
                addToMonitorList(systemFile, chanMap, fswMonitors);
            }
            final String userFile = getUserAlarmFile();
            if (userFile != null && new File(userFile).exists()) {
                processAlarmDictionary(
                		cache.getAlarmDictionary(dictConfig, chanMap, userFile, false),
                        alarmsForFswChannels, comboAlarmsForFswChannels);
           		addToMonitorList(userFile, chanMap, fswMonitors);
            }
        }
    }
    
    /* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#loadSse(java.util.Map)
	 */
    @Override
	public synchronized void loadSse(final Map<String, IChannelDefinition> chanMap) throws DictionaryException {

        if (sse.isAlarmEnabled() && !sseChannelDictionaryLoadFlag.getAndSet(true)) {
            clearSse();

            String systemFile = null;
            try {
                systemFile = dictConfig.findFileForSystemMission(DictionaryType.ALARM);
            } catch (final DictionaryException e) {
                // ignore. ok not to have alarm file.
            }
            if (systemFile != null && new File(systemFile).exists()) {
                processAlarmDictionary(
                		cache.getAlarmDictionary(dictConfig, chanMap, systemFile, true),
                        alarmsForSseChannels, comboAlarmsForSseChannels);
                addToMonitorList(systemFile, chanMap, sseMonitors);
            }
            
            final String userFile = getSseUserAlarmFile();
            if (userFile != null && new File(userFile).exists()) {
                processAlarmDictionary(
                		cache.getAlarmDictionary(dictConfig, chanMap, userFile, true),
                        alarmsForSseChannels, comboAlarmsForSseChannels);
                addToMonitorList(userFile, chanMap, sseMonitors);           
            }
        }
    }

    private void processAlarmDictionary(
            final IAlarmDictionary dict,
            final Map<String, List<IAlarmDefinition>> alarmsForChannels,
            final Map<String, ICombinationAlarmDefinition> comboAlarms) {
        
        // Get the list of alarms turned off by the file just loaded and 
        // process those first.
        removeAlarms(dict.getOffControls(), alarmsForChannels, comboAlarms);

        // Now add simple and combination alarm definitions
        dict.getSingleChannelAlarmDefinitions().forEach(alarm->addFromSimpleAlarmDefinition(alarmsForChannels, alarm));
        comboAlarms.putAll(dict.getCombinationAlarmMap());
    }

    
	/**
	 * Creates and adds a new simple alarm to this table, given its definition.
	 * 
	 * @param def
	 *            The alarm definition to create an alarm for
	 * 
	 */
    private synchronized void addFromSimpleAlarmDefinition(final Map<String, List<IAlarmDefinition>> alarmsForChannels, final IAlarmDefinition def) {
		if (def == null) {
			throw new IllegalArgumentException("Null alarm definition input!");
		}
		
		List<IAlarmDefinition> alarms = alarmsForChannels.get(def.getChannelId());
		if (alarms == null) {
		    alarms = new CopyOnWriteArrayList<IAlarmDefinition>();
		    alarmsForChannels.put(def.getChannelId(), alarms);
		}
		for (int i = 0; i < alarms.size(); i++){
		    if (alarms.get(i).getAlarmId().equals(def.getAlarmId())) {
		        alarms.set(i, def);
		        return;
		    }
		}
		alarms.add(def);
	}

	/**
	 * Adds an alarm file to the watch list of files to be monitored for
	 * changes.
	 * 
	 * @param alarmXmlPath the path to the alarm file
	 */
	private void addToMonitorList(final String alarmXmlPath, final Map<String, IChannelDefinition> chanMap, final List<DirectoryChangeMonitor> monitors) {

	    if (!doAutoReload || dictConfig.getAlarmReloadInterval() == 0) {
	        return;
	    }
		if (alarmXmlPath != null && !alarmFilePaths.contains(alarmXmlPath)) {
			alarmFilePaths.add(new Pair<String, Map<String, IChannelDefinition>>(alarmXmlPath, chanMap));
			final File alarmPath = new File(alarmXmlPath);
			final File parent = alarmPath.getParentFile();
			if (parent != null) {
                final DirectoryChangeMonitor monitor = new DirectoryChangeMonitor();
				monitor.setDirectory(parent);
				monitor.setPollInterval(dictConfig.getAlarmReloadInterval() * 1000);
				monitor.setFilenameFilter(new AlarmFilenameFilter(alarmPath
						.getName()));
				monitor.addDirectoryChangeListener(this);
				monitors.add(monitor);
				monitor.start();
			}
		}
	}

	/**
	 * Removes alarms based upon the supplied list of alarm off controls. Will
	 * also remove alarms from the CombinationAlarmTable.
	 * 
	 * @param offList
	 *            the list of IAlarmOffControl objects defining which alarms to
	 *            remove
	 */
	private void removeAlarms(final List<IAlarmOffControl> offList, final Map<String, List<IAlarmDefinition>> alarmsForChannels,
            final Map<String, ICombinationAlarmDefinition> comboAlarms) {
		if (offList == null) {
			return;
		}

		for (final IAlarmOffControl control: offList) {
			switch (control.getScope()) {
			case ALARM:
				clearAlarmWithAlarmId(control.getAlarmId(), alarmsForChannels, comboAlarms);
				break;
			case CHANNEL:
				clearAlarmsFromChannelId(control.getChannelId(), null, alarmsForChannels);
				break;
			case CHANNEL_AND_LEVEL:
				clearAlarmsFromChannelId(control.getChannelId(), control.getLevel(), alarmsForChannels);
				break;
			default:
				throw new IllegalStateException("Invalid off control scope: " + control.getScope());

			}
		}
	}
	
	/**
     * Given a channel ID and alarm level, clear the alarms
     * associated with the ID and level except for the combination alarms.
     * 
     * @param id
     *            the channel ID for alarms to clear
     * @param level
     *            the alarm level of alarms to clear
     */
    private void clearAlarmsFromChannelId(final String id,
            final AlarmLevel level, final Map<String, List<IAlarmDefinition>> alarmsForChannels) {

        if (id == null) {
            throw new IllegalArgumentException("The input channel ID was null!");
        }

        final List<IAlarmDefinition> list = alarmsForChannels.get(id);
        final List<IAlarmDefinition> newList = new CopyOnWriteArrayList<IAlarmDefinition>();
        if (list != null) {
            for (final IAlarmDefinition ad: list) {
                if (level != null && level != AlarmLevel.NONE && !ad.getAlarmLevel().equals(level)) {
                    newList.add(ad);
                }
            }
        }
        alarmsForChannels.put(id,  newList);

    }

	/**
	 * Removes any simple alarm with the specified alarm id. 
	 * 
	 * @param alarmId the ID of the alarm to remove
	 * 
	 */
	private void clearAlarmWithAlarmId(final String alarmId, final Map<String, List<IAlarmDefinition>> alarmsForChannels,
            final Map<String, ICombinationAlarmDefinition> comboAlarms) {
	    
	    if (alarmId == null) {
            throw new IllegalArgumentException("The input alarm ID was null!");
        }

	    comboAlarms.remove(alarmId);
	    
        final List<IAlarmDefinition> list = alarmsForChannels.get(alarmId);
        if (list != null) {
            for (final Iterator<IAlarmDefinition> iter = list.iterator(); iter
                    .hasNext();) {
                final IAlarmDefinition ad = iter.next();
                if (ad.getAlarmId().equals(alarmId)) {
                    iter.remove();
                }
            }

        }
	}

	/**
	 * 
	 * AlarmFilenameFilter is a FilenameFilter implementation that matches a
	 * single file name.
	 * 
	 *
	 */
	private static class AlarmFilenameFilter implements FilenameFilter {
		private final String filename;

		/**
		 * Creates an instance of AlarmFilenameFilter.
		 * 
		 * @param filename the filename to match
		 */
		public AlarmFilenameFilter(final String filename) {
			this.filename = filename;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(final File dir, final String name) {
			return name.equals(filename);
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.alarm.IAlarmDictionaryManager#directoryChanged(jpl.gds.shared.file.DirectoryChangeEvent)
	 */
	@Override
	public synchronized void directoryChanged(final DirectoryChangeEvent event) {
	    if (!(monitoredFileChanged(event))) {
	        return;
	    }

	    final boolean isFsw = fswMonitors.contains(event.getSource());

	    /*
	     * If any alarm file changes, we reload them all, 
	     * in the order they were originally loaded.
	     */
	    if (isFsw) {
	        clearFsw();
	    } else {
	        clearSse();
	    }

		final Iterator<Pair<String, Map<String, IChannelDefinition>>> files = alarmFilePaths.iterator();
		while (files.hasNext()) {
			try {
				final Pair<String, Map<String, IChannelDefinition>> p = files.next();

				/*
				 * Add canonical path and md5 checksum to alarm reload messages
				 */
				try {
					final String md5 = DigestUtils.md5Hex(FileUtils.readFileToByteArray(Paths.get(p.getOne()).toFile()));
					log.info("Reloading alarm file [", md5, "] ", FileUtility.createFilePathLogMessage(p.getOne()));
				} catch (final IOException e) {
					log.warn(ExceptionTools.getMessage(e), " Unable to compute checksum on " ,
					         FileUtility.createFilePathLogMessage(p.getOne()));
					log.info("Reloading alarm file ", FileUtility.createFilePathLogMessage(p.getOne()));
				}
				if (isFsw) {
					processAlarmDictionary(
							cache.reloadAlarmDictionary(dictConfig, p.getTwo(), p.getOne(), false),
							alarmsForFswChannels, comboAlarmsForFswChannels);
				} else {
					processAlarmDictionary(
							cache.reloadAlarmDictionary(dictConfig, p.getTwo(), p.getOne(), true),
							alarmsForSseChannels, comboAlarmsForSseChannels);
				}
			} catch (final DictionaryException e) {
				log.warn("Problem reloading alarm definitions: " + e.toString());
			}
		}
		
		notifyReloadListeners();
	}


	/**
	 * Event handler triggered when the alarm file changed
	 * @param event the change event
	 * @return true if the event pertains to monitored files being modified
	 */
	private boolean monitoredFileChanged(final DirectoryChangeEvent event) {


		final List<File>  lst = event.getModifiedFiles();		
		if (lst.isEmpty()) { // no file was modified
			return false;	
		} 
		// check if the modified files match the ones being monitored
		final Iterator<File> iterF = lst.iterator();			
		while(iterF.hasNext()) {		
		    final String eventFileStr= (iterF.next()).getAbsolutePath();
            for (final Pair<String, Map<String, IChannelDefinition>> pair: alarmFilePaths)
            if  (pair.getOne().equals(eventFileStr))    {   
                return true;
            }   
		} 	    		 		
		return false;
	}
	
	private void notifyReloadListeners() {
	    listeners.forEach(IAlarmReloadListener::alarmsReloaded);
	}

    /**
     * Returns the user-specific alarm file path.
     * 
     * @return user alarm file
     * 
     */
    private String getUserAlarmFile() {
        String file = null;
        if (sseFlag.isApplicationSse()) {
            file = getSseUserAlarmFile();
        } else {
            file = GdsSystemProperties.getSystemProperty(ALARM_FILE_PROPERTY);
        }
        return file;
    }
    
    /**
     * Returns the user-specific alarm file path for SSE file only
     * 
     * @return SSE user alarm file
     * 
     */
    private String getSseUserAlarmFile() {
        String file = null;       
        file = GdsSystemProperties.getSystemProperty(SSE_ALARM_FILE_PROPERTY);
      
        return file;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    public boolean isLoaded() {
        return this.fswChannelDictionaryLoadFlag.get() || this.sseChannelDictionaryLoadFlag.get();
    }
}
