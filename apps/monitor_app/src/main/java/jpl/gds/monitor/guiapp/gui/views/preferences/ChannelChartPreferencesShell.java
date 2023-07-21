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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.guiapp.common.gui.StationComposite;
import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetEditorShell;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelSetUpdateListener;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.perspective.PromptSettings;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * ChannelChartPreferencesShell is the SWT GUI preferences window class for the
 * channel chart view..
 */
public class ChannelChartPreferencesShell extends AbstractViewPreferences implements ViewPreferencesShell  {

	/**
	 * Channel plot preferences title
	 */
	public static final String TITLE = "Channel Plot Preferences";
	
	private static final String INVALID_ENTRY = "Invalid Entry";

	private Button rangeCheckDn  = null;
	private Button rangeCheckEu  = null;
	private Button domainCheckErt  = null;
	private Button domainCheckScet = null;
	private Button domainCheckSCLK = null;
	private Button domainCheckChannelSelect = null;
	private Button domainChannelSelect = null;

	private ChannelChartViewConfiguration.XAxisChoice domainSelector;
	private ChannelChartViewConfiguration.YAxisChoice rangeSelector;
	private ChannelChartViewConfiguration.RenderingStyle renderingStyle;
	private ChannelChartViewConfiguration.RetentionTimeType retentionType;

	private final Shell parent;
	private Combo tickLabelCombo;
	private boolean verticalTicks;
	private FontComposite legendFontGetter;
	private ChillFont legendFont;
	private ColorComposite legendColorGetter;
	private ChillColor legendColor;
	private Combo renderingCombo;
	private RealtimeRecordedFilterType rtRecFilterType;
	private boolean useChannelName;
	private RealtimeRecordedComposite rtRecComposite;
	private StationComposite stationComposite;
	private int station;

	private Button useChannelNameButton;
	private TabFolder folder;
	private Combo timeFormatCombo;
	private Text hourText;
	private Text minutesText;
	private Text secondsText;
	private String domainTimeFormat;
	private int hours;
	private int minutes;
	private int seconds;
	private Combo retentionTypeCombo;
	private ChannelSet domainChannels;
	private ChannelSetEditorShell chanSetEdit;
    private final Tracer                                    trace;

