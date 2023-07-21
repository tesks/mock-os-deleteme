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
package jpl.gds.monitor.guiapp.gui.views.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.guiapp.common.gui.StationComposite;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.StalenessComposite;
import jpl.gds.shared.swt.TitleComposite;

/**
 * FixedLayoutPreferencesShell is the SWT GUI preferences window class for the
 * message monitor's fixed layout view.
 * 
 */
public class FixedLayoutPreferencesShell extends AbstractViewPreferences {

    /**
     * Fixed layout preferences window title
     */
    public static final String TITLE = "Fixed Layout Preferences";

    private final Shell parent;
    private RealtimeRecordedComposite rtRecComposite;
    private StationComposite stationComposite;
    private StalenessComposite stalenessComposite;
    private int station;
    private RealtimeRecordedFilterType rtRecFilterType;
    private int stalenessInterval;

    /**
     * Creates an instance of FixedLayoutPreferencesShell.
     * 
     * @param parent
     *            the parent display of this widget
     */
    public FixedLayoutPreferencesShell(final ApplicationContext appContext, final Shell parent) {

        super(appContext, TITLE);
        this.parent = parent;
        createControls();
        this.prefShell.setLocation(parent.getLocation().x + 100,
                parent.getLocation().y + 100);
    }

    /**
     * Creates all the preferences controls and composites.
     */
    protected void createControls() {

        this.prefShell = new Shell(this.parent, SWT.SHELL_TRIM
                | SWT.APPLICATION_MODAL);
        this.prefShell.setText(TITLE);
        final FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        this.prefShell.setLayout(fl);

        this.titleText = new TitleComposite(this.prefShell);
        final Composite titleComp = this.titleText.getComposite();
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0, 3);
        titleComp.setLayoutData(fdLabel2);

        this.rtRecComposite = new RealtimeRecordedComposite(this.prefShell);
        final FormData fdRtRec = new FormData();
        fdRtRec.top = new FormAttachment(titleComp);
        fdRtRec.left = new FormAttachment(0, 5);
        this.rtRecComposite.getComposite().setLayoutData(fdRtRec);

        this.stationComposite = new StationComposite(appContext.getBean(MissionProperties.class).getStationMapper(),
        		this.prefShell);
        final FormData fdStation = new FormData();
        fdStation.top = new FormAttachment(this.rtRecComposite.getComposite());
        fdStation.left = new FormAttachment(0, 5);
        this.stationComposite.getComposite().setLayoutData(fdStation);

        this.stalenessComposite = new StalenessComposite(this.prefShell);
        final FormData fdStaleness = new FormData();
        fdStaleness.top = new FormAttachment(
                this.stationComposite.getComposite());
        fdStaleness.left = new FormAttachment(0, 5);
        this.stalenessComposite.getComposite().setLayoutData(fdStaleness);

        final Label line = new Label(this.prefShell, SWT.SEPARATOR
                | SWT.HORIZONTAL | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.top = new FormAttachment(
                this.stalenessComposite.getComposite(), 5);
        line.setLayoutData(formData6);

        final Composite buttonComposite = new Composite(this.prefShell,
                SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        buttonComposite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.top = new FormAttachment(line);
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        buttonComposite.setLayoutData(formData8);

        final Button applyButton = new Button(buttonComposite, SWT.PUSH);
        applyButton.setText("Ok");
        this.prefShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("Cancel");

        applyButton.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {

                try {

                    applyChanges();

                    try {
                        FixedLayoutPreferencesShell.this.stalenessInterval = FixedLayoutPreferencesShell.this.stalenessComposite
                                .getStalenessInterval();
                    } catch (final NumberFormatException ex) {
                        SWTUtilities
                                .showErrorDialog(
                                        FixedLayoutPreferencesShell.this.prefShell,
                                        "Bad Staleness Interval",
                                        "Staleness interval must be a number greater than or equal to 0");
                        return;
                    }
                    FixedLayoutPreferencesShell.this.rtRecFilterType = FixedLayoutPreferencesShell.this.rtRecComposite
                            .getRealtimeRecordedFilterType();
                    FixedLayoutPreferencesShell.this.station = FixedLayoutPreferencesShell.this.stationComposite
                            .getStationId();

                    FixedLayoutPreferencesShell.this.prefShell.close();

                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {

                try {
                    FixedLayoutPreferencesShell.this.canceled = true;
                    FixedLayoutPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {

        super.setValuesFromViewConfiguration(config);
        final IFixedLayoutViewConfiguration fixedConfig = (IFixedLayoutViewConfiguration) config;
        this.stationComposite.setStationId(fixedConfig.getStationId());
        this.rtRecComposite.setRealtimeRecordedFilterType(fixedConfig
                .getRealtimeRecordedFilterType());
        this.stalenessComposite.setStalenessInterval(fixedConfig
                .getStalenessInterval());

        this.prefShell.layout();
        this.prefShell.pack();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {

        super.getValuesIntoViewConfiguration(config);
        final IFixedLayoutViewConfiguration fixedConfig = (IFixedLayoutViewConfiguration) config;
        fixedConfig.setStationId(this.station);
        fixedConfig.setRealtimeRecordedFilterType(this.rtRecFilterType);
        fixedConfig.setStalenessInterval(this.stalenessInterval);
    }
}
