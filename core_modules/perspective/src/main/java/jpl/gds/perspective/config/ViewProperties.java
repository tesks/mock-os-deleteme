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

import java.util.List;
import java.util.Map;

public class ViewProperties {
    
    private static final String VIEW_CONFIG_CLASS_PROPERTY = "viewConfigClass";
    private static final String VIEW_DEFAULT_BLOCK = "defaults.";
      
    private final Map<String, Object> viewProperties;
    private final ViewType viewType;
    
    public ViewProperties(final ViewType vt, final Map<String, Object> viewPropMap) {
        this.viewProperties = viewPropMap;
        this.viewType = vt;
    }
    
    /**
     * Retrieves the name of and creates an instance of the view Class from the GDS
     * configuration file.
     * @return the Class object, or null if the proper class could not 
     * be determined.
     */
    public Class<?> getViewConfigurationClass() {

        final String viewConfigClass = (String) viewProperties.get(VIEW_CONFIG_CLASS_PROPERTY);
        if (viewConfigClass == null) {
            throw new IllegalStateException("No view configuration class defined for view type " + viewType);
        }
        try {
            return Class.forName(viewConfigClass);
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("View configuration class " + viewConfigClass + " was not found");
        }
    }
    
    public String getStringDefault(final String defaultFieldName) {
        return (String)viewProperties.get(VIEW_DEFAULT_BLOCK + defaultFieldName);
    }
    
    public String getStringDefault(final String defaultFieldName, final String defaultVal) {
        final String val = (String)viewProperties.get(VIEW_DEFAULT_BLOCK + defaultFieldName);
        return val == null ? defaultVal : val;
    }
    
    public boolean getBooleanDefault(final String defaultFieldName, final boolean defaultVal) {
        final String val = getStringDefault(defaultFieldName, String.valueOf(defaultVal));
        return Boolean.valueOf(val);
    }
    
    public int getIntegerDefault(final String defaultFieldName, final int defaultVal) {
        final String val = getStringDefault(defaultFieldName, String.valueOf(defaultVal));
        return Integer.valueOf(val);
    }
    
    public class TableProperties {
        private boolean defaultShowColHeader;
        private boolean defaultShowRowHeader;
        private boolean defaultAllowSort;
        private List<Integer> defaultColWidths;
        private List<String> defaultColumns;
        private List<String> deprecatedColumns;
        private List<String> defaultSortTypes;
        
    }

    public ViewType getViewType() {
        return this.viewType;
    }

}
