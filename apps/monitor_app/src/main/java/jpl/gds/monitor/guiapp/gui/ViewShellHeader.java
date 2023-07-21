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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * GUI component placed at the top of a monitor shell. Contains Virtual 
 * Channel ID, Deep Space Station ID and Flight Topic
 *
 */
public class ViewShellHeader {
   
    private final Shell mainShell;
    private final Display mainDisplay;
    private final IViewConfiguration config;
    private Composite group;
    private Label stationLabel;
    private Label station;
    private Label vcidLabel;
    private Label vcid;
    private Label topicLabel;
    private Label topic;
	private final MissionProperties missionProps;
	private final ApplicationContext appContext;
    
    /**
     * Constructor: Sets member variables and creates GUI if header should be 
     * shown
     * @param mainShell is the window in which the header will be placed
     * @param testConfig session configuration used to retrieve VCID, DSS ID, 
     *        and JMS Topic
     * @param tabConfig view configuration used to retrieve data font
     * @param shouldShowHeader true if header should be created and displayed, 
     *        false otherwise
     */
    public ViewShellHeader (final ApplicationContext appContext,
    		final Shell mainShell,  
            final IViewConfiguration tabConfig, final boolean shouldShowHeader) {
    	this.appContext = appContext;
    	this.missionProps = appContext.getBean(MissionProperties.class);
        this.mainShell = mainShell;
        this.mainDisplay = mainShell.getDisplay();
        this.config = tabConfig;
        if(shouldShowHeader) {
            createControls();
        }
    }
    
    /**
     * Creates a group widget that contains different labels with relevant 
     * session configuration information
     */
    public void createControls() {
        
        //TODO how to add a fixed layout as a header?
        
        //MonitorHeaderViewConfiguration mhvc = new MonitorHeaderViewConfiguration(mainShell);
        
        final FormLayout layout = new FormLayout();
        FormData data = new FormData();
        
        final Color white = new Color(mainDisplay, 255, 255, 255);
        final int fontSize = config.getDataFont().getSize();
        
        group = new Composite(mainShell, SWT.NONE);
        group.setLayout(layout);
        final GridData gData = new GridData();
        gData.horizontalAlignment = SWT.FILL;
        group.setBackground(white);
        group.setLayoutData(gData);
        
        stationLabel = new Label (group, SWT.NONE);
        stationLabel.setText("Station ID: ");
        FontData[] fD = stationLabel.getFont().getFontData();
        fD[0].setHeight(fontSize);
        stationLabel.setFont( new Font(mainDisplay,fD[0]));
        stationLabel.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(0, 5);
        stationLabel.setLayoutData(data);
        
        station = new Label (group, SWT.NONE);
        fD = station.getFont().getFontData();
        fD[0].setHeight(fontSize);
        station.setFont( new Font(mainDisplay,fD[0]));
        station.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(stationLabel);
        station.setLayoutData(data);
        setDssIdLabel();
        
        vcidLabel = new Label (group, SWT.NONE);
        vcidLabel.setText("Virtual Channel ID: ");
        fD = vcidLabel.getFont().getFontData();
        fD[0].setHeight(fontSize);
        vcidLabel.setFont( new Font(mainDisplay,fD[0]));
        vcidLabel.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(station, 50);
        vcidLabel.setLayoutData(data);
        
        vcid = new Label (group, SWT.NONE);
        fD = vcid.getFont().getFontData();
        fD[0].setHeight(fontSize);
        vcid.setFont( new Font(mainDisplay,fD[0]));
        vcid.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(vcidLabel);
        vcid.setLayoutData(data);
        setVcidLabel();
        
        topicLabel = new Label (group, SWT.NONE);
        topicLabel.setText("Flight Topic: ");
        fD = topicLabel.getFont().getFontData();
        fD[0].setHeight(fontSize);
        topicLabel.setFont( new Font(mainDisplay,fD[0]));
        topicLabel.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(vcid, 50);
        topicLabel.setLayoutData(data);
        
        topic = new Label (group, SWT.NONE);
        fD = topic.getFont().getFontData();
        fD[0].setHeight(fontSize);
        topic.setFont( new Font(mainDisplay,fD[0]));
        topic.setBackground(white);
        data = new FormData();
        data.left = new FormAttachment(topicLabel);
        topic.setLayoutData(data);
        setTopicLabel();
        
        
        /**
         * Realtime/recorded label
         */
//        dataSubscriptionTypeLabel = new Label (group, SWT.NONE);
//        dataSubscriptionTypeLabel.setText("Data Subscription Type: ");
//        fD = dataSubscriptionTypeLabel.getFont().getFontData();
//        fD[0].setHeight(fontSize);
//        dataSubscriptionTypeLabel.setFont( new Font(mainDisplay,fD[0]));
//        dataSubscriptionTypeLabel.setBackground(white);
//        data = new FormData();
//        data.left = new FormAttachment(vcid, 50);
//        dataSubscriptionTypeLabel.setLayoutData(data);
//        dataSubscriptionTypeLabel.setVisible(false);
//        
//        dataSubscriptionType = new Label (group, SWT.NONE);
//        fD = dataSubscriptionType.getFont().getFontData();
//        fD[0].setHeight(fontSize);
//        dataSubscriptionType.setFont( new Font(mainDisplay,fD[0]));
//        dataSubscriptionType.setBackground(white);
//        data = new FormData();
//        data.left = new FormAttachment(dataSubscriptionTypeLabel);
//        dataSubscriptionType.setLayoutData(data);
//        dataSubscriptionType.setVisible(false);
//        setDataSubscriptionTypeLabel(false);
        
        //TODO need to check current tab opened and show recorded/realtime according to that the first time
        
        //TODO need to add check in preferences shell, so that it updates when that is changed
    }
    
