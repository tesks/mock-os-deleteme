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
package jpl.gds.session.config.gui;

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * The is an SWT GUI composite class that presents the content of the
 * Message Service/Database tab in the session configuration window. This class wraps two
 * SWT ExpandItems containing GUI composites, one for message service information, and one
 * for database information.
 * 
 * Fields in the panel will be editable only if the flag to make them so
 * is passed to the constructor.
 * 
 * Note this class has two ExpandItems, unlike the other panels, so it does
 * not use the ExpandItem member in the base class. 
 * 
 */
public class SessionConfigMessageServiceDbComposite extends AbstractSessionConfigPanel implements ISessionConfigPanel {

    private static final boolean              ENABLE_DB_PASSWORD = false;
    private static final String               ID_DELIMITER       = ",";

    // Static message-service-related GUI controls
    private Group                             jmsGroup;
    private ExpandItem                        jmsGroupItem;
    private Composite                         jmsCompositeTop;
    private Label                             jmsHostLabel;
    private Text                              jmsHostText;
    private Text                              jmsPortText;

    // Dynamic JMS-related GUI controls
    private Composite                         jmsCompositeBottom;
    private Combo                             jmsSubtopicCombo;

    // Database GUI controls
    private Group                             dbGroup;
    private Text                              dbHostText;
    private Text                              dbPortText;
    private Text                              dbNameText;
    private Text                              dbUserText;
    private Text                              dbPasswordText;

    // Other members
    private final IDatabaseProperties         dbProperties;
    private final MessageServiceConfiguration jmsConfig;
    private final GlobalLadProperties         ladConfig;
    private final ChannelLadBootstrapConfiguration bootstrapConfig;
    private final boolean                     editable;

    private Text                              topicText;

    private final Color                       disableColor;

    private Group                             ladGroup;
    private Composite                         ladCompositeTop;
    private Label                             ladHostLabel;
    private Label                             bootStrapLabel;
    private Label                             channelLadIdLabel;
    private Text                              ladHostText;
    private Combo                             bootStrapCombo;
    private Text                              channelLadIdText;
    private Text                              ladPortText;
    private ExpandItem                        ladGroupItem;
    private Text                              ladRestPortText;

    private Button                            topicCheckbox;
    private boolean                           topicOverridden;



	/**
	 * Constructor. Note that this will not complete the GUI creation. To do
	 * that, call createGui().
	 * 
	 * @param appContext  the current ApplicationContext object
	 * 
	 * @param parentWindow
	 *            The parent session configuration GUI object
	 * @param session
	 *            The SessionConfiguration object containing initial data for
	 *            display
	 * @param editableFields
	 *            true if message service and DB host/port and related fields should be
	 *            editable; false if not
	 */
	public SessionConfigMessageServiceDbComposite(final ApplicationContext appContext,
			final SessionConfigShell parentWindow,
			final SessionConfiguration session, final boolean editableFields) {
		super(appContext, parentWindow, session);
        dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);
		jmsConfig = appContext.getBean(MessageServiceConfiguration.class);
		ladConfig = GlobalLadProperties.getGlobalInstance();

