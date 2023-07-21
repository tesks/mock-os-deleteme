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
package jpl.gds.session.config.gui;

import jpl.gds.session.config.SessionConfiguration;

/**
 * This interface is implemented by each of the panels that appear as 
 * ExpandItem contents in the session configuration window.
 * 
 */
public interface ISessionConfigPanel {

	/**
	 * Updates the SessionConfiguration object containing the data backing 
	 * this GUI object.
	 * 
	 * @param sc SessionConfiguration object to set
	 */
	public abstract void setSessionConfiguration(SessionConfiguration sc);

	/**
	 * Sets GUI fields from the values in the SessionConfiguration object
	 * already established in this object. Also calls restoreNetworkSettings().
	 * Does nothing if the whole composite is not enabled.
	 * 
	 * This method should never make changes to the SessionConfiguration
	 * or HostConfiguration object.
	 */
	public abstract void setFieldsFromData();

	/**
     * Sets fields in the current SessionConfiguration and global
     * HostConfigruation objects from the content of the GUI fields.
     * All GUI fields MUST BE validated with a call to validateFields()
     * before this method is called. This method does nothing if the 
     * whole composite is not enabled. 
     * 
     * It is extremely important that this method does nothing that
     * updates GUI fields or otherwise triggers GUI events. Insidious
     * loops result.
     */
	public abstract void setDataFromFields();

	/**
     * Initializes GUI fields to the default values at at startup. 
     * Does nothing if the whole composite is not enabled.
     */
	public abstract void setDefaultFieldValues();

    /**
     * Validates the content of all GUI fields and displays specific
     * error dialogs to the user if any are found to be in error.
     * Does nothing if the whole composite is not enabled.
     * 
     * @return true if all values passed validation; false if not
     */
	public abstract boolean validateInputFields();

}