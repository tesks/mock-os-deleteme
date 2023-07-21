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
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class implements the "FSW" panel in the session configuration GUI
 * window. It is a wrapper around an SWT ExpandItem that in turn contains the
 * composite with all FSW-related GUI fields on it. Constructing this object is
 * insufficient for GUI usage. It must also be enabled.
 * 
 * When enable(true, parentBar) is called on this class, the ExpandItem will be
 * created and will be added to the parent ExpandBar. GUI-related methods will
 * then operate. If enable(false, null) is called, the ExpandItem and all
 * sub-components will be disposed, and all GUI-related methods will do nothing.
 * This allow the session configuration window parent to invoke methods on this
 * class without having to constantly check whether FSW GUI fields are enabled
 * or not.
 * 
 * This composite is enabled on the parent session configuration window in all
 * configurations except a standalone flight software chill_down configuration.
 * 
 *
 */
public class SessionConfigFswComposite extends AbstractSessionConfigPanel
		implements ISessionConfigPanel {

	private static final String VCID_ALL = "[All]";
	private final Tracer trace;

	
	// GUI controls
	private Group fswInfoGroup;
	private Text fswDictionaryDirText;
	private Button fswDictionaryDirBrowseButton;
	private Combo fswVersionCombo;
	private Text fswDownlinkHostText;
	private Text fswUplinkHostText;
	private Text fswUplinkPortText;
	private Combo fswLaunchDownlinkCombo;
	private Text fswDownlinkPortText;
	private Combo fswVcidStringIdCombo;

	// Other members
	private final boolean showUplink;
	private final boolean showDownlink;
	private boolean disabled;
	private final IConnectionMap hostConfig;

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
	public SessionConfigFswComposite(final ApplicationContext appContext,
			final SessionConfigShell parentWindow,
			final SessionConfiguration session, final boolean downlink,
			final boolean uplink) {

		super(appContext, parentWindow, session);
        trace = TraceManager.getDefaultTracer(appContext);
		this.disabled = true;
		this.showDownlink = downlink;
		this.showUplink = uplink;
		this.hostConfig = session.getConnectionConfiguration();
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
			this.fswInfoGroup.dispose();
			this.getExpandItem().dispose();
			this.fswInfoGroup = null;
			this.setExpandItem(null);
		}

		this.disabled = !on;
	}

	/**
	 * Enables or disables GUI fields related to FSW-downlink. For this
	 * composite, this affects the downlink port, host, and VCID fields. Note
	 * that if the launch downlink combo is set to false, the fields will remain
	 * disabled regardless of the value of the input parameter. Does nothing if
	 * the whole composite is not enabled.
	 * 
	 * @param on
	 *            true to enabled fields, false to disable
	 */
	public void enableDownlinkFields(final boolean on) {

		if (this.disabled) {
			return;
		}

		if (this.showDownlink) {

			final boolean launchDownlink = Boolean
					.valueOf(this.fswLaunchDownlinkCombo.getText());
			this.fswDownlinkHostText.setEnabled(on && launchDownlink);
			this.fswDownlinkPortText.setEnabled(on && launchDownlink);
			this.fswVcidStringIdCombo.setEnabled(on && launchDownlink);
		}
	}
	
	/**
	 * Enables/disables fields to account for a change in the downlink
	 * connection type to FILE. For this composite, this means that the downlink
	 * port gets enabled if the connection type is NOT file and the launch
	 * downlink flag is enabled.
	 * 
	 * Does nothing if the whole composite is not enabled.
	 * 
	 * @param isFile
	 *            true if a file connection has been selected, false if not
	 */
	public void enableFieldsForFileConnection(final boolean isFile) {

		if (this.disabled) {
			return;
		}

		final boolean launchDownlink = Boolean
				.valueOf(this.fswLaunchDownlinkCombo.getText());
		enableDownlinkPort(!isFile && launchDownlink);
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

		if (this.fswDownlinkPortText != null) {
			final boolean launchDownlink = Boolean
					.valueOf(this.fswLaunchDownlinkCombo.getText());
			this.fswDownlinkPortText.setEnabled(on && launchDownlink);
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
		if (this.fswUplinkPortText != null) {
			this.fswUplinkPortText.setEnabled(on);
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

		final IDownlinkConnection dc = hostConfig.getFswDownlinkConnection();
		final IUplinkConnection uc = hostConfig.getFswUplinkConnection();
		
		if (dc != null && dc instanceof INetworkConnection) {

			if (this.fswDownlinkHostText != null
					&& ((INetworkConnection)dc).getHost() != null) {
				SessionConfigShellUtil.safeSetText(this.fswDownlinkHostText,
						((INetworkConnection)dc).getHost());
			}
			if (this.fswDownlinkPortText != null
					&& ((INetworkConnection)dc).getPort() >= 0) {
				this.fswDownlinkPortText.setText(String.valueOf(((INetworkConnection)dc).getPort()));
			}
		}

		if (uc != null) {
			if (this.fswUplinkHostText != null
					&& ((INetworkConnection)uc).getHost() != null) {
				SessionConfigShellUtil.safeSetText(this.fswUplinkHostText,
						((INetworkConnection)uc).getHost());
			}


			if (this.fswUplinkPortText != null && uc.getPort() >= 0) {
				this.fswUplinkPortText.setText(String.valueOf(uc.getPort()));
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
	 * @param streamId
	 *            the selected flight downlink stream
	 */
	public void restoreHostDefaultsForVenueAndConnection(final VenueType vt,
			final TelemetryConnectionType ct, final UplinkConnectionType uct,
			final String tbName, final DownlinkStreamType streamId) {

		if (this.disabled) {
			return;
		}
		
		final ConnectionProperties hostProps = appContext.getBean(ConnectionProperties.class);
		

		if (ct != null && this.fswDownlinkHostText != null) {
			SessionConfigShellUtil.safeSetText(this.fswDownlinkHostText,
					hostProps.getDefaultDownlinkHost(vt,
							tbName, false));
		}

		if (ct != null && this.fswDownlinkPortText != null) {
			this.fswDownlinkPortText.setText(String.valueOf(
			        hostProps.getDefaultDownlinkPort(vt, tbName, streamId)));
		}

		if (uct != null && this.fswUplinkHostText != null) {
			SessionConfigShellUtil.safeSetText(this.fswUplinkHostText,
			        hostProps.getDefaultUplinkHost(vt, tbName, false));
		}

		if (uct != null && this.fswUplinkPortText != null) {
			this.fswUplinkPortText.setText(String.valueOf(
			        hostProps.getDefaultUplinkPort(vt, tbName, false)));
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

		if (this.fswDictionaryDirText != null) {
			final String fswDictionaryDir = tc.getDictionaryConfig().getFswDictionaryDir() != null ? tc.getDictionaryConfig().
					getFswDictionaryDir() : "";
			SessionConfigShellUtil.safeSetText(this.fswDictionaryDirText,
					fswDictionaryDir);
			checkAndSetFswVersionList(fswDictionaryDir);
		}

		if (this.fswVersionCombo != null) {
			SessionConfigShellUtil.safeSetText(this.fswVersionCombo,
					tc.getDictionaryConfig().getFswVersion());
		}

		if (this.fswLaunchDownlinkCombo != null) {
			final String text = tc.getRunFsw().isFswDownlinkEnabled() ? Boolean.TRUE.toString()
					: Boolean.FALSE.toString();
			this.fswLaunchDownlinkCombo.setText(text);
		}

		if (this.fswVcidStringIdCombo != null
				&& !this.fswVcidStringIdCombo.isDisposed()) {
			final Integer vcid = tc.getFilterInformation().getVcid();
			if (vcid != null) {
				final String vcidOption = vcid.toString() + " - "
						+ missionProps.mapDownlinkVcidToName(vcid);
				SessionConfigShellUtil.safeSetText(this.fswVcidStringIdCombo,
						vcidOption);
			} else {
				this.fswVcidStringIdCombo.setText(VCID_ALL);
			}
		}

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
		
		final IDownlinkConnection dc = hostConfig.getFswDownlinkConnection();
		final IUplinkConnection uc = hostConfig.getFswUplinkConnection();	

		// Assumes all fields validated before this.

		if (this.fswDownlinkHostText != null && dc instanceof INetworkConnection) {
			final String fswDownlinkHost = this.fswDownlinkHostText.getText()
					.trim();
			((INetworkConnection)dc).setHost(fswDownlinkHost);
		}

		if (this.fswUplinkHostText != null) {
			final String fswUplinkHost = this.fswUplinkHostText.getText()
					.trim();
			uc.setHost(fswUplinkHost);
		}

		if (this.fswUplinkPortText != null) {
			final String port = this.fswUplinkPortText.getText().trim();
			final int fswUplinkPort = Integer.parseInt(port);
		    uc.setPort(fswUplinkPort);
		}

		if (this.fswDownlinkPortText != null  && dc instanceof INetworkConnection) {
			final String port = this.fswDownlinkPortText.getText().trim();
			final int fswDownlinkPort = Integer.parseInt(port);
			((INetworkConnection)dc).setPort(fswDownlinkPort);
		}

		if (this.fswDictionaryDirText != null) {
			final String text = this.fswDictionaryDirText.getText().trim();
			tc.getDictionaryConfig().setFswDictionaryDir(text);
		}

		if (this.fswVersionCombo != null) {
			final String text = this.fswVersionCombo.getText();
			tc.getDictionaryConfig().setFswVersion(text);
		}

		if (this.fswLaunchDownlinkCombo != null) {
			final String run = this.fswLaunchDownlinkCombo.getText();
			tc.getRunFsw().setFswDownlinkEnabled(GDR.parse_boolean(run));
		}

		if (this.fswVcidStringIdCombo != null) {
			final String text = this.fswVcidStringIdCombo.getText().trim();

			if (!text.equals(VCID_ALL)) {
				final int endIndex = text.indexOf(' ');
				tc.getFilterInformation().setVcid(Integer.valueOf(text.substring(0, endIndex)));
			} else {
				tc.getFilterInformation().setVcid(null);
			}
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

		if (this.fswDictionaryDirText != null) {
			SessionConfigShellUtil.safeSetText(this.fswDictionaryDirText,
					(new File(this.getSessionConfig().getDictionaryConfig().getFswDictionaryDir()))
							.getAbsolutePath());
		}

		if (this.fswVersionCombo != null) {
			SessionConfigShellUtil.safeSetText(this.fswVersionCombo,
					this.getSessionConfig().getDictionaryConfig().getDefaultFswVersion());
		}

		if (this.fswLaunchDownlinkCombo != null) {
			String value = Boolean.TRUE.toString();
            if (getSessionConfig().getSseContextFlag()) {
				value = Boolean.FALSE.toString();
			}
			this.fswLaunchDownlinkCombo.setText(value);
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
		if (this.fswDictionaryDirText != null
				&& this.fswDictionaryDirText.isEnabled()) {
			final String dictDir = this.fswDictionaryDirText.getText().trim();
			if (dictDir.isEmpty()) {
				SWTUtilities
						.showErrorDialog(this.getParent(),
								"Bad FSW Dictionary Directory",
								"You must enter a valid FSW Dictionary Directory to proceed");

				if (this.fswDictionaryDirBrowseButton != null) {
					this.fswDictionaryDirBrowseButton.setFocus();
				}

				return (false);
			} else if ((new File(dictDir)).exists() == false) {
				SWTUtilities
						.showErrorDialog(this.getParent(),
								"Bad FSW Dictionary Directory",
								"The specified FSW dictionary directory does not exist.");

				if (this.fswDictionaryDirBrowseButton != null) {
					this.fswDictionaryDirBrowseButton.setFocus();
				}

				return (false);
			}
		}

		if (this.fswVersionCombo != null
				&& this.fswVersionCombo.getText().isEmpty()
				&& this.fswVersionCombo.getEnabled()) {
			SWTUtilities
					.showErrorDialog(
							this.getParent(),
							"Bad FSW Version",
							"You must select a valid FSW version to proceed.  If no versions are available, you must first select a dictionary"
									+ " directory that contains usable FSW versions and then choose an FSW version.");

			this.fswVersionCombo.setFocus();

			return (false);
		}

		final IDownlinkConnection dc = hostConfig.getFswDownlinkConnection();
		final IUplinkConnection uc = hostConfig.getFswUplinkConnection();
		
		if ((this.fswDownlinkHostText != null)
				&& this.fswDownlinkHostText.getEnabled() && dc instanceof INetworkConnection) {
			final String value = SessionConfigShellUtil.getAndValidateHostText(
					this.fswDownlinkHostText, "FSW downlink", this.getParent());
			if (value == null) {
				return false;
			}

			((INetworkConnection)dc).setHost(value);
		}

		if ((this.fswUplinkHostText != null)
				&& this.fswUplinkHostText.getEnabled()) {
			final String value = SessionConfigShellUtil.getAndValidateHostText(
					this.fswUplinkHostText, "FSW uplink", this.getParent());
			if (value == null) {
				return false;
			}

			((INetworkConnection)uc).setHost(value);
		}

		if ((this.fswDownlinkPortText != null)
				&& this.fswDownlinkPortText.getEnabled() && dc instanceof INetworkConnection) {
			final Integer port = SessionConfigShellUtil.getAndValidatePortText(
					this.fswDownlinkPortText, "FSW downlink", this.getParent());
			if (port == null) {
				return false;
			}

			((INetworkConnection)dc).setPort(port);
		}

		if ((this.fswUplinkPortText != null)
				&& this.fswUplinkPortText.getEnabled()) {
			final Integer port = SessionConfigShellUtil.getAndValidatePortText(
					this.fswUplinkPortText, "FSW uplink", this.getParent());
			if (port == null) {
				return false;
			}

			((INetworkConnection)uc).setPort(port);
		}
		return true;
	}

	/**
	 * Instantiates the main group (composite) for the FSW components. Also
	 * creates the parent ExpandItem and attaches it to the parent ExpandBar.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the FSW ExpandItem to
	 */
	private void createFswGroup(final ExpandBar parentTabBar) {

		if (this.fswInfoGroup == null) {
			this.fswInfoGroup = new Group(parentTabBar,
					SessionConfigShellUtil.GROUP_STYLE);
			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.getParent().getDisplay(),
					groupFontData);
			this.fswInfoGroup.setFont(groupFont);
			final FormLayout fl = new FormLayout();
			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;
			fl.marginBottom = 5;
			this.fswInfoGroup.setLayout(fl);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			this.fswInfoGroup.setLayoutData(fd1);

			this.setExpandItem(new ExpandItem(parentTabBar, SWT.NONE,
					parentTabBar.getItemCount()));
			this.getExpandItem().setText("FSW Information");
			this.getExpandItem().setHeight(this.fswInfoGroup.computeSize(
					SWT.DEFAULT, SWT.DEFAULT).y);
			this.getExpandItem().setHeight(200);
			this.getExpandItem().setControl(this.fswInfoGroup);

			this.getExpandItem().setExpanded(true);
		}
	}

	/**
	 * Creates all the GUI components.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the FSW ExpandItem to
	 *
	 */
	private void createGui(final ExpandBar parentTabBar) {

		createFswGroup(parentTabBar);
		
		final Label fswDictionaryDirLabel = new Label(this.fswInfoGroup,
				SessionConfigShellUtil.LABEL_STYLE);
		FormData fd15 = null;

		this.fswDictionaryDirText = new Text(this.fswInfoGroup,
				SessionConfigShellUtil.LONG_TEXT_STYLE);
		this.fswDictionaryDirText.setEditable(false);
		fd15 = SWTUtilities.getFormData(this.fswDictionaryDirText, 3,
				SessionConfigShellUtil.LONG_FIELD_SIZE);

		fd15.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd15.top = new FormAttachment(0);
		this.fswDictionaryDirText.setLayoutData(fd15);
		fswDictionaryDirLabel.setText("Dict Dir:");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd16.right = new FormAttachment(this.fswDictionaryDirText);
		fd16.top = new FormAttachment(this.fswDictionaryDirText, 0, SWT.TOP);
		fswDictionaryDirLabel.setLayoutData(fd16);

		this.fswDictionaryDirBrowseButton = new Button(this.fswInfoGroup,
				SWT.PUSH);
		this.fswDictionaryDirBrowseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(this.fswDictionaryDirText, 5);
		bfd.top = new FormAttachment(this.fswDictionaryDirText, 0, SWT.TOP);
		bfd.right = new FormAttachment(100, -5);
		this.fswDictionaryDirBrowseButton.setLayoutData(bfd);
		this.fswDictionaryDirBrowseButton
				.addSelectionListener(new DictionaryBrowseButtonHandler());

		final Label fswVersionLabel = new Label(this.fswInfoGroup,
				SessionConfigShellUtil.LABEL_STYLE);

		this.fswVersionCombo = new Combo(this.fswInfoGroup,
				SessionConfigShellUtil.COMBO_STYLE);
		initFswVersions(null);

		final FormData fd2 = SWTUtilities.getFormData(this.fswVersionCombo, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd2.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd2.top = new FormAttachment(this.fswDictionaryDirText);
		this.fswVersionCombo.setLayoutData(fd2);
		fswVersionLabel.setText("Version:");
		final FormData fd3 = new FormData();
		fd3.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd3.right = new FormAttachment(this.fswVersionCombo);
		fd3.top = new FormAttachment(this.fswVersionCombo, 0, SWT.CENTER);
		fswVersionLabel.setLayoutData(fd3);

		// These are fields enabled only if showing downlink configuration.
		if (this.showDownlink) {
			final Label fswDownlinkHostLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			fswDownlinkHostLabel.setText("Downlink Host:");
			final FormData fd5 = new FormData();
			fd5.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd5.top = new FormAttachment(this.fswVersionCombo, 0, SWT.CENTER);
			fswDownlinkHostLabel.setLayoutData(fd5);

			this.fswDownlinkHostText = new Text(this.fswInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);

			final FormData fd4 = SWTUtilities.getFormData(
					this.fswDownlinkHostText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd4.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd4.top = new FormAttachment(this.fswVersionCombo, 0, SWT.CENTER);
			this.fswDownlinkHostText.setLayoutData(fd4);

			final Label launchFswDownlinkLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);

			this.fswLaunchDownlinkCombo = new Combo(this.fswInfoGroup,
					SessionConfigShellUtil.COMBO_STYLE);
			SessionConfigShellUtil
					.initBooleanCombo(this.fswLaunchDownlinkCombo);
			this.fswLaunchDownlinkCombo
					.addModifyListener(new LaunchDownlinkComboHandler());
			// Disable the launch combo for standalone chill_down
            if (this.getConfigShell().isStandaloneChillDown()) {
            	this.fswLaunchDownlinkCombo.setEnabled(false);
            }
			final FormData fd8 = SWTUtilities.getFormData(
					this.fswLaunchDownlinkCombo, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd8.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd8.top = new FormAttachment(this.fswVersionCombo);
			this.fswLaunchDownlinkCombo.setLayoutData(fd8);
			launchFswDownlinkLabel.setText("Use Downlink:");
			final FormData fd9 = new FormData();
			fd9.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd9.right = new FormAttachment(this.fswLaunchDownlinkCombo);
			fd9.top = new FormAttachment(this.fswLaunchDownlinkCombo, 0,
					SWT.CENTER);
			launchFswDownlinkLabel.setLayoutData(fd9);

			final Label fswDownlinkPortLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);
			fswDownlinkPortLabel.setText("Downlink Port:");
			final FormData fd13 = new FormData();
			fd13.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd13.top = new FormAttachment(this.fswLaunchDownlinkCombo, 0,
					SWT.CENTER);
			fswDownlinkPortLabel.setLayoutData(fd13);

			this.fswDownlinkPortText = new Text(this.fswInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);

			final FormData fd12 = SWTUtilities.getFormData(
					this.fswDownlinkPortText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd12.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd12.top = new FormAttachment(this.fswLaunchDownlinkCombo, 0,
					SWT.CENTER);
			this.fswDownlinkPortText.setLayoutData(fd12);

			final Label fswVcidStringIdLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);

			this.fswVcidStringIdCombo = new Combo(this.fswInfoGroup,
					SessionConfigShellUtil.COMBO_STYLE);

			final FormData fd14 = SWTUtilities.getFormData(
					this.fswVcidStringIdCombo, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd14.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd14.top = new FormAttachment(this.fswDownlinkPortText);
			this.fswVcidStringIdCombo.setLayoutData(fd14);
			fswVcidStringIdLabel.setText("Virtual Channel ID:");
			final FormData fd17 = new FormData();
			fd17.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd17.right = new FormAttachment(this.fswVcidStringIdCombo);
			fd17.top = new FormAttachment(this.fswVcidStringIdCombo, 0,
					SWT.CENTER);
			fswVcidStringIdLabel.setLayoutData(fd17);

			initVirtualChannelIds();
		}

		// These are fields enabled only if showing uplink configuration.
		if (this.showUplink) {
			final Label fswUplinkHostLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);

			this.fswUplinkHostText = new Text(this.fswInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);

			final FormData fd61 = SWTUtilities.getFormData(
					this.fswUplinkHostText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd61.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd61.top = new FormAttachment(
					this.showDownlink ? this.fswLaunchDownlinkCombo
							: this.fswVersionCombo);
			this.fswUplinkHostText.setLayoutData(fd61);
			fswUplinkHostLabel.setText("Uplink Host:");
			final FormData fd71 = new FormData();
			fd71.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd71.right = new FormAttachment(this.fswUplinkHostText);
			fd71.top = new FormAttachment(this.fswUplinkHostText, 0, SWT.CENTER);
			fswUplinkHostLabel.setLayoutData(fd71);

			final Label fswUplinkPortLabel = new Label(this.fswInfoGroup,
					SessionConfigShellUtil.LABEL_STYLE);

			this.fswUplinkPortText = new Text(this.fswInfoGroup,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);

			final FormData fd6 = SWTUtilities.getFormData(
					this.fswUplinkPortText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd6.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd6.top = new FormAttachment(this.fswUplinkHostText);
			this.fswUplinkPortText.setLayoutData(fd6);
			fswUplinkPortLabel.setText("Uplink Port:");
			final FormData fd7 = new FormData();
			fd7.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd7.right = new FormAttachment(this.fswUplinkPortText);
			fd7.top = new FormAttachment(this.fswUplinkPortText, 0, SWT.CENTER);
			fswUplinkPortLabel.setLayoutData(fd7);
		}

		// This enables all the GUI methods
		this.disabled = false;
	}

	/**
	 * Initialized the VCID combo box with valid values for the current mission.
	 */
	private void initVirtualChannelIds() {

		final List<Integer> ids = missionProps.getAllDownlinkVcids();

		if (this.fswVcidStringIdCombo != null) {
			this.fswVcidStringIdCombo.add(VCID_ALL);

			if (ids != null) {
				for (final Integer id : ids) {
					this.fswVcidStringIdCombo.add(id.toString() + " - "
							+ missionProps.mapDownlinkVcidToName(id));
				}
			}
		}
	}

	/**
	 * Check to see if any FSW dictionary versions are available. If not, posts
	 * an error dialog to the user. Otherwise, initializes the list of items in
	 * the FSW version combo box. The top item is then automatically selected.
	 * 
	 * @param dir
	 *            the FSW dictionary directory path
	 */
	private void checkAndSetFswVersionList(final String dir) {

		final List<String> fswNames = this.getSessionConfig().getDictionaryConfig()
				.getAvailableFswVersions(dir);

		if (fswNames.isEmpty() && this.fswVersionCombo.getEnabled() == true) {
			SWTUtilities
					.showErrorDialog(this.getParent(), "Missing FSW Versions",
							"There are no FSW versions available in the specified dictionary directory.");
			this.fswVersionCombo.setItems(new String[] {});
		} else {
			initFswVersions(dir);
			this.fswVersionCombo.select(0);
		}
	}

	/**
	 * Retrieves the available list of FSW dictionary versions from the given
	 * FSW dictionary directory and sets them into the FSW version combo box. If
	 * the input parameter is null, uses the dictionary path in the current
	 * session configuration object.
	 * 
	 * @param dir
	 *            the FSW dictionary directory path
	 */
	private void initFswVersions(final String dir) {

		final List<String> names = dir == null ? this.getSessionConfig().getDictionaryConfig()
				.getAvailableFswVersions() : this.getSessionConfig().getDictionaryConfig()
				.getAvailableFswVersions(dir);
		String[] items = names.toArray(new String[] {});
		items = SessionConfigShellUtil.reverseSort(items);
		this.fswVersionCombo.removeAll();
		this.fswVersionCombo.setItems(items);
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
				final String dirName = this.util.displayStickyFileChooser(true,
						SessionConfigFswComposite.this.getParent(),
						"fswDictionaryDirBrowseButton",
						SessionConfigFswComposite.this.fswDictionaryDirText
								.getText());
				if (dirName != null) {
					SessionConfigShellUtil
							.safeSetText(
									SessionConfigFswComposite.this.fswDictionaryDirText,
									dirName);
					checkAndSetFswVersionList(dirName);
				}
			} catch (final Exception e) {
				trace.error("Error in SessionConfigFswComposite.DictionaryBrowseButtonHandler.widgetSelected handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}
	}

	/**
	 * This is the event handler invoked when the content of the
	 * "launch FSW downlink" combo is changed.
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
				if (SessionConfigFswComposite.this.fswLaunchDownlinkCombo
						.getText().isEmpty()) {
					return;
				}
				// Enable downlink fields if the combo value is
				// set to "true", or disable them if not.
				final boolean enable = GDR
						.parse_boolean(SessionConfigFswComposite.this.fswLaunchDownlinkCombo
								.getText());
				enableDownlinkFields(enable);

				// This is an unfortunate interaction between
				// GUI components, but if FSW downlink is now
				// enabled, we have to enable the FSW fields on
				// the venue panel. Rather than giving this class
				// knowledge of the venue panel class, we go through 
				// the parent window
				SessionConfigFswComposite.this.getConfigShell()
						.enableFlightDownlinkFields(enable);

			} catch (final Exception e) {
				trace.error("Error in SessionConfigFswComposite.LaunchDownlinkComboHandler.modifyText handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}
	}
}
