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
package jpl.gds.monitor.perspective.view;

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * MessageListViewConfiguration encapsulates the configuration for the general
 * message list display.
 *
 */
public class MessageListViewConfiguration extends ViewConfiguration {
    
    /**
     * Messages table name
     */
    public static final String MESSAGE_TABLE_NAME = "Message";
    
    private static final String DEFAULT_ROWS_PROPERTY = "defaultRows";
    private static final String ALLOWED_TYPES_PROPERTY = "allowedMessageTypes";
    private static final String MAX_ROWS_CONFIG = "maxRows";
    private static final String MESSAGE_FILTER_CONFIG = "messageTypes";
    private static final int DEFAULT_MAX_ROWS = 500;
    
    /**
     * Type column name
     */
    public static final String TYPE_COLUMN = "Type";
    
    /**
     * Event time column name
     */
    public static final String EVENT_TIME_COLUMN = "Event Time";
    
    /**
     * Summary column name
     */
    public static final String SUMMARY_COLUMN = "Summary";
        
    private String[] allowedMessageTypes;
    
    private static final String[] messageTableCols = new String[] {
        TYPE_COLUMN,
        EVENT_TIME_COLUMN,
        SUMMARY_COLUMN,
    };
    
    private String[] filters;
    
    /**
     * Creates an instance of MessageListViewConfiguration.
     */
    public MessageListViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }
    
    /**
     * Gets the array of allowed message types. A value of null indicates that all 
     * message types are allowed.
     * 
     * @return array of all allowed message types
     */
    public String[] getAllowedMessageTypes() {
        return allowedMessageTypes;   
    }
    
    /**
     * Gets the message types as an array
     * 
     * @return Returns the message types to display.
     */
    public String[] getMessageTypes() {
        final String types = this.getConfigItem(MESSAGE_FILTER_CONFIG);
        if (types != null) {
            filters = types.split(",");
        }
        return filters;
    }
    
    /**
     * Sets message types to display.
     *
     * @param types The message types to display as an array of Strings.
     */
    public void setMessageTypes(final String[] types) {
        filters = types;
        if (filters != null) {
            final StringBuffer typeString = new StringBuffer();
            for ( int i = 0; i < types.length; i++ ) {
                typeString.append(types[i].trim());
                if ( i != types.length - 1) {
                    typeString.append(",");
                }
            }
            this.setConfigItem(MESSAGE_FILTER_CONFIG, typeString.toString());
        } else {
            this.removeConfigItem(MESSAGE_FILTER_CONFIG);
        }
    }
    
    /**
     * Gets the maximum number of rows/messages to display at one time.
     * @return the number of rows
     */
    public int getMaxRows() {
        final String str = this.getConfigItem(MAX_ROWS_CONFIG);
        if (str == null) {
            return DEFAULT_MAX_ROWS;
        }
        return Integer.parseInt(str);
    }
    
    /**
     * Sets the maximum number of rows/messages to display at one time.
     * @param rows the number of rows to set
     */
    public void setMaxRows(final int rows) {
        this.setConfigItem(MAX_ROWS_CONFIG, String.valueOf(rows));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults()
    {
        initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.MESSAGE),
                "jpl.gds.monitor.guiapp.gui.views.MessageListComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.MessageListTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.MessageListPreferencesShell");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults(final ViewProperties props, final String viewClass, final String tabClass, final String prefClass) {
        super.initToDefaults(props, viewClass, tabClass, prefClass);

        final int max = props.getIntegerDefault(DEFAULT_ROWS_PROPERTY, DEFAULT_MAX_ROWS);
        setMaxRows(max);
        addTable(createMessageTable());
//        String typesStr = props.getStringDefault(MESSAGE_FILTER_CONFIG);
//        typesStr = typesStr.replaceAll("\n", "");
//        if (typesStr == null) {
//            setMessageTypes(null);
//        } else {
//            String[] filterSet = typesStr.split(",");
//            setMessageTypes(filterSet);
//        }
        String typesStr = props.getStringDefault(ALLOWED_TYPES_PROPERTY);
        typesStr = typesStr.replaceAll("\n", "");
        typesStr = typesStr.replaceAll(" ", "");
        if (typesStr == null) {
            setMessageTypes(null);
            return;
        }
        allowedMessageTypes = typesStr.split(",");
        setMessageTypes(allowedMessageTypes);
    }
    

    private ChillTable createMessageTable() {
    	return ChillTable.createTable(MESSAGE_TABLE_NAME, 
    			viewProperties, 
    			messageTableCols);
    }
}
