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
package jpl.gds.perspective;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * AbstractUserSettings defines the basic methods for maintaining a properties
 * file full of settings. Specific settings classes should extend this class.
 * Settings are kept in a Java Properties object.
 * 
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractUserSettings extends Properties {

    /**
     * The path to the settings file, which is a java properties file.
     */
    private final String settingsFile;

    /**
     * Creates an AbstractUserSettings object that uses the given configuration
     * file to store settings.
     * 
     * @param filename
     *            path to the settings file
     */
    public AbstractUserSettings(final String filename) {
        super();
        this.settingsFile = filename;
        final File fp = new File(this.settingsFile);
        
        if (fp.exists()) {
        	FileInputStream fis = null;
            try {
                fis = new FileInputStream(
                        this.settingsFile);
                this.load(fis);
            } catch (final IOException e) {
                e.printStackTrace();
            } finally {
            	try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
    }

    /**
     * Gets the specified setting as a boolean value.
     * 
     * @param settingName
     *            name of the setting to fetch
     * @param defaultVal
     *            value to return if the setting is not defined
     * @return boolean value of the setting
     */
    protected boolean getBoolean(final String settingName,
            final boolean defaultVal) {
        final String str = getProperty(settingName);
        if (str == null) {
            return defaultVal;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * Gets the specified setting as an integer value.
     * 
     * @param settingName
     *            name of the setting to fetch
     * @param defaultVal
     *            value to return if the setting is not defined
     * @return integer value of the setting
     */
    protected int getInteger(final String settingName, final int defaultVal) {
        final String str = getProperty(settingName);
        if (str == null) {
            return defaultVal;
        }
        return Integer.parseInt(str);
    }

    /**
     * Saves the current settings to the settings file.
     * 
     */
    public void save() {
    	FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.settingsFile);
            this.store(fos, null);
            fos.close();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
        	try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    /**
     * Sets the specified boolean setting into the properties table.
     * 
     * @param settingName
     *            name of the setting to put
     * @param enable
     *            value of the setting
     */
    protected void setBoolean(final String settingName, final boolean enable) {
        setProperty(settingName, String.valueOf(enable));
    }

    /**
     * Sets the specified integer setting into the properties table.
     * 
     * @param settingName
     *            name of the setting to put
     * @param val
     *            value of the setting
     */
    protected void setInteger(final String settingName, final int val) {
        setProperty(settingName, String.valueOf(val));
    }
}
