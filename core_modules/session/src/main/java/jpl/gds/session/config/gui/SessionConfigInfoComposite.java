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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class implements the "Session Information" panel in the session 
 * configuration GUI window. It is a wrapper around an SWT ExpandItem 
 * that in turn contains the composite with the session-related GUI fields 
 * on it.
 * 
 */
public class SessionConfigInfoComposite extends AbstractSessionConfigPanel implements ISessionConfigPanel {
	
	// GUI controls
    private ExpandItem expandItem;
    private Group sessionInfoGroup;
    private Text sessionNameText;
    private Text sessionTypeText;
    private Combo spacecraftIdCombo;
    private Text sessionDescriptionText;
    
    /**
     * Constructor.  Note that this will not complete the GUI creation. To
     * do that, call createGui().
     * 
     * @param appContext  the current ApplicationContext object
     * 
     * @param parentWindow The parent session configuration GUI object
     * @param session The SessionConfiguration object containing initial data 
     *                for display          
     */
    public SessionConfigInfoComposite(ApplicationContext appContext,
    		SessionConfigShell parentWindow, SessionConfiguration session) {
    	super(appContext, parentWindow, session);
    }
    
    /**
	 * Creates all the GUI components.
	 * 
	 * @param parentTabBar the parent ExpandBar to attach the SSE 
	 *                     ExpandItem to
	 *                     
	 */
	public void createGui(ExpandBar parentTabBar) {
    	createSessionGroup(parentTabBar);

    	final FormAttachment labelStart = new FormAttachment(0);
    	final FormAttachment inputStart = new FormAttachment(20);

    	final Label testNameLabel = new Label(sessionInfoGroup, SessionConfigShellUtil.LABEL_STYLE);

    	sessionNameText = new Text(sessionInfoGroup, SessionConfigShellUtil.SHORT_TEXT_STYLE);
    	sessionNameText.setFocus();

    	final FormData fd2 = SWTUtilities.getFormData(sessionNameText, 1, 20);
    	fd2.left = inputStart;
    	fd2.right = new FormAttachment(100);
    	fd2.top = new FormAttachment(0);
    	sessionNameText.setLayoutData(fd2);
    	testNameLabel.setText("Session Name:");
    	final FormData fd3 = new FormData();
    	fd3.left = labelStart;
    	fd3.right = new FormAttachment(sessionNameText);
    	fd3.top = new FormAttachment(sessionNameText, 0, SWT.CENTER);
    	testNameLabel.setLayoutData(fd3);

    	final Label testTypeLabel = new Label(sessionInfoGroup, SessionConfigShellUtil.LABEL_STYLE);
    	sessionTypeText = new Text(sessionInfoGroup, SessionConfigShellUtil.SHORT_TEXT_STYLE);
    	final FormData fd4 = SWTUtilities.getFormData(sessionTypeText, 1, 40);
    	fd4.left = inputStart;
    	fd4.top = new FormAttachment(sessionNameText);
    	sessionTypeText.setLayoutData(fd4);
    	testTypeLabel.setText("Session Type:");
    	final FormData fd5 = new FormData();
    	fd5.left = labelStart;
    	fd5.right = new FormAttachment(sessionTypeText);
    	fd5.top = new FormAttachment(sessionTypeText, 0, SWT.CENTER);
    	testTypeLabel.setLayoutData(fd5);

    	final Label spacecraftIdLabel = new Label(sessionInfoGroup, SessionConfigShellUtil.LABEL_STYLE);
    	final FormData scidlFd = new FormData();
    	scidlFd.left = new FormAttachment(sessionTypeText, 10);
    	scidlFd.top = new FormAttachment(sessionTypeText, 0, SWT.CENTER);
    	spacecraftIdLabel.setLayoutData(scidlFd);

    	spacecraftIdCombo = new Combo(sessionInfoGroup, SessionConfigShellUtil.COMBO_STYLE);
    	initScids();

    	final FormData scidFd = SWTUtilities.getFormData(spacecraftIdCombo, 1, 20);
    	scidFd.left = new FormAttachment(spacecraftIdLabel, 10);
    	scidFd.top = new FormAttachment(sessionNameText);
    	scidFd.right = new FormAttachment(100);
    	spacecraftIdCombo.setLayoutData(scidFd);
    	spacecraftIdLabel.setText("Spacecraft ID:");

    	final Label testDescriptionLabel = new Label(sessionInfoGroup, SessionConfigShellUtil.LABEL_STYLE);

    	sessionDescriptionText = new Text(sessionInfoGroup, SessionConfigShellUtil.SHORT_TEXT_STYLE);

    	final FormData fd6 = SWTUtilities.getFormData(sessionDescriptionText, 1, 20);
    	fd6.left = inputStart;
    	fd6.right = new FormAttachment(100);
    	fd6.top = new FormAttachment(sessionTypeText, 0, SWT.LEFT);
    	sessionDescriptionText.setLayoutData(fd6);
    	testDescriptionLabel.setText("Session Description:");
    	final FormData fd7 = new FormData();
    	fd7.left = labelStart;
    	fd7.right = new FormAttachment(sessionDescriptionText);
    	fd7.top = new FormAttachment(sessionDescriptionText, 0, SWT.CENTER);
    	testDescriptionLabel.setLayoutData(fd7);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.session.config.gui.ISessionConfigPanel#setFieldsFromData()
     */
    @Override
	public void setFieldsFromData() {
  
    	final SessionConfiguration tc = getSessionConfig();
    	
        if (sessionNameText != null) {
            SessionConfigShellUtil.safeSetText(sessionNameText, tc.getContextId().getName());
        }

        if (sessionTypeText != null) {
            SessionConfigShellUtil.safeSetText(sessionTypeText, tc.getContextId().getType());            
        }

        if (spacecraftIdCombo != null) {
            SessionConfigShellUtil.safeSetText(spacecraftIdCombo, String.valueOf(tc.getContextId().getSpacecraftId()));
        }

        if (sessionDescriptionText != null) {
            SessionConfigShellUtil.safeSetText(sessionDescriptionText, tc.getContextId().getDescription());
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDataFromFields()
     */
    @Override
	public void setDataFromFields() {
    	
       	final SessionConfiguration tc = getSessionConfig();
       	
        if (sessionNameText != null) {
            tc.getContextId().setName(sessionNameText.getText().trim());
        }

        if (sessionTypeText != null) {
            tc.getContextId().setType(sessionTypeText.getText().trim());
        }

        if (spacecraftIdCombo != null)
        {
            final int scid = getCurrentSpacecraftId();
            tc.getContextId().setSpacecraftId(scid);
        }

        if (sessionDescriptionText != null) {
            tc.getContextId().setDescription(sessionDescriptionText.getText().trim());
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDefaultFieldValues()
     */
    @Override
	public void setDefaultFieldValues() {
    	
    	if (spacecraftIdCombo != null)
    	{
    		final int scid = getSessionConfig().getContextId().getSpacecraftId();
    		
    		/* Use MissionProperties. */
    		final List<Integer> ids = missionProps.getAllScids();
    		for (final int id: ids) {
    			if (scid != MissionProperties.UNKNOWN_ID
    					&& scid == id) {
    				SessionConfigShellUtil.safeSetText(spacecraftIdCombo, String
    						.valueOf(id));
    			}
    		}

    		if (scid == MissionProperties.UNKNOWN_ID && !ids.isEmpty()) {
    			SessionConfigShellUtil.safeSetText(spacecraftIdCombo, String.valueOf(ids.get(0)));
    		}
    	}
    	
        if (sessionTypeText != null) {
            sessionTypeText.setText("");
        }

        if (sessionDescriptionText != null) {
            sessionDescriptionText.setText("");
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.session.config.gui.ISessionConfigPanel#validateInputFields()
     */
    @Override
	public boolean validateInputFields() {

        if (sessionNameText != null) {
            final String testname = sessionNameText.getText().trim();
            if (testname.length() == 0) {
                SWTUtilities.showErrorDialog(getParent(), "Bad Session Name",
                        "You must enter a valid session name to proceed");

                sessionNameText.setFocus();

                return (false);
                
            /*
             * Prevent a session name longer than 64 characters to be entered.
             * If that illegal name is accepted, a hostname_TempTestConfig.xml
             * will be created by the session configuration application, which
             * then get rejected by subsequent applications because the XML is
             * illegal (schema defines session name to be no longer than 64
             * characters).
             */
            } else if (testname.length() > 64) {
                SWTUtilities.showErrorDialog(getParent(), "Bad Session Name",
                        "Session name cannot exceed 64 characters in length");

                sessionNameText.setFocus();

                return (false);
            	
            } else {
                for (int i = 0; i < testname.length(); i++) {
                    final char c = testname.charAt(i);
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                        SWTUtilities
                                .showErrorDialog(getParent(),
                                        "Bad Session Name",
                                        "Session Name can contain only letters, digits, dashes, and underscores.");
                        sessionNameText.setFocus();

                        return (false);
                    }
                }
            }
        }

        if (sessionTypeText != null) {
            final String type = sessionTypeText.getText().trim();
            if (type.contains("%") || type.contains(">") || type.contains("&")
                    || type.contains("<")) {
                SWTUtilities.showErrorDialog(getParent(), "Bad Session Type",
                        "Session Type cannot contain %, &, <, or >.");
                sessionTypeText.setFocus();

                return (false);
            }
        }

        if (sessionDescriptionText != null) {
            final String desc = sessionDescriptionText.getText().trim();
            if (desc.contains("%") || desc.contains(">") || desc.contains("&")
                    || desc.contains("<")) {
                SWTUtilities.showErrorDialog(getParent(),
                        "Bad Session Description",
                        "Session Description cannot contain %, &, <, or >.");
                sessionDescriptionText.setFocus();

                return (false);
            }
        }
        
        if (spacecraftIdCombo != null) {
            final String scidString = spacecraftIdCombo.getText();
            int scid = MissionProperties.UNKNOWN_ID;
            try {
                scid = Integer.parseInt(scidString);
            } catch (final NumberFormatException nfe) {
                SWTUtilities
                        .showErrorDialog(getParent(), "Bad Spacecraft ID",
                                "The specified spacecraft ID is not a valid integer value.");

                spacecraftIdCombo.setFocus();

                return (false);
            }

            if (scid < 0 || scid > 1023) {
                SWTUtilities
                        .showErrorDialog(
                                getParent(),
                                "Bad Spacecraft ID",
                                "The specified spacecraft ID is outside the allowable range."
                                        + "  A valid spacecraft ID ranges from 0-1023.");

                spacecraftIdCombo.setFocus();

                return (false);
            }
            if (missionProps.mapScidToMnemonic(scid).equals(
                    MissionProperties.UNKNOWN_ID)) {
                final boolean ok = SWTUtilities
                        .showConfirmDialog(
                                getParent(),
                                "Unrecognized Spacecraft ID",
                                "The specified Spacecraft ID "
                                        + scid
                                        + " is not recognized by the system for"
                                        + " this mission.  Do you wish to proceed with this value?");

                if (ok == false) {
                    spacecraftIdCombo.setFocus();
                    return (false);
                }
            }
        }
        return true;
    }
    
    /**
	 * Instantiates the main group (composite) for the
	 * session components. Also creates the parent ExpandItem and attaches
	 * it to the parent ExpandBar.
	 * 
	 * @param parentTabBar the parent ExpandBar to attach the session info 
	 *                     ExpandItem to
	 */
	private void createSessionGroup(ExpandBar parentTabBar) {
	    if (sessionInfoGroup == null) {
	        sessionInfoGroup = new Group(parentTabBar, SessionConfigShellUtil.GROUP_STYLE);
	
	        final FontData groupFontData = new FontData(SessionConfigShellUtil.DEFAULT_FACE, SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
	        final Font groupFont = new Font(getParent().getDisplay(), groupFontData);
	        sessionInfoGroup.setFont(groupFont);
	        final FormData fd1 = new FormData();
	        fd1.left = new FormAttachment(0);
	        fd1.right = new FormAttachment(100);
	        fd1.top = new FormAttachment(0, 10);
	        sessionInfoGroup.setLayoutData(fd1);
	        final FormLayout fl = new FormLayout();
	        fl.spacing = 10;
	        fl.marginHeight = 5;
	        fl.marginWidth = 5;
	        sessionInfoGroup.setLayout(fl);
	
	        expandItem = new ExpandItem(parentTabBar, SWT.NONE,
	        		parentTabBar.getItemCount());
	        expandItem.setText("Session Information");
	        expandItem.setHeight(sessionInfoGroup.computeSize(SWT.DEFAULT,
	                SWT.DEFAULT).y);
	        expandItem.setHeight(150);
	        expandItem.setControl(sessionInfoGroup);
	
	        expandItem.setExpanded(true);
	    }
	}

	/**
     * Get current spacecraft ID from the combo box, defaulting to
     * the mission default if that value is not valid. This happens sometimes
     * if the default SCID is not properly configured.
     *
     * @return scid value as integer
     */
    private int getCurrentSpacecraftId()
    {
        final int defalt = (getSessionConfig() != null) ? getSessionConfig().getContextId().getSpacecraftId() : MissionProperties.UNKNOWN_ID;
        int       result = defalt;

        if (spacecraftIdCombo == null)
        {
            return result;
        }

        String id = spacecraftIdCombo.getText();
        id = StringUtil.safeTrim(id);

        try
        {
            result = Integer.parseInt(id);
        }
        catch (final NumberFormatException nfe)
        {
            result = defalt;
        }

        return result;
    }
    
    /**
     * Initializes the spacecraft ID combo box from the list of valid spacecraft
     * identifiers for the current mission.
     */
    private void initScids() {
        final List<Integer> ids = missionProps.getAllScids();
        for (final int id: ids) {
            spacecraftIdCombo.add(String.valueOf(id));
        }
    }
}
