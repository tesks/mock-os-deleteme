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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * NatEvrViewConfiguration encapsulates the configuration for the NAT EVR message
 * display.
 */
public class NatEvrViewConfiguration extends ViewConfiguration implements RealtimeRecordedSupport {

    private static final String LEVELS_CONFIG = "levels";
    private static final String SOURCES_CONFIG = "sources";
    private static final String MODULES_CONFIG = "modules";
    private static final String RECORDED_DATA_CONFIG = "displayIsRecordedData";
    private static final String COLOR_CODING_CONFIG = "useColorCoding";

    /**
     * Event record table name
     */
    public static final String EVR_TABLE_NAME = "Evr";

    private static final String MARK_COLOR_PROPERTY = "defaultMarkColor";
    
    /**
     * Row color when an EVR is marked
     */
    public static final String DEFAULT_MARK_COLOR = "0,204,255"; // deep sky
                                                                 // blue, picked
                                                                 // by none
                                                                 // other than
                                                                 // Kyran
                                                                 // Owen-Mankovich
    private static final String MARK_COLOR = "markColor";

    private static final String  MAX_ROWS                     = "maxRows";

    private int                  defaultMaxRows;

    /**
     * EVR source column name
     */
    public static final String EVR_SOURCE_COLUMN = "Source";
    
    /**
     * EVR name column name
     */
    public static final String EVR_NAME_COLUMN = "Name";
    
    /**
     * EVR ID column name
     */
    public static final String EVR_ID_COLUMN = "ID";
    
    /**
     * Earth receive time column name
     */
    public static final String EVR_ERT_COLUMN = "ERT";
    
    /**
     * Spacecraft clock column name
     */
    public static final String EVR_SCLK_COLUMN = "SCLK";
    
    /**
     * Spacecraft event time column name
     */
    public static final String EVR_SCET_COLUMN = "SCET";
    
    /**
     * EVR module column name
     */
    public static final String EVR_MODULE_COLUMN = "Module";
    
    /**
     * EVR level column name
     */
    public static final String EVR_LEVEL_COLUMN = "Level";
    
    /**
     * Evr message column name
     */
    public static final String EVR_MESSAGE_COLUMN = "Message";
    
    /**
     * Task column name
     */
    public static final String EVR_TASK_COLUMN = "Task Name";
    
    /**
     * Sequence ID column name
     */
    public static final String EVR_SEQUENCE_COLUMN = "Seq ID";
    
    /**
     * Sequence ID category column name
     */
    public static final String EVR_CATEGORY_SEQUENCE_COLUMN = "Category Seq ID";
    
    /**
     * Local solar time column name
     */
    public static final String EVR_LST_COLUMN = "LST";

    /**
     * Recorded column name.
     */
    public static final String EVR_RECORDED_COLUMN = "Recorded";
    
    /**
     * Virtual channel ID column name.
     */
    public static final String EVR_VCID_COLUMN = "VCID";
    
    /**
     * RCT (record creation time) column name.
     */
    public static final String EVR_RCT_COLUMN = "RCT";
    
    /**
     * Operational category column name.
     */
    public static final String EVR_OPS_CAT_COLUMN = "Ops Cat";
    
    /**
     * DSS ID column name,
     */
    public static final String EVR_DSS_ID_COLUMN = "DSS ID";
    
    /**
     * Session ID column name.
     */
    public static final String EVR_SESSION_ID_COLUMN = "Session ID";
    
    /**
     * Session host column name.
     */
    public static final String EVR_SESSION_HOST_COLUMN = "Session Host";
    
    /**
     * Array of the EVR view column names
     */
    @SuppressWarnings( { "MALICIOUS_CODE", "MS_PKGPROTECT" })    
    public static final String[] evrTableCols = new String[] {
            EVR_SOURCE_COLUMN, EVR_NAME_COLUMN, EVR_ID_COLUMN, EVR_ERT_COLUMN,
            EVR_SCLK_COLUMN, EVR_SCET_COLUMN, EVR_MODULE_COLUMN,
            EVR_LEVEL_COLUMN, EVR_MESSAGE_COLUMN, EVR_TASK_COLUMN,
            EVR_SEQUENCE_COLUMN, EVR_CATEGORY_SEQUENCE_COLUMN, EVR_LST_COLUMN,
            EVR_RECORDED_COLUMN, EVR_RCT_COLUMN, EVR_VCID_COLUMN, 
            EVR_DSS_ID_COLUMN, EVR_OPS_CAT_COLUMN, EVR_SESSION_ID_COLUMN, 
            EVR_SESSION_HOST_COLUMN };

    private static final int     DEFAULT_MAX_ROWS             = 1000;

    /**
     * Creates an instance of NatEvrViewConfiguration.
     */
    public NatEvrViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Gets the selected EVR levels (i.e. Fatal, Diagnostic, etc.)
     * 
     * @return Returns the EVR levels selected for display.
     */
    public List<String> getLevels() {
        final String types = this.getConfigItem(LEVELS_CONFIG);
        if (types != null) {
            final String[] selected = types.split(",");
            return Arrays.asList(selected);
        }
        return new LinkedList<String>();
    }

