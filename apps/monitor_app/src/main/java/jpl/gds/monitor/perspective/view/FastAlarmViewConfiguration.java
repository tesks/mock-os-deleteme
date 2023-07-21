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

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.perspective.view.channel.AlarmFilter;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * FastAlarmViewConfiguration encapsulates the configuration for the new
 * alarm view. This class replaces AlarmViewConfiguration.
 */
public class FastAlarmViewConfiguration extends ViewConfiguration implements StationSupport, RealtimeRecordedSupport {
	/**
	 * Fast alarm view table name
	 */
	public static final String ALARM_TABLE_NAME = "Alarm";

	private static final String CHANNEL_SET_CONFIG = "channelSet";
	private static final String LEVEL_CONFIG = "levelFilter";
	private static final String SUBSYSTEM_CONFIG = "subsystem";
	private static final String MODULE_CONFIG = "module";
	private static final String CATEGORY_CONFIG = "opsCategory";
	private static final String FLUSH_TIME_CONFIG = "resetFlushTime";

	private static final String DEFAULT_FLUSH_CONFIG = "defaultResetFlushInterval";
	private static final String DEFAULT_MONOSPACE_CONFIG = "useMonospace";
	private static final String RECORDED_DATA_CONFIG = "displayIsRecordedData";
	private static final String STATION_CONFIG = "stationId";

	/**
	 * Alarm ID column name
	 */
	public static final String ALARM_ID_COLUMN = "ID";

	/**
	 * Title column name
	 */
	public static final String ALARM_TITLE_COLUMN = "Title";

	/**
	 * Module column name
	 */
	public static final String ALARM_MODULE_COLUMN = "Module";

	/**
	 * Flight software column name
	 */
	public static final String ALARM_FSW_COLUMN = "FSW Name";

	/**
	 * Data number column name
	 */
	public static final String ALARM_DN_COLUMN = "Raw";

	/**
	 * Data number units column name
	 */
	public static final String ALARM_DN_UNITS_COLUMN = "Raw Unit";

	/**
	 * Engineering unit column name
	 */
	public static final String ALARM_EU_COLUMN = "Value";

	/**
	 * Engineering unit units column name
	 */
	public static final String ALARM_EU_UNITS_COLUMN = "Unit";

	/**
	 * Earth Receive Time column name
	 */
	public static final String ALARM_ERT_COLUMN = "ERT";

	/**
	 * Spacecraft clock column name
	 */
	public static final String ALARM_SCLK_COLUMN = "SCLK";

	/**
	 * Spacecraft event time column name
	 */
	public static final String ALARM_SCET_COLUMN = "SCET";

	/**
	 * Earth receive time for when channel went in alarm column name
	 */
	public static final String ALARM_IN_ERT_COLUMN = "In Alarm ERT";

	/**
	 * Earth receive time for when channel stopped being in alarm column name
	 */
	public static final String ALARM_OUT_ERT_COLUMN = "Out Alarm ERT";

	/**
	 *  Alarm state column name
	 */
	public static final String ALARM_ALARM_COLUMN = "Alarm State";

	/**
	 *  Local solar time column name
	 */
	public static final String ALARM_LST_COLUMN = "LST";

	/**
	 *  Data source station ID column name
	 */
	public static final String ALARM_DSS_COLUMN = "DSS ID";

	/**
	 *  Realtime/Recorded flag column name
	 */
	public static final String ALARM_RECORDED_COLUMN = "Recorded";


	private static final String[] chanTableCols = new String[] {
		ALARM_ID_COLUMN,
		ALARM_TITLE_COLUMN,
		ALARM_MODULE_COLUMN,
		ALARM_FSW_COLUMN,
		ALARM_DN_COLUMN,
		ALARM_DN_UNITS_COLUMN,
		ALARM_EU_COLUMN,
		ALARM_EU_UNITS_COLUMN,
		ALARM_ERT_COLUMN,
		ALARM_SCLK_COLUMN,
		ALARM_SCET_COLUMN,
		ALARM_IN_ERT_COLUMN,
		ALARM_OUT_ERT_COLUMN,
		ALARM_ALARM_COLUMN,
		ALARM_LST_COLUMN,
		ALARM_DSS_COLUMN,
		ALARM_RECORDED_COLUMN
	};

	private ChannelSet channels;

	/**
	 * Creates an instance of FastAlarmViewConfiguration.
	 * @param appContext the current application context
	 */
	public FastAlarmViewConfiguration(final ApplicationContext appContext) {
		super(appContext);
		initToDefaults();
	}

