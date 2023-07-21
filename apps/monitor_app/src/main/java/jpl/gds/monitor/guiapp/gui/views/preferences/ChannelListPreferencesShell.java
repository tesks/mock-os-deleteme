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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.guiapp.common.gui.StationComposite;
import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.perspective.view.ChannelListViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * ChannelListPreferencesShell is the SWT GUI preferences window class for the
 * channel list view.
 */
public class ChannelListPreferencesShell extends AbstractViewPreferences
        implements ViewPreferencesShell {

    /**
     * Channel list preferences window title
     */
    public static final String TITLE = "Channel List Preferences";

    private final Shell parent;
    private TableComposite tableConfigurer;
    private boolean dirty;
    private boolean changeColumns;
    private Button monospaceButton;
    private boolean useMonospace;
    private RealtimeRecordedFilterType rtRecFilterType;
    private RealtimeRecordedComposite rtRecComposite;
    private StationComposite stationComposite;
    private int station;
    private final Tracer               trace;

    /**
     * Creates an instance of ChannelListPreferencesShell.
     * 
     * @param appContext
     *            The current application context
     * 
     * @param parent
     *            the parent display of this widget
     */
    public ChannelListPreferencesShell(final ApplicationContext appContext, final Shell parent) {

        super(appContext, TITLE);
        this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        createControls();
        this.prefShell.setLocation(parent.getLocation().x + 75,
                parent.getLocation().y + 75);
    }

    /**
     * Creates all the preferences controls and composites.
     */
    @SuppressWarnings("PMD.IdempotentOperations")
    protected void createControls() {

        /*
         * Create the overall shell.
         */
        this.prefShell = new Shell(this.parent, SWT.SHELL_TRIM
                | SWT.APPLICATION_MODAL);
        this.prefShell.setText(TITLE);
        FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        this.prefShell.setLayout(fl);

        /*
         * Preferences will go onto tabs. Create the tab folder.
         */
        final TabFolder folder = new TabFolder(this.prefShell, SWT.NONE);

        /*
         * Create the general tab.
         */
        final TabItem generalTab = new TabItem(folder, SWT.NONE);
        final FormData folderData = new FormData();
        folderData.left = new FormAttachment(0);
        folderData.right = new FormAttachment(100);
        folder.setLayoutData(folderData);
        generalTab.setText("General");

        /*
         * Put a composite into the general tab.
         */
        final Composite generalComposite = new Composite(folder, SWT.NONE);
        fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        generalComposite.setLayout(fl);
        generalTab.setControl(generalComposite);

        /*
         * Put the view title composite on the general tab.
         */
        this.titleText = new TitleComposite(generalComposite);
        final Composite titleComp = this.titleText.getComposite();
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0, 3);
        titleComp.setLayoutData(fdLabel2);

        /*
         * Put the data font selector composite on the general table.
         */
        this.fontGetter = new FontComposite(generalComposite, "Data Font", trace);
        final Composite fontComp = this.fontGetter.getComposite();
        final FormData fontFd = new FormData();
        fontFd.top = new FormAttachment(this.titleText.getComposite());
        fontFd.left = new FormAttachment(0, 3);
        fontFd.right = new FormAttachment(100);
        fontComp.setLayoutData(fontFd);

        /*
         * Put the monospace selector button on the general table.
         */
        this.monospaceButton = new Button(generalComposite, SWT.CHECK);
        this.monospaceButton.setText("Use Monospaced Font");
        final FormData monoFd = new FormData();
        monoFd.top = new FormAttachment(fontComp, 0, 3);
        monoFd.left = new FormAttachment(0, 10);
        monoFd.right = new FormAttachment(100);
        this.monospaceButton.setLayoutData(monoFd);

        /*
         * Put the foreground color composite on the general tab.
         */
        this.foreColorGetter = new ColorComposite(generalComposite,
                "Foreground Color", trace);
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(this.monospaceButton, 0, 7);
        foreColorFd.left = new FormAttachment(0, 3);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);

        /*
         * Put the background color composite on the general tab.
         */
        this.backColorGetter = new ColorComposite(generalComposite,
                "Background Color", trace);
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 3);
        colorFd.left = new FormAttachment(0, 3);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);

        /*
         * Create the table tab.
         */
        final TabItem tableTab = new TabItem(folder, SWT.NONE);
        tableTab.setText("Table");

        /*
         * Put a composite into the table tab.
         */
        final Composite tableComposite = new Composite(folder, SWT.NONE);
        fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        tableComposite.setLayout(fl);
        tableTab.setControl(tableComposite);

        /*
         * Put the table configuration composite on the table tab.
         */
        this.tableConfigurer = new TableComposite(tableComposite);
        final Composite tableComp = this.tableConfigurer.getComposite();
        final FormData tableFd = new FormData();
        tableFd.top = new FormAttachment(0);
        tableFd.left = new FormAttachment(0);
        tableFd.right = new FormAttachment(100);
        tableComp.setLayoutData(tableFd);

        /*
         * Create the sources tab.
         */
        final TabItem sourcesTab = new TabItem(folder, SWT.NONE);
        sourcesTab.setText("Sources");

        /*
         * Put a composite into the sources tab.
         */
        final Composite sourcesComposite = new Composite(folder, SWT.NONE);
        fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        sourcesComposite.setLayout(fl);
        sourcesTab.setControl(sourcesComposite);

        /*
         * Put the realtime/recorded selection composite on the sources tab.
         */
        this.rtRecComposite = new RealtimeRecordedComposite(sourcesComposite);
        final FormData rtRecFd = new FormData();
        rtRecFd.top = new FormAttachment(0);
        this.rtRecComposite.getComposite().setLayoutData(rtRecFd);

        /*
         * Put the station selection composite on the sources tab.
         */
        this.stationComposite = new StationComposite(appContext.getBean(MissionProperties.class).getStationMapper(), 
        		sourcesComposite);
        final FormData stationFd = new FormData();
        stationFd.top = new FormAttachment(this.rtRecComposite.getComposite(),
                0, 10);
        this.stationComposite.getComposite().setLayoutData(stationFd);

        /*
         * Create the button composite.
         */
        final Composite composite = new Composite(this.prefShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.top = new FormAttachment(folder);
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        /*
         * Create the apply/OK button and define its listener.
         */
        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        this.prefShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {

                applyChanges();
                try {
                    ChannelListPreferencesShell.this.dirty = ChannelListPreferencesShell.this.tableConfigurer
                            .getDirty();
                    if (ChannelListPreferencesShell.this.dirty) {
                        ChannelListPreferencesShell.this.changeColumns = true;
                    }
                    ChannelListPreferencesShell.this.useMonospace = ChannelListPreferencesShell.this.monospaceButton
                            .getSelection();
                    ChannelListPreferencesShell.this.rtRecFilterType = ChannelListPreferencesShell.this.rtRecComposite
                            .getRealtimeRecordedFilterType();
                    ChannelListPreferencesShell.this.station = ChannelListPreferencesShell.this.stationComposite
                            .getStationId();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                } finally {
                    ChannelListPreferencesShell.this.prefShell.close();
                }
            }
        });

        /*
         * Create the cancel button and define its listener.
         */
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {

                try {
                    ChannelListPreferencesShell.this.canceled = true;
                    ChannelListPreferencesShell.this.prefShell.close();
                } catch (final RuntimeException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Determines if selected columns to be displayed have changed
     * 
     * @return true if selected columns have changed, false otherwise
     */
    public boolean needColumnChange() {

        return this.changeColumns;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {

        super.getValuesIntoViewConfiguration(config);
        if (this.tableConfigurer != null) {
            config.setTable(this.tableConfigurer.getTable());
        }
        ((ChannelListViewConfiguration) config)
                .setUseMonospaceFont(this.useMonospace);
        final ChillFont f = config.getDataFont();
        if (this.useMonospace) {
            f.setFace(ChillFont.MONOSPACE_FACE);
        } else {
            f.setFace(ChillFont.DEFAULT_FACE);
        }
        config.setDataFont(f);

        ((RealtimeRecordedSupport) config)
                .setRealtimeRecordedFilterType(this.rtRecFilterType);

        ((StationSupport) config).setStationId(this.station);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {

        super.setValuesFromViewConfiguration(config);
        this.dirty = false;
        this.changeColumns = false;
        final ChannelListViewConfiguration chanConfig = (ChannelListViewConfiguration) config;
        final ChillTable channelTable = chanConfig
                .getTable(ChannelListViewConfiguration.CHANNEL_TABLE_NAME);
        if (this.tableConfigurer != null
                && !this.tableConfigurer.getComposite().isDisposed()) {
            this.tableConfigurer.init(channelTable);
            this.tableConfigurer.disableColumnButtons(new int[] { 0 });
            this.prefShell.layout();
            this.prefShell.pack();
        }
        if (this.monospaceButton != null && !this.monospaceButton.isDisposed()) {
            this.monospaceButton.setSelection(chanConfig.getUseMonospace());
        }

        this.rtRecComposite
                .setRealtimeRecordedFilterType(((RealtimeRecordedSupport) config)
                        .getRealtimeRecordedFilterType());

        this.stationComposite.setStationId(((StationSupport) config)
                .getStationId());
    }
}
