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
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * FrameWatchViewConfiguration encapsulates the configuration for the Frame Watch
 * display.
 *
 */
public class FrameWatchViewConfiguration extends ViewConfiguration {
	/**
	 * Virtual channel ID column name
	 */
	public static final String FRAME_VCID_TYPE_COLUMN = "VC/TYPE";

    /**
     * Frame count column name
     */
	public static final String FRAME_COUNT_COLUMN = "Count";

    /**
     * Frame sequence column name
     */
	public static final String FRAME_SEQ_COLUMN = "Last Seq";

    /**
     * Earth receive time column name
     */
	public static final String FRAME_ERT_COLUMN = "Last ERT";

	/**
	 * Array of frame table columns
	 */
	@SuppressWarnings({"MALICIOUS_CODE","MS_PKGPROTECT"}) 
	public static final String[] frameTableCols = new String[] {
		FRAME_VCID_TYPE_COLUMN
		,FRAME_COUNT_COLUMN
		,FRAME_SEQ_COLUMN
		,FRAME_ERT_COLUMN
	};

	/**
	 * Creates an instance of FrameWatchViewConfiguration.
	 */
	public FrameWatchViewConfiguration(final ApplicationContext appContext) {
		super(appContext);
		initToDefaults();
	}

	/**
     * {@inheritDoc}
	 */
	@Override
	protected void initToDefaults() {
	    super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.FRAME_WATCH),
                "jpl.gds.monitor.guiapp.gui.views.FrameWatchComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.FrameWatchTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.FrameWatchPreferencesShell");
		addTable(createFrameTable());
	}

	private ChillTable createFrameTable() {
		final ChillTable table = ChillTable.createTable(IDbTableNames.DB_FRAME_DATA_TABLE_NAME, viewProperties, frameTableCols);

		table.setSortColumn(FRAME_VCID_TYPE_COLUMN);
		return table;
	}
}
