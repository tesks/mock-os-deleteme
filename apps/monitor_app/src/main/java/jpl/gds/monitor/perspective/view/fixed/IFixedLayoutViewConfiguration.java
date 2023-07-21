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

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.ChannelViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.perspective.view.IViewConfigParser;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.CoordinateSystemType;

public interface IFixedLayoutViewConfiguration extends IViewConfiguration, ChannelViewConfiguration, StationSupport,
        RealtimeRecordedSupport {
    /**
     * Default fixed page font
     */
    public static final ChillFont DEFAULT_FONT = new ChillFont("Courier,10,NORMAL");
    
    /** Staleness interval, seconds */
    public static final int DEFAULT_STALENESS_INTERVAL = 600;

    /**
     * Gets the fields defined in this fixed view.
     * 
     * @return List of FixedFieldConfiguration objects
     */
    public List<IFixedFieldConfiguration> getFieldGroupConfigs();

    /**
     * Sets the list of fields defined in this fixed view.
     * 
     * @param fieldConfigs List of FixedFieldConfiguration objects to set
     */
    public void setFieldConfigs(List<IFixedFieldConfiguration> fieldConfigs);

    /**
     * Adds a field to this fixed view.
     * 
     * @param toAdd List of FixedFieldConfiguration object to add
     */
    public void addField(IFixedFieldConfiguration toAdd);

    /**
     * {@inheritDoc}
     */
    public List<IFixedFieldConfiguration> getFieldConfigs();

    /**
     * Removes the given field configuration from the view.
     * 
     * @param field the FixedFieldConfiguration to remove
     */
    public void removeFieldConfig(IFixedFieldConfiguration field);

    /**
     * Sets the list of conditions defined in this fixed view.
     * 
     * @param conditions HashMap of channelId and condition pairs
     */
    public void setConditionConfigs(Map<String, IConditionConfiguration> conditions);

    /**
     * Adds a ConditionConfiguration to the map of conditions.
     * 
     * @param condId condition ID string
     * @param condition ConditionConfiguration to add
     */
    public void addConditionConfig(String condId, IConditionConfiguration condition);

    /**
     * Retrieves the coordinate system used to position the fields
     * in the fixed layout view.
     * 
     * @return the CoordinateSystemType
     */
    public CoordinateSystemType getCoordinateSystem();

    /**
     * Sets the coordinate system used to position the fields in the
     * fixed layout view.
     * 
     * @param locationType type of coordinate system (character or pixel)
     */
    public void setCoordinateSystem(CoordinateSystemType locationType);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toXML();

    /**
     * Gets the list of channel IDs referenced by dynamic fields in this view. Dynamic 
     * fields are those that may change after their initial drawing, forcing them to 
     * be redrawn when new data arrives.
     * 
     * @return List of channel identifiers, or the empty list, if none found
     */
    public List<String> getDynamicReferencedChannelIds();

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.ChannelViewConfiguration#getReferencedChannelIds()
     */
    @Override
    public List<String> getReferencedChannelIds();

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.ChannelViewConfiguration#containsNullChannelIds()
     */
    @Override
    public boolean containsNullChannelIds();

    /**
     * Adds the given mission name to the list of missions this view applies to.
     *  
     * @param mission the mission to add
     */
    public void addMission(String mission);

    /**
     * Returns the list of missions this view applies to.
     * 
     * @return list of mission identifiers, or null if none defined.
     */
    public List<String> getMissions();

    /**
     * Clears the list of missions this view applies to.
     */
    public void clearMissions();

    /**
     * Sets the staleness interval to sint seconds.
     * 
     * @param sint interval in seconds, or -1 to clear the setting
     */
    public void setStalenessInterval(int sint);

    /**
     * Returns the staleness interval in seconds.
     * 
     * @return interval, or -1 if none defined.
     */
    public int getStalenessInterval();

    /**
     * Moves the given field configuration towards the rear of the list of field configurations.
     * @param config the FixedFieldConfiguration to move
     * 
     * @param allTheWay true to move the field all the way to the front of the list;
     * false to move only one position
     * 
     * TODO: Change this method to take a move counter instead of the boolean
     */
    public void moveFieldTowardsRear(IFixedFieldConfiguration config, boolean allTheWay);

    /**
     * Moves the given field configuration towards index 0 of the list of field
     * configurations. in this view. Note that items on the list are drawn in
     * order starting at index 0 when they are place on the canvas.
     * <br>
     * If the given config is not in the list, no action is taken and no error is
     * generated.
     *
     * @param config the FixedFieldConfiguration to move
     * @param allTheWay true to move the field all the way to the front of the
     *            list; false to move it only one position
     * 
     */
    public void moveFieldTowardsZero(IFixedFieldConfiguration config, boolean allTheWay);

    /**
     * @{inheritDoc}
     */
    @Override
    public IViewConfigParser getParser(ApplicationContext appContext);

}
