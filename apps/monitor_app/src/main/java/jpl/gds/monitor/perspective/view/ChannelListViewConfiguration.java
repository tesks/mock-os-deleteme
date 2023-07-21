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

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
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
 * ChannelListViewConfiguration encapsulates the configuration for the channel
 * list view.
 */
public class ChannelListViewConfiguration extends ViewConfiguration implements StationSupport, RealtimeRecordedSupport {
    /**
     * Channel list view table name
     */
    public static final String CHANNEL_TABLE_NAME = "Channel";

    private static final String CHANNEL_SET_CONFIG = "channelSet";

    private static final String DEFAULT_MONOSPACE_CONFIG = "useMonospace";
    private static final String RECORDED_DATA_CONFIG = "displayIsRecordedData";
    private static final String STATION_CONFIG = "stationId";

    /**
     * Channel ID column name
     */
    public static final String CHAN_ID_COLUMN = "ID";

    /**
     * Channel title column name
     */
    public static final String CHAN_TITLE_COLUMN = "Title";

    /**
     * Channel module column name
     */
    public static final String CHAN_MODULE_COLUMN = "Module";

    /**
     * Flight software column name
     */
    public static final String CHAN_FSW_COLUMN = "FSW Name";

    /**
     * Raw data column name
     */
    public static final String CHAN_DN_COLUMN = "Raw";

    /**
     * Raw unit column name
     */
    public static final String CHAN_DN_UNITS_COLUMN = "Raw Unit";

    /**
     * Engineering unit column name
     */
    public static final String CHAN_EU_COLUMN = "Value";

    /**
     * Engineering unit units column name
     */
    public static final String CHAN_EU_UNITS_COLUMN = "Unit";

    /**
     * Earth Receive Time column name
     */
    public static final String CHAN_ERT_COLUMN = "ERT";

    /**
     * Spacecraft clock column name
     */
    public static final String CHAN_SCLK_COLUMN = "SCLK";

    /**
     * Spacecraft event time column name
     */
    public static final String CHAN_SCET_COLUMN = "SCET";

    /**
     * Alarm state column name
     */
    public static final String CHAN_ALARM_COLUMN = "Alarm State";

    /**
     * Local Solar Time column name
     */
    public static final String CHAN_LST_COLUMN = "LST";

    /**
     * DSS ID column
     */
    public static final String CHAN_DSS_COLUMN = "DSS ID";

    /**
     * Recorded column
     */
    public static final String CHAN_RECORDED_COLUMN = "Recorded";

    private static final String[] chanTableCols = new String[] {
            CHAN_ID_COLUMN, CHAN_TITLE_COLUMN, CHAN_MODULE_COLUMN,
            CHAN_FSW_COLUMN, CHAN_DN_COLUMN, CHAN_DN_UNITS_COLUMN,
            CHAN_EU_COLUMN, CHAN_EU_UNITS_COLUMN, CHAN_ERT_COLUMN,
            CHAN_SCLK_COLUMN, CHAN_SCET_COLUMN, CHAN_ALARM_COLUMN,
            CHAN_LST_COLUMN, CHAN_DSS_COLUMN, CHAN_RECORDED_COLUMN };

    private ChannelSet channels;

    /**
     * Creates an instance of ChannelListViewConfiguration.
     */
    public ChannelListViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Sets the flag indicating whether to use monospace font in the channel
     * list.
     * 
     * @param enable
     *            true for monospace font, false if not
     */
    public void setUseMonospaceFont(final boolean enable) {
        this.setConfigItem(DEFAULT_MONOSPACE_CONFIG, String.valueOf(enable));
    }

    /**
     * Gets the flag indicating whether to use a monospaced font.
     * 
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
            TraceManager.getDefaultTracer().warn("Non-integer station ID " + stationStr + " found in Channel List View Configuration");

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
     * Gets the channels currently selected in the channel view
     * @param defProv the channel definition provider
     * 
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
     * @param channels
     *            The channels to set.
     */
    public void setChannels(final ChannelSet channels) {
        this.channels = channels;
        if (this.channels != null) {
            this.setConfigItem(CHANNEL_SET_CONFIG, "<![CDATA["
                    + this.channels.toString() + "]]>");
        } else {
            this.removeConfigItem(CHANNEL_SET_CONFIG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.CHANNEL_LIST),
                "jpl.gds.monitor.guiapp.gui.views.ChannelListPageComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.ChannelListTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.ChannelListPreferencesShell");

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
        /*
         * New channel list views should default to
         * displaying realtime data only.
         */
        setRealtimeRecordedFilterType(RealtimeRecordedFilterType.REALTIME);
        addTable(createChannelTable());
    }

    private ChillTable createChannelTable() {
        return ChillTable.createTable(CHANNEL_TABLE_NAME, viewProperties, chanTableCols);
    }
}
