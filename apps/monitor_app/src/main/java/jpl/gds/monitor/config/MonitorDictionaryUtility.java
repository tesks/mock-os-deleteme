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
package jpl.gds.monitor.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.IEvr;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.DictionaryTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;

/**
 * This class provides static methods for use by chill_monitor when it requires
 * formatted dictionary information for display.
 *
 */
public class MonitorDictionaryUtility {
    private Tracer                    log;

    
	private DictionaryTemplateManager templateMgr;
    
    private ApplicationContext appContext;
    private IChannelUtilityDictionaryManager chanDictUtil;    
    private IEvrUtilityDictionaryManager evrDictUtil;
    private IAlarmDictionaryManager alarmDictUtil;
    private MissionProperties missionProps; 
    private boolean doSse;
    
    public MonitorDictionaryUtility() {
    	// do nothing
    }
    
    public void init(final ApplicationContext appContext) throws BeansException {
        this.appContext = appContext;
        log = TraceManager.getDefaultTracer(appContext);
        missionProps = appContext.getBean(MissionProperties.class);
        clearAllDictionaries();
        final VenueType vt = appContext.getBean(IVenueConfiguration.class).getVenueType();
        doSse = !vt.isOpsVenue() && missionProps.missionHasSse()
                && !appContext.getBean(SseContextFlag.class).isApplicationSse();
        chanDictUtil = appContext.getBean(IChannelUtilityDictionaryManager.class);
        evrDictUtil = appContext.getBean(IEvrUtilityDictionaryManager.class);
        alarmDictUtil = appContext.getBean(IAlarmDictionaryManager.class);
        missionProps = appContext.getBean(MissionProperties.class);

        appContext.getBean(FlightDictionaryLoadingStrategy.class)
        .setChannel(missionProps.isEhaEnabled())
        .setHeader(missionProps.isEhaEnabled())
        .setMonitor(missionProps.isEhaEnabled())
        .setAlarm(missionProps.isEhaEnabled())
        .setEvr(missionProps.areEvrsEnabled());

        if (doSse) {
            appContext.getBean(SseDictionaryLoadingStrategy.class)
            .setChannel(missionProps.isEhaEnabled())
            .setHeader(missionProps.isEhaEnabled())
            .setAlarm(missionProps.isEhaEnabled())
            .setEvr(missionProps.areEvrsEnabled());
        }
    }

    public synchronized void loadChannelDictionaries() throws DictionaryException {     
        loadFswChannelDictionaries();
        loadSseChannelDictionaries();
    }
    
    public synchronized void loadFswChannelDictionaries() throws DictionaryException {
        
        if (chanDictUtil != null) {
            chanDictUtil.clearAllFsw();
            chanDictUtil.loadFsw(false);
            chanDictUtil.loadFswHeader(false);
            chanDictUtil.loadMonitor(false);
        }
    }
    
    public synchronized void loadSseChannelDictionaries() throws DictionaryException {
        
        if (chanDictUtil != null) {
            chanDictUtil.clearAllSse();
            chanDictUtil.loadSse(false);
            chanDictUtil.loadSseHeader(false);
        }
    }    

    public synchronized void loadEvrDictionaries() throws DictionaryException {     
         loadFswEvrDictionaries();
         loadSseEvrDictionaries();
    }
    
    public synchronized void loadFswEvrDictionaries() throws DictionaryException {     

        if (evrDictUtil != null) {
            evrDictUtil.clearFsw();
            evrDictUtil.loadFsw();
        }
    }

    public synchronized void loadSseEvrDictionaries() throws DictionaryException {     

        if (evrDictUtil != null) {
            evrDictUtil.clearSse();
            evrDictUtil.loadSse();
        }
    }
     
    public synchronized void loadAlarmDictionaries() throws DictionaryException {     
        loadFswAlarmDictionaries();
        loadSseAlarmDictionaries();
    }
    
    public synchronized void loadFswAlarmDictionaries() throws DictionaryException {     
        
        if (alarmDictUtil != null) {
            alarmDictUtil.clearFsw();
            alarmDictUtil.loadFsw(chanDictUtil == null ? new HashMap<String, IChannelDefinition>() :
                chanDictUtil.getChannelDefinitionMap());
        }
    }
    
