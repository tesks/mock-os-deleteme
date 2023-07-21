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
package jpl.gds.monitor.guiapp.common.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This is a common GUI panel for allowing the user to select one of the 
 * mission-configured telemetry receiving stations. It is intended for used by GUI preference 
 * windows.
 * 
 * @see StationSupport
 */
public class StationComposite implements StationSupport {

    private static final String UNSPECIFIED_STATION = "[None]";


    private final Composite parent;
    private Combo stationCombo;
    private Composite mainComposite;

	private final StationMapper mapper;
    
    /**
     * Constructor.
     * @param mapper the mission station mapper object
     * 
     * @param parent
     *            the parent Composite on which to create this composite panel
     */
    public StationComposite(final StationMapper mapper, final Composite parent) {

        this.parent = parent;
        this.mapper = mapper;
        createControls();
    }

    /**
     * Gets the underlying SWT composite object, so that it can be used to
     * establish layout.
     * 
     * @return SWT Composite object
     */
    public Composite getComposite() {

        return this.mainComposite;
    }

    /**
     * Creates and draws the GUI components.
     */
    private void createControls() {

        this.mainComposite = new Group(this.parent, SWT.NULL);
        final FormLayout fl1 = new FormLayout();
        fl1.spacing = 7;
        fl1.marginTop = 5;
        fl1.marginLeft = 5;
        fl1.marginBottom = 5;
        this.mainComposite.setLayout(fl1);

        final Label stationLabel = new Label(this.mainComposite, SWT.NONE);
        stationLabel.setText("DSS ID");
        final FormData realtimeLabelFd = new FormData();
        realtimeLabelFd.top = new FormAttachment(0);
        realtimeLabelFd.left = new FormAttachment(0, 3);
        stationLabel.setLayoutData(realtimeLabelFd);

        this.stationCombo = new Combo(this.mainComposite, SWT.DROP_DOWN);
        /*
         * Made combo smaller because UNSPECIFIED
         * STATION string got smaller.
         */
        final FormData stationFd = SWTUtilities.getFormData(this.stationCombo, 1, 12);
        stationFd.top = new FormAttachment(stationLabel, 0, SWT.CENTER);
        stationFd.left = new FormAttachment(stationLabel);
        this.stationCombo.setLayoutData(stationFd);
        
        initializeStations();
    }
    
    /**
     * Initializes the values displayed by the station
     * combo box.
     */
    private void initializeStations() {
        final Integer[] stations = mapper.getStationIds();
        
        if (stations != null) {
            
            /*
             * Add unspecified station first.
             */
            final String[] stationStrs = new String[stations.length + 1];
            stationStrs[0] = UNSPECIFIED_STATION;
            int index = 1;
            for (final Integer id: stations) {
                stationStrs[index++] = String.valueOf(id);
            }
                
            this.stationCombo.setItems(stationStrs);
            
        } else {
            
            /*
             * No configured stations. Just add the unspecified station.
             */
            this.stationCombo.setItems(new String[] {UNSPECIFIED_STATION}); 
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.StationSupport#getStationId()
     */
    @Override
    public int getStationId() {
        
        if (this.stationCombo.isDisposed()) {
            return StationIdHolder.UNSPECIFIED_VALUE;
        }
        
        final String comboVal = this.stationCombo.getText();
        
        if (comboVal.equals(UNSPECIFIED_STATION)) {
            return StationIdHolder.UNSPECIFIED_VALUE; 
        } 
        return Integer.valueOf(comboVal);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.StationSupport#setStationId(int)
     */
    @Override
    public void setStationId(final int station) {

        if (this.stationCombo.isDisposed()) {
            return;
        }
        
        if (station == StationIdHolder.UNSPECIFIED_VALUE) {
           this.stationCombo.setText(UNSPECIFIED_STATION);
        } else {
            this.stationCombo.setText(String.valueOf(station));
        }
        
    }
}
