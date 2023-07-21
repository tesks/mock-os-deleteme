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
 * PacketWatchViewConfiguration encapsulates the configuration for the Packet Watch
 * display.
 *
 */
public class PacketWatchViewConfiguration extends ViewConfiguration {

	/**
	 * Packet Watch table name
	 */
	public static final String PACKET_TABLE_NAME = "Packet";

	/**
	 * Virtual channel ID column name
	 */
	public static final String PACKET_VCID_APID_COLUMN = "VC/APID";
	
	/**
	 * Packet name column name
	 */
	public static final String PACKET_NAME_COLUMN = "Name";
    
    /**
     * Packet count column name
     */
	public static final String PACKET_COUNT_COLUMN = "Count";
    
    /**
     * Packet sequence column name
     */
	public static final String PACKET_SEQ_COLUMN = "Last Seq";
    
    /**
     * Earth receive time column name
     */
	public static final String PACKET_ERT_COLUMN = "Last ERT";
    
    /**
     * Spacecraft clock column name
     */
	public static final String PACKET_SCLK_COLUMN = "Last SCLK";
    
    /**
     * Spacecraft event time column name
     */
	public static final String PACKET_SCET_COLUMN = "Last SCET";
    
    /**
     * Local solar time column name
     */
	public static final String PACKET_LST_COLUMN = "Last LST";

	/**
	 * Array of packet table column names
	 */
	@SuppressWarnings({"MALICIOUS_CODE","MS_PKGPROTECT"}) 
	public static final String[] packetTableCols = new String[] {
		PACKET_VCID_APID_COLUMN,
		PACKET_NAME_COLUMN,
		PACKET_COUNT_COLUMN,
		PACKET_SEQ_COLUMN,
		PACKET_ERT_COLUMN,
		PACKET_SCLK_COLUMN,
		PACKET_SCET_COLUMN,
		PACKET_LST_COLUMN
	};

	/**
	 * Creates an instance of PacketWatchViewConfiguration.
	 */
	public PacketWatchViewConfiguration(final ApplicationContext appContext) {
		super(appContext);
		initToDefaults();
	}

	/**
     * {@inheritDoc}
	 */
	@Override
	protected void initToDefaults() {
	    super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.PACKET_WATCH),
                "jpl.gds.monitor.guiapp.gui.views.PacketWatchComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.PacketWatchTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.PacketWatchPreferencesShell");

		addTable(createPacketTable());
	}

	private ChillTable createPacketTable() {
		final ChillTable table = ChillTable.createTable(PACKET_TABLE_NAME, 
		        viewProperties,
				packetTableCols);

		table.setSortColumn(PACKET_VCID_APID_COLUMN);
		return table;
	}
}
