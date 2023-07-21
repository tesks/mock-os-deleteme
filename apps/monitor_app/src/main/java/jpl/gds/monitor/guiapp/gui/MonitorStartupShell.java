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

import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.gui.DictionarySelectionComposite;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * MonitorStartupShell is the initial GUI window in chill_monitor. It allows a user to enter
 * the configuration values needed to construct a topic name, message service 
 * host connection information, and dictionary selections.
 *
 */
public class MonitorStartupShell implements ChillShell {
    
    private final Set<String> projectVenues;
    private final Tracer tracer;
    
    private static final String ANY = "[Any]";
    private static final String ALL = "[All]";

    /**
     * Monitor startup shell title
     */
    public static final String TITLE = "Monitor Startup";
    private Shell topicShell;
    private final Shell parent;
    private Text jmsPortText;
    private Text jmsHostText;
    private Combo venueTypeCombo;
    private Combo scidCombo;
    private Combo vcidCombo;
    private Combo dssidCombo;
    private boolean canceled;
    private Button dictionaryCheck;
    private MessageServiceConfiguration jmsConfig;
    private DictionarySelectionComposite dictComp;
    private boolean autoLoadDict = true;
    private boolean runMonitor = false;
    private final ApplicationContext appContext;
    private TopicEntryComposite topicComposite;

    /**
     * Creates an instance of MonitorStartupShell.
     * @param appContext the current application context
     * @param parent the parent Shell
     */
    public MonitorStartupShell(final ApplicationContext appContext, final Shell parent) {
    	this.appContext = appContext;
        this.parent = parent;
        
        this.tracer = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        
        final MissionProperties mp = appContext.getBean(MissionProperties.class);
        
        projectVenues = mp.getAllowedVenueTypesAsStrings();
        
        jmsConfig = appContext.getBean(MessageServiceConfiguration.class);
        
        createControls();
        topicShell.setLocation(100, 100);
    }

