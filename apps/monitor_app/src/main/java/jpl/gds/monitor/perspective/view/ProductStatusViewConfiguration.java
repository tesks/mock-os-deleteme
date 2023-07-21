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
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * ProductStatusViewConfiguration encapsulates the configuration for the 
 * product status display.
 *
 */
public class ProductStatusViewConfiguration extends ViewConfiguration {
    /**
     * Product status table name
     */
    public static final String PRODUCT_TABLE_NAME = "Product";
    
    private static final String FLUSH_INTERVAL_CONFIG = "flushInterval";
    private static final int DEFAULT_FLUSH = 10;
    
    /**
     * Product status column name
     */
    public static final String PRODUCT_STATUS_COLUMN = "Status";
    
    /**
     * Application ID column name
     */
    public static final String PRODUCT_APID_COLUMN = "APID";
    
    /**
     * Product type column name
     */
    public static final String PRODUCT_TYPE_COLUMN = "Type";
    
    /**
     * DVT column name
     */
    public static final String PRODUCT_SCLK_COLUMN = "DVT SCLK";
    
    /**
     * DVT spacecraft event time column name
     */
    public static final String PRODUCT_SCET_COLUMN = "DVT SCET";
    
    /**
     * Product creation time column name
     */
    public static final String PRODUCT_CREATE_TIME_COLUMN = "Creation Time";
    
    /**
     * Product filename column name
     */
    public static final String PRODUCT_FILENAME_COLUMN = "Filename";
    
    /**
     * Number of product parts column name
     */
    public static final String PRODUCT_PARTS_COLUMN = "# Parts";
    
    /**
     * Number of parts received column name
     */
    public static final String PRODUCT_PARTS_RECV_COLUMN = "# Recvd";
    
    /**
     * Last product part received column name
     */
    public static final String PRODUCT_LAST_PART_COLUMN = "Last Recvd";
    
    /**
     * DVT local solar time column name
     */
    public static final String PRODUCT_LST_COLUMN = "DVT LST";
     
    private static final String[] productTableCols = new String[] {
        PRODUCT_STATUS_COLUMN,
        PRODUCT_APID_COLUMN,
        PRODUCT_TYPE_COLUMN,
        PRODUCT_SCLK_COLUMN,
        PRODUCT_SCET_COLUMN,
        PRODUCT_CREATE_TIME_COLUMN,
        PRODUCT_FILENAME_COLUMN,
        PRODUCT_PARTS_COLUMN,
        PRODUCT_PARTS_RECV_COLUMN,
        PRODUCT_LAST_PART_COLUMN,
        PRODUCT_LST_COLUMN
    };
    
    /**
     * Creates an instance of ProductStatusViewConfiguration.
     */
    public ProductStatusViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }
        
    /**
     * Gets the flush interval time between flushes of the product view.
     * @return the flush interval in minutes
     */
    public int getFlushInterval() {
        final String str = this.getConfigItem(FLUSH_INTERVAL_CONFIG);
        if (str == null) {
            return DEFAULT_FLUSH;
        }
        return Integer.parseInt(str);
    }
    
    /**
     * Sets the flush interval time between flushes of the product view.
     * @param interval the flush interval in minutes
     */
    public void setFlushInterval(final int interval) {
        this.setConfigItem(FLUSH_INTERVAL_CONFIG, String.valueOf(interval));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults()
    {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.PRODUCT),
                "jpl.gds.monitor.guiapp.gui.views.ProductStatusComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.ProductStatusTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.ProductStatusPreferencesShell");
        final int flush = viewProperties.getIntegerDefault(FLUSH_INTERVAL_CONFIG, DEFAULT_FLUSH);
        setFlushInterval(flush);
        addTable(createProductTable());
    }
    
    private ChillTable createProductTable() {
    	final ChillTable table = ChillTable.createTable(PRODUCT_TABLE_NAME, 
    	        viewProperties, 
    			productTableCols);
    	table.setSortColumn(PRODUCT_APID_COLUMN);
    	return table;
    }
}
