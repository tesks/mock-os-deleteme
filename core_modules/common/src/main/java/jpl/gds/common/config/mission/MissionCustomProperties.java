package jpl.gds.common.config.mission;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * MissionCustomProperties manages the configuration properties that are wholly
 * unique to a mission and do not belong in any other property class.
 * 
 */
public class MissionCustomProperties extends GdsHierarchicalProperties{
	
	/** default property file name */
	public static final String PROPERTY_FILE = "mission_custom.properties";
	
	private static final String PROPERTY_PREFIX = "missionCustom.";
	
	private final List<String> keys;
	
	/**
     * test constructor
     */
	public MissionCustomProperties(){
        this(new SseContextFlag());
	}

    /**
     * default constructor, loads properties from missionCustom.properties file
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public MissionCustomProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        this.keys = new ArrayList<>(this.properties.stringPropertyNames());
    }
	
	/**
	 * Get the list of keys for the mission custom properties
	 * 
	 * @return A list of keys, as strings, that are present in the mission
	 *         custom properties
	 */
	public List<String> getKeys(){
		return this.keys;
	}
	
	@Override
	public String getProperty(final String propertyName){
		return super.getProperty(propertyName);
	}
	
	/**
	 * Simplified version of getBooleanProperty
	 * 
	 * @param propertyName
	 *            name of the property to be returned
	 * @return TRUE or FALSE
	 */
	public boolean getBooleanProperty(final String propertyName){
		return this.getBooleanProperty(propertyName, false);
	}
	
	/**
	 * Simplified version of getByteProperty
	 * 
	 * @param propertyName
	 *            name of the property value to be fetched
	 * @return the byte value of the property. -1 will be returned if the
	 *         property does not exist
	 */
	public byte getByteProperty(final String propertyName){
		return (byte)this.getIntProperty(propertyName);
	}
	
	/**
	 * Simplified version of getIntProperty
	 * 
	 * @param propertyName
	 *            name of the property value to be fetched
	 * @return the integer value of the property. -1 will be returned if the
	 *         property does not exist
	 */
	public int getIntProperty(final String propertyName){
		return this.getIntProperty(propertyName, -1);
	}
	
	/**
	 * Simplified version of getLongProperty
	 * 
	 * @param propertyName
	 *            name of the property value to be fetched
	 * @return the long value of the property. -1 will be returned if the
	 *         property does not exist
	 */
	public long getLongProperty(final String propertyName){
		return this.getLongProperty(propertyName, -1);
	}
	
	/**
	 * Simplified version of getDoubleProperty
	 * 
	 * @param propertyName
	 *            name of the property value to be fetched
	 * @return the double value of the property. -1 will be returned if the
	 *         property does not exist
	 */
	public double getDoubleProperty(final String propertyName){
		return this.getDoubleProperty(propertyName, -1);
	}
	
	/**
	 * Simplified version of getListProperty. Property values in the list must
	 * be separated by a single comma
	 * 
	 * @param propertyName
	 *            name of the property value to be fetched
	 * @return The list of String properties. null will be returned if the
	 *         property list does not exist
	 */
	public List<String> getListProperty(final String propertyName){
		return this.getListProperty(propertyName, null, ",");
	}
	
	/**
	 * Gets an array property value.
	 * 
	 * @param propertyName name of property
	 * @return list of values in the array, which may be empty
	 */
	public String[] getArrayProperty(final String propertyName){
		final List<String> props = this.getListProperty(propertyName);
		
		final String[] retVals = new String[props.size()];
		
		props.toArray(retVals);
		
		return retVals;
	}
	
	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}	
}