    /**
     * Creates an instance of MonitorStartupShell.
     * @param appContext the current application context
     * @param display the parent Display
     */
    public MonitorStartupShell(final ApplicationContext appContext, final Display display) {
        this(appContext,  new Shell(display, SWT.APPLICATION_MODAL
				    | SWT.DIALOG_TRIM | SWT.RESIZE));
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        topicShell = parent;
        topicShell.setText(TITLE);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        topicShell.setLayout(shellLayout);

        venueTypeCombo = new Combo(topicShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        final Label venueTypeLabel = new Label(topicShell, SWT.NONE);
        venueTypeLabel.setText("Venue Type: ");
        final FormData vtLabelForm = new FormData();
        vtLabelForm.top = new FormAttachment(0, 5);
        vtLabelForm.left = new FormAttachment(0, 5);
        venueTypeLabel.setLayoutData(vtLabelForm);
        final FormData vtForm = SWTUtilities.getFormData(venueTypeCombo, 1, 20);
        vtForm.left = new FormAttachment(venueTypeLabel);
        vtForm.top = new FormAttachment(venueTypeLabel, 0, SWT.CENTER);
        venueTypeCombo.setLayoutData(vtForm);

        final Group filterGroup = new Group(topicShell, SWT.BORDER);
        filterGroup.setText("Subscription Filters");
        final RowLayout filterLayout = new RowLayout();
        filterLayout.spacing = 10;
        filterLayout.marginHeight = 5;
        filterGroup.setLayout(filterLayout);
        final FormData filterGroupData = new FormData();
        filterGroupData.left = new FormAttachment(0);
        filterGroupData.top = new FormAttachment(venueTypeCombo, 7);
        filterGroupData.right = new FormAttachment(100);
        filterGroup.setLayoutData(filterGroupData);
        
        final Label scidLabel = new Label(filterGroup, SWT.NONE);
        scidLabel.setText("Spacecraft ID:"); 
        scidCombo = new Combo(filterGroup, SWT.DROP_DOWN);
        final RowData scidComboData = new RowData();
        scidComboData.width = 75;
        scidCombo.setLayoutData(scidComboData);
      
        final Label vcidLabel = new Label(filterGroup, SWT.NONE);
        vcidCombo = new Combo(filterGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        vcidLabel.setText("Virtual Channel ID: ");
        
        final Label dssIdLabel = new Label(filterGroup, SWT.NONE);
        dssidCombo = new Combo(filterGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        dssIdLabel.setText("DSS ID: ");

        jmsConfig = appContext.getBean(MessageServiceConfiguration.class);

        final Group messageGroup = new Group(topicShell, SWT.BORDER);
        messageGroup.setText("Message Service Connection");
        final RowLayout messageLayout = new RowLayout();
        messageLayout.spacing = 10;
        messageGroup.setLayout(messageLayout);
        final FormData messageGroupData = new FormData();
        messageGroupData.left = new FormAttachment(0);
        messageGroupData.top = new FormAttachment(filterGroup, 5);
        messageGroupData.right = new FormAttachment(100);
        messageGroup.setLayoutData(messageGroupData);
        
        final Label jmsHostLabel = new Label(messageGroup, SWT.NONE);
        jmsHostLabel.setText("Message Service Host:");
        jmsHostText = new Text(messageGroup, SWT.BORDER);
        jmsHostText.setText(jmsConfig.getMessageServerHost());
        final RowData jmsHostData = new RowData();
        jmsHostData.width = 200;
        jmsHostText.setLayoutData(jmsHostData);

        final Label jmsPortLabel = new Label(messageGroup, SWT.NONE);
        jmsPortLabel.setText("Message Service Port:");
        jmsPortText = new Text(messageGroup, SWT.BORDER);
        jmsPortText.setText(String.valueOf(jmsConfig.getMessageServerPort()));
     
        this.topicComposite = new TopicEntryComposite(topicShell, appContext);
        final Composite innerComp = this.topicComposite.getComposite();
        final FormData topicForm = new FormData();
        topicForm.left = new FormAttachment(0);
        topicForm.top = new FormAttachment(messageGroup);
        topicForm.right = new FormAttachment(100);
        innerComp.setLayoutData(topicForm);

        
        dictionaryCheck = new Button(topicShell, SWT.CHECK);
        dictionaryCheck.setText("Automatically Load Dictionaries for Current Session");
        final FormData dcbfd = new FormData();
        dcbfd.left = new FormAttachment(0, 5);
        dcbfd.top = new FormAttachment(innerComp, 5);
        dictionaryCheck.setLayoutData(dcbfd);
        dictionaryCheck.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(final SelectionEvent e) {
                try {
                    dictComp.enable(!dictionaryCheck.getSelection());
                    autoLoadDict = dictionaryCheck.getSelection();
                } catch (final Exception ex) {
                    tracer.error("Unexpected error processing GUI event: " + ExceptionTools.getMessage(ex), ex);
                }
            }
        });
        dictionaryCheck.setSelection(true);

        dictComp = new DictionarySelectionComposite(appContext.getBean(MissionProperties.class),
                                                    appContext.getBean(SseContextFlag.class), topicShell);
        final FormData dcfd = new FormData();
        dcfd.top = new FormAttachment(dictionaryCheck);
        dcfd.left = new FormAttachment(0);
        dcfd.right = new FormAttachment(100);
        dictComp.getComposite().setLayoutData(dcfd);
        dictComp.enable(false);

        final Composite composite = new Composite(topicShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        formData8.top = new FormAttachment(dictComp.getComposite());
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Run");
        topicShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");
        final Label line = new Label(topicShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);
        applyButton.addSelectionListener(new ApplyButtonHandler());

        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    canceled = true;
                    topicShell.close();
                } catch (final Exception eE) {
                    TraceManager.getDefaultTracer().error ( "CANCEL button caught unhandled and unexpected exception in MonitorStartupShell.java", eE);
                }
            }
        });
        
        venueTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String text = venueTypeCombo.getText();
                    final VenueType vt = VenueType.valueOf(text);
                    topicComposite.setVenue(vt);
                    dictComp.setVenue(vt);
                    topicShell.pack();
                } catch (final Exception eE) {
                    TraceManager.getDefaultTracer().error ( "VENUE-COMBO caught unhandled and unexpected exception in MonitorStartupShell.java", eE);
                }
            }
        });
        
        populateFromContext();
        
        topicShell.pack();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        topicShell.open();
        // This addresses a bug in the SWT 3.2 library in which the text
        // does not appear upon open in some fields
     //   userText.setText(userText.getText());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return topicShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return canceled;
    }

    /**
     * Sets the venue
     * @param vt the VenueType to set
     */
    private void setVenueType(final VenueType vt) {
        if (!topicShell.isDisposed() && vt != null) {
            venueTypeCombo.setText(vt.toString());
        }
    }

    /**
     * Sets the spacecraft ID
     * @param id the scid to set
     */
    private void setScid(final int id) {
        if (!topicShell.isDisposed() && id != 0) {
            scidCombo.setText(String.valueOf(id));
        }
    }

    private void setDssId(final Integer id) {
        if (topicShell.isDisposed()) {
            return;
        }
        if (id != null) {
            dssidCombo.setText(String.valueOf(id));
        } else {
            dssidCombo.setText(ALL);
        }
    }

    /**
     * Sets the Virtual Channel ID
     * @param id the vcid to set
     */
    private void setVcid(final Integer id) {
        //need to do a bit more work here since 32 is not the same as 32 - B
        if (!topicShell.isDisposed() && id != null) {
            final String[] items = vcidCombo.getItems();
            for (final String item : items) {
                final int endIndex = item.indexOf(' ');
                if(endIndex != -1) {
                    final Integer itemInCombo = Integer.valueOf(String.valueOf(item).substring(0, endIndex));

                    if(id.equals(itemInCombo)) {
                        vcidCombo.setText(item);
                        break;
                    }
                }
            }
        }
    }


    private void populateFromContext() {
     
        if (!topicShell.isDisposed()) {
            
            final IContextConfiguration config = appContext.getBean(IContextConfiguration.class);
            
            initVenueTypes();
            setVenueType(config.getVenueConfiguration().getVenueType());           
            initScids();
            initVcids();    
            initDssids();
            topicComposite.populateFromContext();
            setScid(config.getContextId().getSpacecraftId());
            setVcid(config.getFilterInformation().getVcid());
            setDssId(config.getFilterInformation().getDssId());
            dictComp.setDictionaryConfiguration(config.getDictionaryConfig());
            jmsHostText.setText(jmsConfig.getMessageServerHost());
            jmsPortText.setText(String.valueOf(jmsConfig.getMessageServerPort()));
            setAutoLoadDictionary();
            dictComp.setVenue(config.getVenueConfiguration().getVenueType());
        }
    }

    private void initVenueTypes()
    {
        venueTypeCombo.removeAll();
      
        for(final String v : projectVenues)
        {
            venueTypeCombo.add(v);
        }
    }

    private void initScids()
    {
        final List<Integer> ids = appContext.getBean(MissionProperties.class).getAllScids();
        scidCombo.add(ANY);
        for (final int id: ids)
        {
            scidCombo.add(String.valueOf(id));
        }
        scidCombo.setText(ANY);
    }

    private void initVcids()
    {
    	final MissionProperties mp = appContext.getBean(MissionProperties.class);
    	
        final List<Integer> ids = mp.getAllDownlinkVcids();
        vcidCombo.add(ALL);
        for (int i = 0; i < ids.size(); i++)
        {
            vcidCombo.add(String.valueOf(ids.get(i)) + " - " + mp.mapDownlinkVcidToName(ids.get(i)));
        }
        vcidCombo.setText(ALL);
    }
    
    private void initDssids()
    {

        final MissionProperties mp = appContext.getBean(MissionProperties.class);

        final Integer[] dssIds = mp.getStationMapper().getStationIds();
        dssidCombo.add(ALL);
        for (int i = 0; i < dssIds.length; i++)
        {
            dssidCombo.add(String.valueOf(dssIds[i]));
        }
        dssidCombo.setText(ALL);
    }

    private void setAutoLoadDictionary() {
        this.autoLoadDict = appContext.getBean(GeneralMessageDistributor.class).isAutoLoadDictionary();
        if (this.dictionaryCheck != null && !this.dictionaryCheck.isDisposed())
        {
            this.dictionaryCheck.setSelection(this.autoLoadDict);
            dictComp.enable(! this.autoLoadDict);
        }
    }

    /**
     * Gets the flag indicating whether the user wants to auto-load dictionaries.
     * @return true if user wants to auto-load, false if not
     */
    public boolean isAutoLoadDictionary() {
        return this.autoLoadDict;
    }

    /**
     * Indicates if user elected to process with the monitor run.
     * @return true if user selected "run", false if not
     */
    public boolean getRunMonitor() {
        return runMonitor;
    }
    
    /**
     * Class to handle the APPLY button selection event.
     *
     */
    public class ApplyButtonHandler implements SelectionListener {
            
        private boolean validate() {
            
            if (! isAutoLoadDictionary() && ! dictComp.checkFields())
            {
                SWTUtilities.showErrorDialog(topicShell, "Empty Fields", "You must fill in all dictionary fields.");
                return false;
            }
            
            final String venueText = venueTypeCombo.getText();
            final VenueType vt = VenueType.valueOf(venueText);
            
            if (!topicComposite.validate(vt)) {
                return false;
            }
            
            final String jmsHost = jmsHostText.getText().trim();
            if (jmsHost.equals("")) {
                SWTUtilities.showMessageDialog(topicShell, "Invalid Message Service Host",
                        "You must enter the message service host name.");
                return false;
            }

            final String jmsPort = jmsPortText.getText().trim();
            if (jmsPort.equals("")) {
                SWTUtilities.showMessageDialog(topicShell, "Invalid Message Service Port",
                        "You must enter the message service port number.");
                return false;
            } else {
                try {
                    Integer.parseInt(jmsPort);
                } catch (final NumberFormatException x) {
                    SWTUtilities.showMessageDialog(topicShell, "Invalid Message Service Port",
                            "Message service port must be a positive integer.");
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
            try {
                
                validate();
       
                runMonitor = true;
                
                populateAppContext();
                
                jmsConfig.setMessageServerHost(jmsHostText.getText().trim());
                jmsConfig.setMessageServerPort(Integer.valueOf(jmsPortText.getText().trim()));

                topicShell.close();
            } catch ( final Exception eE ) {
                TraceManager.getDefaultTracer().error("APPLY button caught unhandled and unexpected exception in MonitorStartupShell.java", e);
            }
        }
        
        private void populateAppContext() {
            
            final String venueText = venueTypeCombo.getText();
            final VenueType vt = VenueType.valueOf(venueText);
            appContext.getBean(IVenueConfiguration.class).setVenueType(vt);
            
            final String vcid = vcidCombo.getText();
            if (vcid.equals(ALL)) {
                 appContext.getBean(IContextFilterInformation.class).setVcid(null);
            } else {
                final int endIndex = vcid.indexOf(' ');
                appContext.getBean(IContextFilterInformation.class).setVcid(Integer.valueOf(vcid.substring(0, endIndex)));
            }
            
            final String scid = scidCombo.getText();
            if (scid.equals(ANY)) {
                 appContext.getBean(IContextIdentification.class).setSpacecraftId(0);
            } else {
                 appContext.getBean(IContextIdentification.class).setSpacecraftId(Integer.parseInt(scid));
            }
            
            final String dssId = dssidCombo.getText();
            if (dssId.equals(ALL)) {
                appContext.getBean(IContextFilterInformation.class).setDssId(0);
            } else {
                appContext.getBean(IContextFilterInformation.class).setDssId(Integer.valueOf(dssId));
            }
            
            final DictionaryProperties dictConfig = appContext.getBean(DictionaryProperties.class);
            
            dictConfig.setFswDictionaryDir(dictComp.getFswDictionaryDir());
            dictConfig.setFswVersion(dictComp.getFswVersion());
            if (appContext.getBean(MissionProperties.class).missionHasSse()) {
                dictConfig.setSseDictionaryDir(dictComp.getSseDictionaryDir());
                dictConfig.setSseVersion(dictComp.getSseVersion());
            }
            
            topicComposite.populateAppContext();
            
            appContext.getBean(GeneralMessageDistributor.class).setAutoLoadDictionary(dictionaryCheck.getSelection());
        }

        @Override
        public void widgetDefaultSelected(final SelectionEvent arg0) {
            // do nothing
        }
    }
}