    /**
     * Sets EVR levels selected for display.
     * 
     * @param levels
     *            The selected EVR levels as List of Strings.
     */
    public void setLevels(final List<String> levels) {
        if (levels != null && !levels.isEmpty()) {
            final StringBuffer typeString = new StringBuffer();
            for (int i = 0; i < levels.size(); i++) {
                typeString.append(levels.get(i));
                if (i != levels.size() - 1) {
                    typeString.append(',');
                }
            }
            this.setConfigItem(LEVELS_CONFIG, typeString.toString());
        } else {
            this.removeConfigItem(LEVELS_CONFIG);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#setRealtimeRecordedFilterType(jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType)
     */
    @Override
    public void setRealtimeRecordedFilterType(final RealtimeRecordedFilterType filterType) {
    	this.setConfigItem(RECORDED_DATA_CONFIG, String.valueOf(filterType));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#getRealtimeRecordedFilterType()
     */
    @Override
    public RealtimeRecordedFilterType getRealtimeRecordedFilterType() {
    	final String str = this.getConfigItem(RECORDED_DATA_CONFIG);
    	if (str == null) {
    		return RealtimeRecordedFilterType.REALTIME;
    	}
    	RealtimeRecordedFilterType result = null;

    	try {
    		result = RealtimeRecordedFilterType.valueOf(str);
    	} catch (final IllegalArgumentException e) {
    		if (str.equals(Boolean.TRUE.toString())) {
    			result = RealtimeRecordedFilterType.RECORDED;
    		} else {
    			result = RealtimeRecordedFilterType.REALTIME;
    		} 

    		this.setConfigItem(RECORDED_DATA_CONFIG, result.toString());
    	}

    	return result;
    }
 
    /**
     * Indicates whether this view is to perform color coding by EVR level.
     * 
     * @param coded
     *            true to use color coding, false if not
     */
    public void setUseColorCoding(final boolean coded) {
        this.setConfigItem(COLOR_CODING_CONFIG, String.valueOf(coded));
    }

    /**
     * Gets the flag indicating if view is to perform color coding by EVR level.
     * 
     * @return true for color coding, false for no color coding
     */
    public boolean isUseColorCoding() {
        final String str = this.getConfigItem(COLOR_CODING_CONFIG);
        if (str == null) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 
     * Sets EVR sources selected for display.
     * 
     * @param srcs
     *            The selected EVR sources as a List of Strings.
     */
    public void setSources(final List<String> srcs) {
        if (srcs != null && !srcs.isEmpty()) {
            final StringBuffer typeString = new StringBuffer();
            for (int i = 0; i < srcs.size(); i++) {
                typeString.append(srcs.get(i));
                if (i != srcs.size() - 1) {
                    typeString.append(',');
                }
            }
            this.setConfigItem(SOURCES_CONFIG, typeString.toString());
        } else {
            this.removeConfigItem(SOURCES_CONFIG);
        }
    }

    /**
     * Gets the EVR sources for display.
     * 
     * @return Returns the EVR sources selected for display.
     */
    public List<String> getSources() {
        final String types = this.getConfigItem(SOURCES_CONFIG);
        if (types != null) {
            final String[] selected = types.split(",");
            return Arrays.asList(selected);
        }
        return new LinkedList<String>();
    }

    /**
     * Sets EVR modules selected for display.
     * 
     * @param modules
     *            The selected EVR modules as a List of Strings.
     */
    public void setModules(final List<String> modules) {
        if (modules != null && !modules.isEmpty()) {
            final StringBuffer typeString = new StringBuffer();
            for (int i = 0; i < modules.size(); i++) {
                typeString.append(modules.get(i));
                if (i != modules.size() - 1) {
                    typeString.append(',');
                }
            }
            this.setConfigItem(MODULES_CONFIG, typeString.toString());
        } else {
            this.removeConfigItem(MODULES_CONFIG);
        }
    }

    /**
     * Gets the selected EVR modules as a List of strings
     * 
     * @return Returns the EVR modules selected for display.
     */
    public List<String> getModules() {
        final String types = this.getConfigItem(MODULES_CONFIG);
        if (types != null) {
            final String[] selected = types.split(",");
            return Arrays.asList(selected);
        }
        return new LinkedList<String>();
    }

    /**
     * Gets the EVR entry mark color.
     * 
     * @return the color in "<red integer>,<green integer>,<blue integer>"
     *         string.
     */
    public String getMarkColor() {
        final String str = this.getConfigItem(MARK_COLOR);
        if (str == null) {
            return DEFAULT_MARK_COLOR;
        }
        return str;
    }

    /**
     * Sets the EVR entry mark color.
     * 
     * @param rgbStr
     *            Color in "<red integer>,<green integer>,<blue integer>"
     *            string.
     */
    public void setMarkColor(final String rgbStr) {
        this.setConfigItem(MARK_COLOR, rgbStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.EVR),
                "jpl.gds.monitor.guiapp.gui.views.NatEvrComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.NatEvrTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.NatEvrPreferencesShell");

        final String markColor = viewProperties.getStringDefault(MARK_COLOR_PROPERTY, DEFAULT_MARK_COLOR);
        setMarkColor(markColor);        
        setRealtimeRecordedFilterType(RealtimeRecordedFilterType.REALTIME);
        
        addTable(createEvrTable());
        defaultMaxRows = DEFAULT_MAX_ROWS;
    }

    /**
     * Creates the EVR ChillTable object.
     * 
     * @return ChillTable
     */
    private ChillTable createEvrTable() {
        final ChillTable table = ChillTable.createTable(EVR_TABLE_NAME, viewProperties, evrTableCols);
        table.setSortColumn(EVR_ERT_COLUMN);
        return table;
    }

    public int getMaxRows() {
        final String str = this.getConfigItem(MAX_ROWS);
        if (str == null) {
            return defaultMaxRows;
        }
        return Integer.parseInt(str);
    }

    /**
     * Get the maximum number of rows to display in the view.
     * 
     * @param maxRows
     */
    public void setMaxRows(final int maxRows) {
        this.setConfigItem(MAX_ROWS, String.valueOf(maxRows));
    }
}
