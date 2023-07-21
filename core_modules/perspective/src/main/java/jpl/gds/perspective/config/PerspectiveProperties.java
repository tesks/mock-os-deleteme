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
package jpl.gds.perspective.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Class for Perspective application properties
 */
public class PerspectiveProperties extends GdsHierarchicalProperties {

 private static final String PROPERTY_FILE = "perspective.properties";
    
    private static final String PROPERTY_PREFIX = "perspective.";
    
    private static final String DEFAULT_APPLICATIONS_PROPERTY = PROPERTY_PREFIX + "defaultApplications";
    private static final String DEFAULT_PERSPECTIVE_FILE_PROPERTY = PROPERTY_PREFIX + "defaultPerspectiveFile";
    private static final String DEFAULT_PERSPECTIVE_NAME_PROPERTY = PROPERTY_PREFIX + "defaultPerspectiveName";
    private static final String HEADERS_PROPERTY = PROPERTY_PREFIX + "headers";
    private static final String DOWNLINK_APP_PROPERTY = PROPERTY_PREFIX + "internal.downlinkApp";
    private static final String UPLINK_APP_PROPERTY = PROPERTY_PREFIX + "internal.uplinkApp";
    private static final String MONITOR_APP_PROPERTY = PROPERTY_PREFIX + "internal.monitorApp";
    private static final String VIEW_DIRS_PROPERTY = PROPERTY_PREFIX + "viewDirs";
    private static final String PERSPECTIVE_DIRS_PROPERTY = PROPERTY_PREFIX + "perspectiveDirs";
    private static final String UTC_TIME_FORMATS_PROPERTY = PROPERTY_PREFIX + "utcTimeFormats";
    private static final String LST_TIME_FORMATS_PROPERTY = PROPERTY_PREFIX + "lstTimeFormats";
    
    private static final String VALIDATION_BLOCK = PROPERTY_PREFIX + "validation.";
    
    private static final String VALIDATE_VIEWCOUNT_PROPERTY = VALIDATION_BLOCK + "viewCountCheckEnabled";
    private static final String MAX_ALARM_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxAlarmViews";
    private static final String MAX_CHANNEL_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxChannelViews";
    private static final String MAX_EVR_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxEvrViews";
    private static final String MAX_FIXED_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxFixedViews";
    private static final String MAX_PLOT_TRACES_PROPERTY = VALIDATION_BLOCK + "maxPlotTraces";
    private static final String MAX_PLOT_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxPlotViews";
    private static final String MAX_PRODUCT_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxProductViews";
    private static final String MAX_VIEWS_PROPERTY = VALIDATION_BLOCK + "maxViews";
    
    private static final String DEFAULT_PERSPECTIVE_FILE = "PerspectiveFile.xml";
    private static final String DEFAULT_APPLICATIONS = "downlink,monitor,sse_downlink,uplink";
    private static final String DEFAULT_PERSPECTIVE_NAME = "DefaultPerspective";
    private static final String DEFAULT_DOWNLINK_APP = "chill_down";
    private static final String DEFAULT_UPLINK_APP = "chill_up";
    private static final String DEFAULT_MONITOR_APP = "chill_monitor";
    private static final String DEFAULT_UTC_FORMAT = "HH:mm:ss";
    private static final String DEFAULT_LST_FORMAT = "SOL-xxxx'M'HH:mm:ss.SSS";
    
    private static final String VIEW_BLOCK = PROPERTY_PREFIX + "views.";
    
    private static final String INTERNAL_VIEW_BLOCK = PROPERTY_PREFIX + "internal.views.";
    
    private final Map<ViewType, ViewProperties> viewPropertiesMap = new HashMap<ViewType, ViewProperties>(); 

    /**
     * Test constructor
     * 
     * @param baseFileName
     *            The property file to load
     */
    public PerspectiveProperties(final String baseFileName) {
        this(baseFileName, new SseContextFlag());
    }

    /**
     * Get the default Perspective Properties
     * 
     * @param baseFileName
     *            The property file to load
     * @param sseFlag
     *            The SSE context flag
     */
    public PerspectiveProperties(final String baseFileName, final SseContextFlag sseFlag) {
        super(baseFileName, sseFlag);
    }
    
    /**
     * Test constructor
     */
    public PerspectiveProperties() {
        this(PROPERTY_FILE);
        
    }