		this.editable = editableFields;
		disableColor = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.LIGHT_GREY));

        this.bootstrapConfig = appContext.getBean(ChannelLadBootstrapConfiguration.class);
	}

    /**
     * Creates all the GUI components.
     * 
     * @param parentTabBar
     *            the parent ExpandBar for the ExpandItem created by this class
     */
	public void createGui(final ExpandBar parentTabBar) {
		createDatabaseGroup(parentTabBar);
		createMessageServiceGroup(parentTabBar);
		createGlobalLadGroup(parentTabBar);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setFieldsFromData()
	 */
	@Override
	public void setFieldsFromData() {
		if (this.jmsSubtopicCombo != null
				&& !this.jmsSubtopicCombo.isDisposed()) {
			SessionConfigShellUtil.safeSetText(this.jmsSubtopicCombo,
					this.getSessionConfig().getGeneralInfo().getSubtopic());
		}
		this.topicOverridden = this.getSessionConfig().getGeneralInfo().isTopicOverridden();
		this.topicCheckbox.setSelection(this.topicOverridden);
		if (this.topicText != null && this.topicOverridden) {
		    SessionConfigShellUtil.safeSetText(this.topicText,
		            this.getSessionConfig().getGeneralInfo().getRootPublicationTopic());
		    this.topicText.setEnabled(true);
		} else if (this.topicText != null) {
		    topicText.setText("<DEFAULT>");
		    this.topicText.setEnabled(false);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDataFromFields()
	 */
	@Override
	public void setDataFromFields() {
		if (this.jmsSubtopicCombo != null
				&& !this.jmsSubtopicCombo.isDisposed()) {
			this.getSessionConfig().getGeneralInfo().setSubtopic(this.jmsSubtopicCombo.getText());
		}
		if (this.topicText != null && this.topicOverridden) {
		    this.getSessionConfig().getGeneralInfo().setRootPublicationTopic(this.topicText.getText().trim());
		    this.getSessionConfig().getGeneralInfo().setTopicIsOverridden(true);
		} else {
		    this.getSessionConfig().getGeneralInfo().setRootPublicationTopic(null);
            this.getSessionConfig().getGeneralInfo().setTopicIsOverridden(false);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#validateInputFields()
	 */
	@Override
	public boolean validateInputFields() {
		if (this.jmsHostText != null) {
			final String jmsHost = this.jmsHostText.getText().trim();
			if (jmsHost.isEmpty()) {
				SWTUtilities.showMessageDialog(this.getParent(), "Invalid Message Service Host",
						"You must enter the message service host name.");
				return false;
			}
		}

		if (this.dbHostText != null) {
		    final String jmsHost = this.dbHostText.getText().trim();
		    if (jmsHost.isEmpty()) {
		        SWTUtilities.showMessageDialog(this.getParent(), "Invalid Database Host",
		                "You must enter the database host name.");
		        return false;
		    }
		}


		if (this.ladHostText != null) {
		    final String jmsHost = this.ladHostText.getText().trim();
		    if (jmsHost.isEmpty()) {
		        SWTUtilities.showMessageDialog(this.getParent(), "Invalid Global LAD Host",
		                "You must enter the Global LAD host name.");
		        return false;
		    }
		}

		if (this.jmsPortText != null && SessionConfigShellUtil.getAndValidatePortText(this.jmsPortText,
				"Message Service", this.getParent()) == null) {
			return false;

		}

		if (this.dbPortText != null && SessionConfigShellUtil.getAndValidatePortText(this.dbPortText,
				"Database", this.getParent()) == null) {
			return false;

		}
		

        if (this.ladPortText != null && SessionConfigShellUtil.getAndValidatePortText(this.ladPortText,
                "Global LAD", this.getParent()) == null) {
            return false;

        }
        
        if (this.ladRestPortText != null && SessionConfigShellUtil.getAndValidatePortText(this.ladRestPortText,
                "Global LAD Rest", this.getParent()) == null) {
            return false;

        }

		if (this.topicText != null && this.topicOverridden && getAndValidateTopicText(this.topicText, this.getParent()) == null) {
		    return false;
		}
		
		if (this.channelLadIdText != null && SessionConfigShellUtil.getAndValidateLadIds(channelLadIdText, this.getParent()) == null) { 
            return false;
		}
		
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDefaultFieldValues()
	 */
	@Override
	public void setDefaultFieldValues() {

		if (this.jmsSubtopicCombo != null
				&& !this.jmsSubtopicCombo.isDisposed()) {
			final String defaultSubtopic = missionProps.getDefaultSubtopic();

			this.jmsSubtopicCombo.select(0);

			if (defaultSubtopic != null) {
				this.jmsSubtopicCombo.setText(defaultSubtopic);
			}
		}
	}

	/**
	 * Sets fields in the global JMS configuration object from the content of
	 * GUI fields.
	 */
	public void setJmsConfigurationFields() {

		if (this.jmsHostText != null) {
			final String jmsHost = this.jmsHostText.getText().trim();
			jmsConfig.setMessageServerHost(jmsHost);
		}

		if (this.jmsPortText != null) {
			final String jmsPort = this.jmsPortText.getText().trim();
			final int jmsPortNum = Integer.parseInt(jmsPort);
			jmsConfig.setMessageServerPort(jmsPortNum);
		}
	}
	
	/**
     * Sets fields in the global LAD configuration object from the content of
     * GUI fields.
     */
    public void setLadConfigurationFields() {

        if (this.ladHostText != null) {
            final String ladHost = this.ladHostText.getText().trim();
            ladConfig.setServerHost(ladHost);
        }

        if (this.ladPortText != null) {
            final String ladPort = this.ladPortText.getText().trim();
            final int ladPortNum = Integer.parseInt(ladPort);
            ladConfig.setGlobalLadSocketServerPort(ladPortNum);
        }

        if (this.bootStrapCombo.getText() != null) {
            final String status = this.bootStrapCombo.getText().trim();
            bootstrapConfig.setLadBootstrapFlag(Boolean.valueOf(status));
        }
        if (this.channelLadIdText != null) {
            Collection<Long> idList = Collections.emptyList();
            
            try { 
            		idList = CliUtility.expandCsvRangeLong(channelLadIdText.getText().trim());
            } catch (final ParseException e) { 
                TraceManager.getDefaultTracer()
                            .warn("Unable to parse ChannelLAD bootstrap ID's : " + ExceptionTools.getMessage(e)
                                    + ". Using default of NO ID");
            }
            bootstrapConfig.setLadBootstrapIds(idList);

        }
    }

    /**
     * Sets fields in the global database configuration object from the content
     * of GUI fields.
     * 
     * The suppress is to avoid a dead code warning.
     */
    @SuppressWarnings("unused")
    public void setDbConfigurationFields() {

        if (this.dbHostText != null) {
            this.dbProperties.setHost(this.dbHostText.getText().trim());
        }

        if (this.dbPortText != null) {
            final String dbPort = this.dbPortText.getText().trim();
            this.dbProperties.setPort(Integer.parseInt(dbPort));
        }

        if (this.dbNameText != null) {
            this.dbProperties.setDatabaseName(this.dbNameText.getText().trim());
        }

        if (this.dbUserText != null) {
            this.dbProperties.setUsername(this.dbUserText.getText().trim());
        }

        if (this.dbPasswordText != null && ENABLE_DB_PASSWORD) {
        	this.dbProperties.setPassword(this.dbPasswordText.getText());
        }
    }

	/**
	 * Creates or destroys dynamic GUI fields related to the message service based upon the
	 * current venue.
	 * 
	 * @param vt
	 *            the currently selected venue type
	 */
	public void drawDynamicMessageServiceFields(final VenueType vt) {

		if (!vt.isOpsVenue() && this.jmsCompositeBottom != null) {
			this.jmsCompositeBottom.dispose();
			this.jmsCompositeBottom = null;
			this.jmsSubtopicCombo = null;
		}

		if (vt.isOpsVenue() && this.jmsCompositeBottom == null) {
			this.jmsCompositeBottom = new Composite(this.jmsGroup, SWT.NONE);
			final FormLayout fl = new FormLayout();
			fl.spacing = 15;
			fl.marginTop = 0;
			fl.marginBottom = 10;
			fl.marginLeft = 0;
			fl.marginRight = 0;
			this.jmsCompositeBottom.setLayout(fl);
			final FormData fd = new FormData();
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.top = new FormAttachment(this.jmsCompositeTop, 0);
			this.jmsCompositeBottom.setLayoutData(fd);

			final Label jmsSubtopicLabel = new Label(this.jmsCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);
			jmsSubtopicLabel.setText("JMS Subtopic:");
			final FormData fd2 = new FormData();
			fd2.left = new FormAttachment(0);
			fd2.top = new FormAttachment(this.jmsHostLabel, 0, SWT.CENTER);
			jmsSubtopicLabel.setLayoutData(fd2);
			this.jmsSubtopicCombo = new Combo(this.jmsCompositeBottom,
					SessionConfigShellUtil.COMBO_STYLE);
			final List<String> subtopics = missionProps.getAllowedSubtopics();
			this.jmsSubtopicCombo.setItems(subtopics.toArray(new String[] {}));
			final FormData fd3 = SWTUtilities.getFormData(
					this.jmsSubtopicCombo, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd3.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd3.top = new FormAttachment(this.jmsHostText);
			this.jmsSubtopicCombo.setLayoutData(fd3);
			this.jmsSubtopicCombo.setEnabled(this.editable && !this.topicCheckbox.getSelection());
			if (!this.editable) {
			    this.jmsSubtopicCombo.setBackground(disableColor);
			}
			
			final Label topicWarnLabel = new Label(this.jmsCompositeBottom, SessionConfigShellUtil.LABEL_STYLE);
			topicWarnLabel.setText("JMS Subtopic is used only with default session topic, not with custom topics");
			final FormData warnLabelData = new FormData();
			warnLabelData.left = new FormAttachment(SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			warnLabelData.top = new FormAttachment(this.jmsSubtopicCombo);
			topicWarnLabel.setLayoutData(warnLabelData);

			// Must do this or the combo will have no value
			setDefaultFieldValues();
		}

		resizeMessageServiceGroup();
	}

	/**
	 * Creates the message service group/composite and all the related GUI components.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar for the ExpandItem created by this class
	 *            
	 */
	private Group createMessageServiceGroup(final ExpandBar parentTabBar) {
	    
	    if (this.jmsGroup == null) {
	        getMessageServiceGroup(parentTabBar);

	        this.jmsCompositeTop = new Composite(this.jmsGroup, SWT.NONE);
	        final FormLayout fl = new FormLayout();
	        fl.spacing = 10;
	        fl.marginTop = 0;
	        fl.marginBottom = 0;
	        fl.marginLeft = 0;
	        fl.marginRight = 0;
	        this.jmsCompositeTop.setLayout(fl);
	        final FormData fd2 = new FormData();
	        fd2.left = new FormAttachment(0);
	        fd2.right = new FormAttachment(100);
	        fd2.top = new FormAttachment(0);
	        this.jmsCompositeTop.setLayoutData(fd2);

	        this.jmsHostLabel = new Label(this.jmsCompositeTop,
	                SessionConfigShellUtil.LABEL_STYLE);
	        this.jmsHostLabel.setText("Msg Service Host:");
	        final FormData fd = new FormData();
	        fd.left = new FormAttachment(0);
	        this.jmsHostLabel.setLayoutData(fd);

	        this.jmsHostText = new Text(this.jmsCompositeTop,
	                SessionConfigShellUtil.SHORT_TEXT_STYLE);
	        this.jmsHostText.setText(this.jmsConfig.getMessageServerHost());
	        final FormData fd1 = SWTUtilities.getFormData(this.jmsHostText, 1,
	                SessionConfigShellUtil.SHORT_FIELD_SIZE);
	        fd1.left = new FormAttachment(
	                SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
	        this.jmsHostText.setLayoutData(fd1);
	        setEditable(this.jmsHostText);

	        final Label jmsPortLabel = new Label(this.jmsCompositeTop,
	                SessionConfigShellUtil.LABEL_STYLE);
	        jmsPortLabel.setText("Msg Service Port:");
	        final FormData fd5 = new FormData();
	        fd5.left = new FormAttachment(
	                SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
	        jmsPortLabel.setLayoutData(fd5);

	        this.jmsPortText = new Text(this.jmsCompositeTop,
	                SessionConfigShellUtil.SHORT_TEXT_STYLE);
	        this.jmsPortText.setText(String.valueOf(this.jmsConfig
	                .getMessageServerPort()));
	        final FormData fd3 = SWTUtilities.getFormData(this.jmsHostText, 1,
	                SessionConfigShellUtil.SHORT_FIELD_SIZE);
	        fd3.left = new FormAttachment(
	                SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
	        this.jmsPortText.setLayoutData(fd3);
	        setEditable(this.jmsPortText);

	        final Label topicLabel = new Label(this.jmsCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
	        topicLabel.setText("Root Topic:");
	        final FormData topicLabelData = new FormData();
	        topicLabelData.left = new FormAttachment(
	                SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);

	        topicLabelData.top = new FormAttachment(this.jmsPortText, 10);
	        topicLabel.setLayoutData(topicLabelData);

	        this.topicText = new Text(this.jmsCompositeTop, SessionConfigShellUtil.SHORT_TEXT_STYLE);
	        final FormData topicTextData = SWTUtilities.getFormData(this.jmsHostText, 1,
	                SessionConfigShellUtil.LONG_FIELD_SIZE);
	        topicTextData.left = new FormAttachment(
	                SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
	        topicTextData.top = new FormAttachment(this.jmsPortText);
	        this.topicText.setLayoutData(topicTextData);
	        setEditable(this.topicText);
	        this.topicText.setEnabled(this.topicOverridden);

	        
	        final Label checkboxLabel = new Label(this.jmsCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
	        checkboxLabel.setText("Override Default Topic");
	        final FormData boxLabelData = new FormData();
	        boxLabelData.left = new FormAttachment(this.topicText);
	        boxLabelData.top = new FormAttachment(this.jmsPortText, 10);
	        checkboxLabel.setLayoutData(boxLabelData);
	        
	        this.topicCheckbox = new Button(this.jmsCompositeTop, SWT.CHECK);
	        final FormData boxData = new FormData();
            boxData.left = new FormAttachment(checkboxLabel);
            boxData.top = new FormAttachment(this.jmsPortText, 5);
            this.topicCheckbox.setLayoutData(boxData);
            this.topicCheckbox.setSelection(this.topicOverridden);
            this.topicCheckbox.setEnabled(this.editable);
            
            this.topicCheckbox.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetDefaultSelected(final SelectionEvent arg0) {
                    // do nothing
                    
                }

                @Override
                public void widgetSelected(final SelectionEvent arg0) {
                    topicOverridden = topicCheckbox.getSelection();
                    topicText.setEnabled(topicOverridden); 
                    if (!topicOverridden) {
                        topicText.setText("<DEFAULT>");
                    } else {
                        topicText.setText(getSessionConfig().getGeneralInfo().getRootPublicationTopic());
                    }
                    if (jmsSubtopicCombo != null) {
                        jmsSubtopicCombo.setEnabled(!topicOverridden);
                    }
                }
            });
	       
	        this.jmsGroupItem = new ExpandItem(parentTabBar, SWT.NONE,
	                parentTabBar.getItemCount());
	        this.jmsGroupItem.setText("Message Service Information");
	        this.jmsGroupItem.setExpanded(true);
	        this.jmsGroupItem.setControl(this.jmsGroup);

	        resizeMessageServiceGroup();
	    }
		
		return this.jmsGroup;
	}

	
	/**
     * Creates the message service group/composite and all the related GUI components.
     * 
     * @param parentTabBar
     *            the parent ExpandBar for the ExpandItem created by this class
     *            
     */
    private void createGlobalLadGroup(final ExpandBar parentTabBar) {
        if (this.ladGroup == null) {
            getGlobalLadGroup(parentTabBar);

            this.ladCompositeTop = new Composite(this.ladGroup, SWT.NONE);
            final FormLayout fl = new FormLayout();
            fl.spacing = 10;
            fl.marginTop = 0;
            fl.marginBottom = 0;
            fl.marginLeft = 0;
            fl.marginRight = 0;
            this.ladCompositeTop.setLayout(fl);
            final FormData fd2 = new FormData();
            fd2.left = new FormAttachment(0);
            fd2.right = new FormAttachment(100);
            fd2.top = new FormAttachment(0);
            this.ladCompositeTop.setLayoutData(fd2);

            this.ladHostLabel = new Label(this.ladCompositeTop,
                    SessionConfigShellUtil.LABEL_STYLE);
            this.ladHostLabel.setText("LAD Host:");
            final FormData fd = new FormData();
            fd.left = new FormAttachment(0);
            this.ladHostLabel.setLayoutData(fd);

            this.ladHostText = new Text(this.ladCompositeTop,
                    SessionConfigShellUtil.SHORT_TEXT_STYLE);
            this.ladHostText.setText(this.ladConfig.getServerHost());
            final FormData fd1 = SWTUtilities.getFormData(this.ladHostText, 1,
                    SessionConfigShellUtil.SHORT_FIELD_SIZE);
            fd1.left = new FormAttachment(
                    SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
            this.ladHostText.setLayoutData(fd1);
            setEditable(this.ladHostText);

            this.bootStrapLabel = new Label(this.ladCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
            this.bootStrapLabel.setText("Bootstrap LAD:");
            final FormData bootstrapData = new FormData();
            bootstrapData.left = new FormAttachment(0);
            bootstrapData.top = new FormAttachment(ladHostText, 5);
            this.bootStrapLabel.setLayoutData(bootstrapData);

            this.bootStrapCombo = new Combo(this.ladCompositeTop, SessionConfigShellUtil.COMBO_STYLE);
            bootStrapCombo.setItems(new String[] {Boolean.TRUE.toString(), Boolean.FALSE.toString()});
            
            if (bootstrapConfig.getLadBootstrapFlag()) {
                bootStrapCombo.select(0);
            }
            else {
                bootStrapCombo.select(1);
            }
            
            final FormData enblProp = SWTUtilities.getFormData(this.bootStrapCombo, 1,
                                                               SessionConfigShellUtil.SHORT_FIELD_SIZE);
            enblProp.left = new FormAttachment(SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
            enblProp.top = new FormAttachment(ladHostText);
            bootStrapCombo.setLayoutData(enblProp);
            this.bootStrapCombo.setEnabled(this.editable);
            

            this.channelLadIdLabel = new Label(this.ladCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
            this.channelLadIdLabel.setText("Channel LAD IDs: ");
            final FormData idData = new FormData();
            idData.left = new FormAttachment(SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
            idData.top = new FormAttachment(this.bootStrapLabel, 5);
            this.channelLadIdLabel.setLayoutData(idData);

            this.channelLadIdText = new Text(this.ladCompositeTop, SessionConfigShellUtil.SHORT_TEXT_STYLE);
            
            /**
             * Only set the ids if there are any.
             */
            if (bootstrapConfig.getLadBootstrapIds().isEmpty()) {
            		// Disable the lad ids.
            		channelLadIdText.setEditable(false);
            } else {
                channelLadIdText.setText(StringUtils.join(bootstrapConfig.getLadBootstrapIds(), ID_DELIMITER));
            }

            final FormData idsForm = SWTUtilities.getFormData(this.channelLadIdText, 1,
                                                              SessionConfigShellUtil.SHORT_FIELD_SIZE);
            idsForm.left = new FormAttachment(SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
            idsForm.top = new FormAttachment(this.bootStrapLabel, 5);
            this.channelLadIdText.setLayoutData(idsForm);
            channelLadIdText.setEnabled(this.editable);

            // Add listener for the bootstrap options.
            bootStrapCombo.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(final ModifyEvent arg0) {
					if (bootStrapCombo.getSelectionIndex() == 0) {
						channelLadIdText.setEditable(true);
					} else {
						channelLadIdText.setText("");
						channelLadIdText.setEditable(false);
					}
				}
			});

            if (!this.editable) {
                this.channelLadIdText.setBackground(disableColor);
                this.bootStrapCombo.setBackground(disableColor);
            }

            final Label ladPortLabel = new Label(this.ladCompositeTop,
                    SessionConfigShellUtil.LABEL_STYLE);
            ladPortLabel.setText("LAD Socket Port:");
            final FormData fd5 = new FormData();
            fd5.left = new FormAttachment(
                    SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
            ladPortLabel.setLayoutData(fd5);

            this.ladPortText = new Text(this.ladCompositeTop,
                    SessionConfigShellUtil.SHORT_TEXT_STYLE);
            this.ladPortText.setText(String.valueOf(this.ladConfig.getSocketServerPort()));
            final FormData fd3 = SWTUtilities.getFormData(this.ladHostText, 1,
                    SessionConfigShellUtil.SHORT_FIELD_SIZE);
            fd3.left = new FormAttachment(
                    SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
            this.ladPortText.setLayoutData(fd3);
            setEditable(this.ladPortText);

            final Label ladRestPortLabel = new Label(this.ladCompositeTop,
                    SessionConfigShellUtil.LABEL_STYLE);
            ladRestPortLabel.setText("LAD REST Port:");
            final FormData restPortLabelData = new FormData();
            restPortLabelData.left = new FormAttachment(
                    SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
            restPortLabelData.top = new FormAttachment(ladPortText, 5);
            ladRestPortLabel.setLayoutData(restPortLabelData);

            this.ladRestPortText = new Text(this.ladCompositeTop,
                    SessionConfigShellUtil.SHORT_TEXT_STYLE);
            this.ladRestPortText.setText(String.valueOf(this.ladConfig.getRestPort()));
            final FormData restPortTextData = SWTUtilities.getFormData(this.ladHostText, 1,
                    SessionConfigShellUtil.SHORT_FIELD_SIZE);
            restPortTextData.left = new FormAttachment(
                    SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
            restPortTextData.top = new FormAttachment(ladPortText);
            this.ladRestPortText.setLayoutData(restPortTextData);
            setEditable(this.ladRestPortText);


            this.ladGroupItem = new ExpandItem(parentTabBar, SWT.NONE,
                    parentTabBar.getItemCount());
            this.ladGroupItem.setText("Global LAD Information");
            this.ladGroupItem.setControl(this.ladGroup);
            this.ladGroupItem.setHeight(this.ladGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
            this.ladGroupItem.setExpanded(true);
        }

    }

	/**
	 * Creates the message service group/composite and all the related GUI components.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar for the ExpandItem created by this class
	 */
	private void getMessageServiceGroup(final ExpandBar parentTabBar) {
		if (this.jmsGroup == null) {
			this.jmsGroup = new Group(parentTabBar,
					SessionConfigShellUtil.GROUP_STYLE);
			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.getParent().getDisplay(),
					groupFontData);
			this.jmsGroup.setFont(groupFont);
			final FormLayout fl1 = new FormLayout();
			fl1.spacing = 10;
			fl1.marginHeight = 5;
			fl1.marginBottom = 6;
			fl1.marginWidth = 5;
			this.jmsGroup.setLayout(fl1);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			fd1.top = new FormAttachment(createDatabaseGroup(parentTabBar));
			this.jmsGroup.setLayoutData(fd1);
		}
	}
	
	/**
     * Creates the Global LAD group/composite and all the related GUI components.
     * 
     * @param parentTabBar
     *            the parent ExpandBar for the ExpandItem created by this class
     */
    private void getGlobalLadGroup(final ExpandBar parentTabBar) {
        if (this.ladGroup == null) {
            this.ladGroup = new Group(parentTabBar,
                    SessionConfigShellUtil.GROUP_STYLE);
            final FontData groupFontData = new FontData(
                    SessionConfigShellUtil.DEFAULT_FACE,
                    SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
            final Font groupFont = new Font(this.getParent().getDisplay(),
                    groupFontData);
            this.ladGroup.setFont(groupFont);
            final FormLayout fl1 = new FormLayout();
            fl1.spacing = 10;
            fl1.marginHeight = 5;
            fl1.marginBottom = 6;
            fl1.marginWidth = 5;
            this.ladGroup.setLayout(fl1);
            final FormData fd1 = new FormData();
            fd1.left = new FormAttachment(0);
            fd1.right = new FormAttachment(100);
            fd1.top = new FormAttachment(createMessageServiceGroup(parentTabBar));
            this.ladGroup.setLayoutData(fd1);
        }
    }

	/**
	 * Resizes the message service group.
	 */
	private void resizeMessageServiceGroup() {
		this.jmsGroupItem.setHeight(this.jmsGroup.computeSize(SWT.DEFAULT,
				SWT.DEFAULT).y);
	}

	/**
	 * Creates the DB group/composite and all the DB-related GUI components.
	 * 
	 * The suppression is because we are using the ENABLE_DB_PASSWORD in
	 * conditionals and FindBugs does not like it.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar for the ExpandItem created by this class
	 *            
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings("UCF_USELESS_CONTROL_FLOW_NEXT_LINE")
	private Group createDatabaseGroup(final ExpandBar parentTabBar) {
		if (this.dbGroup == null) {
			this.dbGroup = new Group(parentTabBar,
					SessionConfigShellUtil.GROUP_STYLE);

			final ExpandItem item0 = new ExpandItem(parentTabBar, SWT.NONE,
					parentTabBar.getItemCount());

			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.getParent().getDisplay(),
					groupFontData);
			this.dbGroup.setFont(groupFont);
			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginBottom = 6;
			fl.marginWidth = 5;
			this.dbGroup.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			fd1.top = new FormAttachment(0, 10);
			this.dbGroup.setLayoutData(fd1);

			final Label dbHostLabel = new Label(this.dbGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			dbHostLabel.setText("DB Host:");
			final FormData fd = new FormData();
			fd.left = new FormAttachment(0);
			fd.top = new FormAttachment(0);
			dbHostLabel.setLayoutData(fd);

			this.dbHostText = new Text(this.dbGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.dbHostText.setText(this.dbProperties.getHost());
			final FormData fd3 = SWTUtilities.getFormData(this.dbHostText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd3.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd3.top = new FormAttachment(0);
			this.dbHostText.setLayoutData(fd3);
			setEditable(this.dbHostText);

			final Label dbPortLabel = new Label(this.dbGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			dbPortLabel.setText("DB Port:");
			final FormData fd2 = new FormData();
			fd2.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd2.top = new FormAttachment(0);
			dbPortLabel.setLayoutData(fd2);

			this.dbPortText = new Text(this.dbGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.dbPortText.setText(String.valueOf(this.dbProperties.getPort()));
			final FormData fd4 = SWTUtilities.getFormData(this.dbPortText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd4.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd4.top = new FormAttachment(0);
			this.dbPortText.setLayoutData(fd4);
		    setEditable(this.dbPortText);

			this.dbNameText = new Text(this.dbGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.dbNameText.setText(this.dbProperties.getDatabaseName());
			final FormData fd6 = SWTUtilities.getFormData(this.dbNameText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd6.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd6.top = new FormAttachment(this.dbHostText);
			this.dbNameText.setLayoutData(fd6);
	        setEditable(this.dbNameText);

			final Label dbNameLabel = new Label(this.dbGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			dbNameLabel.setText("DB Name:");
			final FormData fd5 = new FormData();
			fd5.left = new FormAttachment(0);
			fd5.top = new FormAttachment(this.dbNameText, 0, SWT.CENTER);
			dbNameLabel.setLayoutData(fd5);

			this.dbUserText = new Text(this.dbGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.dbUserText
					.setText(String.valueOf(this.dbProperties.getUsername()));
			final FormData fd10 = SWTUtilities.getFormData(this.dbUserText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd10.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd10.top = new FormAttachment(this.dbNameText);
			this.dbUserText.setLayoutData(fd10);
			setEditable(this.dbUserText);
            
			this.dbPasswordText = new Text(this.dbGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE | SWT.PASSWORD);
			this.dbPasswordText.setText("");

			final FormData fd8 = SWTUtilities.getFormData(this.dbPasswordText,
					1, SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd8.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd8.top = new FormAttachment(this.dbNameText);
			this.dbPasswordText.setLayoutData(fd8);
			this.dbPasswordText.setEnabled(this.editable && ENABLE_DB_PASSWORD);
		    if (!this.editable || !ENABLE_DB_PASSWORD) {
                this.dbPasswordText.setBackground(disableColor);
            }

			final Label dbPasswordLabel = new Label(this.dbGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			dbPasswordLabel.setText("DB Password:");
			final FormData fd7 = new FormData();
			fd7.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd7.top = new FormAttachment(this.dbPasswordText, 0, SWT.CENTER);
			dbPasswordLabel.setLayoutData(fd7);

			final Label dbUserLabel = new Label(this.dbGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			dbUserLabel.setText("DB Username:");
			final FormData fd9 = new FormData();
			fd9.left = new FormAttachment(0);
			fd9.top = new FormAttachment(this.dbUserText, 0, SWT.CENTER);
			dbUserLabel.setLayoutData(fd9);

			item0.setText("Database Information");
			item0.setHeight(this.dbGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			item0.setControl(this.dbGroup);

			item0.setExpanded(true);
		}
		return this.dbGroup;
	}
	
	   
    private static String getAndValidateTopicText(final Text topicText, final Shell parent) {
        final String topic = topicText.getText().trim();
        final String error = ContextTopicNameFactory.checkTopic(topic, true);
        if (error != null) {
            SWTUtilities.showErrorDialog(parent, "Bad root topic", "The root topic " + topic + " is invalid.\n" + error);
            topicText.setFocus();
            return null;
        }
        return topic;
    }
    
    private void setEditable(final Text text) {
        if (!this.editable) {
            text.setBackground(disableColor);
        }
        text.setEnabled(this.editable);
    }
    

}
