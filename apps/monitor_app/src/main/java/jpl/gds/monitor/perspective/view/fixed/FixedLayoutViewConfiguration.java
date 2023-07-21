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
package jpl.gds.monitor.perspective.view.fixed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.ChannelViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfigParser;
import jpl.gds.perspective.view.ViewConfiguration;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * FixedLayoutViewConfiguration encapsulates the configuration values for a
 * fixed layout channel view.
 */
public class FixedLayoutViewConfiguration extends ViewConfiguration implements IFixedLayoutViewConfiguration {

	private static final String RECORDED_DATA_CONFIG = "showRecorded";
	private static final String STALENESS_CONFIG = "stalenessInterval";
	private static final String MISSION_CONFIG = "missions";
	private static final String COORDINATE_TYPE_CONFIG = "coordinateType";
	private static final String STATION_CONFIG = "stationId";

	// The list of fixed field configurations this view contains.
	private List<IFixedFieldConfiguration> fieldConfigs = new ArrayList<IFixedFieldConfiguration>();

	// The list of conditions this view contains
	private Map<String, IConditionConfiguration> conditions = new HashMap<String, IConditionConfiguration>();

	// There are no other members. That is because all simple configuration values are
	// stored in a Hash table in the ViewConfiguration class and automatically written as
	// XML element.  Please continue that trend wherever possible.

