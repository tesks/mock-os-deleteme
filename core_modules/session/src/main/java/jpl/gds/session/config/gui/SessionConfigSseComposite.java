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

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.File;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class implements the "SSE" panel in the session configuration GUI
 * window. It is a wrapper around an SWT ExpandItem that in turn contains the
 * composite with all SSE-related GUI fields on it. Constructing this object is
 * insufficient for GUI usage. It must also be enabled.
 * 
 * When enable(true, parentBar) is called on this class, the ExpandItem will be
 * created and will be added to the parent ExpandBar. GUI-related methods will
 * then operate. If enable(false, null) is called, the ExpandItem and all
 * sub-components will be disposed, and all GUI-related methods will do nothing.
 * This allow the session configuration window parent to invoke methods on this
 * class without having to constantly check whether SSE GUI fields are enabled
 * or not.
 * 
 * This composite is enabled on the parent session configuration window in
 * non-OPS configurations for missions that support SSE, if the parent session
 * configuration window is not for a standalone FSW downlink process (which
 * requires no SSE parameters). If is disabled for missions that do not support
 * SSE, or when an OPS venue is selected by the user.
 * 
 *
 */
public class SessionConfigSseComposite extends AbstractSessionConfigPanel
		implements ISessionConfigPanel {

	private final Tracer trace; 


	// GUI controls
	private Group sseInfoGroup;
	private Text sseDictionaryDirText;
	private Button sseDictionaryDirBrowseButton;
	private Combo sseVersionCombo;
	private Text sseHostText;
	private Text sseUplinkPortText;
	private Combo sseLaunchDownlinkCombo;
	private Text sseDownlinkPortText;
	private ExpandItem expandItem;

	// Other member fields
	private final IConnectionMap hostConfig;
	private final boolean showUplink;
	private final boolean showDownlink;
	private boolean disabled;


	/**
	 * Constructor. Note that this will not initialize the actual GUI
	 * components. To do that, call enableBar().
	 * 
	 * @param appContext  the current ApplicationContext object
	 * 
	 * @param parentWindow
	 *            The parent session configuration GUI object
	 * @param session
	 *            The SessionConfiguration object containing initial data for
	 *            display
	 * @param downlink
	 *            true if downlink fields should be displayed
	 * @param uplink
	 *            true if uplink fields should be displayed
	 */
	public SessionConfigSseComposite(final ApplicationContext appContext, final SessionConfigShell parentWindow,
			final SessionConfiguration session, final boolean downlink, final boolean uplink) {
		super(appContext, parentWindow, session);
        trace = TraceManager.getDefaultTracer(appContext);
		this.disabled = true;
		this.showDownlink = downlink;
		this.showUplink = uplink;
		hostConfig = session.getConnectionConfiguration();
	}
	
	/**
	 * Indicates whether the SSE composite is currently enabled.
	 * @return true if enabled; false if not
	 * 
	 */
	public boolean isEnabled() {
		return !this.disabled;
	}

	/**
	 * Actually enables or disables GUI operations on this object. When the
	 * object is disabled, all GUI components are non-existent (disposed).
	 * 
	 * @param on
	 *            true to enable GUI operation; false to disable
	 * @param parentTabBar
	 *            the ExpandBar parent to the ExpandItem created by this class
	 */
	public void enable(final boolean on, final ExpandBar parentTabBar) {
		if (on && this.disabled) {
			createGui(parentTabBar);
		} else if (!on && !this.disabled) {
			this.sseInfoGroup.dispose();
			this.expandItem.dispose();
			this.sseInfoGroup = null;
			this.expandItem = null;
		}
		this.disabled = !on;
	}


	/**
	 * Enables/disables fields to account for a change in the downlink
	 * connection type to FILE. For this composite, this means that in
	 * integrated configurations (flight software downlink is present) the
	 * launch SSE downlink flag is set to false and it is disabled. This is
	 * because we cannot support integrated configurations in which both FSW and
	 * SSE downlink processes get data from a file. If not running integrated
	 * (i.e., this is a standalone SSE downlink instance) we leave the launch
	 * flag true and enabled, but turn off the SSE downlink port if the
	 * connection is FILE.
	 * 
	 * Does nothing if the whole composite is not enabled.
	 *
     * NB: Keep "logic" open so it's easier to understand
     *
	 * @param isFile True if a file connection has been selected
     *
	 */
    public void enableFieldsForFileConnection(final boolean isFile)
    {
        if (disabled || (sseLaunchDownlinkCombo == null))
        {
            return;
        }

        if (getSessionConfig().getSseContextFlag())
        {
            // Not integrated
            // SSE standalone, we can always run

            sseLaunchDownlinkCombo.setText(Boolean.TRUE.toString());
            sseLaunchDownlinkCombo.setEnabled(false);

            enableDownlinkPort(! isFile);
        }
        else if (isFile)
        {
            // Integrated with FILE
            // Cannot run

            sseLaunchDownlinkCombo.setText(Boolean.FALSE.toString());
            sseLaunchDownlinkCombo.setEnabled(false);

            enableDownlinkPort(false);
        }
        else
        {
            // Fetch current value of the run SSE flag

            final boolean canRunSse = Boolean.valueOf(sseLaunchDownlinkCombo.getText());

            if (canRunSse)
            {
                // Integrated without FILE and allowed to run

                // sseLaunchDownlinkCombo is already TRUE

                sseLaunchDownlinkCombo.setEnabled(true);

                enableDownlinkPort(true);
            }
            else
            {
                // Integrated without FILE and not allowed to run

                // sseLaunchDownlinkCombo is already FALSE

                sseLaunchDownlinkCombo.setEnabled(true);

                enableDownlinkPort(false);
            }
        }
    }


	/**
	 * Enables or disables the downlink port field. Does nothing if the whole
	 * composite is not enabled. Primary purpose of this method is to turn
	 * on/off the port field depending on the current downlink connection type.
	 * Note that if the launch downlink combo is set to false, the field will
	 * stay disabled regardless of the input parameter.
	 * 
	 * @param on
	 *            true to enable; false to disable
	 */
	public void enableDownlinkPort(final boolean on) {
		if (this.disabled) {
			return;
		}
		if (this.sseDownlinkPortText != null) {
			final boolean launchDownlink = Boolean
					.valueOf(this.sseLaunchDownlinkCombo.getText());
			this.sseDownlinkPortText.setEnabled(on && launchDownlink);
		}
	}

	/**
	 * Enables or disables the uplink port field. Does nothing if the whole
	 * composite is not enabled. Primary purpose of this method is to turn
	 * on/off the port field depending on the current uplink connection type.
	 * 
	 * @param on
	 *            true to enable; false to disable
	 */
	public void enableUplinkPort(final boolean on) {
		if (this.disabled) {
			return;
		}
		if (this.sseUplinkPortText != null) {
			this.sseUplinkPortText.setEnabled(on);
		}
	}

	/**
	 * Restores the host and port GUI fields from the global HostConfiguration
	 * object. Does nothing if the whole composite is not enabled.
	 */
	public void restoreNetworkSettings() {
		if (this.disabled) {
			return;
		}

		final IDownlinkConnection dc = hostConfig.getSseDownlinkConnection();
		final IUplinkConnection uc = hostConfig.getSseUplinkConnection();
		
		if (dc != null && dc instanceof INetworkConnection) {

			if (this.sseHostText != null &&  ((INetworkConnection)dc).getHost() != null) {
				SessionConfigShellUtil.safeSetText(this.sseHostText,
						((INetworkConnection)dc).getHost());
			}

			if (this.sseDownlinkPortText != null
					&& ((INetworkConnection)dc).getPort() >= 0) {
				this.sseDownlinkPortText.setText(String.valueOf(((INetworkConnection)dc).getPort()));
			}
		}
		
		if (uc != null) {

			if (this.sseUplinkPortText != null && uc.getPort() >= 0) {
				this.sseUplinkPortText.setText(String.valueOf(uc.getPort()));
			}
			if (this.sseHostText != null && uc.getHost() != null) {
				this.sseHostText.setText(String.valueOf(uc.getHost()));
			}
		}
	}

	/**
	 * Restores the host and port settings to the mission's configuration file
	 * values, given a venue, downlink and uplink connection types, and a
	 * testbed name. Does nothing if the whole composite is not enabled.
	 * 
	 * @param vt
	 *            the current venue type
	 * @param ct
	 *            the current downlink connection type
	 * @param uct
	 *            the current uplink connection type
	 * @param tbName
	 *            the currently selected testbed name
	 */
	public void restoreHostDefaultsForVenueAndConnection(final VenueType vt,
			final TelemetryConnectionType ct, final UplinkConnectionType uct, final String tbName) {

		if (this.disabled) {
			return;
		}
        final ConnectionProperties hostProps = appContext.getBean(ConnectionProperties.class);
        
		if (this.sseHostText != null) {
			String host = hostProps.getDefaultDownlinkHost(vt, tbName, true);

			if (host == null) {
			    host = hostProps.getDefaultUplinkHost(vt, tbName, true);
			}
	
			SessionConfigShellUtil.safeSetText(this.sseHostText, host);
		}

		if (this.sseUplinkPortText != null) {
			this.sseUplinkPortText.setText(String.valueOf(
			        hostProps.getDefaultUplinkPort(vt, tbName, true)));
		}

		if (this.sseDownlinkPortText != null) {
			this.sseDownlinkPortText.setText(String.valueOf(
			        hostProps.getDefaultDownlinkPort(vt, tbName, true)));
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setFieldsFromData()
	 */
	@Override
	public void setFieldsFromData() {
		if (this.disabled) {
			return;
		}
		final SessionConfiguration tc = this.getSessionConfig();

		if (this.sseDictionaryDirText != null) {
			final String sseDictionaryDir = tc.getDictionaryConfig().getSseDictionaryDir() != null ? tc.getDictionaryConfig()
					.getSseDictionaryDir() : "";
			SessionConfigShellUtil.safeSetText(this.sseDictionaryDirText,
					sseDictionaryDir);
			checkAndSetSseVersionList(sseDictionaryDir);
		}

		if (this.sseVersionCombo != null) {
			SessionConfigShellUtil.safeSetText(this.sseVersionCombo,
					tc.getDictionaryConfig().getSseVersion());
		}

		if (this.sseLaunchDownlinkCombo != null) {
		    /*  Need to set the combo value to true or false string */
			this.sseLaunchDownlinkCombo.setText(String.valueOf(tc.getRunSse().isSseDownlinkEnabled() ? 
			        Boolean.TRUE.toString(): Boolean.FALSE.toString()));
		}
		
		/*
		 * Enable/disable fields as appropriate for FILE connection type.
		 */
		enableFieldsForFileConnection(tc.getConnectionConfiguration().getSseDownlinkConnection() instanceof IFileConnectionSupport);

		restoreNetworkSettings();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDataFromFields()
	 */
	@Override
	public void setDataFromFields() {
		if (this.disabled) {
			return;
		}
		final SessionConfiguration tc = this.getSessionConfig();
		

		final IDownlinkConnection dc = hostConfig.getSseDownlinkConnection();
		final IUplinkConnection uc = hostConfig.getSseUplinkConnection();
		
		// Assumes all fields validated before this. It is not
		// necessary to check the values.

		if (this.sseHostText != null && dc instanceof INetworkConnection) {
			final String sseDownlinkHost = this.sseHostText.getText().trim();
			((INetworkConnection)dc).setHost(sseDownlinkHost);
		}

		if (this.sseHostText != null && uc instanceof INetworkConnection) {
			final String sseUplinkHost = this.sseHostText.getText().trim();
			((INetworkConnection)uc).setHost(sseUplinkHost);
		}

		if (this.sseUplinkPortText != null) {
			final String port = this.sseUplinkPortText.getText().trim();
			final int sseUplinkPort = Integer.parseInt(port);
			uc.setPort(sseUplinkPort);
		}

		if (this.sseDownlinkPortText != null && dc instanceof INetworkConnection) {
			final String port = this.sseDownlinkPortText.getText().trim();
			final int sseDownlinkPort = Integer.parseInt(port);
			((INetworkConnection)dc).setPort(sseDownlinkPort);
		}

		if (this.sseDictionaryDirText != null) {
			final String text = this.sseDictionaryDirText.getText().trim();
			tc.getDictionaryConfig().setSseDictionaryDir(text);
		}

		if (this.sseVersionCombo != null) {
			final String text = this.sseVersionCombo.getText();
			tc.getDictionaryConfig().setSseVersion(text);
		}

		if (this.sseLaunchDownlinkCombo != null) {
			final String run = this.sseLaunchDownlinkCombo.getText();
			tc.getRunSse().setSseDownlinkEnabled(GDR.parse_boolean(run));
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDefaultFieldValues()
	 */
	@Override
	public void setDefaultFieldValues() {

		if (this.disabled) {
			return;
		}

		restoreNetworkSettings();

		if (this.sseDictionaryDirText != null) {
			SessionConfigShellUtil.safeSetText(this.sseDictionaryDirText,
					this.getSessionConfig().getDictionaryConfig().getSseDictionaryDir());
		}

		if (this.sseVersionCombo != null) {
			SessionConfigShellUtil.safeSetText(this.sseVersionCombo,
					this.getSessionConfig().getDictionaryConfig().getDefaultSseVersion());
		}

		if (this.sseLaunchDownlinkCombo != null) {
			this.sseLaunchDownlinkCombo.setText(Boolean.TRUE.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#validateInputFields()
	 */
	@Override
	public boolean validateInputFields() {
		
		if (this.disabled) {
			return true;
		}
		if (this.sseDictionaryDirText != null) {
			
			final String dictDir = this.sseDictionaryDirText.getText().trim();
			if (dictDir.isEmpty()) {
				SWTUtilities
						.showErrorDialog(this.getParent(),
								"Bad SSE Dictionary Directory",
								"You must enter a valid SSE Dictionary Directory to proceed");

				this.sseDictionaryDirBrowseButton.setFocus();
				return false;

			} else if ((new File(dictDir)).exists() == false) {
				
				SWTUtilities
						.showErrorDialog(this.getParent(),
								"Bad SSE Dictionary Directory",
								"The specified SSE dictionary directory does not exist.");

				this.sseDictionaryDirBrowseButton.setFocus();
				return false;
			}
		}

		if (this.sseVersionCombo != null
				&& this.sseVersionCombo.getText().trim().isEmpty()) {
			
			SWTUtilities
					.showErrorDialog(
							this.getParent(),
							"Bad SSE Version",
							"You must select a valid SSE version to proceed.  If no versions are available, you must first select a dictionary"
									+ " directory that contains usable SSE versions and then choose an SSE version.");

			this.sseVersionCombo.setFocus();
			return false;
		}

		if ((this.sseHostText != null) && this.sseHostText.getEnabled()) {
			
			final String value = SessionConfigShellUtil.getAndValidateHostText(
					this.sseHostText, "SSE downlink", this.getParent());
			if (value == null) {
				return false;
			}
		}

		if ((this.sseDownlinkPortText != null) && this.sseDownlinkPortText.getEnabled()) {
			
			final Integer port = SessionConfigShellUtil.getAndValidatePortText(
					this.sseDownlinkPortText, "SSE downlink", this.getParent());
			if (port == null) {
				return false;
			}
		}

		if ((this.sseUplinkPortText != null) && this.sseUplinkPortText.getEnabled()) {
			
			final Integer port = SessionConfigShellUtil.getAndValidatePortText(
					this.sseUplinkPortText, "SSE uplink", this.getParent());
			if (port == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates all the GUI components.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the SSE ExpandItem to
	 *
	 */
	private void createGui(final ExpandBar parentTabBar) {
		
		createSseGroup(parentTabBar);
		final Label sseDictionaryDirLabel = new Label(this.sseInfoGroup,
				SessionConfigShellUtil.LABEL_STYLE);
		FormData fd15 = null;

		this.sseDictionaryDirText = new Text(this.sseInfoGroup,
				SessionConfigShellUtil.LONG_TEXT_STYLE);
		this.sseDictionaryDirText.setEditable(false);
		fd15 = SWTUtilities.getFormData(this.sseDictionaryDirText, 3,
				SessionConfigShellUtil.LONG_FIELD_SIZE);

		fd15.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd15.top = new FormAttachment(0);
		this.sseDictionaryDirText.setLayoutData(fd15);
		sseDictionaryDirLabel.setText("Dict Dir:");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd16.right = new FormAttachment(this.sseDictionaryDirText);
		fd16.top = new FormAttachment(this.sseDictionaryDirText, 0, SWT.TOP);
		sseDictionaryDirLabel.setLayoutData(fd16);

		this.sseDictionaryDirBrowseButton = new Button(this.sseInfoGroup, SWT.PUSH);
		this.sseDictionaryDirBrowseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(this.sseDictionaryDirText, 5);
		bfd.top = new FormAttachment(this.sseDictionaryDirText, 0, SWT.TOP);
		bfd.right = new FormAttachment(100, -5);
		this.sseDictionaryDirBrowseButton.setLayoutData(bfd);
		this.sseDictionaryDirBrowseButton
				.addSelectionListener(new DictionaryBrowseButtonHandler());

		final Label sseVersionLabel = new Label(this.sseInfoGroup,
				SessionConfigShellUtil.LABEL_STYLE);
		this.sseVersionCombo = new Combo(this.sseInfoGroup,
				SessionConfigShellUtil.COMBO_STYLE);
		initSseVersions(null);

		final FormData fd2 = SWTUtilities.getFormData(this.sseVersionCombo, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd2.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd2.top = new FormAttachment(this.sseDictionaryDirText);
		this.sseVersionCombo.setLayoutData(fd2);
		sseVersionLabel.setText("Version:");
		final FormData fd3 = new FormData();
		fd3.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd3.right = new FormAttachment(this.sseVersionCombo);
		fd3.top = new FormAttachment(this.sseVersionCombo, 0, SWT.CENTER);
		sseVersionLabel.setLayoutData(fd3);

		final Label sseHostLabel = new Label(this.sseInfoGroup,
				SessionConfigShellUtil.LABEL_STYLE);
		sseHostLabel.setText("Host:");
		final FormData fd5 = new FormData();
		fd5.left = new FormAttachment(
				SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
		fd5.top = new FormAttachment(this.sseVersionCombo, 0, SWT.CENTER);
		sseHostLabel.setLayoutData(fd5);
		this.sseHostText = new Text(this.sseInfoGroup,
				SessionConfigShellUtil.SHORT_TEXT_STYLE);
		final FormData fd4 = SWTUtilities.getFormData(this.sseHostText, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd4.left = new FormAttachment(
				SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
		fd4.top = new FormAttachment(this.sseVersionCombo, 0, SWT.CENTER);
		this.sseHostText.setLayoutData(fd4);

		// These are fields enabled only if showing downlink configuration.
		if (this.showDownlink == true) {
			final Label launchSseDownlinkLabel = new Label(this.sseInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			this.sseLaunchDownlinkCombo = new Combo(this.sseInfoGroup,
					SessionConfigShellUtil.COMBO_STYLE);
			SessionConfigShellUtil.initBooleanCombo(this.sseLaunchDownlinkCombo);
			this.sseLaunchDownlinkCombo
					.addModifyListener(new LaunchDownlinkComboHandler());
			final FormData fd8 = SWTUtilities.getFormData(this.sseLaunchDownlinkCombo, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd8.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd8.top = new FormAttachment(this.sseVersionCombo);
			this.sseLaunchDownlinkCombo.setLayoutData(fd8);
			launchSseDownlinkLabel.setText("Use Downlink:");
			final FormData fd9 = new FormData();
			fd9.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd9.right = new FormAttachment(this.sseLaunchDownlinkCombo);
			fd9.top = new FormAttachment(this.sseLaunchDownlinkCombo, 0, SWT.CENTER);
			launchSseDownlinkLabel.setLayoutData(fd9);
			// Disable the combo for sse_chill_down
            this.sseLaunchDownlinkCombo.setEnabled(!appContext.getBean(SseContextFlag.class).isApplicationSse());

			final Label sseDownlinkPortLabel = new Label(this.sseInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			sseDownlinkPortLabel.setText("Downlink Port:");
			final FormData fd13 = new FormData();
			fd13.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd13.top = new FormAttachment(this.sseLaunchDownlinkCombo, 0, SWT.CENTER);
			sseDownlinkPortLabel.setLayoutData(fd13);
			this.sseDownlinkPortText = new Text(this.sseInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			final FormData fd12 = SWTUtilities.getFormData(this.sseDownlinkPortText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd12.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd12.top = new FormAttachment(this.sseLaunchDownlinkCombo, 0, SWT.CENTER);
			this.sseDownlinkPortText.setLayoutData(fd12);
		}

		// These are fields enabled only if showing uplink configuration.
		if (this.showUplink == true) {
			final Label sseUplinkPortLabel = new Label(this.sseInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			this.sseUplinkPortText = new Text(this.sseInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			final FormData fd6 = SWTUtilities.getFormData(this.sseUplinkPortText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd6.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd6.top = new FormAttachment(this.showDownlink ? this.sseLaunchDownlinkCombo
					: this.sseVersionCombo);
			this.sseUplinkPortText.setLayoutData(fd6);
			sseUplinkPortLabel.setText("Uplink Port:");
			final FormData fd7 = new FormData();
			fd7.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd7.right = new FormAttachment(this.sseUplinkPortText);
			fd7.top = new FormAttachment(this.sseUplinkPortText, 0, SWT.CENTER);
			sseUplinkPortLabel.setLayoutData(fd7);
		}

		// This enables all the GUI methods
		this.disabled = false;
	}

	/**
	 * Instantiates the main group (composite) for the SSE components. Also
	 * creates the parent ExpandItem and attaches it to the parent ExpandBar.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the SSE ExpandItem to
	 */
	private void createSseGroup(final ExpandBar parentTabBar) {
		if (this.sseInfoGroup == null) {
			this.sseInfoGroup = new Group(parentTabBar,
					SessionConfigShellUtil.GROUP_STYLE);

			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.getParent().getDisplay(), groupFontData);
			this.sseInfoGroup.setFont(groupFont);
			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;
			fl.marginBottom = 5;
			this.sseInfoGroup.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			this.sseInfoGroup.setLayoutData(fd1);

			this.expandItem = new ExpandItem(parentTabBar, SWT.NONE,
					parentTabBar.getItemCount());
			this.expandItem.setText("SSE Information");
			this.expandItem.setHeight(this.sseInfoGroup.computeSize(SWT.DEFAULT,
					SWT.DEFAULT).y);
			this.expandItem.setHeight(200);
			this.expandItem.setControl(this.sseInfoGroup);

			this.expandItem.setExpanded(true);
		}
	}

	/**
	 * Enables/disables downlink-specific GUI fields.
	 * 
	 * @param on
	 *            true to enable, false to disable
	 */
	private void enableDownlinkFields(final boolean on) {
		if (this.disabled) {
			return;
		}
		if (this.showDownlink) {
			this.sseDownlinkPortText.setEnabled(on);
		}
	}

	/**
	 * Check to see if any SSE dictionary versions are available. If not, posts
	 * an error dialog to the user. Otherwise, initializes the list of items in
	 * the SSE version combo box. The top item is then automatically selected.
	 * 
	 * @param dir
	 *            the SSE dictionary directory path
	 */
	private void checkAndSetSseVersionList(final String dir) {

		final List<String> sseNames = this.getSessionConfig().getDictionaryConfig().getAvailableSseVersions(dir);

		if (sseNames.isEmpty() && this.sseVersionCombo.getEnabled()) {
			SWTUtilities
					.showErrorDialog(this.getParent(), "Missing SSE Versions",
							"There are no SSE versions available in the specified dictionary directory.");
			this.sseVersionCombo.setItems(new String[] {});
		} else {
			initSseVersions(dir);
			this.sseVersionCombo.select(0);
		}
	}

	/**
	 * Retrieves the available list of SSE dictionary versions from the current
	 * SSE dictionary directory and sets them into the SSE version combo box. If
	 * the input parameter is null, uses the dictionary path in the current
	 * session configuration object.
	 * 
	 * @param dir
	 *            the SSE dictionary directory path
	 */
	private void initSseVersions(final String dir) {
		final List<String> names = dir == null ? this.getSessionConfig().getDictionaryConfig()
				.getAvailableSseVersions() : this.getSessionConfig().getDictionaryConfig()
				.getAvailableSseVersions(dir);
		String[] items = names.toArray(new String[] {});
		items = SessionConfigShellUtil.reverseSort(items);
		this.sseVersionCombo.removeAll();
		this.sseVersionCombo.setItems(items);
	}

	/**
	 * Event handler class for the dictionary browse button.
	 * 
	 *
	 */
	private class DictionaryBrowseButtonHandler implements SelectionListener {

		private final SWTUtilities util = new SWTUtilities();

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(final SelectionEvent event) {
			// Empty method for interface only
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent event) {
			try {
				final String dirName = this.util.displayStickyFileChooser(true, SessionConfigSseComposite.this.getParent(),
						"sseDictionaryDirBrowseButton",
						SessionConfigSseComposite.this.sseDictionaryDirText.getText());
				if (dirName != null) {
					SessionConfigShellUtil.safeSetText(SessionConfigSseComposite.this.sseDictionaryDirText,
							dirName);
					checkAndSetSseVersionList(dirName);
				}
			} catch (final Exception e) {
				trace.error("Error in SessionConfigSseComposite.DictionaryBrowseButtonHandler.widgetSelected handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}
	}

	/**
	 * This is the event handler invoked when the content of the
	 * "launch SSE downlink" combo is changed.
	 * 
	 *
	 */
	private class LaunchDownlinkComboHandler implements ModifyListener {

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(final ModifyEvent event) {
			try {
				if (SessionConfigSseComposite.this.sseLaunchDownlinkCombo.getText().isEmpty()) {
					return;
				}
				// Enable downlink fields if the combo value is
				// set to "true", or disable them if not
				final boolean enable = GDR.parse_boolean(SessionConfigSseComposite.this.sseLaunchDownlinkCombo
						.getText());
				enableDownlinkFields(enable);
			} catch (final Exception e) {
				trace.error("Error in SessionConfigSseComposite.LaunchDownlinkComboHandler.modifyText handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}
	}
}
