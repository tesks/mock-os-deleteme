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
package jpl.gds.context.api;

import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * An interface to be implemented by context identification classes.
 * 
 *
 * @since R8
 *
 */
public interface IContextIdentification extends StaxSerializable, Templatable {

	/**
	 * Gets the context key associated with this context identification.
	 * 
	 * @return context key
	 */
	public IContextKey getContextKey();
	
	/**
     * Sets the current spacecraft ID.
     * 
     * @param scid numeric spacecraft ID
     */
    public void setSpacecraftId(Integer scid);

    /**
     * Gets the current spacecraft ID.
     * 
     * @return current spacecraft ID; never null
     */
    public Integer getSpacecraftId();

	/**
	 * Returns the user who created this configuration.
	 * 
	 * @return the session user
	 */
	public abstract String getUser();

	/**
	 * Sets the user who created this configuration.
	 * 
	 * @param user
	 *            The session user to set.
	 */
	public abstract void setUser(String user);

	/**
	 * Returns the end time (Timestamp) for the time range that this
	 * configuration was in use. May be null if the use of this configuration
	 * has not ended.
	 * 
	 * @return end time
	 */
    public abstract IAccurateDateTime getEndTime();

	/**
	 * Returns the endTime (Timestamp) as an ISO formatted String.
	 * May be null if use of this configuration has not ended.
	 * 
	 * @return end time string
	 */
	public abstract String getEndTimeStr();

	/**
	 * Sets the end time for the time range that this configuration was in use.
	 * Has the side effect of setting the end time string.
	 * 
	 * @param inEndTime
	 *            The session end time (Timestamp) to set.
	 */
    public abstract void setEndTime(IAccurateDateTime inEndTime);

	/**
	 * Returns the configuration start time.
	 * 
	 * @return start time
	 */
    public abstract IAccurateDateTime getStartTime();

	/**
	 * Returns the start time formatted as an Iso string.
	 * 
	 * @return start time string
	 */
	public abstract String getStartTimeStr();

	/**
	 * Sets the configuration start time. Has the side effect of setting the
	 * start time string.
	 * 
	 * @param inStartTime
	 *            The Timestamp to set.
	 */
    public abstract void setStartTime(IAccurateDateTime inStartTime);

	/**
	 * Returns the configuration description. This field may be used as desired by users.
	 * May be null if the user supplied no description.
	 * 
	 * @return description string
	 */
	public abstract String getDescription();

	/**
	 * Sets the configuration description. This field may be used as desired by users.
	 * The characters &,>,<, and % will be replaced by spaces.
	 * 
	 * @param desc
	 *            The description to set.
	 */
	public abstract void setDescription(String desc);

	/**
	 * Returns the configuration name. Should not be null for a valid configuration.
	 * 
	 * @return configuration name
	 */
	public abstract String getName();

	/**
	 * Sets the configuration name.
	 * 
	 * @param name
	 *            The name to set.
	 */
	public abstract void setName(String name);

	/**
	 * Returns the configuration type. This field may be used as desired by users. 
	 * May be null if the user supplied no type for the configuration.
	 * 
	 * @return type
	 */
	public abstract String getType();

	/**
	 * Sets the configuration type.  This field may be used as desired by users.
	 * The characters &,>,<, and % will be replaced by spaces.
	 * 
	 * @param type
	 *            The type to set.
	 */
	public abstract void setType(String type);

	/**
	 * Gets a unique key for the session. The key is only guaranteed
	 * unique if all the session identification fields are set.
	 * 
	 * @return the key as a String
	 */
	public abstract String getFullName(); 
	
	   /**
     * Clear fields needed to start a new context.
     */
    public abstract void clearFieldsForNewConfiguration();

    /**
     * Copies values into this context identification from another instance.
     * 
     * @param ck the instance containing values to copy
     */
    public abstract void copyValuesFrom(IContextIdentification ck);

    /**
     * Gets the string representation of the context key.
     * 
     * @return key string
     */
    public abstract String getContextId();

    /**
     * Indicates whether any fields in the key have changed since the
     * last time a metadata header was requested from it.
     * 
     * @return true if dirty, false if not
     */
    public boolean isDirty();
    
    /**
     * Returns the ID number the database assigned to this context.
     * 
     * @return ID number
     */
    public abstract Long getNumber();

    /**
     * Returns the host that initiated this context.
     * 
     * @return host
     */
    public abstract String getHost();

    /**
     * Returns the ID number of the host. This number is assigned 
     * by the database for each unique host encountered.
     * 
     * @return host identifier
     */
    public abstract Integer getHostId();

    /**
     * Sets the host that initiated this context.
     * 
     * @param host
     *            The host to set.
     */
    public abstract void setHost(String host);


    /**
     * Sets the host ID number. This number is assigned 
     * by the database for each unique host encountered.
     * 
     * @param hostId the host identifier to set
     */
    public abstract void setHostId(Integer hostId);

    /**
     * Sets the ID number as assigned by the database to this context.
     * 
     * @param key
     *            The ID number supplied by the database.
     */
    public abstract void setNumber(Long key);

    /**
     * Sets the fragment ID as assigned by the database to this context.
     * 
     * @param fragment The fragment supplied by the database
     */
    public abstract void setFragment(Integer fragment);

    /**
     * Returns the fragment ID the database assigned to this 
     * context.
     * 
     * @return fragment ID holder
     */
    public abstract Integer getFragment();
    
    /**
     * Gets the metadata header for this object.
     * 
     * @return the populated metadata header
     */
    public ISerializableMetadata getMetadataHeader();

    /** 
     * Determines if the supplied data item should be accepted given the current
     * configured context filters.
     * 
     * @param toFilter the data item to check
     * @return true if the given data item passes the filter, false if not
     */
    public boolean accept(IFilterableDataItem toFilter);

	
}