	/**
	 * Creates an instance of FixedLayoutViewConfiguration.
	 * @param appContext the current application context
	 */
	public FixedLayoutViewConfiguration(final ApplicationContext appContext) {
		super(appContext);
		initToDefaults();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<IFixedFieldConfiguration> getFieldGroupConfigs() {
		return fieldConfigs;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setFieldConfigs(
			final List<IFixedFieldConfiguration> fieldConfigs) {
		this.fieldConfigs = new ArrayList<IFixedFieldConfiguration>(fieldConfigs);
		for (final IFixedFieldConfiguration config: this.fieldConfigs) {
			config.setCoordinateSystem(getCoordinateSystem());
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addField(final IFixedFieldConfiguration toAdd) {
		fieldConfigs.add(toAdd);
		toAdd.setCoordinateSystem(getCoordinateSystem());
	}


	/**
     * {@inheritDoc}
     */
    @Override
    public List<IFixedFieldConfiguration> getFieldConfigs() {
		return new ArrayList<IFixedFieldConfiguration>(fieldConfigs);
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public void removeFieldConfig(final IFixedFieldConfiguration field) {
		fieldConfigs.remove(field);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setConditionConfigs(
			final Map<String, IConditionConfiguration> conditions) {
		this.conditions = new HashMap<String, IConditionConfiguration>(conditions);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void addConditionConfig(final String condId, final IConditionConfiguration condition) {
	    this.conditions.put(condId, condition);
	}


	/**
     * {@inheritDoc}
     */
    @Override
    public CoordinateSystemType getCoordinateSystem() {
		final String str = this.getConfigItem(COORDINATE_TYPE_CONFIG);
		if (str == null) {
			return CoordinateSystemType.PIXEL;
		}
		return Enum.valueOf(CoordinateSystemType.class, str);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setCoordinateSystem(final CoordinateSystemType locationType) {
		this.setConfigItem(COORDINATE_TYPE_CONFIG, locationType.toString());
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
			TraceManager.getDefaultTracer().warn("Non-integer station ID " + stationStr + " found in Fixed View Configuration");

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
	 * {@inheritDoc}
	 */
	@Override
	protected void initToDefaults()
	{
	    initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.FIXED_LAYOUT),
                "jpl.gds.monitor.guiapp.gui.views.FixedLayoutComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.FixedLayoutTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.FixedLayoutPreferencesShell");
	    
		final ChillFont f = new ChillFont("Courier,10,NORMAL");
		setDataFont(f);
		this.setDisplayViewTitle(false);
		/*
		 * Fixed views should default to
		 * displaying realtime data only.
		 */
		setRealtimeRecordedFilterType(RealtimeRecordedFilterType.REALTIME);
		this.setCoordinateSystem(CoordinateSystemType.CHARACTER);
        addMission(GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()));
	}


	/**
     * {@inheritDoc}
     */
    @Override
	public String toXML() {
		final StringBuilder result = new StringBuilder();

		if (this.isReference()) {
			result.append(reference.toXml());
		} else {
			result.append("<" + VIEW_TAG + " " + VIEW_NAME_TAG + "=\"" + StringEscapeUtils.escapeXml(viewName) + "\" ");
			result.append(VIEW_TYPE_TAG + "=\"" + viewType.getValueAsString() + "\" ");
			result.append(VIEW_VERSION_TAG + "=\"" + WRITE_VERSION + "\">\n");
			result.append(getAttributeXML());
			result.append(getConfigItemXML());
			result.append(getConditionXML());
			result.append(getFieldXML());
			result.append("</" + VIEW_TAG + ">\n");
		}

		return result.toString();
	}

	private String getConditionXML() {
		final StringBuilder result = new StringBuilder();

		final Iterator<Map.Entry<String, IConditionConfiguration>> it = conditions.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<String, IConditionConfiguration> pairs = 
					it.next();
			result.append(pairs.getValue().toXML());
		}

		return result.toString();
	}

	private String getFieldXML() {
		final StringBuilder result = new StringBuilder();
		for (final IFixedFieldConfiguration config: fieldConfigs) {
			result.append(config.toXML());
		}
		return result.toString();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<String> getDynamicReferencedChannelIds() {

		final ArrayList<String> result = new ArrayList<String>();

		for (final IFixedFieldConfiguration config: fieldConfigs) {
			if (config instanceof ChannelViewConfiguration && !config.isStatic()) {
				result.addAll(((ChannelViewConfiguration)config).getReferencedChannelIds());

			} else if (config instanceof HeaderFieldConfiguration) {

				final List<IFixedFieldConfiguration> childConfigs = ((HeaderFieldConfiguration)config).getFieldConfigs();
				for (final IFixedFieldConfiguration childConfig: childConfigs) {
					if (childConfig instanceof ChannelViewConfiguration && !config.isStatic()) {
						result.addAll(((ChannelViewConfiguration)childConfig).getReferencedChannelIds());
					}
				}
			}
		}
		return result;
	}

	/**
     * {@inheritDoc}
     */
    @Override
	public List<String> getReferencedChannelIds() {
		final ArrayList<String> result = new ArrayList<String>();
		for (final IFixedFieldConfiguration config: fieldConfigs) {
			if (config instanceof ChannelViewConfiguration) {
				result.addAll(((ChannelViewConfiguration)config).getReferencedChannelIds());
			}
		}
		return result;
	}

	/**
     * {@inheritDoc}
     */
    @Override
	public boolean containsNullChannelIds() {
		boolean hasNullChanId = false;
		for (final IFixedFieldConfiguration config: fieldConfigs) {
			if (config instanceof ChannelViewConfiguration) {
				hasNullChanId = ((ChannelViewConfiguration)config).containsNullChannelIds();
				if(hasNullChanId) {
					return hasNullChanId;
				}
			}
		}
		return hasNullChanId;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addMission(final String mission) {
		final String missions = this.getConfigItem(MISSION_CONFIG);

		final StringBuilder allMissions = new StringBuilder();
        if (missions == null) {
			allMissions.append(mission);
		} else {
		    allMissions.append(missions);
		    allMissions.append(',');
		    allMissions.append(mission);        
		}
		this.setConfigItem(MISSION_CONFIG, allMissions.toString());
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<String> getMissions() {

		final String str = this.getConfigItem(MISSION_CONFIG);
		if (str == null) {
            return null;
		}
		final ArrayList<String> missions = new ArrayList<String>();
		final String[] pieces = str.split(",");
		for (final String piece: pieces) {
			missions.add(piece);
		}
		return missions;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void clearMissions() {
		this.removeConfigItem(MISSION_CONFIG);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setStalenessInterval(final int sint) {
		this.setConfigItem(STALENESS_CONFIG, String.valueOf(sint));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getStalenessInterval() {
		final String str = this.getConfigItem(STALENESS_CONFIG);
		if (str == null) {
			return DEFAULT_STALENESS_INTERVAL;
		}
		return Integer.valueOf(str);
	}   

	/**
     * {@inheritDoc}
     */
	@Override
    public void moveFieldTowardsRear(final IFixedFieldConfiguration config, final boolean allTheWay) {
		int index = fieldConfigs.indexOf(config);
		if (config == null) {
			return;
		}
		boolean moved = true;
		while (moved) {
			moved = false;
			if (index < fieldConfigs.size() - 1) {
				final IFixedFieldConfiguration swap = fieldConfigs.get(index + 1);
				fieldConfigs.set(index, swap);
				fieldConfigs.set(index + 1, config);
				moved = true;
			}
			if (!allTheWay) {
				moved = false;
			}
			index = fieldConfigs.indexOf(config);
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void moveFieldTowardsZero(final IFixedFieldConfiguration config, final boolean allTheWay) {
		if (config == null) {
			return;
		}
		int index = fieldConfigs.indexOf(config);
		if (index == -1) {
			return;
		}
		boolean moved = true;
		while (moved) {
			moved = false;
			if (index > 0) {
				final IFixedFieldConfiguration swap = fieldConfigs.get(index - 1);
				fieldConfigs.set(index, swap);
				fieldConfigs.set(index - 1, config);
				moved = true;
			}
			if (!allTheWay) {
				moved = false;
			}
			index = fieldConfigs.indexOf(config);
		}
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public IViewConfigParser getParser(final ApplicationContext appContext) {
        return new FixedViewParseHandler(appContext, this);
    }
}
