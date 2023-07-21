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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.guiapp.common.gui.StationComposite;
import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.perspective.view.FastAlarmViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * AlarmPreferencesShell is the SWT GUI preferences window class for the 
 * alarm view..
 *
 */
public class FastAlarmPreferencesShell extends AbstractViewPreferences implements ViewPreferencesShell {
	/**
	 * Alarm preferences window title
	 */
	public static final String TITLE = "Alarm Preferences";
	private static final String NO_FILTER = "-no filter-";
	private static final String FLUSH_NOTE = "Entering a flush time of 0 disables automatic flushing.";

	private final Shell parent;
	private TableComposite tableConfigurer;
	private ChillTable channelTable;
	private boolean dirty;
	private boolean changeColumns;
	private Combo levelCombo;
	private Text flushText;
	private List<String> modules;
	private List<String> subsystems;
	private List<String> categories;
	private Combo moduleCombo;
	private Combo subsystemCombo;
	private Combo categoryCombo;
	private String selectedModule;
	private String selectedSubsystem;
	private String selectedCategory;
	private int flushInterval;
	private AlarmLevel displayLevel;
	private Button monospaceButton;
	private boolean useMonospace;
	private RealtimeRecordedComposite rtRecComposite;
	private RealtimeRecordedFilterType rtRecFilterType;

	private StationComposite stationComposite;
	private int station;

    private final Tracer                     trace;

	/**
     * Creates an instance of AlarmPreferencesShell.
     * 
     * @param appContext
     *            The current application context
     * @param parent
     *            the parent display of this widget
     */
	public FastAlarmPreferencesShell(final ApplicationContext appContext, final Shell parent) {
		super(appContext, TITLE);
		this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		initDictionaryLists();
		createControls();
		prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
	}

	private void initDictionaryLists() {
		final IChannelUtilityDictionaryManager util = appContext.getBean(IChannelUtilityDictionaryManager.class);
		subsystems = util.getSubsystems();
		modules = util.getModules();
		categories =  util.getOpsCategories();
	}

