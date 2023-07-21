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
package jpl.gds.monitor.guiapp.gui.views;

public class WarningUtil {

    private static boolean widthWarningShown = false;

    public WarningUtil() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets the static flag indicating whether a table width warning from the
     * perspective has been displayed once in the entire GUI.
     * @return true if warning has been displayed, false if not
     */
    public synchronized static boolean getWidthWarningShown() {
    	return widthWarningShown;
    }

    /**
     * Sets the static flag indicating whether a table width warning from the
     * perspective has been displayed once in the entire GUI.
     * @param set true if warning has been displayed, false if not
     */
    public synchronized static void setWidthWarningShown(boolean set) {
    	widthWarningShown = set;
    }

}
