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
package jpl.gds.shared.performance.gui;

import org.eclipse.swt.widgets.Composite;

import jpl.gds.shared.performance.HealthStatus;

/**
 * An extension of health status bar that assumes there is no real maximum
 * volume limit and uses a supplied critical volume level as the maximum volume.
 * The assumption is that when the current volume = critical volume, the
 * status bar is 100% full. Current volume label and tooltip are adjusted
 * from their behavior in the parent class, in that the current volume label
 * reflects actual current volume rather than percentage, and the tooltip
 * indicates there is no maximum size.
 * 
 */
public class UnboundedHealthStatusBarComposite extends
        HealthStatusBarComposite {


    /**
     * Constructs an UnboundedHealthBarStatusComposite with a title.
     * 
     * @param parent the parent Composite
     * @param critVolume the critical volume level, used as max volume (represents 100% full)
     * @param barWidth the pixel width of the actual color bar
     * @param unit the units in which volume is expressed; may be null
     * @param enableMonospace use fixed font rather than variable font for label text
     */
    public UnboundedHealthStatusBarComposite(Composite parent, int critVolume,
            int barWidth, String unit, boolean enableMonospace) {
        super(parent, critVolume, barWidth, unit);
    }
    
   /**
    * Constructs an UnboundedHealthBarStatusComposite with no title.
    * 
    * @param parent the parent Composite
    * @param title title text for the bar
    * @param critVolume the critical volume level, used as max volume (represents 100% full)
    * @param barWidth the pixel width of the actual color bar
    * @param unit the units in which volume is expressed; may be null
    * @param enableMonospace use fixed font rather than variable font for label text
    */
    public UnboundedHealthStatusBarComposite(Composite parent, String title, int critVolume,
            int barWidth, String unit, boolean enableMonospace) {
        super(parent, title, critVolume, barWidth, unit, enableMonospace);
    }

    /**
     * Override of method to set the current volume label. Sets the label to "NNN" + units,
     * where NNN is the current volume.
     * 
     * @param status the current health status
     * @param currentVolume the current volume
     * @param percentage the current percentage of max volume
     */
    @Override
    protected void setCurrentVolumeLabel(HealthStatus status, int currentVolume, int percentage) {
        String maxDisplayStr = String.format("%s", String.valueOf(currentVolume) + " " + unitString);
        this.currentVolumeLabel.setText(maxDisplayStr);
    }
    
    /**
     * Overrideable method to set color bar tooltip text. Includes current
     * volume with units and percentage of RED level, an indicator there is
     * no max volume, and health status.
     * 
     * @param status
     *            current health status
     * @param currentVolume
     *            the current volume
     * @param percentage
     *            the current percentage of max volume
     */
    @Override
    protected void setBarTooltipText(HealthStatus status, int currentVolume, int percentage) {
        StringBuilder b = new StringBuilder("Health: " + status);
        b.append("\nCurrent Size: " + currentVolume + " " + unitString + " (" + percentage + "% of Critical level)");
        b.append("\nNo Maximum");
        colorBar.setToolTipText(b.toString());       
    }
 
}
