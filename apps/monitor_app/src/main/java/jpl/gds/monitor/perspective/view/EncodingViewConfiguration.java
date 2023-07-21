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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * EncodingWatchViewConfiguration encapsulates the configuration for the
 * Encoding Watch display.
 */
public class EncodingViewConfiguration extends ViewConfiguration {

    /**
     * Encoding view table name
     */
    public static final String ENCODING_TABLE_NAME = "Encoding";

    /**
     * Virtual Channel ID type column name
     */
    public static final String ENCODING_VCID_TYPE_COLUMN = "VC/TYPE";
    
    /**
     * Count column name
     */
    public static final String ENCODING_COUNT_COLUMN = "Count";
    
    /**
     * Last sequence column name
     */
    public static final String ENCODING_SEQ_COLUMN = "Last Seq";
    
    /**
     * Last Earth Receive Time column name
     */
    public static final String ENCODING_ERT_COLUMN = "Last ERT";
    
    /**
     * Bad count column name
     */
    public static final String ENCODING_BAD_COUNT_COLUMN = "Bad Frames";
    
    /**
     * Error count column name
     */
    public static final String ENCODING_ERROR_COUNT_COLUMN = "Errors";

    /**
     * Array of table column names
     */
    @SuppressWarnings( { "MALICIOUS_CODE", "MS_PKGPROTECT" })
    public static final String[] encodingTableCols = new String[] {
            ENCODING_VCID_TYPE_COLUMN, ENCODING_COUNT_COLUMN,
            ENCODING_BAD_COUNT_COLUMN, ENCODING_ERROR_COUNT_COLUMN,
            ENCODING_SEQ_COLUMN, ENCODING_ERT_COLUMN, };

    /**
     * Creates an instance of EncodingViewConfiguration.
     */
    public EncodingViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.ENCODING_WATCH),
                "jpl.gds.monitor.guiapp.gui.views.EncodingWatchComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.EncodingWatchTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.EncodingWatchPreferencesShell");
        addTable(createFrameTable());
    }

    private ChillTable createFrameTable() {
        final ChillTable table = ChillTable.createTable(ENCODING_TABLE_NAME,
                viewProperties, encodingTableCols);

        table.setSortColumn(ENCODING_VCID_TYPE_COLUMN);
        return table;
    }
}
