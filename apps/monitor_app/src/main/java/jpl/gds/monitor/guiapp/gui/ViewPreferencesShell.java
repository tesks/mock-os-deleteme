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
package jpl.gds.monitor.guiapp.gui;

import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ChillShell;


/**
 * ViewPreferencesShell is an interface to be implemented by all preferences
 * controls for views.
 *
 */
public interface ViewPreferencesShell extends ChillShell {
    
    /**
     * Set values in the preferences control from the given view configuration.
     * @param config the ViewConfiguration
     */
   public void setValuesFromViewConfiguration(IViewConfiguration config);
   
   /**
    * Set values from the preferences control into the given view configuration.
    * @param config the ViewConfiguration
    */
   public void getValuesIntoViewConfiguration(IViewConfiguration config);
}