	/**
	 * Sets the flag indicating whether to use monospace font in the channel list.
	 * @param enable true for monospace font, false if not
	 */
	public void setUseMonospaceFont(final boolean enable) {
		this.setConfigItem(DEFAULT_MONOSPACE_CONFIG, String.valueOf(enable));
	}

	/**
	 * Gets the flag indicating whether to use a monospaced font.
	 * @return true if using monospaced font; false if not
	 */
	public boolean getUseMonospace() {
		final String boolStr = this.getConfigItem(DEFAULT_MONOSPACE_CONFIG);
		if (boolStr == null) {
			setUseMonospaceFont(false);
			return false;
		} else {
			return Boolean.valueOf(boolStr);
		}  
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.StationSupport#getStationId()
	 */
	@Override
	@ToDo("Figure out how to implement in a common location")
	public int getStationId() {
		final String stationStr = this.getConfigItem(STATION_CONFIG);
		if (stationStr == null) {
			return StationIdHolder.UNSPECIFIED_VALUE;
		}
		try {
			return Integer.valueOf(stationStr);
		} catch (final NumberFormatException e) {
			TraceManager.getDefaultTracer().warn("Non-integer station ID " + stationStr + " found in Alarm View Configuration");

			return StationIdHolder.UNSPECIFIED_VALUE;
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.StationSupport#setStationId(int)
	 */
	@Override
	@ToDo("Figure out how to implement in a common location")
	public void setStationId(final int station) {
		this.setConfigItem(STATION_CONFIG, String.valueOf(station));
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#setRealtimeRecordedFilterType(jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType)
	 */
	@Override
	@ToDo("Figure out how to implement in a common location")
	public void setRealtimeRecordedFilterType(final RealtimeRecordedFilterType filterType) {
		this.setConfigItem(RECORDED_DATA_CONFIG, String.valueOf(filterType));
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#getRealtimeRecordedFilterType()
	 */
	@Override
	@ToDo("Figure out how to implement in a common location")
	public RealtimeRecordedFilterType getRealtimeRecordedFilterType() {
		final String str = this.getConfigItem(RECORDED_DATA_CONFIG);
		if (str == null) {
			return RealtimeRecordedFilterType.REALTIME;
		}
		RealtimeRecordedFilterType result = null;
		/*
		 * Old perspective will have
		 * a true/false value for this config item rather than
		 * the new enum value. Detect this and convert here.
		 */
		try {
			result = RealtimeRecordedFilterType.valueOf(str);
		} catch (final IllegalArgumentException e) {
			if (str.equals(Boolean.TRUE.toString())) {
				result = RealtimeRecordedFilterType.RECORDED;
			} else {
				result = RealtimeRecordedFilterType.REALTIME;
			} 
			// Set the converted value back into the config so it
			// will be saved properly.
			this.setConfigItem(RECORDED_DATA_CONFIG, result.toString());
		}

		return result;
	}

	/**
	 * Sets the alarm level filter. Only alarms at this level will be shown on the display.
	 * @param level alarm filter level. can be red, yellow or none
	 */
	public void setLevelFilter(final AlarmLevel level) {
		if (level == null) {
			this.removeConfigItem(LEVEL_CONFIG);
		} else {
			this.setConfigItem(LEVEL_CONFIG, level.toString());
		}
	}

	/**
	 * Gets the alarm level filter, if any. Only alarms at this level will be shown on the display.
	 * @return the AlarmLevel the current alarm filter, can be red, yellow or none
	 */
	public AlarmLevel getLevelFilter() {
		final String levelStr = getConfigItem(LEVEL_CONFIG);
		if (levelStr == null) {
			return null;
		} else {
			return Enum.valueOf(AlarmLevel.class, levelStr);
		}
	}

	/**
	 * Sets the channel subsystem filter.
	 * @param subsystem the current subsystem filter
	 */
	public void setSubsystemFilter(final String subsystem) {
		if (subsystem == null) {
			this.removeConfigItem(SUBSYSTEM_CONFIG);
		} else {
			this.setConfigItem(SUBSYSTEM_CONFIG, subsystem);
		}
	}

	/**
	 * Gets the channel subsystem filter, if any.
	 * @return the current subsystem filter
	 */
	public String getSubsystemFilter() {
		return this.getConfigItem(SUBSYSTEM_CONFIG);
	}

	/**
	 * Sets the channel operational category filter.
	 * @param subsystem the current subsystem filter
	 */
	public void setOpsCategoryFilter(final String subsystem) {
		if (subsystem == null) {
			this.removeConfigItem(CATEGORY_CONFIG);
		} else {
			this.setConfigItem(CATEGORY_CONFIG, subsystem);
		}
	}

	/**
	 * Gets the channel module filter.
	 * @return the current module filter
	 */
	public String getModuleFilter() {
		return this.getConfigItem(MODULE_CONFIG);
	}

	/**
	 * Sets the channel module filter.
	 * @param module the current module filter
	 */
	public void setModuleFilter(final String module) {
		if (module == null) {
			this.removeConfigItem(MODULE_CONFIG);
		} else {
			this.setConfigItem(MODULE_CONFIG, module);
		}
	}

	/**
	 * Gets the channel operational category filter.
	 * @return the current operational category filter
	 */
	public String getOpsCategoryFilter() {
		return this.getConfigItem(CATEGORY_CONFIG);
	}

	/**
	 * Gets the channels currently selected in this config
	 * @param defProv the channel definition provider
	 * @return Returns the channel set.
	 */
	public ChannelSet getChannels(final IChannelDefinitionProvider defProv) {
		String chans = this.getConfigItem(CHANNEL_SET_CONFIG);
		if (chans != null) {
			this.channels = new ChannelSet();
			if (chans.equalsIgnoreCase("<![CDATA[]]>")) {
				return this.channels;
			} else if (chans.startsWith("<![CDATA[")) {
				chans = chans.substring(9, chans.length() - 3);
			}
			this.channels.loadFromString(defProv, chans);
		}
		return this.channels;
	}

	/**
	 * Sets the channel set.
	 *
	 * @param channels The channels to set.
	 */
	public void setChannels(final ChannelSet channels) {
		this.channels = channels;
		if (this.channels != null) {
			this.setConfigItem(CHANNEL_SET_CONFIG, "<![CDATA[" + this.channels.toString() + "]]>");
		} else {
			this.removeConfigItem(CHANNEL_SET_CONFIG);
		}
	}

	/**
	 * Sets the time (in minutes) that cleared alarm channels should be left on the display.
	 * @param interval the flush interval in minutes;  a value of 0 means never flush cleared alarms
	 */
	public void setResetFlushInterval(final int interval) {
		this.setConfigItem(FLUSH_TIME_CONFIG, String.valueOf(interval));
	}

	/**
	 * Gets the time (in minutes) that cleared alarm channels should be left on the display.
	 * @return the flush interval in minutes; a value of 0 means never flush cleared alarms
	 */
	public int getResetFlushInterval() {
		final String valStr = this.getConfigItem(FLUSH_TIME_CONFIG);
		if (valStr == null) {
			return 0;
		} else {
			return Integer.parseInt(valStr);
		}
	}

	/**
	 * Gets an AlarmViewFilter object containing the entire set of alarm filter criteria.
	 * @param defProv the channel definition provider
	 * @return a new AlarmViewFilter object
	 */
	public AlarmFilter getAlarmFilter(final IChannelDefinitionProvider defProv) {
		return new AlarmFilter(getChannels(defProv), getSubsystemFilter(), getModuleFilter(), getOpsCategoryFilter(), getLevelFilter());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initToDefaults()
	{
	    super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.FAST_ALARM),
                "jpl.gds.monitor.guiapp.gui.views.FastAlarmComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.FastAlarmTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.FastAlarmPreferencesShell");
	    
		setChannels(new ChannelSet());
		final ChillFont f = new ChillFont();
		f.setSize(ChillFont.FontSize.SMALL);
		if (this.getConfigItem(DEFAULT_MONOSPACE_CONFIG) == null) {
			final boolean useMonospace = viewProperties.getBooleanDefault(DEFAULT_MONOSPACE_CONFIG, true);
			this.setUseMonospaceFont(useMonospace);
			if (useMonospace) {
				f.setFace(ChillFont.MONOSPACE_FACE);
			}
		}
		setDataFont(f);
		setResetFlushInterval(viewProperties.getIntegerDefault(DEFAULT_FLUSH_CONFIG, 0));
		/*
		 * New alarm views should default to
		 * displaying realtime data only.
		 */
		setRealtimeRecordedFilterType(RealtimeRecordedFilterType.REALTIME);
		addTable(createAlarmTable());
	}  

	private ChillTable createAlarmTable() {
		final ChillTable table = ChillTable.createTable(ALARM_TABLE_NAME, 
		        viewProperties,
				chanTableCols);
		table.deprecateColumn(table.getColumnIndex(ALARM_IN_ERT_COLUMN));
		table.deprecateColumn(table.getColumnIndex(ALARM_OUT_ERT_COLUMN));
		return table;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDeprecations() {	
		final ChillTable table = getTable(ALARM_TABLE_NAME);
		table.deprecateColumns(viewProperties, chanTableCols);
	}
}
