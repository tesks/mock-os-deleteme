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
package jpl.gds.tcapp.app.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.config.PlopType;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.SessionLocationType;

/**
 * Class that controls the uplink configuration shell for display
 * 
 *
 */
public class UplinkConfigShell implements ChillShell
{
	private static final String DEFAULT_VALUE_STRING = "DEFAULT";

    private final SWTUtilities swtutil;

	private final CommandProperties cmdConfig;
	/** CLTU (Command Link Transmission Unit) configuration */
	protected CltuProperties cltuConfig;
	/** Physical Layer Operations Procedure (PLOP) configuration */
	protected PlopProperties plopConfig;
	/** transmission frame configuration */
	protected CommandFrameProperties frameConfig;
	/** Spacecraft Command Message File (SCMF) configuration */
	protected ScmfProperties scmfConfig;
	private final IConnectionMap hostConfig;
    private final IDatabaseProperties         dbProperties;

	private final Shell parentShell;
    private Shell configShell;

    private Group commandInfoGroup;
    private Text sseCommandPrefixText;
    private Combo validateCommandsCombo;
    private Text spacecraftIdText;
    //private Combo stringIdCombo;

    private Group commandLoadGroup;
    private Combo plopTypeCombo;
    private Combo idleSeqLocationCombo;
    private Text idleSeqLengthText;
    private Text acqSeqLengthText;
    private Combo longStartSeqCombo;
    private Combo beginCommandLoadCombo;

    private Group scmfGroup;
    private Combo onlyScmfCombo;
    private Text scmfFileText;
    private Button scmfBrowseButton;

    private Group uplinkSessionGroup;
    private Combo beginUplinkSessionCombo;
    private Combo endUplinkSessionCombo;
    private Text uplinkSessionRepeatText;

    private Group networkGroup;
    private Text fswUplinkHostText;
    private Text fswUplinkPortText;
    private Text sseUplinkHostText;
    private Text sseUplinkPortText;
    private Combo useDatabaseCombo;
    private Combo useJmsCombo;
    private Combo sendOnFailureCombo;

    private Button restoreDefaultsButton;
    private Button okButton;
    private Button cancelButton;

    private boolean canceled;
    
   // private final SessionConfiguration sessionConfig;
    private final MessageServiceConfiguration msgConfig;
    private final ApplicationContext appContext;


	/**
	 * Constructor that ties this shell to a parent shell
	 * 
	 * @param appContext
	 *            the ApplicationContext in which this object is being used
	 * @param parent
	 *            the parent Shell
	 * @param swtu
	 *            SWTUtilities
	 */
	public UplinkConfigShell(final ApplicationContext appContext,
			                 final Shell        parent,
                             final SWTUtilities swtu)
	{
		this.parentShell = parent;
        this.swtutil     = (swtu != null) ? swtu : new SWTUtilities();
        
        this.appContext = appContext;
		this.cmdConfig = appContext.getBean(CommandProperties.class);
		this.cltuConfig = appContext.getBean(CltuProperties.class);
		this.frameConfig = appContext.getBean(CommandFrameProperties.class);
		this.plopConfig = appContext.getBean(PlopProperties.class);
		this.scmfConfig = appContext.getBean(ScmfProperties.class);
		this.hostConfig = appContext.getBean(IConnectionMap.class);
        this.dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);
		this.msgConfig = appContext.getBean(MessageServiceConfiguration.class);

