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
package jpl.gds.shared.metadata.context;

import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.template.Templatable;


/**
 * An interface to be implemented by objects playing the role of a context key,
 * which is used in metadata, message headers, and database objects to identify 
 * their context.
 * 
 *
 * @since R8
 */
public interface IContextKey extends Templatable {
	
	/**
	 * String used to separate elements in the context key when generating
	 * a string ID for it.
	 */
	public static final String ID_SEPARATOR = "/";

	/**
	 * Clear fields needed to start a new context.
	 */
	public abstract void clearFieldsForNewConfiguration();

	/**
	 * Copies values into this context key from another instance.
	 * 
	 * @param ck the instance containing key values to copy
	 */
	public abstract void copyValuesFrom(IContextKey ck);

	/**
	 * Gets the string representation of the context key.
	 * 
	 * @return key string
	 */
	public abstract String getContextId();

    /**
     * Gets the "session number" representation of the current context key
     * 
     * @return Context ID without session host or session part fields
     */
    public int getShortContextId();

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
	 * Returns the "parent" ID number assigned to this context.
	 * For example, a context that later creates a session can set the context as parent
	 *
	 * @return ID number
	 */
	public Long getParentNumber();


	/**
	 * Returns the parent ID number of the host. This number is assigned
	 * by the database for each unique host encountered.
	 *
	 *
	 * @return ID number
	 */
	public Integer getParentHostId();

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
	 * Sets the parent host ID number. This number is assigned
	 * by the database for each unique host encountered.
	 *
	 * @param hostId the host identifier to set
	 *
	 */
	public abstract void setParentHostId(Integer hostId);

	/**
	 * Sets the ID number as assigned by the database to this context.
	 * 
	 * @param key
	 *            The ID number supplied by the database.
	 */
	public abstract void setNumber(Long key);

	/**
	 * Sets the "parent" ID number as assigned by the database to this context.
	 *
	 * @param key
	 *            The ID number supplied by the database.
	 *
	 */
	public void setParentNumber(Long key);

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
	 * Gets the type of context
	 *
	 * @return type of context
	 */
    public ContextConfigurationType getType();

	/**
	 * Sets the type of context
	 *
	 * @return type of context
	 */
    public void setType(ContextConfigurationType type);

}