	/**
	 * Creates all the preferences controls and composites.
	 */
	protected void createControls() {
		prefShell = new Shell(parent, SWT.SHELL_TRIM | 
				SWT.APPLICATION_MODAL);
		prefShell.setText(TITLE);
		FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		prefShell.setLayout(fl);

		final TabFolder folder = new TabFolder(prefShell, SWT.NONE);
		final TabItem generalTab = new TabItem(folder, SWT.NONE);
		final FormData folderData = new FormData();
		folderData.left = new FormAttachment(0);
		folderData.right = new FormAttachment(100);
		folder.setLayoutData(folderData);

		generalTab.setText("General");

		final Composite generalComposite = new Composite(folder, SWT.NONE);
		fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		generalComposite.setLayout(fl);

		generalTab.setControl(generalComposite);

		titleText = new TitleComposite(generalComposite);
		final Composite titleComp = titleText.getComposite();
		final FormData fdLabel2 = new FormData();
		fdLabel2.top = new FormAttachment(0, 10);
		fdLabel2.left = new FormAttachment(0, 3);
		titleComp.setLayoutData(fdLabel2);



        fontGetter = new FontComposite(generalComposite, "Data Font", trace);
		final Composite fontComp = fontGetter.getComposite();
		final FormData fontFd = new FormData();
		fontFd.top = new FormAttachment(titleComp, 0, 10);
		fontFd.left = new FormAttachment(0);
		fontFd.right = new FormAttachment(100);
		fontComp.setLayoutData(fontFd);

		monospaceButton = new Button(generalComposite, SWT.CHECK);
		monospaceButton.setText("Use Monospaced Font");
		final FormData monoFd = new FormData();
		monoFd.top = new FormAttachment(fontComp, 0, 3);
		monoFd.left = new FormAttachment(0);
		monoFd.right = new FormAttachment(100);
		monospaceButton.setLayoutData(monoFd);

		foreColorGetter = new ColorComposite(generalComposite,
                "Foreground Color", trace);
		final Composite foreColorComp = foreColorGetter.getComposite();
		final FormData foreColorFd = new FormData();
		foreColorFd.top = new FormAttachment(monospaceButton, 0, 7);
		foreColorFd.left = new FormAttachment(0, 2);
		foreColorFd.right = new FormAttachment(100);
		foreColorComp.setLayoutData(foreColorFd);

        backColorGetter = new ColorComposite(generalComposite, "Background Color", trace);
		final Composite colorComp = backColorGetter.getComposite();
		final FormData colorFd = new FormData();
		colorFd.top = new FormAttachment(foreColorComp, 0, 3);
		colorFd.left = new FormAttachment(0);
		colorFd.right = new FormAttachment(100);
		colorComp.setLayoutData(colorFd);

		final TabItem filterTab = new TabItem(folder, SWT.NONE);
		filterTab.setText("Filters");
		final Composite filterComp = new Composite(folder, SWT.NONE);
		filterComp.setLayout(new RowLayout(SWT.VERTICAL));
		filterTab.setControl(filterComp);

		if (!modules.isEmpty()) {

			final Composite moduleComposite = new Composite(filterComp, SWT.NONE);
			final GridLayout gl = new GridLayout(2, true);
			gl.marginHeight = 5;
			gl.marginWidth = 5;
			moduleComposite.setLayout(gl);
			final Label moduleText = new Label(moduleComposite, SWT.NONE);
			moduleText.setText("Module:");
			moduleCombo = new Combo(moduleComposite, SWT.DROP_DOWN);
			moduleCombo.add(NO_FILTER);
			for (int i = 0; i < modules.size(); i++) {
				moduleCombo.add(modules.get(i));
			}
		}

		if (!subsystems.isEmpty()) {
			final Composite subsystemComposite = new Composite(filterComp, SWT.NONE);
			final GridLayout gl = new GridLayout(2, true);
			gl.marginHeight = 5;
			gl.marginWidth = 5;
			subsystemComposite.setLayout(gl);
			final Label subsystemText = new Label(subsystemComposite, SWT.NONE);
			subsystemText.setText("Subsystem:");
			subsystemCombo = new Combo(subsystemComposite, SWT.DROP_DOWN);
			subsystemCombo.add(NO_FILTER);

			for (int i = 0; i < subsystems.size(); i++) {
				subsystemCombo.add(subsystems.get(i));
			}
		}

		if (!categories.isEmpty()) {
			final Composite categoryComposite = new Composite(filterComp, SWT.NONE);
			final GridLayout gl = new GridLayout(2, true);
			gl.marginHeight = 5;
			gl.marginWidth = 5;
			categoryComposite.setLayout(gl);
			final Label categoryText = new Label(categoryComposite, SWT.NONE);
			categoryText.setText("Category:");
			categoryCombo = new Combo(categoryComposite, SWT.DROP_DOWN);
			categoryCombo.add(NO_FILTER);

			for (int i = 0; i < categories.size(); i++) {
				categoryCombo.add(categories.get(i));
			}
		}

		final TabItem displayTab = new TabItem(folder, SWT.NONE);
		displayTab.setText("Sources");

		final Composite sourcesComposite = new Composite(folder, SWT.NONE);
		final RowLayout rl = new RowLayout(SWT.VERTICAL);
		rl.spacing = 10;
		sourcesComposite.setLayout(rl);

		displayTab.setControl(sourcesComposite);

		final Composite levelComposite = new Composite(sourcesComposite, SWT.NONE);
		final FormLayout lfl = new FormLayout();
		lfl.spacing = 10;
		lfl.marginTop = 10;
		lfl.marginLeft = 5;
		lfl.marginBottom = 10;
		levelComposite.setLayout(lfl);
		final Label levelLabel = new Label(levelComposite, SWT.NONE);
		levelLabel.setText("Alarm Level:");
		final FormData labelData = new FormData();
		labelData.top = new FormAttachment(0);
		labelData.left = new FormAttachment(0);
		levelLabel.setLayoutData(labelData);

		levelCombo = new Combo(levelComposite, SWT.DROP_DOWN);
		final FormData gdLevel = SWTUtilities.getFormData(levelCombo, 1, 15);
		gdLevel.top = new FormAttachment(levelLabel, -1, SWT.CENTER);
		gdLevel.left = new FormAttachment(levelLabel);
		levelCombo.setLayoutData(gdLevel);

		levelCombo.add("ANY");
		levelCombo.add("RED");
		levelCombo.add("YELLOW");

		levelCombo.setText("ANY");

		rtRecComposite = new RealtimeRecordedComposite(sourcesComposite);

		stationComposite = new StationComposite(appContext.getBean(MissionProperties.class).getStationMapper(),
				sourcesComposite);

		final Composite flushComposite = new Composite(sourcesComposite, SWT.NONE);
		flushComposite.setLayout(new GridLayout(3, false));
		final Label flushLabel = new Label(flushComposite, SWT.NONE);
		flushLabel.setText("Flush Cleared Alarms Every ");
		flushText = new Text(flushComposite, SWT.BORDER);
		final GridData gdFlush = SWTUtilities.getGridData(flushText, 1, 10);
		flushText.setLayoutData(gdFlush);
		final Label flushMinsLabel = new Label(flushComposite, SWT.NONE);
		flushMinsLabel.setText(" Minutes");

		final Label flushNoteLabel = new Label(sourcesComposite, SWT.NONE);
		flushNoteLabel.setText(" " + FLUSH_NOTE);

		final TabItem tableTab = new TabItem(folder, SWT.NONE);
		tableTab.setText("Table");

		final Composite tableComposite = new Composite(folder, SWT.NONE);
		fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		tableComposite.setLayout(fl);

		tableTab.setControl(tableComposite);

		tableConfigurer = new TableComposite(tableComposite, false);
		final Composite tableComp = tableConfigurer.getComposite();
		final FormData tableFd = new FormData();
		tableFd.top = new FormAttachment(colorComp, 0, 3);
		tableFd.left = new FormAttachment(0);
		tableFd.right = new FormAttachment(100);
		tableComp.setLayoutData(tableFd);

		final Composite composite = new Composite(prefShell, SWT.NONE);
		final GridLayout gl = new GridLayout(2, true);
		composite.setLayout(gl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(folder);
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		final Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Ok");
		prefShell.setDefaultButton(applyButton);
		final GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);
		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					try {
						final int minutes = Integer.parseInt(flushText.getText().trim());
						if (minutes < 0) {
							SWTUtilities.showMessageDialog(FastAlarmPreferencesShell.this.prefShell, "Invalid Number",
									"The flush interval for cleared alarms must be greater than 0.");
							return;                        
						}
						if (tableConfigurer.getTable().getActualColumnCount() == 0) {
						    SWTUtilities.showMessageDialog(FastAlarmPreferencesShell.this.prefShell, "No Columns",
						            "You cannot disable all the table columns, or the table will be blank.");
						    return;
						}
						applyChanges();
						flushInterval = minutes;
						if (moduleCombo != null) {
							selectedModule = moduleCombo.getText();
							if (selectedModule.equals(NO_FILTER)) {
								selectedModule = null;
							}
						}
						if (categoryCombo != null) {
							selectedCategory = categoryCombo.getText();
							if (selectedCategory.equals(NO_FILTER)) {
								selectedCategory = null;
							}
						}
						if (subsystemCombo != null) {
							selectedSubsystem = subsystemCombo.getText();
							if (selectedSubsystem.equals(NO_FILTER)) {
								selectedSubsystem = null;
							}
						}
						if (levelCombo.getText().equals("ANY")) {
							displayLevel = null;
						} else {
							displayLevel = Enum.valueOf(AlarmLevel.class, levelCombo.getText());
						}

						dirty = tableConfigurer.getDirty();
						if (dirty) {
							changeColumns = true;
						}
						useMonospace = monospaceButton.getSelection();
					} catch (final NumberFormatException ex) {
						SWTUtilities.showMessageDialog(FastAlarmPreferencesShell.this.prefShell, "Invalid Number",
								"You must enter the flush interval must be an integer.");
						return;
					}
					/*
					set the realtime/recorded
					 * enum and station to whatever is in the composites
					 */
					rtRecFilterType = rtRecComposite.getRealtimeRecordedFilterType();
					station = stationComposite.getStationId();
					FastAlarmPreferencesShell.this.prefShell.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
	                   FastAlarmPreferencesShell.this.prefShell.close();
				} 
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					FastAlarmPreferencesShell.this.canceled = true;
					FastAlarmPreferencesShell.this.prefShell.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}



	/**
	 * Determines if the selected columns to be displayed in the alarm table have changed
	 * 
	 * @return true if selected columns have changed, false otherwise
	 */
	public boolean needColumnChange() {
		return changeColumns;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
		super.getValuesIntoViewConfiguration(config);
		final FastAlarmViewConfiguration viewConfig = (FastAlarmViewConfiguration)config;
		if (tableConfigurer != null) {
			config.setTable(tableConfigurer.getTable());  
		}
		viewConfig.setModuleFilter(selectedModule);
		viewConfig.setSubsystemFilter(selectedSubsystem);
		viewConfig.setOpsCategoryFilter(selectedCategory);
		viewConfig.setResetFlushInterval(flushInterval);
		viewConfig.setLevelFilter(displayLevel);
		viewConfig.setUseMonospaceFont(useMonospace);
		final ChillFont f = config.getDataFont();
		if (useMonospace) {
			f.setFace(ChillFont.MONOSPACE_FACE);
		} else {
			f.setFace(ChillFont.DEFAULT_FACE);
		}
		config.setDataFont(f);
		/*
		 * Recorded/realtime flag is  an enum
		 * rather than a boolean. Temporarily map to back to an enum.
		 */
		/* Set realtime/recorded enum and
		 * station values in config */

		viewConfig.setRealtimeRecordedFilterType(rtRecFilterType);
		viewConfig.setStationId(station);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void setValuesFromViewConfiguration(final IViewConfiguration config) {
		super.setValuesFromViewConfiguration(config);
		dirty = false;
		changeColumns = false;
		final FastAlarmViewConfiguration alarmConfig = (FastAlarmViewConfiguration)config;
		if (!prefShell.isDisposed()) {
			flushText.setText(String.valueOf(alarmConfig.getResetFlushInterval()));
			if(alarmConfig.getLevelFilter()==null)
			{
				levelCombo.setText("ANY");
			}
			else
			{
				levelCombo.setText(alarmConfig.getLevelFilter().toString()); 
			}
			setSelectedCategory(alarmConfig.getOpsCategoryFilter());
			setSelectedModule(alarmConfig.getModuleFilter());
			setSelectedSubsystem(alarmConfig.getSubsystemFilter());
			channelTable = alarmConfig.getTable(FastAlarmViewConfiguration.ALARM_TABLE_NAME);
			if (tableConfigurer != null && !tableConfigurer.getComposite().isDisposed()) {
				tableConfigurer.init(channelTable);
				prefShell.layout();
				prefShell.pack();
			}
			if (monospaceButton != null) {
				monospaceButton.setSelection(alarmConfig.getUseMonospace());
			}
		}    
		/*
		 * Recorded/realtime flag is  an enum
		 * rather than a boolean. Temporarily map to a boolean.
		 * @ToDo("Make these buttons a 3-way selector")
		 */
		/* Set realtime/recorded and station
		 * values in composites from config */
		rtRecComposite.setRealtimeRecordedFilterType(alarmConfig.getRealtimeRecordedFilterType());
		stationComposite.setStationId(alarmConfig.getStationId());
	}

	private void setSelectedCategory(final String value) {
		if (categoryCombo == null) {
			return;
		}
		if (value == null) {
			categoryCombo.setText(NO_FILTER);
		} else {
			categoryCombo.setText(value);
		}
	}

	private void setSelectedSubsystem(final String value) {
		if (subsystemCombo == null) {
			return;
		}
		if (value == null) {
			subsystemCombo.setText(NO_FILTER);
		} else {
			subsystemCombo.setText(value);
		}
	}

	private void setSelectedModule(final String value) {
		if (moduleCombo == null) {
			return;
		}
		if (value == null) {
			moduleCombo.setText(NO_FILTER);
		} else {
			moduleCombo.setText(value);
		}
	}

}