    public synchronized void loadSseAlarmDictionaries() throws DictionaryException {     

        if (alarmDictUtil != null) {
            alarmDictUtil.clearSse();
            alarmDictUtil.loadSse(chanDictUtil == null ? new HashMap<String, IChannelDefinition>() :
                chanDictUtil.getChannelDefinitionMap());
        }
    }

    public synchronized void loadAllDictionaries() throws DictionaryException {
        clearAllDictionaries();
        loadChannelDictionaries();
        loadEvrDictionaries();
        loadAlarmDictionaries(); 	
    }
    
    public synchronized void loadFswDictionaries() throws DictionaryException {
        clearFswDictionaries();
        loadFswChannelDictionaries();
        loadFswEvrDictionaries();
        loadFswAlarmDictionaries();    
    }
    
    public synchronized void loadSseDictionaries() throws DictionaryException {
        clearSseDictionaries();
        loadSseChannelDictionaries();
        loadSseEvrDictionaries();
        loadSseAlarmDictionaries();    
    }

    public synchronized void clearAllDictionaries() {
        clearFswDictionaries();
        clearSseDictionaries();
    }
    
    public synchronized void clearFswDictionaries() {
        if (chanDictUtil != null) {
            chanDictUtil.clearAllFsw();
        }
        if (evrDictUtil != null) {
            evrDictUtil.clearFsw();
        }
    }
    
    public synchronized void clearSseDictionaries() {
        if (chanDictUtil != null) {
            chanDictUtil.clearAllSse();
        }
        if (evrDictUtil != null) {
            evrDictUtil.clearSse();
        }
    }
    
    public synchronized IChannelUtilityDictionaryManager getChannelDictionaryUtil() {
        return chanDictUtil;
    }

 
    public IChannelDefinition getDefinitionFromChannelId(final String channelId) {
        if (chanDictUtil == null) {
            return null;
        }
        return chanDictUtil.getDefinitionFromChannelId(channelId); 
    }
    
	/**
	 * Gets a formatted EVR definition for display.
	 * 
	 * @param evr an EVR to get the definition for
	 * @return formatted text for display
	 */
	public String getEvrText(final IEvr evr) {

		try {
			if (templateMgr == null) {
                templateMgr = MissionConfiguredTemplateManagerFactory.getNewDictionaryTemplateManager(appContext.getBean(SseContextFlag.class));
			}

			final Template t = templateMgr.getTemplateForStyle("Evr", "keyvalue");
			if (t == null) {
				return "Unable to format EVR definition";
			}

			IEvrDefinition def = null;

			if (evr.isFromSse()) {
			    def = evrDictUtil.getSseDefinition(evr.getEventId());
			} else {
			    def = evrDictUtil.getFswDefinition(evr.getEventId());
			}
			if (def == null) {
				return "No definition found for EVR ID " + evr.getEventId();
			}
			
			final Map<String,Object> variables = new HashMap<String, Object>();
			variables.put("evrDef", def);

			final String text = TemplateManager.createText(t, variables);
			return text;

		} catch (final TemplateException e) {
			return "Unable for format EVR definition";
		}
	}
	
	/**
	 * Gets the complete list of EVR modules found in both the flight and
	 * GSE/SSE dictionaries.  Loads the EVR dictionaries if they are not 
	 * already loaded.
	 * 
	 * @return Sorted list of module strings; never null
	 */
	public synchronized List<String> getEvrModules() {
	     return new LinkedList<>(evrDictUtil.getAllCategories(ICategorySupport.MODULE));
	}
	
	/**
     * Gets the complete list of EVR levels found in both the flight and
     * GSE/SSE dictionaries.  Loads the EVR dictionaries if they are not 
     * already loaded.
     * 
     * @return Sorted list of level strings; never null
     */
	public synchronized List<String> getEvrLevels() {
	    return new LinkedList<>(evrDictUtil.getAllLevels());	    
	}

}