    /**
     * The default Perspective Properties constructor
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public PerspectiveProperties(final SseContextFlag sseFlag) {
        this(PROPERTY_FILE, sseFlag);

    }

    public String getDownlinkApplicationName() {
        return getProperty(DOWNLINK_APP_PROPERTY, DEFAULT_DOWNLINK_APP);
    }

    public String getUplinkApplicationName() {
        return getProperty(UPLINK_APP_PROPERTY, DEFAULT_UPLINK_APP);
    }

    public String getMonitorApplicationName() {
        return getProperty(MONITOR_APP_PROPERTY, DEFAULT_MONITOR_APP);
    }

    public String getDefaultPerspectiveName() {
        return getProperty(DEFAULT_PERSPECTIVE_NAME_PROPERTY, DEFAULT_PERSPECTIVE_NAME);
    }

    public List<String> getDefaultApplications() {
        return getListProperty(DEFAULT_APPLICATIONS_PROPERTY, DEFAULT_APPLICATIONS, ",");

    }

    public String getDefaultPerspectiveFile() {
        return getProperty(DEFAULT_PERSPECTIVE_FILE_PROPERTY, DEFAULT_PERSPECTIVE_FILE);
    }

    public int getMaxPlotViews() {
        return getIntProperty(MAX_PLOT_VIEWS_PROPERTY, 100);
    }

    public int getMaxPlotTraces() {
        return getIntProperty(MAX_PLOT_TRACES_PROPERTY, 120);
    }

    public int getMaxChannelViews() {
        return getIntProperty(MAX_CHANNEL_VIEWS_PROPERTY, 150);
    }

    public int getMaxEvrViews() {
        return getIntProperty(MAX_EVR_VIEWS_PROPERTY, 15);
    }

    public int getMaxFixedViews() {
        return getIntProperty(MAX_FIXED_VIEWS_PROPERTY, 150);  
    }

    public int getMaxProductViews() {
        return getIntProperty(MAX_PRODUCT_VIEWS_PROPERTY, 5);
    }

    public int getMaxAlarmViews() {
        return getIntProperty(MAX_ALARM_VIEWS_PROPERTY, 5);
    }

    public int getMaxViews() {
        return getIntProperty(MAX_VIEWS_PROPERTY, 200);
    }

    public boolean isViewCountCheckEnabled() {
        return getBooleanProperty(VALIDATE_VIEWCOUNT_PROPERTY, false);
    }
    
    public List<String> getViewDirectories() {
        return getListProperty(VIEW_DIRS_PROPERTY, null, ",");
    }
    
    public List<String> getPerspectiveDirectories() {
        return getListProperty(PERSPECTIVE_DIRS_PROPERTY, null, ",");
    }
    
    
    public List<String> getUtcTimeFormats() {
        return getListProperty(UTC_TIME_FORMATS_PROPERTY, DEFAULT_UTC_FORMAT, ",");
        
        
    }
    
    public List<String> getLstTimeFormats() {
        return getListProperty(LST_TIME_FORMATS_PROPERTY, DEFAULT_LST_FORMAT, ",");
     
    }
    
    public List<String> getHeaderNames() {
        return getListProperty(HEADERS_PROPERTY, null, ",");
    }
    
    public ViewProperties getViewProperties(final ViewType vt) {
        
        if (viewPropertiesMap.containsKey(vt)) {
            return viewPropertiesMap.get(vt);
        }
        final String prefix = VIEW_BLOCK + vt.toString().replaceAll(" ", "_");
        Map<String, String> viewProps = getMatchingProperties(prefix + "[.].*");
        final Map<String, Object> viewPropMap = new HashMap<String, Object>();
        for (final String key : viewProps.keySet()) {
            final Object val = viewProps.get(key);
            viewPropMap.put(key.substring(prefix.length() +1), val);
        }
        final String internalPrefix = INTERNAL_VIEW_BLOCK + vt.toString().replaceAll(" ", "_");
        viewProps = getMatchingProperties(internalPrefix + "[.].*");
        for (final String key : viewProps.keySet()) {
            final Object val = viewProps.get(key);
            viewPropMap.put(key.substring(internalPrefix.length() +1), val);
        }
        
        final ViewProperties vp = new ViewProperties(vt, viewPropMap);
        viewPropertiesMap.put(vt, vp);
        return vp;
    }
    
    public Map<ViewType, ViewProperties> getViewPropertiesMap() {
        return Collections.unmodifiableMap(this.viewPropertiesMap);
    }
    
    public void addProperties(final PerspectiveProperties p) {
        this.properties.putAll(p.properties);
        
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