    /**
     * Sets the Deep Space Station ID value as text that will be displayed in 
     * the header
     */
    private void setDssIdLabel() {
		if (appContext.getBean(IContextFilterInformation.class).getDssId() == null) {
			station.setText("All");
		} else {
			station.setText(String.valueOf(appContext.getBean(IContextFilterInformation.class).getDssId()));
		}

    }
    
    /**
     * Sets the Virtual Channel ID value as text that will be displayed in the 
     * header
     */
    private void setVcidLabel() {
        final Integer vcidNumber = appContext.getBean(IContextFilterInformation.class).getVcid();
        if(vcidNumber == null) {
            vcid.setText("All");
        } else {
            vcid.setText(vcidNumber + " - " + missionProps.mapDownlinkVcidToName(vcidNumber));
        }
    }
    
    /**
     * Sets the flight topic value as text for the label that will be 
     * displayed in the header
     */
    private void setTopicLabel() {
        final String topicName = ContextTopicNameFactory.getMissionSessionTopic(appContext);
        
        if(topicName == null) {
            topic.setText("not set");
        } else {
            topic.setText(topicName);
        }
    }
    
//    /**
//     * Sets visibility for the realtime/recorded labels to true
//     */
//    public void showDisplayDataTypeLabel() {
//        dataSubscriptionTypeLabel.setVisible(true);
//        dataSubscriptionType.setVisible(true);
//    }
//    
//    /**
//     * Sets visibility for the realtime/recorded labels to false
//     */
//    public void hideDisplayDataTypeLabel() {
//        dataSubscriptionTypeLabel.setVisible(false);
//        dataSubscriptionType.setVisible(false);
//    }
//    
//    public void setDataSubscriptionTypeLabel(boolean isRecorded) {
//        //need to check which view is currently displayed in this monitor window. and then 
//        //TODO
//        if(isRecorded) {
//            dataSubscriptionType.setText("Recorded");
//        } else {
//            dataSubscriptionType.setText("Realtime");
//        }
//    }
    
    /**
     * Dynamically hides or displays the header by disposing of it or creating 
     * the GUI parts, respectively
     * @param shouldShow is true if header should be created and displayed, 
     * false otherwise
     */
    public void showHeader(final boolean shouldShow) {
        if(shouldShow) {
            createControls();
            group.moveAbove(null);  //force header to be on top
            mainShell.layout();
        }
        else {
            group.dispose();
            mainShell.layout();
        }
    }
}
