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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * A GUI composite for entering a list of topic names.
 * @since R8
 */
public class TopicEntryComposite {

    private final Composite parent;
    private Composite mainComposite;
    private Text topicText;
    private Button addButton;
    private List topicList;
    private Button removeButton;
    private Button defaultButton;
    private final Color disableColor;
    private final Color enableColor;
    private Combo jmsSubtopicCombo;
    private Group topicEntryGroup;
    private Group sessionEntryGroup;
    private Text userText;
    private Text hostText;
    private Combo testbedCombo;   
    private Combo streamIdCombo;
    private final ApplicationContext appContext;
    private final Tracer tracer;
    private Collection<String> defaultTopics;

    /**
     * Constructor.
     * 
     * @param parent the parent composite
     * @param appContext the current application context
     */
    public TopicEntryComposite(final Composite parent, final ApplicationContext appContext) {
        this.parent = parent;
        this.appContext = appContext;
        this.tracer = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        disableColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.LIGHT_GREY));
        enableColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.WHITE));
        createGui();
    }
    
    /**
     * Creates the GUI components.
     */
    private void createGui() {
        this.mainComposite = new Composite(parent, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 5;
        fl.marginHeight = 3;
        fl.marginBottom = 3;
        fl.marginWidth = 5;
        this.mainComposite.setLayout(fl);
             
        this.defaultButton = new Button(this.mainComposite, SWT.CHECK);
        defaultButton.setText("Override Default Topics");
        final FormData defButtonData = new FormData();
        defButtonData.left = new FormAttachment(0);
        defButtonData.top = new FormAttachment(0);
        defaultButton.setLayoutData(defButtonData);
    
        this.defaultButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                // do nothing               
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                enableTopicEntry(defaultButton.getSelection());
                
            }
            
        });
        
        createSessionEntryGroup();
        createTopicEntryGroup();
        
        populateFromContext();
        
    }
    
    /**
     * Populates the current application context from GUI fields.
     */
    public void populateFromContext() {
        final VenueType vt = appContext.getBean(IVenueConfiguration.class).getVenueType();
        this.userText.setText(appContext.getBean(IContextIdentification.class).getUser());
        this.hostText.setText(appContext.getBean(IContextIdentification.class).getHost());
        setTopics(appContext.getBean(IGeneralContextInformation.class).getSubscriptionTopics());
        initTestbedNames(vt);
        initDownlinkStreamIds(vt);
        initSubtopics(vt);
        setVenue(vt);
        enableTopicEntry(defaultButton.getSelection());
    }

  private void createSessionEntryGroup() {
        
        sessionEntryGroup = new Group(this.mainComposite, SWT.BORDER);
        sessionEntryGroup.setText("Session Topic Parameters");
        final GridLayout fl = new GridLayout();
        fl.numColumns = 4;
        
        sessionEntryGroup.setLayout(fl);
        final FormData sessionEntryData = new FormData();
        sessionEntryData.left = new FormAttachment(0);
        sessionEntryData.right = new FormAttachment(100);
        sessionEntryData.top = new FormAttachment(this.defaultButton);
        sessionEntryGroup.setLayoutData(sessionEntryData);
        
        final Label testbedLabel = new Label(sessionEntryGroup, SWT.NONE);
        testbedCombo = new Combo(sessionEntryGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        testbedLabel.setText("Testbed Name: ");
        final GridData tbComboData = new GridData();
        tbComboData.widthHint = 150;
        tbComboData.horizontalAlignment = GridData.FILL_HORIZONTAL;        
        testbedCombo.setLayoutData(tbComboData);
        
        final Label streamLabel = new Label(sessionEntryGroup, SWT.NONE);
        streamLabel.setText("Downlink Stream ID:");
        streamIdCombo = new Combo(sessionEntryGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        final GridData streamComboData = new GridData();
        streamComboData.widthHint = 150;
        streamComboData.horizontalAlignment = GridData.FILL_HORIZONTAL;  
        streamIdCombo.setLayoutData(streamComboData);

        final Label userLabel = new Label(sessionEntryGroup, SWT.RIGHT);
        userLabel.setText("User Name:");
        userText = new Text(sessionEntryGroup, SWT.SINGLE | SWT.BORDER);
        final GridData userTextData = new GridData();
        userTextData.widthHint = 150;
        userTextData.horizontalAlignment = GridData.FILL_HORIZONTAL;  
        userText.setLayoutData(userTextData);

        final Label hostLabel = new Label(sessionEntryGroup, SWT.NONE);
        hostLabel.setText("Host Name:");
        hostText = new Text(sessionEntryGroup, SWT.SINGLE | SWT.BORDER);
        final GridData hostTextData = new GridData();
        hostTextData.widthHint = 250;
        hostTextData.horizontalAlignment = GridData.FILL_HORIZONTAL;  
        hostText.setLayoutData(hostTextData);
        
        final Label jmsSubtopicLabel = new Label(sessionEntryGroup, SWT.NONE);
        jmsSubtopicLabel.setText("Message Subtopic: ");
        jmsSubtopicCombo = new Combo(sessionEntryGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        final GridData jmsComboData = new GridData();
        jmsComboData.widthHint = 200;
        jmsComboData.horizontalAlignment = GridData.FILL_HORIZONTAL;  
        jmsSubtopicCombo.setLayoutData(jmsComboData);
        
    }
    
    private void createTopicEntryGroup() {
        topicEntryGroup = new Group(this.mainComposite, SWT.BORDER);
        topicEntryGroup.setText("Custom Topic Entry");
        final FormLayout fl = new FormLayout();
        fl.spacing = 5;
        fl.marginHeight = 3;
        fl.marginBottom = 3;
        fl.marginTop = 3;
        fl.marginWidth = 5;
        topicEntryGroup.setLayout(fl);
        final FormData topicEntryData = new FormData();
        topicEntryData.left = new FormAttachment(0);
        topicEntryData.right = new FormAttachment(100);
        topicEntryData.top = new FormAttachment(sessionEntryGroup,3);
        topicEntryGroup.setLayoutData(topicEntryData);
        
        final Label topicTextLabel = new Label(topicEntryGroup, SWT.NONE);
        topicTextLabel.setText("New Topic:");
        final FormData topicLabelData = new FormData();
        topicLabelData.left = new FormAttachment(0);
        topicLabelData.top = new FormAttachment(0);
        topicTextLabel.setLayoutData(topicLabelData);
        
        this.topicText = new Text(topicEntryGroup, SWT.SINGLE | SWT.BORDER);
        final FormData topicTextData = SWTUtilities.getFormData(topicText, 1, 80);
        topicTextData.left = new FormAttachment(10);
        topicTextData.top = new FormAttachment(this.defaultButton);
        topicTextData.right = new FormAttachment(85);
        topicText.setLayoutData(topicTextData); 
        
        this.addButton = new Button(topicEntryGroup, SWT.PUSH);
        this.addButton.setText("Add");
        final FormData addButtonData = new FormData();
        addButtonData.left = new FormAttachment(this.topicText);
        addButtonData.top = new FormAttachment(this.topicText, -5, SWT.TOP);
        addButtonData.right = new FormAttachment(100);
        addButton.setLayoutData(addButtonData);
        this.addButton.setEnabled(false);
        
        final Label topicListLabel =  new Label(topicEntryGroup, SWT.NONE);
        topicListLabel.setText("Topics:");
        final FormData listLabelData = new FormData();
        listLabelData.left = new FormAttachment(0);
        listLabelData.top = new FormAttachment(this.topicText);
        topicListLabel.setLayoutData(listLabelData);
        
        this.topicList = new List(topicEntryGroup, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        final FormData topicListData = SWTUtilities.getFormData(topicList, 3, 80);
        topicListData.left =  new FormAttachment(10);
        topicListData.top = new FormAttachment(this.topicText);
        topicListData.right = new FormAttachment(85);
        this.topicList.setLayoutData(topicListData);
        
        this.removeButton = new Button(topicEntryGroup, SWT.PUSH);
        this.removeButton.setText("Remove");
        final FormData removeButtonData = new FormData();
        removeButtonData.left = new FormAttachment(this.topicList);
        removeButtonData.right = new FormAttachment(100);
        removeButtonData.top = new FormAttachment(this.topicText);
        removeButton.setLayoutData(removeButtonData);
        this.removeButton.setEnabled(false);
        
        addTopicEntryListeners();

    }
    
    /**
     * Creates the event listeners for the topic entry GUI objects.
     */
    private void addTopicEntryListeners() {
        
        this.defaultButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                // do nothing              
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                enableTopicEntry(defaultButton.getSelection());
                
            }
            
        });
        
        this.topicText.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(final KeyEvent arg0) {
                addButton.setEnabled(!topicText.getText().isEmpty());                     
            }

            @Override
            public void keyReleased(final KeyEvent arg0) {
                addButton.setEnabled(!topicText.getText().isEmpty());           
            }
            
        });
        
        this.topicList.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                // do nothing               
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                removeButton.setEnabled(topicList.getSelectionCount() == 1);
            }
            
        });
        
        this.addButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                // do nothing              
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
               final String topic = topicText.getText().trim();
               final String error = ContextTopicNameFactory.checkTopic(topic, true);
               if (error != null) {
                   SWTUtilities.showErrorDialog(mainComposite.getShell(), "Bad Topic Name", error);
               } else {
                   topicList.add(topicText.getText().trim()); 
                   topicText.setText("");
                   addButton.setEnabled(false);
               }
            }
            
        });
        
        this.removeButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                // do nothing                
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
               topicList.remove(topicList.getSelectionIndex());
               removeButton.setEnabled(false);  
            }
            
        });
    }
    
    /**
     * Gets the main composite object.
     * 
     * @return main composite
     */
    public Composite getComposite() {
        return this.mainComposite;
    }
    
    /**
     * Gets the list of topics entered by the user. Must be called before the GUI is disposed.
     * 
     * @return collection of entered topics, or an empty collection if none entered
     */
    public Collection<String> getTopics() {
        if (!this.topicList.isDisposed() && this.defaultButton.getSelection()) {
            final String[] topics = this.topicList.getItems();
            return new TreeSet<>(Arrays.asList(topics));            
        }
        return new TreeSet<>();
    }
    
    /**
     * Sets the list of user-selected topics. 
     * @param topics collection of topic strings to set
     */
    public void setTopics(final Collection<String> topics) {
        if (this.topicList.isDisposed()) {
            return;
        }
        if (topics == null || topics.isEmpty()) {
            this.defaultTopics = new ArrayList<>();
            enableTopicEntry(false);
         } else {
             this.defaultTopics = topics;
             enableTopicEntry(true);
         }       
    }
    
    
    /** Enables or disables topic entry.
     * 
     * @param enable true to enable topic entry, false to disable
     */
    private void enableTopicEntry(final boolean enable) {
        this.defaultButton.setSelection(enable);
        if (!enable) {
            this.topicList.setItems(new String[] {"<DEFAULT>"});
        } else {            
            this.topicList.setItems(defaultTopics.toArray(new String[] {}));
        }
        
        this.topicEntryGroup.setEnabled(enable);

        this.topicText.setEnabled(enable);
        this.topicList.setEnabled(enable);
        this.topicText.setBackground(enable ? enableColor : disableColor);
        this.topicList.setBackground(enable ? enableColor : disableColor);

        this.sessionEntryGroup.setEnabled(!enable);
        
        this.userText.setEnabled(!enable);
        this.hostText.setEnabled(!enable);
        this.userText.setBackground(!enable ? enableColor : disableColor);
        this.hostText.setBackground(!enable ? enableColor : disableColor);
        this.testbedCombo.setBackground(!enable ? enableColor : disableColor);
        this.streamIdCombo.setBackground(!enable ? enableColor : disableColor);
        this.jmsSubtopicCombo.setBackground(!enable ? enableColor : disableColor);
        this.jmsSubtopicCombo.setEnabled(!enable);
        this.streamIdCombo.setEnabled(!enable);
        this.testbedCombo.setEnabled(!enable);
      
    }
   
    private void initTestbedNames(final VenueType vt)
    {
        if (testbedCombo == null || testbedCombo.isDisposed()) {
            return;
        }
        testbedCombo.removeAll();
        
        if (!vt.isTestFacility()) {
            return;
        }

        final MissionProperties mp = appContext.getBean(MissionProperties.class);

        final java.util.List<String> testbeds = mp.getAllowedTestbedNames(vt);
        final String defaultTb = appContext.getBean(IVenueConfiguration.class).getTestbedName();
       
        if(testbeds != null)
        {
            testbedCombo.setItems(testbeds.toArray(new String[] {}));
            testbedCombo.setText(defaultTb == null ? testbeds.get(0) : defaultTb);
        }
    }
    
    private void initDownlinkStreamIds(final VenueType vt)
    {        
        if (streamIdCombo == null || streamIdCombo.isDisposed()) {
            return;
        }
        streamIdCombo.removeAll();
        
        if (!vt.isTestFacility()) {
            return;
        }

        final java.util.List<String> chans = appContext.getBean(MissionProperties.class).getAllowedDownlinkStreamIdsAsStrings(vt);
        
        final DownlinkStreamType defStream = appContext.getBean(IVenueConfiguration.class).getDownlinkStreamId();

        if(chans != null)
        {
            chans.add(DownlinkStreamType.NOT_APPLICABLE.name());
            streamIdCombo.setItems(chans.toArray(new String[] {}));
            streamIdCombo.setText(defStream == null ? DownlinkStreamType.SELECTED_DL.name() : defStream.name());
        }
        
        
    }
    
    private void initSubtopics(final VenueType vt)
    {

        if (jmsSubtopicCombo == null || jmsSubtopicCombo.isDisposed()) {
            return;
        }
        jmsSubtopicCombo.removeAll();
        
        if (!vt.isOpsVenue()) {
            jmsSubtopicCombo.setEnabled(false);
            jmsSubtopicCombo.setBackground(disableColor);
            jmsSubtopicCombo.removeAll();
            return;
        } else {
            jmsSubtopicCombo.setEnabled(!defaultButton.getSelection());
            jmsSubtopicCombo.setBackground(defaultButton.getSelection() ? disableColor : enableColor);
        }
        
        final MissionProperties mp = appContext.getBean(MissionProperties.class);

        final java.util.List<String> subtopics = mp.getAllowedSubtopics();

        if (subtopics == null || subtopics.isEmpty()) {
            tracer.warn("Error retrieving list of JMS subtopics from GDS configuration");
            return;
        }

        jmsSubtopicCombo.setItems(subtopics.toArray(new String[] {}));
        jmsSubtopicCombo.select(0);

        final String defaultSubtopic = appContext.getBean(IGeneralContextInformation.class).getSubtopic();

        if (defaultSubtopic == null) {
            tracer.warn("Could not retrieve default subtopic from the configuration. Using the first in list.");
        } else {
            jmsSubtopicCombo.setText(defaultSubtopic);
        }

    }
    
    /**
     * Updates GUI fields for a venue type change.
     * 
     * @param vt the new venue type
     */
    public void setVenue(final VenueType vt) {
        initTestbedNames(vt);
        initDownlinkStreamIds(vt);
        initSubtopics(vt);
        if (!defaultButton.isDisposed()) {
	    enableTopicEntry(defaultButton.getSelection());
	}
    }
    
    /**
     * Validates GUI fields.
     * 
     * @param vt the current venue type
     * 
     * @return true if fields valid, false if not
     */
    public boolean validate(final VenueType vt) {
        if (mainComposite.isDisposed()) {
            return false;
        }
        
        if (vt.equals(VenueType.TESTSET) && !this.defaultButton.getSelection()) {
            final String user = userText.getText().trim();
            if(user.equals(""))
            {
                SWTUtilities.showMessageDialog(mainComposite.getShell(), "Invalid User",
                        "You must enter the session user name");
                return false;
            }

            final String host = hostText.getText().trim();
            if (host.equals("")) {
                SWTUtilities.showMessageDialog(mainComposite.getShell(), "Invalid Host",
                        "You must enter the session host name.");
                return false;
            }

        }

        if (this.defaultButton.getSelection() && this.topicList.getItemCount() == 0) {
            SWTUtilities.showMessageDialog(mainComposite.getShell(), "Missing Topics",
                    "You must enter at least one custom topic if you are overriding default topics.");
            return false;
        }

        return true;
    }
    
    /**
     * Populates the current application context from GUI fields.
     */
    public void populateAppContext() {
        if (this.mainComposite.isDisposed()) {
            return;
        }
        
        if (this.defaultButton.getSelection()) {
            appContext.getBean(IGeneralContextInformation.class).setSubscriptionTopics(getTopics());
        } else {
            final IVenueConfiguration vc = appContext.getBean(IVenueConfiguration.class);
            if (vc.getVenueType().isTestFacility()) {
                vc.setTestbedName(this.testbedCombo.getText());
                vc.setDownlinkStreamId(DownlinkStreamType.valueOf(this.streamIdCombo.getText()));
            } else if (vc.getVenueType().isOpsVenue()) {
                appContext.getBean(IGeneralContextInformation.class).setSubtopic(jmsSubtopicCombo.getText());
            }
            appContext.getBean(IContextIdentification.class).setUser(this.userText.getText().trim());
            appContext.getBean(IContextIdentification.class).setHost(this.hostText.getText().trim());
        }           
    }
}
