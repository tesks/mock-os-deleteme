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

package jpl.gds.monitor.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.widgets.Display;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * This class is used to manage global configuration values, such as plot
 * refresh rate, in the real-time monitor.
 */
public class MonitorConfigValues {

    private static MonitorConfigValues instance;
    private final Map<GlobalPerspectiveParameter,Object> theValueMap;
    private final CopyOnWriteArrayList<IMonitorConfigChangeListener> listeners = 
            new CopyOnWriteArrayList<IMonitorConfigChangeListener>();
	private final MonitorGuiProperties guiProps;
	private final SclkFormatter sclkFmt;
    
    public enum SclkFormat {
        DECIMAL,
        SUBTICK;
    }

    /**
     * Creates an instance of a MonitorConfigValues for managing integer default values.
     */
    public MonitorConfigValues(final MonitorGuiProperties guiProps, final TimeProperties tc) {
    	this.guiProps = guiProps;
    	sclkFmt = tc.getSclkFormatter();
        theValueMap = new HashMap<GlobalPerspectiveParameter,Object>();
        initDefaults();
    }

    /**
     * Initialize defaults.
     */ 
    private void initDefaults() {
        // Get the default parameter for the defaultResetFlushInterval from GdsSystemConfig.xml
        final long defaultFlushValue = guiProps.getDefaultFlushInterval();
        addValue(GlobalPerspectiveParameter.
                CHANNEL_ALARM_UPDATE_RATE, defaultFlushValue);
        addValue(GlobalPerspectiveParameter.
                CHANNEL_LIST_UPDATE_RATE, defaultFlushValue);
        addValue(GlobalPerspectiveParameter.
                CHANNEL_PLOT_UPDATE_RATE, defaultFlushValue);
        addValue(GlobalPerspectiveParameter.
                FIXED_VIEW_STALENESS_INTERVAL, Long.valueOf(-1));


        addValue(GlobalPerspectiveParameter.SCLK_FORMAT, sclkFmt.usesFractional() ? 
                SclkFormat.DECIMAL : SclkFormat.SUBTICK);
    }

    /**
     * Stores a new value as the current working value in the configuration table.
     * This routine also will set the default if the specified key does not currently
     * have an associated value. Registered change listeners are notified of the change.
      * 
     * @param key the key name of the configuration value to set
     * @param value the value to set
     */
    public void addValue ( final GlobalPerspectiveParameter key, final Object value ) {
        theValueMap.put ( key, value );

        if (listeners.isEmpty()) {
        	return;
        }

        /* Notify listeners on the SWT thread in case they have to do GUI things. */
        Display.getDefault().asyncExec(new Runnable() {
            /**
             * {@inheritDoc}
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                try {
                    for (final IMonitorConfigChangeListener l : listeners) {
                        l.globalConfigurationChange(key, value);
                    }              

                } catch (final Exception e) {
                    e.printStackTrace();
                    TraceManager.getDefaultTracer().error("In MonitorConfigValues addValue: Ignoring exception: " + e.getMessage());

                } 
            }
        });
    }

    /**
     * Returns the value associated with the given key.
     * 
     * @param key
     *            key for the configuration value to find
     * @return value for the configuration parameter identified by the key, or
     *         null if not found.
     */
    public Object getValue ( final GlobalPerspectiveParameter key ) {
        return theValueMap.get ( key );
    }

    /**
     * Returns true if the key exists in table of configuration values.
     * 
     * @param key the key name of the configuration value to find
     * @return boolean true if the key exists in the current value table
     * 
     * Note: This method can return null.
     * 
     */
    public boolean keyExists(final GlobalPerspectiveParameter key ) {
        return theValueMap.containsKey ( key );
    }
    
    /**
     * Adds a configuration change listener.
     * 
     * @param l
     *            IMonitorConfigChangeListener to add
     */
    public void addListener(final IMonitorConfigChangeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /**
     * Removes a configuration change listener.
     * 
     * @param l
     *            IMonitorConfigChangeListener to remove
     */
    public void removeListener(final IMonitorConfigChangeListener l) {
        listeners.remove(l);
    }
}