		createComponents();
		this.configShell.pack();
	}


	/**
	 * Constructor that ties this shell to a display
	 * 
	 * @param appContext
	 *            the ApplicationContext in which this object is being used
	 * @param display
	 *            the display that will include this shell
	 * @param swtu
	 *            SWTUtilities
	 */
	public UplinkConfigShell(final ApplicationContext appContext,
			                 final Display      display,
                             final SWTUtilities swtu)
	{
        this(appContext, new Shell(display, SWT.SHELL_TRIM), swtu);
    }


	private void createComponents()
	{
		this.configShell = new Shell(this.parentShell, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
        this.configShell.setText(getTitle());
        //configShell.setSize(600,400);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 10;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        this.configShell.setLayout(shellLayout);

        createCommandInformationGroup();
        createCommandLoadGroup();
        createUplinkSessionGroup();
        createNetworkGroup();
        createScmfGroup();
        createButtons();

        setFieldsFromConfigData();
	}

    @Override
	public Shell getShell()
	{
		return(this.configShell);
	}

    @Override
	public String getTitle()
	{
		return("Uplink Configuration");
	}

    @Override
	public void open()
	{
		this.configShell.open();
	}

    @Override
	public boolean wasCanceled()
	{
		return(this.canceled);
	}

	private void setConfigDataFromFields() throws Exception
	{
		this.cmdConfig.setSseCommandPrefix(this.sseCommandPrefixText.getText());
		appContext.getBean(IContextIdentification.class).setSpacecraftId(Integer.parseInt(this.spacecraftIdText.getText()));
		this.cmdConfig.setValidateCommands(GDR.parse_boolean(this.validateCommandsCombo.getText()));
//		this.frameConfig.setStringId(ExecutionStringType.valueOf(this.stringIdCombo.getText()));
		this.plopConfig.setType(new PlopType(this.plopTypeCombo.getText()));
		this.plopConfig.setIdleSequenceLocation(SessionLocationType.valueOf(this.idleSeqLocationCombo.getText()));
		this.plopConfig.setIdleSequenceBitLength(Integer.parseInt(this.idleSeqLengthText.getText()));
		this.plopConfig.setAcquisitionSequenceBitLength(Integer.parseInt(this.acqSeqLengthText.getText()));
		this.cltuConfig.setUseLongStartSequence(GDR.parse_boolean(this.longStartSeqCombo.getText()));
		this.plopConfig.setAcquisitionSequenceLocation(SessionLocationType.valueOf(this.beginCommandLoadCombo.getText()));
		this.frameConfig.setSessionRepeatCount(Integer.parseInt(this.uplinkSessionRepeatText.getText()));
		this.hostConfig.getFswUplinkConnection().setHost(this.fswUplinkHostText.getText());
		this.hostConfig.getFswUplinkConnection().setPort(Integer.parseInt(this.fswUplinkPortText.getText()));
		if (this.hostConfig.getSseUplinkConnection() != null) {
			this.hostConfig.getSseUplinkConnection().setHost(this.sseUplinkHostText.getText());
			this.hostConfig.getSseUplinkConnection().setPort(Integer.parseInt(this.sseUplinkPortText.getText()));
		}
		this.dbProperties.setUseDatabase(GDR.parse_boolean(this.useDatabaseCombo.getText()));
		msgConfig.setUseMessaging(GDR.parse_boolean(this.useJmsCombo.getText()));
		this.cmdConfig.setSendJmsMessageOnSocketFailure(GDR.parse_boolean(this.sendOnFailureCombo.getText()));
		this.scmfConfig.setOnlyWriteScmf(GDR.parse_boolean(this.onlyScmfCombo.getText()));
		if(this.scmfFileText.getText().trim().length() == 0)
		{
			this.scmfConfig.setScmfName(null);
		}
		else
		{
			this.scmfConfig.setScmfName(this.scmfFileText.getText());
		}

		if(DEFAULT_VALUE_STRING.equalsIgnoreCase(this.beginUplinkSessionCombo.getText()))
		{
			this.frameConfig.setOverrideBeginType(null);
		}
		else
		{
			this.frameConfig.setOverrideBeginType(SessionLocationType.valueOf(this.beginUplinkSessionCombo.getText()));
		}

		if(DEFAULT_VALUE_STRING.equalsIgnoreCase(this.endUplinkSessionCombo.getText()))
		{
			this.frameConfig.setOverrideEndType(null);
		}
		else
		{
			this.frameConfig.setOverrideEndType(SessionLocationType.valueOf(this.endUplinkSessionCombo.getText()));
		}
	}

	private void setFieldsFromConfigData()
	{
		this.sseCommandPrefixText.setText(this.cmdConfig.getSseCommandPrefix() == null ? "" : this.cmdConfig.getSseCommandPrefix());
		this.spacecraftIdText.setText(String.valueOf(appContext.getBean(IContextIdentification.class).getSpacecraftId()));
		this.validateCommandsCombo.setText(this.cmdConfig.getValidateCommands() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
//		this.stringIdCombo.setText(this.frameConfig.getStringId() == null ? ExecutionStringType.A.toString() : this.frameConfig.getStringId());
		this.plopTypeCombo.setText(String.valueOf(this.plopConfig.getType() == null ? PlopType.PLOP_2.toString() : this.plopConfig.getType().toString()));
		this.idleSeqLocationCombo.setText(this.plopConfig.getIdleSequenceLocation() == null ? SessionLocationType.LAST.toString() : this.plopConfig.getIdleSequenceLocation().toString());
		this.idleSeqLengthText.setText(String.valueOf(this.plopConfig.getIdleSequenceBitLength()));
		this.acqSeqLengthText.setText(String.valueOf(this.plopConfig.getAcquisitionSequenceBitLength()));
		this.longStartSeqCombo.setText(this.cltuConfig.getUseLongStartSequence() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		this.beginCommandLoadCombo.setText(this.plopConfig.getAcquisitionSequenceLocation() == null ? SessionLocationType.FIRST.toString() : this.plopConfig.getAcquisitionSequenceLocation().toString());
		this.uplinkSessionRepeatText.setText(String.valueOf(this.frameConfig.getSessionRepeatCount()));
		this.fswUplinkHostText.setText(this.hostConfig.getFswUplinkConnection().getHost() == null ? "" : 
			this.hostConfig.getFswUplinkConnection().getHost());
		this.fswUplinkPortText.setText(String.valueOf(this.hostConfig.getFswUplinkConnection().getPort()));
		if (this.hostConfig.getSseUplinkConnection() != null) {
			this.sseUplinkHostText.setText(this.hostConfig.getSseUplinkConnection().getHost() == null ? "" : 
				this.hostConfig.getSseUplinkConnection().getHost());
			this.sseUplinkPortText.setText(String.valueOf(this.hostConfig.getSseUplinkConnection().getPort()));
		}
		this.useDatabaseCombo.setText(this.dbProperties.getUseDatabase() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		this.useJmsCombo.setText(msgConfig.getUseMessaging() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		this.sendOnFailureCombo.setText(this.cmdConfig.getSendJmsMessageOnSocketFailure() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		this.onlyScmfCombo.setText(this.scmfConfig.getOnlyWriteScmf() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		this.scmfFileText.setText(this.scmfConfig.getScmfName() == null ? "" : this.scmfConfig.getScmfName());
		this.beginUplinkSessionCombo.setText(this.frameConfig.getOverrideBeginType() == null ? DEFAULT_VALUE_STRING : this.frameConfig.getOverrideBeginType().toString());
		this.endUplinkSessionCombo.setText(this.frameConfig.getOverrideEndType() == null ? DEFAULT_VALUE_STRING : this.frameConfig.getOverrideEndType().toString());
	}

	private void createCommandInformationGroup()
    {
    	this.commandInfoGroup = new Group(this.configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(this.configShell.getDisplay(), groupFontData);
        this.commandInfoGroup.setFont(groupFont);
        this.commandInfoGroup.setText("Command Information");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.marginBottom = 10;
        this.commandInfoGroup.setLayout(fl);
        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(0);
        fd2.right = new FormAttachment(50);
        fd2.top = new FormAttachment(0, 15);
        fd2.bottom = new FormAttachment(25); //TODO:
        this.commandInfoGroup.setLayoutData(fd2);

        final Label sseCommandPrefixLabel = new Label(this.commandInfoGroup, SWT.LEFT);
        this.sseCommandPrefixText = new Text(this.commandInfoGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd9 = SWTUtilities.getFormData(this.sseCommandPrefixText, 1, 15);
        fd9.left = new FormAttachment(60);
        fd9.right = new FormAttachment(100);
        fd9.top = new FormAttachment(0);
        this.sseCommandPrefixText.setLayoutData(fd9);
        sseCommandPrefixLabel.setText("SSE Command Prefix:");
        final FormData fd10 = new FormData();
        fd10.left = new FormAttachment(0);
        fd10.right = new FormAttachment(this.sseCommandPrefixText);
        fd10.top = new FormAttachment(this.sseCommandPrefixText, 0, SWT.CENTER);
        sseCommandPrefixLabel.setLayoutData(fd10);

        final Label spacecraftIdLabel = new Label(this.commandInfoGroup, SWT.LEFT);
        this.spacecraftIdText = new Text(this.commandInfoGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd13 = SWTUtilities.getFormData(this.spacecraftIdText, 1, 15);
        fd13.left = new FormAttachment(60);
        fd13.right = new FormAttachment(100);
        fd13.top = new FormAttachment(this.sseCommandPrefixText,0,SWT.LEFT);
        this.spacecraftIdText.setLayoutData(fd13);
        spacecraftIdLabel.setText("Spacecraft ID:");
        final FormData fd14 = new FormData();
        fd14.left = new FormAttachment(0);
        fd14.right = new FormAttachment(this.spacecraftIdText);
        fd14.top = new FormAttachment(this.spacecraftIdText, 0, SWT.CENTER);
        spacecraftIdLabel.setLayoutData(fd14);

        final Label validateCommandsLabel = new Label(this.commandInfoGroup, SWT.LEFT);
        this.validateCommandsCombo = new Combo(this.commandInfoGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.validateCommandsCombo.add(Boolean.TRUE.toString());
        this.validateCommandsCombo.add(Boolean.FALSE.toString());
        final FormData fd15 = SWTUtilities.getFormData(this.validateCommandsCombo, 1, 15);
        fd15.left = new FormAttachment(60);
        fd15.right = new FormAttachment(100);
        fd15.top = new FormAttachment(this.spacecraftIdText,0,SWT.LEFT);
        this.validateCommandsCombo.setLayoutData(fd15);
        validateCommandsLabel.setText("Validate Commands:");
        final FormData fd16 = new FormData();
        fd16.left = new FormAttachment(0);
        fd16.right = new FormAttachment(this.validateCommandsCombo);
        fd16.top = new FormAttachment(this.validateCommandsCombo, 0, SWT.CENTER);
        validateCommandsLabel.setLayoutData(fd16);

//        Label stringIdLabel = new Label(this.commandInfoGroup, SWT.LEFT);
//        this.stringIdCombo = new Combo(this.commandInfoGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
//        for(ExecutionStringType est : ExecutionStringType.values())
//        {
//        	this.stringIdCombo.add(est.toString());
//        }
//        FormData fd17 = SWTUtilities.getFormData(this.stringIdCombo, 1, 15);
//        fd17.left = new FormAttachment(60);
//        fd17.right = new FormAttachment(100);
//        fd17.top = new FormAttachment(this.validateCommandsCombo,0,SWT.LEFT);
//        this.stringIdCombo.setLayoutData(fd17);
//        stringIdLabel.setText("String ID:");
//        FormData fd18 = new FormData();
//        fd18.left = new FormAttachment(0);
//        fd18.right = new FormAttachment(this.stringIdCombo);
//        fd18.top = new FormAttachment(this.stringIdCombo, 0, SWT.CENTER);
//        stringIdLabel.setLayoutData(fd18);
    }

    private void createCommandLoadGroup()
    {
    	this.commandLoadGroup = new Group(this.configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(this.configShell.getDisplay(), groupFontData);
        this.commandLoadGroup.setFont(groupFont);
        this.commandLoadGroup.setText("Command Load");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.marginBottom = 10;
        this.commandLoadGroup.setLayout(fl);
        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(0);
        fd2.right = new FormAttachment(50);
        //fd2.top = new FormAttachment(this.commandInfoGroup, 15);
        fd2.top = new FormAttachment(30); //TODO:
        fd2.bottom = new FormAttachment(70); //TODO:
        this.commandLoadGroup.setLayoutData(fd2);

        final Label plopTypeLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.plopTypeCombo = new Combo(this.commandLoadGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for(int i=0; i < PlopType.types.length; i++)
        {
        	this.plopTypeCombo.add(String.valueOf(PlopType.types[i]));
        }
        final FormData fd9 = SWTUtilities.getFormData(this.plopTypeCombo, 1, 15);
        fd9.left = new FormAttachment(60);
        fd9.right = new FormAttachment(100);
        fd9.top = new FormAttachment(0);
        this.plopTypeCombo .setLayoutData(fd9);
        plopTypeLabel.setText("PLOP Type:");
        final FormData fd10 = new FormData();
        fd10.left = new FormAttachment(0);
        fd10.right = new FormAttachment(this.plopTypeCombo);
        fd10.top = new FormAttachment(this.plopTypeCombo , 0, SWT.CENTER);
        plopTypeLabel.setLayoutData(fd10);

        final Label idleSeqLocationLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.idleSeqLocationCombo = new Combo(this.commandLoadGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.idleSeqLocationCombo.add(SessionLocationType.ALL.toString());
        this.idleSeqLocationCombo.add(SessionLocationType.LAST.toString());
        this.idleSeqLocationCombo.add(SessionLocationType.NONE.toString());
        final FormData fd13 = SWTUtilities.getFormData(this.idleSeqLocationCombo, 1, 15);
        fd13.left = new FormAttachment(60);
        fd13.right = new FormAttachment(100);
        fd13.top = new FormAttachment(this.plopTypeCombo,0,SWT.LEFT);
        this.idleSeqLocationCombo.setLayoutData(fd13);
        idleSeqLocationLabel.setText("Idle Sequence Location:");
        final FormData fd14 = new FormData();
        fd14.left = new FormAttachment(0);
        fd14.right = new FormAttachment(this.idleSeqLocationCombo);
        fd14.top = new FormAttachment(this.idleSeqLocationCombo, 0, SWT.CENTER);
        idleSeqLocationLabel.setLayoutData(fd14);

        final Label idleSeqLengthLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.idleSeqLengthText = new Text(this.commandLoadGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd15 = SWTUtilities.getFormData(this.idleSeqLengthText, 1, 15);
        fd15.left = new FormAttachment(60);
        fd15.right = new FormAttachment(100);
        fd15.top = new FormAttachment(this.idleSeqLocationCombo,0,SWT.LEFT);
        this.idleSeqLengthText.setLayoutData(fd15);
        idleSeqLengthLabel.setText("Idle Sequence Bit Length:");
        final FormData fd16 = new FormData();
        fd16.left = new FormAttachment(0);
        fd16.right = new FormAttachment(this.idleSeqLengthText);
        fd16.top = new FormAttachment(this.idleSeqLengthText, 0, SWT.CENTER);
        idleSeqLengthLabel.setLayoutData(fd16);

        final Label acqSeqLengthLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.acqSeqLengthText = new Text(this.commandLoadGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd17 = SWTUtilities.getFormData(this.acqSeqLengthText, 1, 15);
        fd17.left = new FormAttachment(60);
        fd17.right = new FormAttachment(100);
        fd17.top = new FormAttachment(this.idleSeqLengthText,0,SWT.LEFT);
        this.acqSeqLengthText.setLayoutData(fd17);
        acqSeqLengthLabel.setText("Acquisition Sequence Bit Length:");
        final FormData fd18 = new FormData();
        fd18.left = new FormAttachment(0);
        fd18.right = new FormAttachment(this.acqSeqLengthText);
        fd18.top = new FormAttachment(this.acqSeqLengthText, 0, SWT.CENTER);
        acqSeqLengthLabel.setLayoutData(fd18);

        final Label longStartSeqLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.longStartSeqCombo = new Combo(this.commandLoadGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.longStartSeqCombo.add(Boolean.TRUE.toString());
        this.longStartSeqCombo.add(Boolean.FALSE.toString());
        final FormData fd19 = SWTUtilities.getFormData(this.longStartSeqCombo, 1, 15);
        fd19.left = new FormAttachment(60);
        fd19.right = new FormAttachment(100);
        fd19.top = new FormAttachment(this.acqSeqLengthText,0,SWT.LEFT);
        this.longStartSeqCombo.setLayoutData(fd19);
        longStartSeqLabel.setText("Use Long Start Sequence:");
        final FormData fd20 = new FormData();
        fd20.left = new FormAttachment(0);
        fd20.right = new FormAttachment(this.longStartSeqCombo);
        fd20.top = new FormAttachment(this.longStartSeqCombo, 0, SWT.CENTER);
        longStartSeqLabel.setLayoutData(fd20);

        final Label beginCommandLoadLabel = new Label(this.commandLoadGroup, SWT.LEFT);
        this.beginCommandLoadCombo = new Combo(this.commandLoadGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.beginCommandLoadCombo.add(SessionLocationType.ALL.toString());
        this.beginCommandLoadCombo.add(SessionLocationType.FIRST.toString());
        this.beginCommandLoadCombo.add(SessionLocationType.NONE.toString());
        final FormData fd21 = SWTUtilities.getFormData(this.beginCommandLoadCombo, 1, 15);
        fd21.left = new FormAttachment(60);
        fd21.right = new FormAttachment(100);
        fd21.top = new FormAttachment(this.longStartSeqCombo,0,SWT.LEFT);
        this.beginCommandLoadCombo.setLayoutData(fd21);
        beginCommandLoadLabel.setText("Begin Command Load:");
        final FormData fd22 = new FormData();
        fd22.left = new FormAttachment(0);
        fd22.right = new FormAttachment(this.beginCommandLoadCombo);
        fd22.top = new FormAttachment(this.beginCommandLoadCombo, 0, SWT.CENTER);
        beginCommandLoadLabel.setLayoutData(fd22);
    }

    private void createScmfGroup()
    {
    	this.scmfGroup = new Group(this.configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(this.configShell.getDisplay(), groupFontData);
        this.scmfGroup.setFont(groupFont);
        this.scmfGroup.setText("SCMF");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        this.scmfGroup.setLayout(fl);
        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(0);
        fd2.right = new FormAttachment(100);
        //fd2.top = new FormAttachment(this.commandLoadGroup, 15);
        fd2.top = new FormAttachment(75); //TODO:
        fd2.bottom = new FormAttachment(90); //TODO:
        this.scmfGroup.setLayoutData(fd2);

        final Label onlyScmfLabel = new Label(this.scmfGroup, SWT.LEFT);
        this.onlyScmfCombo = new Combo(this.scmfGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.onlyScmfCombo.add(Boolean.TRUE.toString());
        this.onlyScmfCombo.add(Boolean.FALSE.toString());
        final FormData fd24 = SWTUtilities.getFormData(this.onlyScmfCombo, 1, 15);
        fd24.left = new FormAttachment(40);
        fd24.right = new FormAttachment(60);
        fd24.top = new FormAttachment(0);
        this.onlyScmfCombo.setLayoutData(fd24);
        onlyScmfLabel.setText("Only Generate SCMF:");
        final FormData fd25 = new FormData();
        fd25.left = new FormAttachment(0);
        fd25.right = new FormAttachment(this.onlyScmfCombo);
        fd25.top = new FormAttachment(this.onlyScmfCombo, 0, SWT.CENTER);
        onlyScmfLabel.setLayoutData(fd25);

        final Label scmfNameLabel = new Label(this.scmfGroup, SWT.LEFT);
        this.scmfFileText = new Text(this.scmfGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd15 = SWTUtilities.getFormData(this.scmfFileText, 1, 15);
        fd15.left = new FormAttachment(40);
        fd15.right = new FormAttachment(85);
        fd15.top = new FormAttachment(this.onlyScmfCombo,0,SWT.LEFT);
        this.scmfFileText.setLayoutData(fd15);
        scmfNameLabel.setText("SCMF Output File:");
        final FormData fd16 = new FormData();
        fd16.left = new FormAttachment(0);
        fd16.right = new FormAttachment(this.scmfFileText);
        fd16.top = new FormAttachment(this.scmfFileText, 0, SWT.CENTER);
        scmfNameLabel.setLayoutData(fd16);
    }

    private void createUplinkSessionGroup()
    {
    	this.uplinkSessionGroup = new Group(this.configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(this.configShell.getDisplay(), groupFontData);
        this.uplinkSessionGroup.setFont(groupFont);
        this.uplinkSessionGroup.setText("Uplink Session");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.marginBottom = 10;
        this.uplinkSessionGroup.setLayout(fl);
        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(this.commandInfoGroup);
        fd2.right = new FormAttachment(100);
        fd2.top = new FormAttachment(this.commandInfoGroup, 0, SWT.TOP);
        fd2.bottom = new FormAttachment(25); //TODO:
        this.uplinkSessionGroup.setLayoutData(fd2);

        final Label beginUplinkSessionLabel = new Label(this.uplinkSessionGroup, SWT.LEFT);
        this.beginUplinkSessionCombo = new Combo(this.uplinkSessionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.beginUplinkSessionCombo.add(SessionLocationType.ALL.toString());
        this.beginUplinkSessionCombo.add(SessionLocationType.FIRST.toString());
        this.beginUplinkSessionCombo.add(SessionLocationType.NONE.toString());
        this.beginUplinkSessionCombo.add(DEFAULT_VALUE_STRING);
        final FormData fd9 = SWTUtilities.getFormData(this.beginUplinkSessionCombo, 1, 15);
        fd9.left = new FormAttachment(60);
        fd9.right = new FormAttachment(100);
        fd9.top = new FormAttachment(0);
        this.beginUplinkSessionCombo.setLayoutData(fd9);
        beginUplinkSessionLabel.setText("Begin Uplink Session:");
        final FormData fd10 = new FormData();
        fd10.left = new FormAttachment(0);
        fd10.right = new FormAttachment(this.beginUplinkSessionCombo);
        fd10.top = new FormAttachment(this.beginUplinkSessionCombo , 0, SWT.CENTER);
        beginUplinkSessionLabel.setLayoutData(fd10);

        final Label endUplinkSessionLabel = new Label(this.uplinkSessionGroup, SWT.LEFT);
        this.endUplinkSessionCombo = new Combo(this.uplinkSessionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.endUplinkSessionCombo.add(SessionLocationType.ALL.toString());
        this.endUplinkSessionCombo.add(SessionLocationType.LAST.toString());
        this.endUplinkSessionCombo.add(SessionLocationType.NONE.toString());
        this.endUplinkSessionCombo.add(DEFAULT_VALUE_STRING);
        final FormData fd13 = SWTUtilities.getFormData(this.endUplinkSessionCombo, 1, 15);
        fd13.left = new FormAttachment(60);
        fd13.right = new FormAttachment(100);
        fd13.top = new FormAttachment(this.beginUplinkSessionCombo,0,SWT.LEFT);
        this.endUplinkSessionCombo.setLayoutData(fd13);
        endUplinkSessionLabel.setText("End Uplink Session:");
        final FormData fd14 = new FormData();
        fd14.left = new FormAttachment(0);
        fd14.right = new FormAttachment(this.endUplinkSessionCombo);
        fd14.top = new FormAttachment(this.endUplinkSessionCombo, 0, SWT.CENTER);
        endUplinkSessionLabel.setLayoutData(fd14);

        final Label uplinkSessionRepeatLabel = new Label(this.uplinkSessionGroup, SWT.LEFT);
        this.uplinkSessionRepeatText = new Text(this.uplinkSessionGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd15 = SWTUtilities.getFormData(this.uplinkSessionRepeatText, 1, 15);
        fd15.left = new FormAttachment(60);
        fd15.right = new FormAttachment(100);
        fd15.top = new FormAttachment(this.endUplinkSessionCombo,0,SWT.LEFT);
        this.uplinkSessionRepeatText.setLayoutData(fd15);
        uplinkSessionRepeatLabel.setText("Uplink Session Repeat Count:");
        final FormData fd16 = new FormData();
        fd16.left = new FormAttachment(0);
        fd16.right = new FormAttachment(this.uplinkSessionRepeatText);
        fd16.top = new FormAttachment(this.uplinkSessionRepeatText, 0, SWT.CENTER);
        uplinkSessionRepeatLabel.setLayoutData(fd16);
    }

    private void createNetworkGroup()
    {
    	this.networkGroup = new Group(this.configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(this.configShell.getDisplay(), groupFontData);
        this.networkGroup.setFont(groupFont);
        this.networkGroup.setText("Network Settings");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        this.networkGroup.setLayout(fl);
        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(this.commandLoadGroup);
        fd2.right = new FormAttachment(100);
        //fd2.top = new FormAttachment(this.uplinkSessionGroup, 15);
        fd2.top = new FormAttachment(30); //TODO:
        fd2.bottom = new FormAttachment(70); //TODO:
        //fd2.bottom = new FormAttachment(this.commandLoadGroup, 0, SWT.BOTTOM);
        this.networkGroup.setLayoutData(fd2);

        final Label fswUplinkHostLabel = new Label(this.networkGroup, SWT.LEFT);
        this.fswUplinkHostText = new Text(this.networkGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd9 = SWTUtilities.getFormData(this.fswUplinkHostText, 1, 15);
        fd9.left = new FormAttachment(60);
        fd9.right = new FormAttachment(100);
        fd9.top = new FormAttachment(0);
        this.fswUplinkHostText.setLayoutData(fd9);
        fswUplinkHostLabel.setText("FSW Uplink Host:");
        final FormData fd10 = new FormData();
        fd10.left = new FormAttachment(0);
        fd10.right = new FormAttachment(this.fswUplinkHostText);
        fd10.top = new FormAttachment(this.fswUplinkHostText, 0, SWT.CENTER);
        fswUplinkHostLabel.setLayoutData(fd10);

        final Label fswUplinkPortLabel = new Label(this.networkGroup, SWT.LEFT);
        this.fswUplinkPortText = new Text(this.networkGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd13 = SWTUtilities.getFormData(this.fswUplinkPortText, 1, 15);
        fd13.left = new FormAttachment(60);
        fd13.right = new FormAttachment(100);
        fd13.top = new FormAttachment(this.fswUplinkHostText,0,SWT.LEFT);
        this.fswUplinkPortText.setLayoutData(fd13);
        fswUplinkPortLabel.setText("FSW Uplink Port:");
        final FormData fd14 = new FormData();
        fd14.left = new FormAttachment(0);
        fd14.right = new FormAttachment(this.fswUplinkPortText);
        fd14.top = new FormAttachment(this.fswUplinkPortText, 0, SWT.CENTER);
        fswUplinkPortLabel.setLayoutData(fd14);

        final Label sseUplinkHostLabel = new Label(this.networkGroup, SWT.LEFT);
        this.sseUplinkHostText = new Text(this.networkGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd15 = SWTUtilities.getFormData(this.sseUplinkHostText, 1, 15);
        fd15.left = new FormAttachment(60);
        fd15.right = new FormAttachment(100);
        fd15.top = new FormAttachment(this.fswUplinkPortText);
        this.sseUplinkHostText.setLayoutData(fd15);
        sseUplinkHostLabel.setText("SSE Uplink Host:");
        final FormData fd16 = new FormData();
        fd16.left = new FormAttachment(0);
        fd16.right = new FormAttachment(this.sseUplinkHostText);
        fd16.top = new FormAttachment(this.sseUplinkHostText, 0, SWT.CENTER);
        sseUplinkHostLabel.setLayoutData(fd16);

        final Label sseUplinkPortLabel = new Label(this.networkGroup, SWT.LEFT);
        this.sseUplinkPortText = new Text(this.networkGroup, SWT.SINGLE | SWT.BORDER);
        final FormData fd17 = SWTUtilities.getFormData(this.sseUplinkPortText, 1, 15);
        fd17.left = new FormAttachment(60);
        fd17.right = new FormAttachment(100);
        fd17.top = new FormAttachment(this.sseUplinkHostText,0,SWT.LEFT);
        this.sseUplinkPortText.setLayoutData(fd17);
        sseUplinkPortLabel.setText("SSE Uplink Port:");
        final FormData fd18 = new FormData();
        fd18.left = new FormAttachment(0);
        fd18.right = new FormAttachment(this.sseUplinkPortText);
        fd18.top = new FormAttachment(this.sseUplinkPortText, 0, SWT.CENTER);
        sseUplinkPortLabel.setLayoutData(fd18);

        final Label useDatabaseLabel = new Label(this.networkGroup, SWT.LEFT);
        this.useDatabaseCombo = new Combo(this.networkGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.useDatabaseCombo.add(Boolean.TRUE.toString());
        this.useDatabaseCombo.add(Boolean.FALSE.toString());
        final FormData fd20 = SWTUtilities.getFormData(this.useDatabaseCombo, 1, 15);
        fd20.left = new FormAttachment(60);
        fd20.right = new FormAttachment(100);
        fd20.top = new FormAttachment(this.sseUplinkPortText,0,SWT.LEFT);
        this.useDatabaseCombo.setLayoutData(fd20);
        useDatabaseLabel.setText("Use Database On Uplink");
        final FormData fd21 = new FormData();
        fd21.left = new FormAttachment(0);
        fd21.right = new FormAttachment(this.useDatabaseCombo);
        fd21.top = new FormAttachment(this.useDatabaseCombo, 0, SWT.CENTER);
        useDatabaseLabel.setLayoutData(fd21);

        final Label useJmsLabel = new Label(this.networkGroup, SWT.LEFT);
        this.useJmsCombo = new Combo(this.networkGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.useJmsCombo.add(Boolean.TRUE.toString());
        this.useJmsCombo.add(Boolean.FALSE.toString());
        final FormData fd22 = SWTUtilities.getFormData(this.useJmsCombo, 1, 15);
        fd22.left = new FormAttachment(60);
        fd22.right = new FormAttachment(100);
        fd22.top = new FormAttachment(this.useDatabaseCombo,0,SWT.LEFT);
        this.useJmsCombo.setLayoutData(fd22);
        useJmsLabel.setText("Use Message Service On Uplink");
        final FormData fd23 = new FormData();
        fd23.left = new FormAttachment(0);
        fd23.right = new FormAttachment(this.useJmsCombo);
        fd23.top = new FormAttachment(this.useJmsCombo, 0, SWT.CENTER);
        useJmsLabel.setLayoutData(fd23);
        this.useJmsCombo.addModifyListener(new ModifyListener()
        {
            @Override
			public void modifyText(final ModifyEvent me)
			{
        		final String value = ((Combo)me.getSource()).getText();
        		UplinkConfigShell.this.sendOnFailureCombo.setEnabled(GDR.parse_boolean(value));
			}
        });

        final Label sendOnFailureLabel = new Label(this.networkGroup, SWT.LEFT);
        this.sendOnFailureCombo = new Combo(this.networkGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.sendOnFailureCombo.add(Boolean.TRUE.toString());
        this.sendOnFailureCombo.add(Boolean.FALSE.toString());
        final FormData fd24 = SWTUtilities.getFormData(this.sendOnFailureCombo, 1, 15);
        fd24.left = new FormAttachment(60);
        fd24.right = new FormAttachment(100);
        fd24.top = new FormAttachment(this.useJmsCombo,0,SWT.LEFT);
        this.sendOnFailureCombo.setLayoutData(fd24);
        sendOnFailureLabel.setText("Send to Message Service On Socket Failure:");
        final FormData fd25 = new FormData();
        fd25.left = new FormAttachment(0);
        fd25.right = new FormAttachment(this.sendOnFailureCombo);
        fd25.top = new FormAttachment(this.sendOnFailureCombo, 0, SWT.CENTER);
        sendOnFailureLabel.setLayoutData(fd25);
    }

    private void restoreDefaults()
    {
    	frameConfig.resetConfiguration();
    	plopConfig.resetConfiguration();
    	cltuConfig.resetConfiguration();
    	scmfConfig.resetConfiguration();
    	
    	/* R8 Refactor TODO - Commenting this out for now but we need to do something.
    	 * I think this breaks something.
    	 */
//    	CommandProperties.resetConfiguration();
//    	ConnectionConfiguration.resetConfiguration();
//    	DatabaseConfiguration.reset();

    	setFieldsFromConfigData();
    }

    private void createButtons()
    {
    	final ButtonHandler handler = new ButtonHandler();

    	this.scmfBrowseButton = new Button(this.scmfGroup, SWT.PUSH);
        this.scmfBrowseButton.setText("Browse...");
        final FormData bfd = new FormData();
        bfd.left = new FormAttachment(this.scmfFileText, 10);
        bfd.top = new FormAttachment(this.scmfFileText, 0, SWT.CENTER);
        bfd.right = new FormAttachment(100);
        this.scmfBrowseButton.setLayoutData(bfd);
        this.scmfBrowseButton.addSelectionListener(handler);

        final Composite buttonComposite = new Composite(this.configShell, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        buttonComposite.setLayout(fl);
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0);
        fd1.right = new FormAttachment(100);
        fd1.bottom = new FormAttachment(100);
        //fd1.top = new FormAttachment(this.scmfGroup, 15);
        fd1.top = new FormAttachment(90); //TODO:
        buttonComposite.setLayoutData(fd1);

        this.restoreDefaultsButton = new Button(buttonComposite, SWT.PUSH);
        this.restoreDefaultsButton.setText("Restore Defaults");
        final FormData fd2 = new FormData();
        fd2.top = new FormAttachment(0,10); //TODO:
        fd2.left = new FormAttachment(10);
        fd2.right = new FormAttachment(30);
        //fd2.bottom = new FormAttachment(100); //TODO:
        this.restoreDefaultsButton.setLayoutData(fd2);
        this.restoreDefaultsButton.addSelectionListener(handler);

        this.okButton = new Button(buttonComposite, SWT.PUSH);
        this.okButton.setText("OK");
        final FormData fd3 = new FormData();
        fd3.top = new FormAttachment(0,10); //TODO:
        fd3.left = new FormAttachment(40);
        fd3.right = new FormAttachment(60);
        //fd3.bottom = new FormAttachment(100); //TODO:
        this.okButton.setLayoutData(fd3);
        this.okButton.addSelectionListener(handler);

        this.cancelButton = new Button(buttonComposite, SWT.PUSH);
        this.cancelButton.setText("Cancel");
        final FormData fd4 = new FormData();
        fd4.top = new FormAttachment(0,10); //TODO:
        fd4.left = new FormAttachment(70);
        fd4.right = new FormAttachment(90);
        //fd4.bottom = new FormAttachment(100); //TODO:
        this.cancelButton.setLayoutData(fd4);
        this.cancelButton.addSelectionListener(handler);
    }

    private class ButtonHandler extends SelectionAdapter
    {
    	public ButtonHandler()
    	{
    		super();
    	}

    	@Override
		public void widgetSelected(final SelectionEvent se)
    	{
    		try
    		{
    			if (se.getSource() == UplinkConfigShell.this.scmfBrowseButton)
    			{
    				final String filename =
                        swtutil.displayStickyFileChooser(
                            false,
                            UplinkConfigShell.this.configShell,
                            "scmfBrowseButton");

    				if(filename != null)
    				{
    					UplinkConfigShell.this.scmfFileText.setText(filename);
    				}
    			}
    			else if(se.getSource() == UplinkConfigShell.this.cancelButton)
    			{
    				UplinkConfigShell.this.canceled = true;
                    UplinkConfigShell.this.configShell.close();
    			}
    			else if(se.getSource() == UplinkConfigShell.this.okButton)
    			{
    				try
    				{
    					setConfigDataFromFields();
    					UplinkConfigShell.this.configShell.close();
    				}
    				catch(final Exception e)
    				{
    					SWTUtilities.showErrorDialog(UplinkConfigShell.this.configShell,"Invalid Configuration Entry",e.getMessage());
    				}
    			}
    			else if(se.getSource() == UplinkConfigShell.this.restoreDefaultsButton)
    			{
    				restoreDefaults();
    			}
    		}
    		catch(final Exception e)
    		{
    			SWTUtilities.showErrorDialog(UplinkConfigShell.this.configShell,"Execution Error","Error executing button operation: " + e.getMessage());
    		}
    	}
    }
}