	/**
	 * Creates an instance of ChannelChartPreferencesShell.
	 * @param parent the parent display of this widget
	 */
	public ChannelChartPreferencesShell(final ApplicationContext appContext, final Shell parent) {
		super(appContext, TITLE);
		this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		createControls();
		prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void setValuesFromViewConfiguration(final IViewConfiguration config) {
		super.setValuesFromViewConfiguration(config);
		final ChannelChartViewConfiguration chanConfig = (ChannelChartViewConfiguration)config;
		verticalTicks = chanConfig.isVerticalDomainTickLabels();
		if (tickLabelCombo != null && !tickLabelCombo.isDisposed()) {
			if (verticalTicks) {
				tickLabelCombo.setText("VERTICAL");
			} else {
				tickLabelCombo.setText("HORIZONTAL");
			}
		}
		legendColor = chanConfig.getLegendBackgroundColor();
		if (legendColor != null && legendColorGetter != null && !legendColorGetter.getComposite().isDisposed()) {
			legendColorGetter.setCurrentColor(legendColor);
		}
		legendFont = chanConfig.getLegendFont();
		if (legendFont != null && legendFontGetter != null && !legendFontGetter.getComposite().isDisposed()) {
			legendFontGetter.setCurrentFont(legendFont);
		}

		renderingStyle = chanConfig.getRenderingStyle();

		if (renderingCombo != null && !renderingCombo.isDisposed()) {
			renderingCombo.setText(renderingStyle.toString());
		}

		rangeSelector = chanConfig.getRangeValueSelector();
		domainSelector = chanConfig.getDomainValueSelector();
		
		domainChannels = chanConfig.getDomainChannels(appContext.getBean(IChannelDefinitionProvider.class));
		if (domainChannels == null){
			domainChannels = new ChannelSet();
		}
		
		if (domainChannels.size() == 0 && !domainChannelSelect.isDisposed()){
			domainChannelSelect.setEnabled(false);
		} else if (!domainChannelSelect.isDisposed()) {
			domainChannelSelect.setText(domainChannels.getIds()[0]);
		}

		if (domainCheckErt != null && !domainCheckErt.isDisposed()) {
			domainCheckErt.setSelection  ( false );
			domainCheckScet.setSelection ( false );
			domainCheckSCLK.setSelection ( false );
			domainCheckChannelSelect.setSelection(false);

			if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.ERT ) ) {
				domainCheckErt.setSelection ( true );
			} else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCET ) ) {
				domainCheckScet.setSelection ( true );
			} else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCLK ) ) {
				domainCheckSCLK.setSelection ( true );
			} else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.CHANNEL)){
				domainCheckChannelSelect.setSelection(true);
			}
		}
		if (rangeCheckDn != null && !rangeCheckDn.isDisposed()) {
			rangeCheckDn.setSelection(false);
			rangeCheckEu.setSelection(false);
			if ( rangeSelector.equals( ChannelChartViewConfiguration.YAxisChoice.Raw ) ) {
				rangeCheckDn.setSelection ( true );
			} else {
				rangeCheckEu.setSelection ( true );
			}
		}
		this.rtRecComposite.setRealtimeRecordedFilterType(chanConfig.getRealtimeRecordedFilterType());
		this.stationComposite.setStationId(chanConfig.getStationId());
		setUseChannelName(chanConfig.isUseChannelNameForLegend());

		hours = chanConfig.getDataRetentionHours();
		if (!hourText.isDisposed()) {
			hourText.setText(String.valueOf(hours));
		}
		minutes = chanConfig.getDataRetentionMinutes();
		if (!minutesText.isDisposed()) {
			minutesText.setText(String.valueOf(minutes));
		}
		
		seconds = chanConfig.getDataRetentionSeconds();
		if (!secondsText.isDisposed()) {
			secondsText.setText(String.valueOf(seconds));
		}
		domainTimeFormat = chanConfig.getDomainTimeFormat();
		if (timeFormatCombo != null && !timeFormatCombo.isDisposed()) {
			timeFormatCombo.setText(domainTimeFormat);
		}
		retentionType = chanConfig.getDataRetentionTimeType();
		if (!retentionTypeCombo.isDisposed()) {
			retentionTypeCombo.setText(retentionType.toString());
		}
		prefShell.pack();
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
		super.getValuesIntoViewConfiguration(config);
		final ChannelChartViewConfiguration chanConfig = (ChannelChartViewConfiguration)config;
		chanConfig.setVerticalDomainTickLabels(verticalTicks);
		chanConfig.setLegendFont(legendFont);
		chanConfig.setLegendBackgroundColor(legendColor);
		if (!chanConfig.getRangeValueSelector().equals(rangeSelector)) {
			chanConfig.setRangeValueSelector(rangeSelector);
		}
		if (!chanConfig.getDomainValueSelector().equals(domainSelector) || domainSelector == ChannelChartViewConfiguration.XAxisChoice.CHANNEL) {
			chanConfig.setDomainValueSelector(domainSelector);
			if (domainSelector == ChannelChartViewConfiguration.XAxisChoice.CHANNEL && domainChannels.size() > 0){
				chanConfig.setDomainLabel(domainSelector.name() + " " + domainChannels.getIds()[0]);
			}
			else{
				chanConfig.setDomainLabel(domainSelector.name());
			}
		}
		chanConfig.setRenderingStyle(renderingStyle);
		chanConfig.setRealtimeRecordedFilterType(this.rtRecFilterType);
		chanConfig.setStationId(this.station);
		chanConfig.setUseChannelNameForLegend(isUseChannelName());
		chanConfig.setDomainTimeFormat(domainTimeFormat);
		chanConfig.setDataRetentionHours(hours);
		chanConfig.setDataRetentionMinutes(minutes);
		chanConfig.setDataRetentionSeconds(seconds);
		chanConfig.setDataRetentionTimeType(retentionType);
		chanConfig.setDomainChannels(domainChannels);
	}

	/**
	 * Creates all the preferences controls and composites.
	 */
	protected void createControls() {
		final ChannelChartViewConfiguration config = new ChannelChartViewConfiguration(appContext);
		domainSelector = config.getDomainValueSelector();
		rangeSelector  = config.getRangeValueSelector ();

		// Set up the defaults
		domainSelector = domainSelector == null ? ChannelChartViewConfiguration.XAxisChoice.ERT : domainSelector;
		rangeSelector  = rangeSelector == null  ? ChannelChartViewConfiguration.YAxisChoice.Value: rangeSelector;

		prefShell = new Shell(parent, SWT.SHELL_TRIM);
		prefShell.setText(TITLE);
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		prefShell.setLayout(fl);

		folder = new TabFolder(prefShell, SWT.NONE);
		final FormData folderData = new FormData();
		folderData.left = new FormAttachment(0);
		folderData.right = new FormAttachment(100);
		folder.setLayoutData(folderData);

		createGeneralTab();
		createDataTab();
		createChartTab();

		final Label line = new Label(prefShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.top = new FormAttachment(folder, 15);
		formData6.left = new FormAttachment(0, 3);
		formData6.right = new FormAttachment(100, 3);
		line.setLayoutData(formData6);

		final Composite composite = new Composite(prefShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(line);
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

		applyButton.addSelectionListener(new ApplySelectionListener());
		
		cancelButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					ChannelChartPreferencesShell.this.canceled = true;
					ChannelChartPreferencesShell.this.prefShell.close();
				} catch (final RuntimeException e1) {
					e1.printStackTrace();
				}
			}
		});
	}


	private void setUseChannelName(final boolean enable) {
	    useChannelName = enable;
	    if (useChannelNameButton != null
	            && !useChannelNameButton.isDisposed()) {
			useChannelNameButton.setSelection(enable);
		}
	}

	private boolean isUseChannelName() {
		return useChannelName;
	}

	private void createGeneralTab() {
		final TabItem generalTab = new TabItem(folder, SWT.NONE);
		generalTab.setText("General");

		final Composite generalComposite = new Composite(folder, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		generalComposite.setLayout(fl);

		generalTab.setControl(generalComposite);

		titleText = new TitleComposite(generalComposite);
		final Composite titleComp = titleText.getComposite();
		final FormData fdLabel2 = new FormData();
		fdLabel2.top = new FormAttachment(0, 9);
		fdLabel2.left = new FormAttachment(0, 3);
		titleComp.setLayoutData(fdLabel2);

        fontGetter = new FontComposite(generalComposite, "View Font", trace);
		final Composite fontComp = fontGetter.getComposite();
		final FormData fontFd = new FormData();
		fontFd.top = new FormAttachment(titleComp);
		fontFd.left = new FormAttachment(0);
		fontFd.right = new FormAttachment(100);
		fontComp.setLayoutData(fontFd);

        foreColorGetter = new ColorComposite(generalComposite, "View Foreground Color", trace);
		final Composite foreColorComp = foreColorGetter.getComposite();
		final FormData foreColorFd = new FormData();
		foreColorFd.top = new FormAttachment(fontComp, 0, 10);
		foreColorFd.left = new FormAttachment(0);
		foreColorFd.right = new FormAttachment(100);
		foreColorComp.setLayoutData(foreColorFd);

        backColorGetter = new ColorComposite(generalComposite, "View Background Color", trace);
		final Composite colorComp = backColorGetter.getComposite();
		final FormData colorFd = new FormData();
		colorFd.top = new FormAttachment(foreColorComp, 0, 10);
		colorFd.left = new FormAttachment(0);
		colorFd.right = new FormAttachment(100);
		colorComp.setLayoutData(colorFd);
	}

	private void createDataTab() {

	    final TabItem dataTab = new TabItem(folder, SWT.NONE);
	    dataTab.setText("Data");

	    final Composite dataComposite = new Composite(folder, SWT.NONE);
	    final FormLayout fl = new FormLayout();
	    fl.marginHeight = 5;
	    fl.marginWidth = 5;
	    fl.spacing = 5;
	    dataComposite.setLayout(fl);

	    dataTab.setControl(dataComposite);

	    this.rtRecComposite = new RealtimeRecordedComposite(dataComposite);
	    FormData groupFd = new FormData();
	    groupFd.top = new FormAttachment(0,3);
	    groupFd.left = new FormAttachment(0);
	    this.rtRecComposite.getComposite().setLayoutData(groupFd);

	    this.stationComposite = new StationComposite(appContext.getBean(MissionProperties.class).getStationMapper(), dataComposite);
	    groupFd = new FormData();
	    groupFd.top = new FormAttachment(this.rtRecComposite.getComposite());
	    groupFd.left = new FormAttachment(0);
	    this.stationComposite.getComposite().setLayoutData(groupFd);

	    final Group domainGroup = new Group(dataComposite, SWT.None);
	    domainGroup.setLayout(new GridLayout(5, false));
	    domainGroup.setText("Domain (X Axis) Units");
	    final FormData dgFd = new FormData();
	    dgFd.top = new FormAttachment(this.stationComposite.getComposite(), 15);
	    dgFd.left = new FormAttachment(0);
	    domainGroup.setLayoutData(dgFd);

	    domainCheckErt  = new Button ( domainGroup, SWT.RADIO );
	    domainCheckScet = new Button ( domainGroup, SWT.RADIO );
	    domainCheckSCLK = new Button ( domainGroup, SWT.RADIO );
	    domainCheckChannelSelect = new Button (domainGroup, SWT.RADIO );
	    domainChannelSelect = new Button( domainGroup, SWT.PUSH );
	    domainCheckErt.setText ( "ERT" );
	    domainCheckScet.setText ( "SCET" );
	    domainCheckSCLK.setText ( "SCLK" );
	    domainCheckChannelSelect.setText ( "Channel");

	    domainChannelSelect.setText ( "Select Channel" );

	    domainCheckChannelSelect.addSelectionListener(new SelectionListener() {
	        @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
	            SystemUtilities.doNothing();			
	        }

	        @Override
            public void widgetSelected(final SelectionEvent arg0) {
	            try {
	                domainChannelSelect.setEnabled(domainCheckChannelSelect.getSelection());
	                if (!domainCheckChannelSelect.getSelection()) {
	                    domainChannelSelect.setText("Select Channel");
	                    domainChannels.clearChannels();
	                }
	                prefShell.layout();
	            } catch (final Exception e) {
	                e.printStackTrace();
	            }
	        }

	    });

	    domainChannelSelect.addSelectionListener(new SelectionListener(){

	        @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
	            SystemUtilities.doNothing();            
	        }

	        @Override
            public void widgetSelected(final SelectionEvent arg0) {
	            try {
                    chanSetEdit = new ChannelSetEditorShell(1,
                                                            appContext.getBean(IChannelUtilityDictionaryManager.class),
                                                            appContext.getBean(SprintfFormat.class));
	                domainChannelSelect.setEnabled(false);
	                chanSetEdit.popupShell(prefShell, new ChannelSetUpdateListener(){

	                    @Override
                        public boolean updateSet(final ChannelSet set) {
	                        domainChannels.clearChannels();
	                        domainChannels.loadFromString(appContext.getBean(IChannelDefinitionProvider.class), set.toString());

	                        if (domainChannels.size() == 0){
	                            domainChannelSelect.setText("Select Channel");
	                        }
	                        else{
	                            domainChannelSelect.setEnabled(true);
	                            domainChannelSelect.setText(domainChannels.getIds()[0]);
	                        }
	                        prefShell.layout();

							return true;
						}
						
					}, domainChannels, false);
					
					chanSetEdit.getShell().addDisposeListener(new DisposeListener() {
						@Override
                        public void widgetDisposed(
								final DisposeEvent event) {
							chanSetEdit = null;
							domainChannelSelect.setEnabled(domainCheckChannelSelect.getSelection());
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			
		});

		final Group rangeGroup = new Group(dataComposite, SWT.None);
		rangeGroup.setLayout(new GridLayout(2, false));
		rangeGroup.setText("Range (Y Axis) Units");
		final FormData rgFd = new FormData();
		rgFd.top = new FormAttachment(stationComposite.getComposite(), 15);
		rgFd.left = new FormAttachment(domainGroup, 15);
		rangeGroup.setLayoutData(rgFd);

		rangeCheckDn  = new Button ( rangeGroup, SWT.RADIO );
		rangeCheckEu  = new Button ( rangeGroup, SWT.RADIO );
		rangeCheckDn.setText ( "Raw" );
		rangeCheckEu.setText ( "Value" );
		rangeCheckEu.addSelectionListener(new SelectionListener(){

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                SystemUtilities.doNothing();            
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                if (rangeCheckEu.getSelection()) {
                    SWTUtilities
                        .showErrorDialog(
                            prefShell,
                            "Warning",
                            "Channel plotting will not plot channels of type Boolean or Status in Value mode");
                }
            }
		    
		});

		rangeCheckEu.setSelection ( false );
		if ( rangeSelector.equals ( ChannelChartViewConfiguration.YAxisChoice.Raw ) ) {
			rangeCheckDn.setSelection ( true );
		} else {
			rangeCheckEu.setSelection ( true );
		}

		domainCheckErt.setSelection  ( false );
		domainCheckScet.setSelection ( false );
		domainCheckSCLK.setSelection ( false );
		domainCheckChannelSelect.setSelection ( false );

		if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.ERT ) ) {
			domainCheckErt.setSelection ( true );
		} else if (domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCET ) ) {
			domainCheckScet.setSelection ( true );
		} else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCLK )) {
			domainCheckSCLK.setSelection ( true );
		} else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.CHANNEL)){
			domainCheckChannelSelect.setSelection( true );
		}

		final Group expireGroup = new Group(dataComposite, SWT.NONE);
		final FormData egFd = new FormData();
		egFd.top = new FormAttachment(rangeGroup, 15);
		egFd.left = new FormAttachment(0);
		expireGroup.setLayoutData(egFd);        
		expireGroup.setText("Expire data points older than:");
		final RowLayout rl = new RowLayout();
		rl.spacing = 10;
		expireGroup.setLayout(rl);

		hourText = new Text(expireGroup, SWT.NONE);
		final Label hourLabel = new Label(expireGroup, SWT.NONE);
		hourLabel.setText("Hours");
		RowData rd = SWTUtilities.getRowData(hourText, 1, 3);
		hourText.setLayoutData(rd);
		minutesText = new Text(expireGroup, SWT.NONE);
		final Label minutesLabel = new Label(expireGroup, SWT.NONE);
		minutesLabel.setText("Minutes");
		rd = SWTUtilities.getRowData(minutesText, 1, 3);
		minutesText.setLayoutData(rd);
		secondsText = new Text(expireGroup, SWT.NONE);
		final Label secondsLabel = new Label(expireGroup, SWT.NONE);
		secondsLabel.setText("Seconds");
		rd = SWTUtilities.getRowData(secondsText, 1, 3);
		secondsText.setLayoutData(rd);

		final Label retentionLabel = new Label(expireGroup, SWT.NONE);
		retentionLabel.setText("Using Time:");

		retentionTypeCombo = new Combo(expireGroup, SWT.DROP_DOWN);
		for (final ChannelChartViewConfiguration.RetentionTimeType type: ChannelChartViewConfiguration.RetentionTimeType.values()) {
			retentionTypeCombo.add(type.toString());
		}
	}

	private void createChartTab() {
		final TabItem chartTab = new TabItem(folder, SWT.NONE);
		chartTab.setText("Plot");

		final Composite chartComposite = new Composite(folder, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		chartComposite.setLayout(fl);

		chartTab.setControl(chartComposite);

        legendFontGetter = new FontComposite(chartComposite, "Legend Font", trace);
		final Composite legendFontComp = legendFontGetter.getComposite();
		final FormData legendFontFd = new FormData();
		legendFontFd.top = new FormAttachment(0);
		legendFontFd.left = new FormAttachment(0);
		legendFontFd.right = new FormAttachment(100);
		legendFontComp.setLayoutData(legendFontFd);

        legendColorGetter = new ColorComposite(chartComposite, "Legend Background Color", trace);
		final Composite legendColorComp = legendColorGetter.getComposite();
		final FormData legendColorFd = new FormData();
		legendColorFd.top = new FormAttachment(legendFontComp, 0, 3);
		legendColorFd.left = new FormAttachment(0);
		legendColorFd.right = new FormAttachment(100);
		legendColorComp.setLayoutData(legendColorFd);

		final Label renderingLabel = new Label(chartComposite, SWT.NONE);
		renderingLabel.setText("Line Rendering Style: ");
		final FormData renderLabelFd = new FormData();
		renderLabelFd.top = new FormAttachment(legendColorComp, 0 , 10);
		renderLabelFd.left = new FormAttachment(0, 4);
		renderingLabel.setLayoutData(renderLabelFd);

		renderingCombo = new Combo(chartComposite,SWT.DROP_DOWN | SWT.READ_ONLY);
		final FormData renderFd = SWTUtilities.getFormData(renderingCombo, 1, 30);
		renderFd.top = new FormAttachment(renderingLabel, 0 , SWT.CENTER);
		renderFd.left = new FormAttachment(renderingLabel);
		renderingCombo.setLayoutData(renderFd);
		final ChannelChartViewConfiguration.RenderingStyle[] styles = ChannelChartViewConfiguration.RenderingStyle.values();
		for ( int i = 0; i < styles.length; i++ ) {
			renderingCombo.add(styles[i].toString());
		}

		final Label tickOrientLabel = new Label(chartComposite, SWT.NONE);
		tickOrientLabel.setText("Domain (X Axis) Tick Label Orientation: ");
		final FormData fdLabel4 = new FormData();
		fdLabel4.top = new FormAttachment(renderingCombo, 15);
		fdLabel4.left = new FormAttachment(0, 4);
		tickOrientLabel.setLayoutData(fdLabel4);
		tickLabelCombo = new Combo(chartComposite, SWT.NONE);
		tickLabelCombo.add("VERTICAL");
		tickLabelCombo.add("HORIZONTAL");
		final FormData tickFd = SWTUtilities.getFormData(tickLabelCombo, 1, 20);
		tickFd.top = new FormAttachment(tickOrientLabel, 0, SWT.CENTER);
		tickFd.left = new FormAttachment(tickOrientLabel);
		tickLabelCombo.setLayoutData(tickFd);

		final Label timeLabel = new Label(chartComposite, SWT.NONE);
		timeLabel.setText("ERT/SCET Format");
		final FormData timeLabelFd = new FormData();
		timeLabelFd.top = new FormAttachment(tickLabelCombo, 15);
		timeLabelFd.left = new FormAttachment(0, 4);
		timeLabel.setLayoutData(timeLabelFd);
		timeFormatCombo = new Combo(chartComposite, SWT.DROP_DOWN);
		final FormData timeComboFd = new FormData();
		timeComboFd.top = new FormAttachment(timeLabel, 0, SWT.CENTER);
		timeComboFd.left = new FormAttachment(timeLabel);
		timeFormatCombo.setLayoutData(timeComboFd);
		final String[] formats = getConfiguredTimeFormats();
		timeFormatCombo.setItems(formats);
		timeFormatCombo.setText(formats[0]);

		useChannelNameButton = new Button(chartComposite, SWT.CHECK);
		useChannelNameButton.setText("Label Domain (X) Axis with Channel Name");
		final FormData useChannelFd = new FormData();
		useChannelFd.top = new FormAttachment(timeFormatCombo, 15);
		useChannelFd.left = new FormAttachment(0, 4);
		useChannelNameButton.setLayoutData(useChannelFd);
	}


	private String[] getConfiguredTimeFormats() {
	    final String[] timeFormats = appContext.getBean(PerspectiveProperties.class).getUtcTimeFormats().toArray(new String[] {});
		final List<String> timeFormatsNoExtendedPrecision = new ArrayList<String>();
		
		for (final String format : timeFormats) {
			if (!format.endsWith("e")) {
				timeFormatsNoExtendedPrecision.add(format);
			}
		}
		
		String[] formats = new String[timeFormatsNoExtendedPrecision.size()];
		formats = timeFormatsNoExtendedPrecision.toArray(formats);
		
		return formats;
	}
	
	/**
	 * Selection Listener class for the APPLY button.
	 */
	private class ApplySelectionListener extends SelectionAdapter {

	    /**
	     * Validates user entries.
	     * 
	     * @return true of entries ok, false if not
	     */
	    private boolean validateFields() {
	        int tempHours = 0;
	        try {
	            if (!hourText.getText().trim().equals("")) {
	                tempHours = Integer.parseInt(hourText.getText());
	            }
	            if (tempHours < 0) {
	                SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Hours must be a number greater than or equal to 0");
	                return false;
	            }
	        } catch (final NumberFormatException ex) {
	            SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Hours must be a number greater than or equal to 0");
	            return false;
	        }
	        int tempMinutes  = 0;
	        try {
	            if (!minutesText.getText().trim().equals("")) {
	                tempMinutes = Integer.parseInt(minutesText.getText());
	            }
	            if (tempMinutes < 0) {
	                SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Minutes must be a number greater than or equal to 0");
	                return false;
	            }
	        } catch (final NumberFormatException ex) {
	            SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Minutes must be a number greater than or equal to 0");
	            return false;
	        }

	        int tempSeconds  = 0;
	        try {
	            if (!secondsText.getText().trim().equals("")) {
	                tempSeconds = Integer.parseInt(secondsText.getText());
	            }
	            if (tempSeconds < 0) {
	                SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Seconds must be a number greater than or equal to 0");
	                return false;
	            }
	        } catch (final NumberFormatException ex) {
	            SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Seconds must be a number greater than or equal to 0");
	            return false;
	        }
	        if (tempHours == 0 && tempMinutes == 0 && tempSeconds == 0) {
	            SWTUtilities.showErrorDialog(prefShell, INVALID_ENTRY, "Hours, minutes, and seconds cannot all be 0");
	            return false;
	        } 
	        return true;
	    }
	    /**
	     * {@inheritDoc}
	     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	     */
	    @Override
	    public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
	        try {

	            /*
	             * Validate fields first.
	             */
	            if (!validateFields()) {
	                return;
	            }

	            /*
	             * Prompt for confirmation if it's not turned off.
	             */
	            final PromptSettings settings = new PromptSettings();
	            if (settings.showMonitorPlotPreferencesConfirmation()) {
	                final ConfirmationShell confirmShell = new ConfirmationShell(prefShell,
	                        "Changing plot preferences will clear all data currently on the plot.\n" +
	                                "Are you sure you want to update the preferences?",
                                                                                 true);
	                confirmShell.open();
	                while (!confirmShell.getShell().isDisposed()) {
	                    if (!confirmShell.getShell().getDisplay().readAndDispatch())
	                    {
	                        confirmShell.getShell().getDisplay().sleep();
	                    }
	                }

	                /*
	                 * Save the state of the "prompt again" flag in the confirmation
	                 * window for next time.
	                 */
	                final boolean promptAgain = confirmShell.getPromptAgain();
	                if (!promptAgain) {
	                    settings.setMonitorPlotPreferencesConfirmation(false);
	                    settings.save();
	                }

	                /*
	                 * User did not confirm changes, so go no further.
	                 */
	                if (confirmShell.wasCanceled()) {
	                    return;
	                }
	            }

	            /*
	             * User confirmed changes.
	             */
	            applyChanges();
	            verticalTicks = tickLabelCombo.getText().equalsIgnoreCase("Vertical");
	            legendFont = legendFontGetter.getCurrentFont();
	            legendColor = legendColorGetter.getCurrentColor();
	            renderingStyle = ChannelChartViewConfiguration.RenderingStyle.valueOf(renderingCombo.getText());
	            final ChannelChartViewConfiguration.XAxisChoice origDomainSelector = domainSelector;
	            if ( domainCheckErt.getSelection() ) {
	                domainSelector = ChannelChartViewConfiguration.XAxisChoice.ERT;
	            } else if ( domainCheckScet.getSelection() ) {
	                domainSelector = ChannelChartViewConfiguration.XAxisChoice.SCET;
	            } else if ( domainCheckSCLK.getSelection() ) {
	                domainSelector = ChannelChartViewConfiguration.XAxisChoice.SCLK;
	            } else if ( domainCheckChannelSelect.getSelection() ){
	                domainSelector = ChannelChartViewConfiguration.XAxisChoice.CHANNEL;
	            }
	            if (rangeCheckDn.getSelection()) {
	                rangeSelector = ChannelChartViewConfiguration.YAxisChoice.Raw;
	            }
	            else {
	                rangeSelector = ChannelChartViewConfiguration.YAxisChoice.Value;
	            }
	            if (domainCheckChannelSelect.getSelection() && (domainChannels == null || domainChannels.size() == 0)) {
	                SWTUtilities.showErrorDialog(prefShell, "No Channel Selected", "You must select a channel for the X Axis");
	                return;
	            }
	            if (domainCheckChannelSelect.getSelection()) {
	                final String id = domainChannels.getIds()[0];
	                final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(id);
	                if (def.getChannelType().equals(ChannelType.ASCII)) {
	                    SWTUtilities.showErrorDialog(prefShell, "Bad Domain Channel", "You cannot use channel " + id + " for the domain (X) axis because it has a string value.");
	                    return;
	                } else if (rangeCheckEu.getSelection() && (def.getChannelType().equals(ChannelType.STATUS) || def.getChannelType().equals(ChannelType.BOOLEAN))) {
	                    SWTUtilities.showErrorDialog(prefShell, "Bad Domain Channel", "You cannot use channel " + id + "'s value for the domain (X) axis because it has a string value.");
	                    return;
	                }

	            }
	            if (!origDomainSelector.equals(domainSelector)) {
	                final boolean proceed = SWTUtilities.showConfirmDialog(prefShell, "Confirm Domain (X Axis) Change", "Changing the plot domain will result in the loss of all\ncurrent data points on the plot. Are you sure you want to proceed?");
	                if (!proceed) {
	                    domainSelector = origDomainSelector;
	                    if (domainCheckErt != null && !domainCheckErt.isDisposed()) {
	                        domainCheckErt.setSelection  ( false );
	                        domainCheckScet.setSelection ( false );
	                        domainCheckSCLK.setSelection ( false );
	                        domainCheckChannelSelect.setSelection(false);

	                        if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.ERT ) ) {
	                            domainCheckErt.setSelection ( true );
	                        } else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCET ) ) {
	                            domainCheckScet.setSelection ( true );
	                        } else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.SCLK ) ) {
	                            domainCheckSCLK.setSelection ( true );
	                        } else if ( domainSelector.equals( ChannelChartViewConfiguration.XAxisChoice.CHANNEL)){
	                            domainCheckChannelSelect.setSelection(true);
	                        }
	                    }

	                    return;
	                }
	            }
	            /*
	             * RT/Recorded filter type is an enum
	             * and comes from the shared composite. Station has been added.
	             */ 
	            rtRecFilterType = rtRecComposite.getRealtimeRecordedFilterType();
	            station = stationComposite.getStationId();
	            useChannelName = useChannelNameButton.getSelection();
	            domainTimeFormat = timeFormatCombo.getText();
	            hours = Integer.parseInt(hourText.getText());
	            minutes = Integer.parseInt(minutesText.getText());
	            seconds = Integer.parseInt(secondsText.getText());
	            retentionType = Enum.valueOf(ChannelChartViewConfiguration.RetentionTimeType.class, retentionTypeCombo.getText().trim());
	            if (chanSetEdit != null) {
	                chanSetEdit.close();
	            }

	            ChannelChartPreferencesShell.this.prefShell.close();

	        } catch (final RuntimeException e1) {
	            e1.printStackTrace();
	        } 
	    }
	}
}
