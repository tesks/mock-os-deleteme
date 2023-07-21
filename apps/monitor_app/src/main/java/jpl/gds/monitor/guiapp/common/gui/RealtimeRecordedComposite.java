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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;

/**
 * This is a common GUI panel for allowing the user to select one of the valid
 * realtime/recorded filter types. It is intended for used by GUI preference 
 * windows.
 * 
 * @see RealtimeRecordedFilterType
 * @see RealtimeRecordedSupport
 * 
 */
public class RealtimeRecordedComposite implements RealtimeRecordedSupport {

    private final Composite parent;
    private Button displayRealtimeButton;
    private Button displayRecordedButton;
    private Button displayBothButton;
    private Group buttonGroup;
    private String bothLabelText = "Latest of Both";

    /**
     * Constructor. Assumes the label for the Both button is "Latest of Both".
     * 
     * @param parent
     *            the parent Composite on which to create this composite panel
     */
    public RealtimeRecordedComposite(final Composite parent) {

        this.parent = parent;
        createControls();
    }
    

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent Composite on which to create this composite panel
     * @param bothLabel label text for the BOTH recored/realtime button label           
     *
     */
    public RealtimeRecordedComposite(final Composite parent, String bothLabel) {
        this.bothLabelText = bothLabel;
        this.parent = parent;
        createControls();
    }


    /**
     * Gets the underlying SWT composite object, so that it can be used to
     * establish layout.
     * 
     * @return SWT Composite object
     */
    public Composite getComposite() {

        return this.buttonGroup;
    }

    /**
     * Creates and draws the GUI components.
     */
    private void createControls() {

        this.buttonGroup = new Group(this.parent, SWT.NULL);
        final FormLayout fl1 = new FormLayout();
        fl1.spacing = 7;
        this.buttonGroup.setLayout(fl1);

        final Label realtimeLabel = new Label(this.buttonGroup, SWT.NONE);
        realtimeLabel.setText("Realtime");
        final FormData realtimeLabelFd = new FormData();
        realtimeLabelFd.top = new FormAttachment(0);
        realtimeLabelFd.left = new FormAttachment(0, 3);
        realtimeLabel.setLayoutData(realtimeLabelFd);

        this.displayRealtimeButton = new Button(this.buttonGroup, SWT.RADIO);
        final FormData realtimeFd = new FormData();
        realtimeFd.top = new FormAttachment(realtimeLabel, 0, SWT.CENTER);
        realtimeFd.left = new FormAttachment(realtimeLabel);
        this.displayRealtimeButton.setLayoutData(realtimeFd);

        final Label recordedLabel = new Label(this.buttonGroup, SWT.NONE);
        recordedLabel.setText("Recorded");
        final FormData recordedLabelFd = new FormData();
        recordedLabelFd.top = new FormAttachment(0);
        recordedLabelFd.left = new FormAttachment(this.displayRealtimeButton,
                0, 10);
        recordedLabel.setLayoutData(recordedLabelFd);

        this.displayRecordedButton = new Button(this.buttonGroup, SWT.RADIO);
        final FormData recordedFd = new FormData();
        recordedFd.top = new FormAttachment(recordedLabel, 0, SWT.CENTER);
        recordedFd.left = new FormAttachment(recordedLabel);
        this.displayRecordedButton.setLayoutData(recordedFd);

        final Label bothLabel = new Label(this.buttonGroup, SWT.NONE);
        bothLabel.setText(bothLabelText);
        final FormData bothLabelFd = new FormData();
        bothLabelFd.top = new FormAttachment(0);
        bothLabelFd.left = new FormAttachment(this.displayRecordedButton, 0, 10);
        bothLabel.setLayoutData(bothLabelFd);

        this.displayBothButton = new Button(this.buttonGroup, SWT.RADIO);
        final FormData bothFd = new FormData();
        bothFd.top = new FormAttachment(bothLabel, 0, SWT.CENTER);
        bothFd.left = new FormAttachment(bothLabel);
        this.displayBothButton.setLayoutData(bothFd);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#setRealtimeRecordedFilterType(jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType)
     */
    @Override
    public void setRealtimeRecordedFilterType(
            final RealtimeRecordedFilterType filterType) {

        if (this.buttonGroup.isDisposed()) {
            return;
        }
        switch (filterType) {
        case REALTIME:
            this.displayRealtimeButton.setSelection(true);
            break;
        case RECORDED:
            this.displayRecordedButton.setSelection(true);
            break;
        case BOTH:
            this.displayBothButton.setSelection(true);
            break;
        default:
            break;
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.perspective.view.RealtimeRecordedSupport#getRealtimeRecordedFilterType()
     */
    @Override
    public RealtimeRecordedFilterType getRealtimeRecordedFilterType() {

        if (this.buttonGroup.isDisposed()) {
            return RealtimeRecordedFilterType.REALTIME;
        }
        if (this.displayRealtimeButton.getSelection()) {
            return RealtimeRecordedFilterType.REALTIME;
        } else if (this.displayRecordedButton.getSelection()) {
            return RealtimeRecordedFilterType.RECORDED;
        } else {
            return RealtimeRecordedFilterType.BOTH;
        }
    }
}
