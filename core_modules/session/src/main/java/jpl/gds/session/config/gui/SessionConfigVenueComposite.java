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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.util.HostPortUtility;

/**
 * This class implements the "Venue" panel in the session configuration GUI
 * window. It is a wrapper around an SWT ExpandItem that in turn contains the
 * composite with all venue-related GUI fields on it.
 * 
 * This composite is the only GUI sub-panel within the session configuration GUI
 * that is aware of the other sub-panels. As such, it will not operate properly
 * if it has not been supplied with the FSW, SSE, and message service/DB composite objects
 * by the parent session configuration window.
 * 
 * There are static and dynamic fields in this panel. Static fields are
 * established by the createGui() method and are guaranteed to remain on the
 * panel for its lifetime. Dynamic GUI fields come and go as the venue and
 * connection types change.
 * 
 *
 */
public class SessionConfigVenueComposite extends AbstractSessionConfigPanel
		implements ISessionConfigPanel {
	private final Tracer trace; 


	/**
	 * Display string for selecting all stations.
	 */
	private static final String STATION_ALL = "[All]";

	// Static GUI controls
	private Group venueInfoGroup;
	private Combo venueTypeCombo;
	private Combo fswInputTypeCombo;
	private Text userText;
	private Text hostText;
	private Composite venueCompositeTop;
	private Combo downlinkConnectionTypeCombo;
	private Combo uplinkConnectionTypeCombo;

	// Dynamic GUI controls
	private Composite venueCompositeBottom;
	private Combo downlinkStreamIdCombo;
	private Combo testbedNameCombo;
	private Text inputFileText;
	private Button inputFileBrowseButton;
	private Button pvlCreateButton;
	private Button pvlEditButton;
	private Text databaseSessionKeyText;
	private Text databaseSessionHostText;
	private Combo stationIdCombo;
	private Label stationIdLabel;

	// Other members
	private final boolean showUplink;
	private final boolean showDownlink;
	private SessionConfigFswComposite fswComposite;
	private SessionConfigSseComposite sseComposite;
	private SessionConfigMessageServiceDbComposite jmsDbComposite;
	private final ModifyEventHandler modifyHandler = new ModifyEventHandler();
	private ExpandBar parentBar;
	private final Set<String> downlinkStreamIdComboSet = new TreeSet<String>();
	private final Set<String> testbedNameComboSet = new TreeSet<String>();
	private final Set<String> inputTypeComboSet = new TreeSet<String>();

	/**
	 * Constructor. Note that this will not complete the GUI creation or enable
	 * GUI operations. To complete initialization, call createGui(),
	 * setFswPanel(), setSsePanel(), and setMessageServiceDbPanel().
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
	public SessionConfigVenueComposite(final ApplicationContext appContext,
			final SessionConfigShell parentWindow,
			final SessionConfiguration session, final boolean downlink,
			final boolean uplink) {

		super(appContext, parentWindow, session);
        trace = TraceManager.getDefaultTracer(appContext);
		this.showDownlink = downlink;
		this.showUplink = uplink;
	}

	/**
	 * Sets the FSW composite object, which allows this object to notify the FSW
	 * panel of changes in this venue panel.
	 * 
	 * @param panel
	 *            the SessionConfigFswComposite from the parent session
	 *            configuration window
	 */
	public void setFswPanel(final SessionConfigFswComposite panel) {

		this.fswComposite = panel;
	}

	/**
	 * Sets the SSE composite object, which allows this object to notify the SSE
	 * panel of changes in this venue panel.
	 * 
	 * @param panel
	 *            the SessionConfigSseComposite from the parent session
	 *            configuration window
	 */
	public void setSsePanel(final SessionConfigSseComposite panel) {

		this.sseComposite = panel;
	}

	/**
	 * Sets the message service/DB composite object, which allows this object to notify the
	 * message service/DB panel of changes in this venue panel.
	 * 
	 * @param panel
	 *            the SessionConfigJmsDbComposite from the parent session
	 *            configuration window
	 */
	public void setMessageServiceDbPanel(final SessionConfigMessageServiceDbComposite panel) {

		this.jmsDbComposite = panel;
	}

	/**
	 * Creates all the GUI components.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the venue ExpandItem to
	 *            		/*
	 */
	public void createGui(final ExpandBar parentTabBar) {

		this.parentBar = parentTabBar;
		createVenueGroup(parentTabBar);
		this.venueCompositeTop = new Composite(this.venueInfoGroup, SWT.NONE);
		final FormLayout fl2 = new FormLayout();
		fl2.spacing = 10;
		fl2.marginTop = 0;
		fl2.marginBottom = 0;
		fl2.marginLeft = 0;
		fl2.marginRight = 0;
		this.venueCompositeTop.setLayout(fl2);
		final FormData fd6 = new FormData();
		fd6.left = new FormAttachment(0);
		fd6.right = new FormAttachment(100);
		fd6.top = new FormAttachment(0);
		this.venueCompositeTop.setLayoutData(fd6);

		final Label userLabel = new Label(this.venueCompositeTop,
				SessionConfigShellUtil.LABEL_STYLE);
		this.userText = new Text(this.venueCompositeTop,
				SessionConfigShellUtil.SHORT_TEXT_STYLE);

		final FormData fd21 = SWTUtilities.getFormData(this.userText, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd21.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd21.top = new FormAttachment(0);
		this.userText.setLayoutData(fd21);
		userLabel.setText("User:");
		final FormData fd22 = new FormData();
		fd22.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd22.right = new FormAttachment(this.userText);
		fd22.top = new FormAttachment(this.userText, 0, SWT.CENTER);
		userLabel.setLayoutData(fd22);

		final Label venueTypeLabel = new Label(this.venueCompositeTop,
				SessionConfigShellUtil.LABEL_STYLE);
		venueTypeLabel.setText("Venue:");
		final FormData fd3 = new FormData();
		fd3.left = new FormAttachment(
				SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
		fd3.top = new FormAttachment(this.userText, 0, SWT.CENTER);
		venueTypeLabel.setLayoutData(fd3);
		this.venueTypeCombo = new Combo(this.venueCompositeTop,
				SessionConfigShellUtil.COMBO_STYLE);
		initVenueTypes();
		this.venueTypeCombo.addModifyListener(this.modifyHandler);

		final FormData fd2 = SWTUtilities.getFormData(this.venueTypeCombo, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd2.left = new FormAttachment(
				SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
		fd2.top = new FormAttachment(0);
		fd2.right = new FormAttachment(100);
		this.venueTypeCombo.setLayoutData(fd2);

		final Label hostLabel = new Label(this.venueCompositeTop,
				SessionConfigShellUtil.LABEL_STYLE);

		this.hostText = new Text(this.venueCompositeTop,
				SessionConfigShellUtil.SHORT_TEXT_STYLE);
		this.hostText.setEditable(false);
		this.hostText.setEnabled(false);

		final FormData fd23 = SWTUtilities.getFormData(this.hostText, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);
		fd23.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
		fd23.top = new FormAttachment(this.userText);
		this.hostText.setLayoutData(fd23);
		hostLabel.setText("Host:");
		final FormData fd24 = new FormData();
		fd24.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd24.right = new FormAttachment(this.hostText);
		fd24.top = new FormAttachment(this.hostText, 0, SWT.CENTER);
		hostLabel.setLayoutData(fd24);

		createDownlinkConnectionTypeFields();
		createUplinkConnectionTypeFields();

		// Although station ID is technically a dynamic field, its position
		// among the other GUI controls makes it difficult to actually create
		// and dispose the control as settings change. As a result, the station
		// fields always exist, but will be hidden/unhidden as venue setting
		// changes
		this.stationIdLabel = new Label(this.venueCompositeTop,
				SessionConfigShellUtil.LABEL_STYLE);
		this.stationIdLabel.setText("Station ID:");
		this.stationIdCombo = new Combo(this.venueCompositeTop,
				SessionConfigShellUtil.COMBO_STYLE);

		final FormData fd25 = SWTUtilities.getFormData(this.stationIdCombo, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);

		final FormData fd26 = SWTUtilities.getFormData(this.stationIdCombo, 1,
				SessionConfigShellUtil.SHORT_FIELD_SIZE);

		fd25.height = (int) (1.5F * fd25.height);
		fd26.height = (int) (1.5F * fd26.height);

		fd25.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
		fd25.right = new FormAttachment(this.stationIdCombo);
		this.stationIdLabel.setLayoutData(fd25);

		fd26.left = new FormAttachment(
				SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);

		if (this.uplinkConnectionTypeCombo != null) {
			fd25.top = new FormAttachment(this.uplinkConnectionTypeCombo);
			fd26.top = new FormAttachment(this.uplinkConnectionTypeCombo);
		} else {
			fd25.top = new FormAttachment(this.hostText);
			fd26.top = new FormAttachment(this.hostText);
		}

		this.stationIdCombo.setLayoutData(fd26);

		initStationIds();

		if (this.showDownlink == true) {
			final Label inputTypeLabel = new Label(this.venueCompositeTop,
					SessionConfigShellUtil.LABEL_STYLE);
			inputTypeLabel.setText("Input Format:");
			final FormData fd5 = new FormData();
			fd5.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd5.top = new FormAttachment(this.hostText, 0, SWT.CENTER);
			inputTypeLabel.setLayoutData(fd5);
			this.fswInputTypeCombo = new Combo(this.venueCompositeTop,
					SessionConfigShellUtil.COMBO_STYLE);
			this.fswInputTypeCombo.addModifyListener(this.modifyHandler);

			final FormData fd4 = new FormData();
			fd4.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd4.top = new FormAttachment(this.hostText, 0, SWT.CENTER);
			fd4.right = new FormAttachment(100);
			this.fswInputTypeCombo.setLayoutData(fd4);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setFieldsFromData()
	 */
	@Override
	public void setFieldsFromData() {
	
		final SessionConfiguration tc = this.getSessionConfig();

		// Set static GUI fields
		if (this.userText != null) {
			SessionConfigShellUtil.safeSetText(this.userText, tc.getContextId().getUser());
		}

		if (this.hostText != null) {
			SessionConfigShellUtil.safeSetText(this.hostText, tc.getContextId().getHost());
		}

		if (tc.getVenueConfiguration().getVenueType() != null && this.venueTypeCombo != null) {
			final String value = tc.getVenueConfiguration().getVenueType().toString();
			SessionConfigShellUtil.safeSetText(this.venueTypeCombo, value);
		}

		if ( tc.getConnectionConfiguration().getDownlinkConnection() != null
				&& this.downlinkConnectionTypeCombo != null) {
			final TelemetryConnectionType value = tc.getConnectionConfiguration().getDownlinkConnection()
					.getDownlinkConnectionType();
			SessionConfigShellUtil.safeSetText(
					this.downlinkConnectionTypeCombo, value.toString());
		}

		if (tc.getConnectionConfiguration().getFswUplinkConnection() != null
				&& this.uplinkConnectionTypeCombo != null) {
			final UplinkConnectionType value = tc.getConnectionConfiguration().getFswUplinkConnection()
                    .getUplinkConnectionType();
			SessionConfigShellUtil.safeSetText(this.uplinkConnectionTypeCombo,
					value.toString());
		}

		// Before going any further, we have to make sure that all the fields
		// called for by the current venue and connection types have been
		// created, both here and in the message service panel. These calls will create
		// the dynamic GUI objects as necessary.
		
		if (this.downlinkStreamIdCombo != null
				&& !this.downlinkStreamIdCombo.isDisposed()) {
			final String text = tc.getVenueConfiguration().getDownlinkStreamId().name();
			SessionConfigShellUtil
					.safeSetText(this.downlinkStreamIdCombo, text);
		}
		
		drawDynamicVenueFields();
		this.jmsDbComposite.drawDynamicMessageServiceFields(getCurrentVenue());

		// Now we can set the dynamic GUI fields.
		if (this.fswInputTypeCombo != null) {
			final TelemetryInputType rit = tc.getConnectionConfiguration().getDownlinkConnection().getInputType();
			final String ritText = (rit != null) ? rit.toString() : "";
			SessionConfigShellUtil.safeSetText(this.fswInputTypeCombo, ritText);
		}

		final IDownlinkConnection dc = tc.getConnectionConfiguration().getDownlinkConnection();
		if (this.inputFileText != null && !this.inputFileText.isDisposed()) {
			if (dc instanceof IFileConnectionSupport) {
			SessionConfigShellUtil.safeSetText(this.inputFileText,
					((IFileConnectionSupport)dc).getFile());
			SWTUtilities.adjustRowsForLineWrap(this.inputFileText);
			}
		}

		if (this.testbedNameCombo != null
				&& !this.testbedNameCombo.isDisposed()) {
			SessionConfigShellUtil.safeSetText(this.testbedNameCombo,
					tc.getVenueConfiguration().getTestbedName());
		}

		if (dc instanceof IDatabaseConnectionSupport) {
			final DatabaseConnectionKey dsi = ((IDatabaseConnectionSupport)dc).getDatabaseConnectionKey();


			if (this.databaseSessionHostText != null
					&& !this.databaseSessionHostText.isDisposed()) {
				SessionConfigShellUtil.safeSetText(this.databaseSessionHostText,
						DatabaseConnectionKey.getHostPattern(dsi, "localhost"));
			}

			if (this.databaseSessionKeyText != null
					&& !this.databaseSessionKeyText.isDisposed()) {
				SessionConfigShellUtil.safeSetText(this.databaseSessionKeyText,
						String.valueOf(DatabaseConnectionKey.getSessionKey(dsi, 0)));
			}

		}
		
		if ((this.stationIdCombo != null) && !this.stationIdCombo.isDisposed()) {
			final Integer dssId = tc.getFilterInformation().getDssId();

			if (dssId != null) {
				final String dssString = (dssId != 0) ? dssId.toString()
						: STATION_ALL;
				SessionConfigShellUtil.safeSetText(this.stationIdCombo,
						dssString);
			} else {
			    SessionConfigShellUtil.safeSetText(this.stationIdCombo,
			            STATION_ALL);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDataFromFields()
	 */
	@Override
	public void setDataFromFields() {

		final SessionConfiguration tc = this.getSessionConfig();

		if (this.userText != null) {
			final String text = this.userText.getText().trim();
			tc.getContextId().setUser(text);
		}

		if (this.hostText != null) {
			final String text = this.hostText.getText().trim();
			tc.getContextId().setHost(text);
		}

		if (this.venueTypeCombo != null) {
			final String text = this.venueTypeCombo.getText();
			tc.getVenueConfiguration().setVenueType(VenueType.valueOf(text));
		}


		if (this.downlinkConnectionTypeCombo != null) {
			final String connectionText = this.downlinkConnectionTypeCombo
					.getText();
			final TelemetryConnectionType type = TelemetryConnectionType.valueOf(connectionText);
			final IDownlinkConnection dc = tc.getConnectionConfiguration().getDownlinkConnection();
			if (type != dc.getDownlinkConnectionType()) {
				tc.getConnectionConfiguration().createDownlinkConnection(type);
			}
		} else {
		    tc.getConnectionConfiguration().remove(ConnectionKey.FSW_DOWNLINK);
		}

		if (this.uplinkConnectionTypeCombo != null) {
			final String uplinkConnectionText = this.uplinkConnectionTypeCombo
					.getText();
			final UplinkConnectionType type = UplinkConnectionType.valueOf(uplinkConnectionText);
			final IUplinkConnection uc = tc.getConnectionConfiguration().getFswUplinkConnection();
			if (type != uc.getUplinkConnectionType()) {
				tc.getConnectionConfiguration().createFswUplinkConnection(type);
			}
		} else {
			  tc.getConnectionConfiguration().remove(ConnectionKey.FSW_UPLINK);
		}
				

		final IDownlinkConnection dc = tc.getConnectionConfiguration().getDownlinkConnection();
		
		if (this.fswInputTypeCombo != null) {
			final String text = this.fswInputTypeCombo.getText();
			dc.setInputType(TelemetryInputType.valueOf(text));
		}

		if (this.inputFileText != null && !this.inputFileText.isDisposed() && dc instanceof IFileConnectionSupport) {
			((IFileConnectionSupport)dc).setFile(this.inputFileText.getText().trim());
		}

		if (this.testbedNameCombo != null
				&& !this.testbedNameCombo.isDisposed()) {
			tc.getVenueConfiguration().setTestbedName(this.testbedNameCombo.getText().trim());
		}

		if (this.downlinkStreamIdCombo != null
				&& !this.downlinkStreamIdCombo.isDisposed()) {
			final String text = this.downlinkStreamIdCombo.getText().trim();
			tc.getVenueConfiguration().setDownlinkStreamId(DownlinkStreamType.convert(text));
		}


		if (this.databaseSessionHostText != null
				&& !this.databaseSessionHostText.isDisposed() && dc instanceof IDatabaseConnectionSupport) {
			((IDatabaseConnectionSupport)dc).getDatabaseConnectionKey().setHostPattern(this.databaseSessionHostText.getText().trim());
		}

		if (this.databaseSessionKeyText != null
				&& !this.databaseSessionKeyText.isDisposed()  && dc instanceof IDatabaseConnectionSupport) {
			final long key = GDR.parse_long(this.databaseSessionKeyText
					.getText().trim());
			((IDatabaseConnectionSupport)dc).getDatabaseConnectionKey().setSessionKey(key);
		}

		/*
		 * Allow station ID selection in all venues
		 * instead of just ops.
		 */ 
		if ((this.stationIdCombo != null) && !this.stationIdCombo.isDisposed()) {
		    final String text = this.stationIdCombo.getText();

		    if (!text.equals(STATION_ALL)) {
		        tc.getFilterInformation().setDssId(Integer.parseInt(text));
		    } else {
		        tc.getFilterInformation().setDssId(null);
		    }

		} else {
		    tc.getFilterInformation().setDssId(null);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#setDefaultFieldValues()
	 */
	@Override
	public void setDefaultFieldValues() {
		
		final VenueType vt = missionProps.getDefaultVenueType();

		if (this.venueTypeCombo != null) {
			SessionConfigShellUtil.safeSetText(this.venueTypeCombo,
					vt.toString());
		}

		if (this.downlinkConnectionTypeCombo != null) {
            final TelemetryConnectionType ct = connectProps.getDefaultDownlinkConnectionType(vt,
                                                                                             sseFlag.isApplicationSse());
			final String connection = ct == null ? TelemetryConnectionType.UNKNOWN
					.toString() : ct.toString();
			SessionConfigShellUtil.safeSetText(
					this.downlinkConnectionTypeCombo, connection);
		}

		if (this.uplinkConnectionTypeCombo != null) {
            final UplinkConnectionType ct = connectProps.getDefaultUplinkConnectionType(vt,
                                                                                        sseFlag.isApplicationSse());
			final String connection = ct == null ? UplinkConnectionType.UNKNOWN
					.toString() : ct.toString();
			SessionConfigShellUtil.safeSetText(this.uplinkConnectionTypeCombo,
					connection);
		}

		if (this.userText != null) {
            SessionConfigShellUtil.safeSetText(this.userText, GdsSystemProperties.getSystemUserName());
		}

		if (this.hostText != null) {
			SessionConfigShellUtil.safeSetText(this.hostText,
					HostPortUtility.getLocalHostName());
		}

        final TelemetryConnectionType ct = connectProps.getDefaultDownlinkConnectionType(vt,
                                                                                         sseFlag.isApplicationSse());

		initInputTypes(vt, ct, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.config.gui.ISessionConfigPanel#validateInputFields()
	 */
	@Override
	public boolean validateInputFields() {

		if (this.userText != null
				&& this.userText.getText().trim().length() == 0) {

			SWTUtilities.showErrorDialog(this.getParent(), "Bad User",
					"You must enter a valid User to proceed");
			this.userText.setFocus();
			return (false);
		}

		if (this.inputFileText != null && !this.inputFileText.isDisposed()
				&& this.inputFileText.isEnabled()) {

			final String file = this.inputFileText.getText().trim();
			if (file.length() == 0) {
				SWTUtilities.showErrorDialog(this.getParent(),
						"Bad Input File",
						"You must enter a valid Input File to proceed");

				this.inputFileText.setFocus();

				return (false);
			}
			if ((new File(file)).exists() == false) {
				SWTUtilities.showErrorDialog(this.getParent(),
						"Bad Input File",
						"The specified Input File does not exist.");

				this.inputFileText.setFocus();

				return (false);
			}
		}

		if (this.databaseSessionKeyText != null
				&& !this.databaseSessionKeyText.isDisposed()
				&& this.databaseSessionKeyText.isEnabled()) {

			final String keyString = this.databaseSessionKeyText.getText()
					.trim();
			final String error = SessionConfigShellUtil
					.validateDatabaseSessionKey(keyString);
			if (error != null) {
				SWTUtilities
						.showErrorDialog(
								this.getParent(),
								"Bad Database Session Key",
								error
										+ ": You must enter a valid database session key to proceed");
				this.databaseSessionKeyText.setFocus();
				return (false);
			}
		}

		if (this.databaseSessionHostText != null
				&& !this.databaseSessionHostText.isDisposed()
				&& this.databaseSessionHostText.isEnabled()) {

			final String hostString = this.databaseSessionHostText.getText()
					.trim();
			final String error = SessionConfigShellUtil
					.validateDatabaseSessionHost(hostString);

			if (error != null) {
				SWTUtilities
						.showErrorDialog(
								this.getParent(),
								"Bad Database Session Host",
								error
										+ ": You must enter a valid database session host to proceed");

				this.databaseSessionHostText.setFocus();
				return (false);
			}
		}

		return true;
	}

	/**
	 * Enables of disables fields in this panel related to flight downlink. This
	 * is used when the launch of flight downlink is disabled or enabled by the
	 * user.
	 * 
	 * @param enable
	 *            true to enabled flight downlink fields; false to disable
	 */
	public void enableFlightDownlinkFields(final boolean enable) {

		if (this.fswInputTypeCombo != null) {
			this.fswInputTypeCombo.setEnabled(enable);
		}
		if (this.downlinkConnectionTypeCombo != null) {
			this.downlinkConnectionTypeCombo.setEnabled(enable);
		}
		if (this.inputFileBrowseButton != null
				&& !this.inputFileBrowseButton.isDisposed()) {
			this.inputFileBrowseButton.setEnabled(enable);
		}
		if (this.inputFileText != null && !this.inputFileText.isDisposed()) {
			this.inputFileText.setEnabled(enable);
		}
		if (this.pvlCreateButton != null && !this.pvlCreateButton.isDisposed()) {
			this.pvlCreateButton.setEnabled(enable);
		}
		if (this.pvlEditButton != null && !this.pvlEditButton.isDisposed()) {
			this.pvlEditButton.setEnabled(enable);
		}
		if (this.databaseSessionHostText != null
				&& !this.databaseSessionHostText.isDisposed()) {
			this.databaseSessionHostText.setEnabled(enable);
		}
		if (this.databaseSessionKeyText != null
				&& !this.databaseSessionKeyText.isDisposed()) {
			this.databaseSessionKeyText.setEnabled(enable);
		}
	}

	/**
	 * Sets the host and port fields in the FSW panel based upon the current
	 * venue and connection types.
	 */
	public void restoreFswDefaultsForVenueAndConnection() {

		/*
		 * Do not set defaults if
		 * the parent window has suppressed it. 
		 */
		if (this.getConfigShell().isSuppressDefaulting()) {
			return;
		}
				
		String tbName = null;
		DownlinkStreamType streamId = null;
		final VenueType vt = getCurrentVenue();

		String ctString = "";

		if (this.showDownlink) {
			ctString = this.downlinkConnectionTypeCombo.getText();
		}

		String uctString = "";

		if (this.showUplink) {
			uctString = this.uplinkConnectionTypeCombo.getText();
		}

		TelemetryConnectionType ct = null;
		UplinkConnectionType uct = null;

		if (!ctString.isEmpty()) {
			ct = TelemetryConnectionType.valueOf(ctString);
		}

		if (!uctString.isEmpty()) {
			uct = UplinkConnectionType.valueOf(uctString);
		}

		if ((vt == VenueType.TESTBED)
				|| (vt == VenueType.ATLO)) {
			if (this.testbedNameCombo != null) {
				tbName = this.testbedNameCombo.getText();
			}

			if (this.downlinkStreamIdCombo != null) {
				final String text = this.downlinkStreamIdCombo.getText();
				streamId = DownlinkStreamType.convert(text);
			}
		}

		final boolean haveTestbed = ((tbName != null) && !tbName.isEmpty());
		final boolean testbedOrAtlo = ((vt == VenueType.TESTBED) || (vt == VenueType.ATLO));

		if (haveTestbed || !testbedOrAtlo) {
			this.fswComposite.restoreHostDefaultsForVenueAndConnection(vt, ct,
					uct, tbName, streamId);

			/* R8 Refactor TODO - find some other way to do this */
//			 * Pull the relevant PVL file path from
//			 * the configuration and update the configuration display
//			 */
//			
//			if(this.fswInputTypeCombo != null){
//				String itString = this.fswInputTypeCombo.getText();
//				RawInputType it = null;
//				if (!itString.isEmpty()) {
//					it = RawInputType.valueOf(itString);
//				}
//
//				String pvlFileLoc = HostConfiguration.getFswPvlFilePath(vt, ct, it, tbName, streamId);
//				if (this.inputFileText != null) {
//					SessionConfigShellUtil.safeSetText(this.inputFileText, pvlFileLoc);
//				}
//
//			}
			
		}
	}

	/**
	 * Sets the host and port fields in the SSE panel based upon the current
	 * venue and connection types.
	 */
	public void restoreSseDefaultsForVenueAndConnection() {

		if (this.sseComposite == null) {
			return;
		}
		
		/*
		 * Do not set defaults if
		 * the parent window has suppressed it. 
		 */
		if (this.getConfigShell().isSuppressDefaulting()) {
			return;
		}
		
		String tbName = null;
		final VenueType vt = getCurrentVenue();

		String ctString = "";

		if (this.showDownlink) {
			ctString = this.downlinkConnectionTypeCombo.getText();
		}

		String uctString = "";

		if (this.showUplink) {
			uctString = this.uplinkConnectionTypeCombo.getText();
		}

		TelemetryConnectionType ct = null;
		UplinkConnectionType uct = null;

		if (!ctString.isEmpty()) {
			ct = TelemetryConnectionType.valueOf(ctString);
		}

		if (!uctString.isEmpty()) {
			uct = UplinkConnectionType.valueOf(uctString);
		}

		if ((vt == VenueType.TESTBED)
				|| (vt == VenueType.ATLO)
				&& (this.testbedNameCombo != null)) {

			tbName = this.testbedNameCombo.getText();
		}

		final boolean haveTestbed = ((tbName != null) && !tbName.isEmpty());
		final boolean testbedOrAtlo = ((vt == VenueType.TESTBED) || (vt == VenueType.ATLO));

		if (haveTestbed || !testbedOrAtlo) {
			this.sseComposite.restoreHostDefaultsForVenueAndConnection(vt, ct,
					uct, tbName);
		}
	}

	/**
	 * Get current VenueType from the venue type combo box. Returns the
	 * mission's default venue if the value in the combo is not valid for some
	 * reason.
	 * 
	 * @return VenueType
	 */
	public VenueType getCurrentVenue() {

		final VenueType defalt = missionProps.getDefaultVenueType();
		VenueType result = defalt;

		if (this.venueTypeCombo == null) {
			return result;
		}

		String id = this.venueTypeCombo.getText();

		id = StringUtil.safeTrim(id);

		try {
			result = VenueType.valueOf(id);
		} catch (final IllegalArgumentException iae) {
			result = defalt;
		}

		return result;
	}

	/**
	 * Instantiates the main group (composite) for the venue components. Also
	 * creates the parent ExpandItem and attaches it to the parent ExpandBar.
	 * 
	 * @param parentTabBar
	 *            the parent ExpandBar to attach the venue ExpandItem to
	 * 
	 */
	private void createVenueGroup(final ExpandBar parentTabBar) {

		if (this.venueInfoGroup == null) {
			this.venueInfoGroup = new Group(parentTabBar,
					SessionConfigShellUtil.GROUP_STYLE);
			final FontData groupFontData = new FontData(
					SessionConfigShellUtil.DEFAULT_FACE,
					SessionConfigShellUtil.DEFAULT_FONT_SIZE, SWT.BOLD);
			final Font groupFont = new Font(this.getParent().getDisplay(),
					groupFontData);
			this.venueInfoGroup.setFont(groupFont);
			final FormLayout fl1 = new FormLayout();
			fl1.spacing = 0;
			fl1.marginTop = 5;
			fl1.marginWidth = 5;
			fl1.marginBottom = 5;
			this.venueInfoGroup.setLayout(fl1);
			final FormData fd1 = new FormData();
			fd1.left = new FormAttachment(0);
			fd1.right = new FormAttachment(100);
			this.venueInfoGroup.setLayoutData(fd1);

			setExpandItem(new ExpandItem(parentTabBar, SWT.NONE,
					parentTabBar.getItemCount()));
			getExpandItem().setText("Venue Information");
			getExpandItem()
					.setHeight(
							this.venueInfoGroup.computeSize(SWT.DEFAULT,
									SWT.DEFAULT).y);
			getExpandItem().setHeight(200);
			getExpandItem().setControl(this.venueInfoGroup);
			getExpandItem().setExpanded(true);
		}
	}

	/**
	 * Creates the downlink connection type combo and label fields.
	 */
	private void createDownlinkConnectionTypeFields() {

		if (this.showDownlink) {
			this.downlinkConnectionTypeCombo = new Combo(
					this.venueCompositeTop, SessionConfigShellUtil.COMBO_STYLE);

			final VenueType vt = getCurrentVenue();
			initDownlinkConnectionTypes(vt);

			this.downlinkConnectionTypeCombo
					.addModifyListener(this.modifyHandler);

			final FormData fd2 = new FormData();
			fd2.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd2.top = new FormAttachment(this.hostText);
			fd2.right = new FormAttachment(100);
			this.downlinkConnectionTypeCombo.setLayoutData(fd2);

			final Label downlinkConnectionTypeLabel = new Label(
					this.venueCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
			downlinkConnectionTypeLabel.setText("Downlink Connection:");
			final FormData fd = new FormData();
			fd.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd.top = new FormAttachment(this.downlinkConnectionTypeCombo, 0,
					SWT.CENTER);
			fd.right = new FormAttachment(this.downlinkConnectionTypeCombo);
			downlinkConnectionTypeLabel.setLayoutData(fd);
		}
	}

	/**
	 * Create and the uplink connection type label and combo box.
	 */
	private void createUplinkConnectionTypeFields() {

		if (this.showUplink) {
			this.uplinkConnectionTypeCombo = new Combo(this.venueCompositeTop,
					SessionConfigShellUtil.COMBO_STYLE);

			populateUplinkConnectionType();

			this.uplinkConnectionTypeCombo
					.addModifyListener(this.modifyHandler);

			final FormData fd2 = SWTUtilities.getFormData(
					this.uplinkConnectionTypeCombo, 1, 30);
			fd2.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd2.top = new FormAttachment(this.hostText);
			this.uplinkConnectionTypeCombo.setLayoutData(fd2);

			final Label uplinkConnectionTypeLabel = new Label(
					this.venueCompositeTop, SessionConfigShellUtil.LABEL_STYLE);
			uplinkConnectionTypeLabel.setText("Uplink Connection:");
			final FormData fd = new FormData();
			fd.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd.top = new FormAttachment(this.uplinkConnectionTypeCombo, 0,
					SWT.CENTER);
			fd.right = new FormAttachment(this.uplinkConnectionTypeCombo);
			uplinkConnectionTypeLabel.setLayoutData(fd);
		}
	}

	/**
	 * Populate uplink connection type if necessary.
	 * 
	 * @param scid
	 *            S/C id selected
	 */
	private void populateUplinkConnectionType() {

		if (!this.showUplink || (this.uplinkConnectionTypeCombo == null)) {
			return;
		}

		final VenueType venue = getCurrentVenue();
		final Combo combo = this.uplinkConnectionTypeCombo;
		final String currentSetting = StringUtil.emptyAsNull(combo.getText());

		combo.removeAll();

		// Get all configured connection types

		final Set<UplinkConnectionType> uplinkConnectionTypes = 
                connectProps.getAllowedUplinkConnectionTypes(venue,
                                                             sseFlag.isApplicationSse());

		if ((uplinkConnectionTypes == null) || uplinkConnectionTypes.isEmpty()) {
			return;
		}

		final Set<String> uct = new TreeSet<String>();

		for (final UplinkConnectionType type : uplinkConnectionTypes) {
			uct.add(type.toString());
		}

		// If COMMAND_SERVICE is configured, allow it only if the S/C
		// id is authorized.

		final String commandService = UplinkConnectionType.COMMAND_SERVICE
				.toString();

		final String socket = UplinkConnectionType.SOCKET.toString();

        /* Fix population of default uplink connection type. */
		
        final String defalt = connectProps.getDefaultUplinkConnectionType(venue,
                                                                          sseFlag.isApplicationSse())
                                          .toString();

		if (uct.isEmpty()) {
			// No settings, leave it unset
			return;
		}

		boolean hasCurrentSetting = false;
		boolean hasSocket = false;
		boolean hasCommandService = false;
		boolean hasDefalt = false;

		for (final String type : uct) {
			combo.add(type);

			if ((currentSetting != null)
					&& type.equalsIgnoreCase(currentSetting)) {
				hasCurrentSetting = true;
			}

			if (type.equalsIgnoreCase(socket)) {
				hasSocket = true;
			}

			if (type.equalsIgnoreCase(commandService)) {
				hasCommandService = true;
			}

			if ((defalt != null) && type.equalsIgnoreCase(defalt)) {
				hasDefalt = true;
			}
		}

		final boolean wantsCommandService = (venue.equals(VenueType.ATLO) || venue
				.equals(VenueType.TESTBED));

		if (hasCommandService && wantsCommandService) {
			SessionConfigShellUtil.safeSetText(combo, commandService);
		} else if (hasDefalt) {
			SessionConfigShellUtil.safeSetText(combo, defalt);
		} else if (hasCurrentSetting) {
			SessionConfigShellUtil.safeSetText(combo, currentSetting);
		} else if (hasSocket) {
			SessionConfigShellUtil.safeSetText(combo, socket);
		} else {
			combo.select(0);
		}
	}

	/**
	 * Creates the dynamic GUI fields.
	 */
	private void drawDynamicVenueFields() {

		// Save field values we might want to restore
		final FieldStash stash = new FieldStash();
		stash.stashFields();

		// Destroy the old composite containing dynamic values
		if (this.venueCompositeBottom != null) {
			this.venueCompositeBottom.dispose();
			this.venueCompositeBottom = null;
		}

		// Recreate the dynamic composite and all GUI fields on it
		// according to current configuration
		this.venueCompositeBottom = new Composite(this.venueInfoGroup, SWT.NONE);
		final FormLayout fl1 = new FormLayout();
		fl1.spacing = 5;
		fl1.marginTop = 5;
		fl1.marginBottom = 10;
		fl1.marginLeft = 0;
		fl1.marginRight = 0;
		this.venueCompositeBottom.setLayout(fl1);
		final FormData fd1 = new FormData();
		fd1.left = new FormAttachment(0);
		fd1.right = new FormAttachment(100);
		fd1.top = new FormAttachment(this.venueCompositeTop, 0);
		this.venueCompositeBottom.setLayoutData(fd1);

		this.inputFileText = null;
		this.inputFileBrowseButton = null;
		this.downlinkStreamIdCombo = null;

		this.testbedNameCombo = null;

		Control bottomLeftControl = this.hostText;

		final VenueType vt = getCurrentVenue();

		TelemetryConnectionType ct = null;
		
		if (this.showDownlink) {
			
			final String text = this.downlinkConnectionTypeCombo.getText();

			if (text == null || text.isEmpty()) {
                ct = connectProps.getDefaultDownlinkConnectionType(vt, sseFlag.isApplicationSse());
			} else {
				ct = TelemetryConnectionType.valueOf(text);
			}
			DownlinkStreamType dst = null;
			if (downlinkStreamIdCombo != null) {
				final String dstStr = downlinkStreamIdCombo.getText();
				dst = DownlinkStreamType.valueOf(dstStr);
			}
			initInputTypes(vt, ct, dst);
		}

	
		if (vt.equals(VenueType.TESTBED) || vt.equals(VenueType.ATLO)) {

			final Label testbedNameLabel = new Label(this.venueCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);

			this.testbedNameCombo = new Combo(this.venueCompositeBottom,
					SessionConfigShellUtil.COMBO_STYLE);
			/* Replaced venue type switch to init
			 * testbed names with a single method call that takes venue as arg.
			 */
			initTestbedNames(vt);
			
			this.testbedNameCombo.addModifyListener(this.modifyHandler);

			final FormData fd10 = SWTUtilities.getFormData(
					this.testbedNameCombo, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd10.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd10.top = new FormAttachment(this.downlinkConnectionTypeCombo);
			this.testbedNameCombo.setLayoutData(fd10);
			testbedNameLabel.setText("Testbed Name:");
			final FormData fd11 = new FormData();
			fd11.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd11.right = new FormAttachment(this.testbedNameCombo);
			fd11.top = new FormAttachment(this.testbedNameCombo, 0, SWT.CENTER);
			testbedNameLabel.setLayoutData(fd11);
			bottomLeftControl = this.testbedNameCombo;

			if (this.showDownlink   &&
                    !sseFlag.isApplicationSse() &&
            	/* Replaced explicit compare with multiple
            	 * connection types with a call to DownlinkConnectionType.hasStreamId()
            	 */
                ct.usesStreamId())
            {
				final Label downlinkStreamIdLabel = new Label(
						this.venueCompositeBottom,
						SessionConfigShellUtil.LABEL_STYLE);
				downlinkStreamIdLabel.setText("D/L Stream:");
				final FormData fd5 = new FormData();
				fd5.left = new FormAttachment(
						SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
				fd5.top = new FormAttachment(this.testbedNameCombo, 0,
						SWT.CENTER);
				downlinkStreamIdLabel.setLayoutData(fd5);

				this.downlinkStreamIdCombo = new Combo(
						this.venueCompositeBottom,
						SessionConfigShellUtil.COMBO_STYLE);

				/* Replaced venue type switch to init
	             * stream IDs with a single method call that takes venue as arg.
	             */
				initDownlinkStreamIds(vt);
				
				this.downlinkStreamIdCombo
						.addModifyListener(this.modifyHandler);

				final FormData fd4 = SWTUtilities.getFormData(
						this.downlinkStreamIdCombo, 1,
						SessionConfigShellUtil.SHORT_FIELD_SIZE);
				fd4.left = new FormAttachment(
						SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
				fd4.top = new FormAttachment(this.fswInputTypeCombo);
				this.downlinkStreamIdCombo.setLayoutData(fd4);
			}
		}

		final VenueButtonSelectionHandler buttonHandler = new VenueButtonSelectionHandler();

		if (ct == TelemetryConnectionType.FILE) {
			FormData fd15 = null;
			final Label inputFileLabel = new Label(this.venueCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);

			this.inputFileText = new Text(this.venueCompositeBottom,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			// Make input file column bigger than the
			// standard text fields.
			fd15 = SWTUtilities.getFormData(this.inputFileText, 1, 78);

			fd15.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd15.top = new FormAttachment(
					(this.testbedNameCombo != null) ? this.testbedNameCombo
							: this.downlinkConnectionTypeCombo, 8);
			this.inputFileText.setLayoutData(fd15);
			inputFileLabel.setText("Input File:");
			final FormData fd16 = new FormData();
			fd16.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd16.right = new FormAttachment(this.inputFileText);
			fd16.top = new FormAttachment(this.inputFileText, 0, SWT.CENTER);
			inputFileLabel.setLayoutData(fd16);

			SWTUtilities.adjustRowsForLineWrap(this.inputFileText);

			this.inputFileBrowseButton = new Button(this.venueCompositeBottom,
					SWT.PUSH);
			this.inputFileBrowseButton.setText("Browse...");
			final FormData bfd = new FormData();
			bfd.left = new FormAttachment(this.inputFileText, 5);
			bfd.top = new FormAttachment(this.inputFileText, 0, SWT.CENTER);
			bfd.right = new FormAttachment(100, -5);
			this.inputFileBrowseButton.setLayoutData(bfd);
			this.inputFileBrowseButton.addSelectionListener(buttonHandler);

			bottomLeftControl = this.inputFileText;

		} else if (ct == TelemetryConnectionType.DATABASE) {
			final Label databaseSessionKeyLabel = new Label(
					this.venueCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);
			this.databaseSessionKeyText = new Text(this.venueCompositeBottom,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.databaseSessionKeyText.setText("1");

			final FormData fd8 = SWTUtilities.getFormData(
					this.databaseSessionKeyText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd8.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd8.top = new FormAttachment(
					(this.testbedNameCombo != null) ? this.testbedNameCombo
							: this.downlinkConnectionTypeCombo, 8);
			this.databaseSessionKeyText.setLayoutData(fd8);
			databaseSessionKeyLabel.setText("Session Key:");
			final FormData fd9 = new FormData();
			fd9.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);

			fd9.right = new FormAttachment(this.databaseSessionKeyText);
			fd9.top = new FormAttachment(this.databaseSessionKeyText, 0,
					SWT.CENTER);
			databaseSessionKeyLabel.setLayoutData(fd9);
			bottomLeftControl = this.databaseSessionKeyText;

			final Label databaseSessionHostLabel = new Label(
					this.venueCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);
			databaseSessionHostLabel.setText("Session Host:");
			final FormData fd5 = new FormData();
			fd5.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_LABEL_START);
			fd5.top = new FormAttachment(this.databaseSessionKeyText, 0,
					SWT.CENTER);

			databaseSessionHostLabel.setLayoutData(fd5);
			this.databaseSessionHostText = new Text(this.venueCompositeBottom,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			this.databaseSessionHostText.setText(HostPortUtility
					.getLocalHostName());

			final FormData fd4 = SWTUtilities.getFormData(
					this.databaseSessionHostText, 1,
					SessionConfigShellUtil.SHORT_FIELD_SIZE);
			fd4.left = new FormAttachment(
					SessionConfigShellUtil.RIGHT_COLUMN_INPUT_START);
			fd4.top = new FormAttachment(
					(this.testbedNameCombo != null) ? this.testbedNameCombo
							: this.downlinkConnectionTypeCombo, 8);
			this.databaseSessionHostText.setLayoutData(fd4);

			final FormData pfb2 = new FormData();

			pfb2.left = new FormAttachment(this.databaseSessionHostText);
			pfb2.top = new FormAttachment(
					(this.testbedNameCombo != null) ? this.testbedNameCombo
							: this.downlinkConnectionTypeCombo, 8);
		}

		if (ct == TelemetryConnectionType.TDS && this.showDownlink) {
			final Label separator = new Label(this.venueCompositeBottom,
					SWT.SEPARATOR | SWT.HORIZONTAL);
			final FormData sfd = new FormData();
			sfd.left = new FormAttachment(0);
			sfd.right = new FormAttachment(100);
			sfd.top = new FormAttachment(bottomLeftControl, 5);
			separator.setLayoutData(sfd);

			final Label pvlFileLabel = new Label(this.venueCompositeBottom,
					SessionConfigShellUtil.LABEL_STYLE);

			this.inputFileText = new Text(this.venueCompositeBottom,
					SessionConfigShellUtil.SHORT_TEXT_STYLE);
			final FormData fd15 = SWTUtilities.getFormData(this.inputFileText,
					1, SessionConfigShellUtil.LONG_FIELD_SIZE);
			fd15.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_INPUT_START);
			fd15.right = new FormAttachment(100);
			fd15.top = new FormAttachment(separator, 5);
			this.inputFileText.setLayoutData(fd15);
			pvlFileLabel.setText("PVL File:");
			final FormData fd16 = new FormData();
			fd16.left = new FormAttachment(
					SessionConfigShellUtil.LEFT_COLUMN_LABEL_START);
			fd16.right = new FormAttachment(this.inputFileText);
			fd16.top = new FormAttachment(this.inputFileText, 0, SWT.CENTER);
			pvlFileLabel.setLayoutData(fd16);

			this.pvlCreateButton = new Button(this.venueCompositeBottom,
					SWT.PUSH);
			this.pvlCreateButton.setText("Create PVL...");
			final FormData pfd = new FormData();
			pfd.left = new FormAttachment(10);
			pfd.right = new FormAttachment(30);
			pfd.top = new FormAttachment(this.inputFileText);
			this.pvlCreateButton.setLayoutData(pfd);
			this.pvlCreateButton.addSelectionListener(buttonHandler);

			this.pvlEditButton = new Button(this.venueCompositeBottom, SWT.PUSH);
			this.pvlEditButton.setText("Edit PVL...");
			final FormData ped = new FormData();
			ped.left = new FormAttachment(40);
			ped.right = new FormAttachment(60);
			ped.top = new FormAttachment(this.inputFileText);
			this.pvlEditButton.setLayoutData(ped);
			this.pvlEditButton.addSelectionListener(buttonHandler);

			this.inputFileBrowseButton = new Button(this.venueCompositeBottom,
					SWT.PUSH);
			this.inputFileBrowseButton.setText("Load PVL...");
			final FormData bfd = new FormData();
			bfd.left = new FormAttachment(70);
			bfd.right = new FormAttachment(90);
			bfd.top = new FormAttachment(this.inputFileText);
			this.inputFileBrowseButton.setLayoutData(bfd);
			this.inputFileBrowseButton.addSelectionListener(buttonHandler);
		}

		// Set default values for the new dynamic GUI fields
		setDynamicVenueFieldDefaults();

		// Now restore any values the user entered which are still valid
		stash.restoreFields();

		// The parent window must be laid out again and packed because fields
		// in the display have changed.
		this.getConfigShell().resizeAndPackLater();
	}

	/**
	 * Sets default values into dynamic GUI fields as necessary.
	 */
	private void setDynamicVenueFieldDefaults() {

		VenueType vt = missionProps.getDefaultVenueType();

		if (this.venueTypeCombo != null) {
			vt = getCurrentVenue();
		}

		if (this.sseComposite != null) {
			final boolean wasEnabledSse = this.sseComposite.isEnabled();
			final boolean enableSse = !vt.isOpsVenue()
					&& !this.getConfigShell().isStandaloneChillDown();
			this.sseComposite.enable(enableSse, this.parentBar);
			/*
			 * Do not do set defaults
			 * in the SSE composite unless enabling AFTER it was
			 * previously disabled.
			 * */
			if (enableSse && !wasEnabledSse) {
				this.sseComposite.setDefaultFieldValues();
				restoreSseDefaultsForVenueAndConnection();
				this.sseComposite.setFieldsFromData();
			}
		}

		if (this.fswInputTypeCombo != null) {
            final TelemetryInputType defaultType = connectProps.getDefaultSourceFormat(vt,
                                                                                       sseFlag.isApplicationSse());
			if (this.inputTypeComboSet.contains(defaultType.toString())) {
				SessionConfigShellUtil.safeSetText(this.fswInputTypeCombo,
						defaultType.toString());
			} else {
				this.fswInputTypeCombo.select(0);
			}
		}
		
		/* Replaced venue type switch to set
         * default testbed name with a single method call to SessionProperteis
         * that takes venue as arg.
         */
		if (vt.hasTestbedName()) {
		    SessionConfigShellUtil.safeSetText(this.testbedNameCombo, 
		            missionProps.getDefaultTestbedName(vt, getSessionConfig().getContextId().getHost()));
		}
	
	}

	/**
	 * Initializes the contents of the FSW input type combo box based upon a
	 * venue and connection type. This method will preserve the old value of the
	 * combo if possible.
	 * 
	 * @param vt
	 *            the venue to use when getting the valid input types
	 * @param ct
	 *            the connection type to use when getting the valid input types
	 */
	private void initInputTypes(final VenueType vt, final TelemetryConnectionType ct, final DownlinkStreamType dst) {

	    /* This method returns a set but we need a list,
	     * so we convert it.
	     */
		final Set<String> temp = connectProps.
                                             getAllowedDownlinkSourceFormatsAsStrings(ct,
                                                                                      sseFlag.isApplicationSse());
		
		if (dst != null && dst == DownlinkStreamType.COMMAND_ECHO) {
			temp.clear();
			temp.add(TelemetryInputType.CMD_ECHO.toString());
		}

        final List<String> types = new LinkedList<>(temp);
		
        if (!types.isEmpty() && this.fswInputTypeCombo != null) {

			final String saveVal = this.fswInputTypeCombo.getText();

			this.fswInputTypeCombo.removeAll();
			this.inputTypeComboSet.clear();
			this.inputTypeComboSet.addAll(types);
			this.fswInputTypeCombo.setItems(this.inputTypeComboSet
					.toArray(new String[this.inputTypeComboSet.size()]));

			if (this.inputTypeComboSet.contains(saveVal)) {
				SessionConfigShellUtil.safeSetText(this.fswInputTypeCombo, saveVal);
			} else {
				SessionConfigShellUtil.safeSetText(this.fswInputTypeCombo, types.get(0));
			}
		}
	}

	/* Combined initTestbedNames() and initAtloNames()
	 * with a single method that takes venue.
	 */
	/**
	 * Initializes the testbed name combo with a lit of valid values based upon
	 * the mission's configured testbed names.
	 */
	private void initTestbedNames(final VenueType vt) {

	    /* Go through SessionProperties to get testbeds */
		final List<String> testbeds = missionProps.getAllowedTestbedNames(vt);
	
		if (testbeds != null && this.testbedNameCombo != null
				&& !this.testbedNameCombo.isDisposed()) {
			this.testbedNameComboSet.clear();
			this.testbedNameComboSet.addAll(testbeds);

			this.testbedNameCombo.setItems(this.testbedNameComboSet
					.toArray(new String[this.testbedNameComboSet.size()]));

			final String defaultTb = missionProps.getDefaultTestbedName(vt, getSessionConfig().getContextId().getHost());
			if (defaultTb != null) {
			    this.testbedNameCombo.setText(defaultTb);
			} else {
			    this.testbedNameCombo.select(0);
			}
		}
	}

	/**
	 * Initializes the station ID combo with the list of valid station IDs in
	 * the current mission configuration.
	 */
	private void initStationIds() {

		if (this.stationIdCombo == null) {
			return;
		}

		this.stationIdCombo.add(STATION_ALL);
		/*
		 * Replaced ConfiguredDssIds access with
		 * StationMapper access to consolidate station configuration.
		 */
		for (final Integer dssId : missionProps.getStationMapper().getStationIdsAsSet()) {
			// STATION_ALL takes care of a zero in the list

			if (dssId > 0) {
				this.stationIdCombo.add(dssId.toString());
			}
		}
	}
	
	/* Combined initTetsbedDownlinkStreamIds() and
	 * initAtloDownlinkStreamIds() with a single method that takes venue.
     */

	/**
	 * Initializes the downlink stream combo with a lit of valid values based
	 * upon the mission's configured testbed downlink stream IDs.
	 */
	private void initDownlinkStreamIds(final VenueType vt) {

	    /* Go through SessionProperties to get Stream IDs */
		final List<String> chans =  missionProps.getAllowedDownlinkStreamIdsAsStrings(vt);
	
		if (chans != null && this.downlinkStreamIdCombo != null) {
			this.downlinkStreamIdComboSet.clear();
			this.downlinkStreamIdComboSet.addAll(chans);

			this.downlinkStreamIdCombo.setItems(this.downlinkStreamIdComboSet
					.toArray(new String[this.downlinkStreamIdComboSet.size()]));

			SessionConfigShellUtil.safeSetText(this.downlinkStreamIdCombo, chans.get(0));
		}
	}

	/**
	 * Initializes the venue type combo box with the set of venue values from
	 * the mission's configuration.
	 */
	private void initVenueTypes() {

		final Set<String> venueTypes = missionProps.getAllowedVenueTypesAsStrings();
		if (venueTypes == null) {
			return;
		}

		for (final String v: venueTypes) {
			this.venueTypeCombo.add(v);
		}
	}

	/**
	 * Initializes the downlink connection type combo box with the set of venue
	 * values from the mission's configuration.
	 */
	private void initDownlinkConnectionTypes(final VenueType vt) {

		//  added VenueType argument to function call, to get non-generic set
        final Set<String> connects = connectProps.getAllowedDownlinkConnectionTypesAsStrings(vt,
                                                                                             sseFlag.isApplicationSse());

		if (connects != null && !connects.isEmpty()
				&& this.downlinkConnectionTypeCombo != null) {

			this.downlinkConnectionTypeCombo
					.setItems(connects.toArray(new String[connects.size()]));
		}
	}

	/**
	 * This class stashes away the values of some of the dynamic GUI fields
	 * before a GUI changes that may lose them, and then restores them to the
	 * fields upon request.
	 * 
	 *
	 */
	private class FieldStash {
		private String downlinkStreamIdComboContents = null;
		private String testbedNameComboContents = null;
		private String inputFileTextContents = null;
		private String databaseSessionKeyTextContents = null;
		private String databaseSessionHostTextContents = null;
		private String stationIdComboContents = null;
		private String fswInputTypeComboContents = null;

		/**
		 * Stashes the current value of the downlink stream, testbed name,
		 * database session key and host, FSW inptu type, and station ID fields.
		 */
		public void stashFields() {

			if (SessionConfigVenueComposite.this.downlinkStreamIdCombo != null
					&& !SessionConfigVenueComposite.this.downlinkStreamIdCombo
							.isDisposed()) {
				this.downlinkStreamIdComboContents = SessionConfigVenueComposite.this.downlinkStreamIdCombo
						.getText();
			}
			if (SessionConfigVenueComposite.this.testbedNameCombo != null
					&& !SessionConfigVenueComposite.this.testbedNameCombo
							.isDisposed()) {
				this.testbedNameComboContents = SessionConfigVenueComposite.this.testbedNameCombo
						.getText();
			}
			if (SessionConfigVenueComposite.this.inputFileText != null
					&& !SessionConfigVenueComposite.this.inputFileText
							.isDisposed()) {
				this.inputFileTextContents = SessionConfigVenueComposite.this.inputFileText
						.getText().trim();
			}
			if (SessionConfigVenueComposite.this.databaseSessionKeyText != null
					&& !SessionConfigVenueComposite.this.databaseSessionKeyText
							.isDisposed()) {
				this.databaseSessionKeyTextContents = SessionConfigVenueComposite.this.databaseSessionKeyText
						.getText().trim();
			}
			if (SessionConfigVenueComposite.this.databaseSessionHostText != null
					&& !SessionConfigVenueComposite.this.databaseSessionHostText
							.isDisposed()) {
				this.databaseSessionHostTextContents = SessionConfigVenueComposite.this.databaseSessionHostText
						.getText().trim();
			}
			if (SessionConfigVenueComposite.this.stationIdCombo != null
					&& !SessionConfigVenueComposite.this.stationIdCombo
							.isDisposed()) {
				this.stationIdComboContents = SessionConfigVenueComposite.this.stationIdCombo
						.getText();
			}
			if (SessionConfigVenueComposite.this.fswInputTypeCombo != null
					&& !SessionConfigVenueComposite.this.fswInputTypeCombo
							.isDisposed()) {
				this.fswInputTypeComboContents = SessionConfigVenueComposite.this.fswInputTypeCombo
						.getText();
			}
		}

		/**
		 * Restores the current value of the downlink stream, testbed name,
		 * database session key and host, FSW input type, and station ID fields
		 * from stashed copies, if and only if the assocaited GUI widgets are
		 * still there and the stowed values are still appropriate.
		 * 
		 * The Collapsable If warning is suppressed because IMHO
		 * collapsing these IF statements make the code less readable rather
		 * than more.
		 */
		@SuppressWarnings("PMD.CollapsibleIfStatements")
		public void restoreFields() {

			if (SessionConfigVenueComposite.this.downlinkStreamIdCombo != null
					&& !SessionConfigVenueComposite.this.downlinkStreamIdCombo
							.isDisposed()
					&& this.downlinkStreamIdComboContents != null) {

				if (SessionConfigVenueComposite.this.downlinkStreamIdComboSet
						.contains(this.downlinkStreamIdComboContents)) {

					SessionConfigShellUtil
							.safeSetText(
									SessionConfigVenueComposite.this.downlinkStreamIdCombo,
									this.downlinkStreamIdComboContents);
				}
			}

			if (SessionConfigVenueComposite.this.testbedNameCombo != null
					&& !SessionConfigVenueComposite.this.testbedNameCombo
							.isDisposed()
					&& this.testbedNameComboContents != null) {

				if (SessionConfigVenueComposite.this.testbedNameComboSet
						.contains(this.testbedNameComboContents)) {

					SessionConfigShellUtil.safeSetText(
							SessionConfigVenueComposite.this.testbedNameCombo,
							this.testbedNameComboContents);
				}
			}

			if (SessionConfigVenueComposite.this.inputFileText != null
					&& !SessionConfigVenueComposite.this.inputFileText
							.isDisposed() && this.inputFileTextContents != null) {

				SessionConfigShellUtil.safeSetText(
						SessionConfigVenueComposite.this.inputFileText,
						this.inputFileTextContents);
			}

			if (SessionConfigVenueComposite.this.databaseSessionKeyText != null
					&& !SessionConfigVenueComposite.this.databaseSessionKeyText
							.isDisposed()
					&& this.databaseSessionKeyTextContents != null) {

				SessionConfigShellUtil
						.safeSetText(
								SessionConfigVenueComposite.this.databaseSessionKeyText,
								this.databaseSessionKeyTextContents);
			}

			if (SessionConfigVenueComposite.this.databaseSessionHostText != null
					&& !SessionConfigVenueComposite.this.databaseSessionHostText
							.isDisposed()
					&& this.databaseSessionHostTextContents != null) {

				SessionConfigShellUtil
						.safeSetText(
								SessionConfigVenueComposite.this.databaseSessionHostText,
								this.databaseSessionHostTextContents);
			}

			if (SessionConfigVenueComposite.this.stationIdCombo != null
					&& !SessionConfigVenueComposite.this.stationIdCombo
							.isDisposed()
					&& this.stationIdComboContents != null) {

				SessionConfigShellUtil.safeSetText(
						SessionConfigVenueComposite.this.stationIdCombo,
						this.stationIdComboContents);
			}

			if (SessionConfigVenueComposite.this.fswInputTypeCombo != null
					&& !SessionConfigVenueComposite.this.fswInputTypeCombo
							.isDisposed()
					&& this.fswInputTypeComboContents != null) {

				if (SessionConfigVenueComposite.this.inputTypeComboSet
						.contains(this.fswInputTypeComboContents)) {

					SessionConfigShellUtil.safeSetText(
							SessionConfigVenueComposite.this.fswInputTypeCombo,
							this.fswInputTypeComboContents);
				}
			}
		}
	}

	/**
	 * Class to handle venue button events.
	 * 
	 *
	 */
	private class VenueButtonSelectionHandler implements SelectionListener {
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
				internalWidgetSelected(event);
			} catch (final Exception e) {
				trace.error("Error in SessionConfigVenueComposite.VenueButtonSelectionHandler.widgetSelected handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}

		/**
		 * Actual button event processing.
		 * 
		 * @param event
		 *            the SWT SelectionEvent
		 */
		private void internalWidgetSelected(final SelectionEvent event) {

			// Note: The input file "Browse" button doubles as the "Load PVL"
			// button.
			if (event.getSource() == SessionConfigVenueComposite.this.inputFileBrowseButton) {

				final String fileName = this.util.displayStickyFileChooser(
						false, SessionConfigVenueComposite.this.getParent(),
						"inputFileBrowseButton");

				if (fileName != null) {
					SessionConfigShellUtil.safeSetText(SessionConfigVenueComposite.this.inputFileText,
							fileName);
					SessionConfigVenueComposite.this.inputFileText.setFocus();

					// If the connection type is FILE (as opposed to TDS) and
					// the data type can be recognized from the file extension,
					// set the value of the FSW Input type combo box to match
					// the input data type.
					final String text = SessionConfigVenueComposite.this.downlinkConnectionTypeCombo
							.getText();
					final TelemetryConnectionType ct = TelemetryConnectionType.valueOf(text);

					if (ct == TelemetryConnectionType.FILE) {
						for (final TelemetryInputType type : TelemetryInputType.values()) {
							if (fileName.toLowerCase().endsWith(
									type.toString().toLowerCase())) {
								SessionConfigShellUtil
										.safeSetText(
												SessionConfigVenueComposite.this.fswInputTypeCombo,
												type.toString());
								break;
							}
						}
					}
				}
			} else if (event.getSource() == SessionConfigVenueComposite.this.pvlCreateButton) {

				/*
				 *  Updated this if statement. If we have a PVL file
				 *  specified, allow the "create" button to load the file and save any changes
				 *   as a new file
				 */
				final String pvlFile = SessionConfigVenueComposite.this.inputFileText
						.getText().trim();
				final File pvl = pvlFile.isEmpty() ? null : new File(pvlFile);
				if (pvl != null && !pvl.exists()) {
					SWTUtilities.showErrorDialog(SessionConfigVenueComposite.this.getParent(), "PVL File Not Found",
							"The specified PVL file \"" + pvlFile + "\" could not be opened for editing.");
				}
				final PvlEditorShell pes = (pvl == null || !pvl.exists()) ?
                        new PvlEditorShell(SessionConfigVenueComposite.this.getParent(),
                                           appContext.getBean(GeneralProperties.class),
                                           appContext.getBean(SseContextFlag.class))
                        : new PvlEditorShell(SessionConfigVenueComposite.this.getParent(), pvl, false,
                                             appContext.getBean(GeneralProperties.class),
                                             appContext.getBean(SseContextFlag.class));

				pes.open();

				final Display mainDisplay = pes.getShell().getDisplay();
				while (!pes.getShell().isDisposed()) {
					if (!mainDisplay.readAndDispatch()) {
						mainDisplay.sleep();
					}
				}

                if (!pes.canceled) {
					SessionConfigShellUtil.safeSetText(SessionConfigVenueComposite.this.inputFileText, 
							pes.getPvlFile().getAbsolutePath());
				}

			} else if (event.getSource() == SessionConfigVenueComposite.this.pvlEditButton) {

				final String pvlFile = SessionConfigVenueComposite.this.inputFileText
						.getText().trim();
				if (pvlFile.isEmpty()) {
					SWTUtilities.showErrorDialog(
							SessionConfigVenueComposite.this.getParent(),
							"PVL File Not Entered",
							"The PVL file path has not been specified.");
					return;
				}
				final File pvl = new File(pvlFile);
                if (!pvl.exists()) {
					SWTUtilities.showErrorDialog(
							SessionConfigVenueComposite.this.getParent(),
							"PVL File Not Found", "The specified PVL file \""
									+ pvlFile
									+ "\" could not be opened for editing.");
					return;
				}

                final PvlEditorShell pes = new PvlEditorShell(SessionConfigVenueComposite.this.getParent(), pvl,
                                                              appContext.getBean(GeneralProperties.class),
                                                              appContext.getBean(SseContextFlag.class));
				pes.open();

				final Display mainDisplay = pes.getShell().getDisplay();
				while (!pes.getShell().isDisposed()) {
					if (!mainDisplay.readAndDispatch()) {
						mainDisplay.sleep();
					}
				}

				if (pes.canceled == false) {
					SessionConfigShellUtil.safeSetText(
							SessionConfigVenueComposite.this.inputFileText, pes
									.getPvlFile().getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Combined listener class for modify events on all the venue GUI widgets.
	 * This is where the real work is done in terms of responding to changes the
	 * user makes on the display.
	 */
	private class ModifyEventHandler implements ModifyListener {

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(final ModifyEvent event) {

			try {
				internalModifyText(event);
			} catch (final Exception e) {
				trace.error("Error in SessionConfigVenueComposite.ModifyEventHandler.modifyText handling: "
						+ rollUpMessages(e));

				e.printStackTrace();
			}
		}

		/**
		 * Actual modify event handling.
		 * 
		 * @param event
		 *            the SWT ModifyEvent
		 */
		private void internalModifyText(final ModifyEvent event) {

			if (event.getSource() == SessionConfigVenueComposite.this.venueTypeCombo) {
				if (SessionConfigVenueComposite.this.venueTypeCombo.getText()
						.equals("")) {
					return;
				}

				// A venue change results in rearrangement of the dynamic
				// controls in both this panel and the message service panel. Old
				// field values will be preserved if they are still valid.
				final VenueType vt = getCurrentVenue();
				drawDynamicVenueFields();
				SessionConfigVenueComposite.this.jmsDbComposite
						.drawDynamicMessageServiceFields(vt);

				if (SessionConfigVenueComposite.this.downlinkConnectionTypeCombo != null) {
					// Restrict Connection Type drop down to only
					// valid connection types for the particular Venue type
					initDownlinkConnectionTypes(vt);

					// Set the default connection type based on venue and pick
					// the first thing in the list if the default isn't valid
					// (e.g. CRUISE defaults to TDS, but standalone uplink
					// doesn't allow TDS)
					SessionConfigShellUtil
					.safeSetText(
					        SessionConfigVenueComposite.this.downlinkConnectionTypeCombo,
                                                       connectProps.getDefaultDownlinkConnectionType(vt,
                                                                                                     sseFlag.isApplicationSse())
                                                                   .toString());
					if (SessionConfigVenueComposite.this.downlinkConnectionTypeCombo
							.getSelectionIndex() == -1) {
						SessionConfigVenueComposite.this.downlinkConnectionTypeCombo
								.select(0);
					}
				}

				// Set up uplink connection type again with new venue
				populateUplinkConnectionType();

				// Set the default FSW/SSE host and port information for the
				// current configuration
				restoreFswDefaultsForVenueAndConnection();
				restoreSseDefaultsForVenueAndConnection();

			} else if (event.getSource() == SessionConfigVenueComposite.this.downlinkConnectionTypeCombo) {

				// A downlink connection type change results in rearrangement of
				// the dynamic control to this panel only
				drawDynamicVenueFields();

				final VenueType vt = getCurrentVenue();

				final String ctString = SessionConfigVenueComposite.this.downlinkConnectionTypeCombo
						.getText();
				if (!ctString.trim().isEmpty()) {

					// Special handling when connection type = FILE. An SSE
					// downlink cannot run with a FILE connection if a FSW
					// downlink is also run. Also, the downlink port needs to
					// be turned off in both FSW and SSE panels if the
					// connection
					// type is FILE.
					final TelemetryConnectionType ct = TelemetryConnectionType.valueOf(ctString);
					if (SessionConfigVenueComposite.this.sseComposite != null && !getConfigShell().isSuppressDefaulting()) {
						SessionConfigVenueComposite.this.sseComposite
								.enableFieldsForFileConnection(ct == TelemetryConnectionType.FILE);
					}
					SessionConfigVenueComposite.this.fswComposite
							.enableFieldsForFileConnection(ct == TelemetryConnectionType.FILE);
					
					DownlinkStreamType dst = null;
					if (downlinkStreamIdCombo != null) {
						final String dstStr = downlinkStreamIdCombo.getText();
						dst = DownlinkStreamType.valueOf(dstStr);
					}

					// Must reinitialize the list of FSW input types when
					// connection type changes. The old value will be
					// preserved if it is still valid.
					initInputTypes(vt, ct, dst);
					
					// Set the default FSW/SSE host and port information for the
					// current configuration because each connection type may
					// have a separate port.
					restoreFswDefaultsForVenueAndConnection();
					restoreSseDefaultsForVenueAndConnection();
				}

			} else if (event.getSource() == SessionConfigVenueComposite.this.uplinkConnectionTypeCombo) {

				final String ctString = SessionConfigVenueComposite.this.uplinkConnectionTypeCombo
						.getText();

				// If there is any valid uplink connection type at all, the
				// SSE and FSW uplink ports should be enabled.
				if (!ctString.isEmpty()) {
					SessionConfigVenueComposite.this.fswComposite
							.enableUplinkPort(true);
					if (SessionConfigVenueComposite.this.sseComposite != null) {
						SessionConfigVenueComposite.this.sseComposite
								.enableUplinkPort(true);
					}
				}

			} else if (event.getSource() == SessionConfigVenueComposite.this.downlinkStreamIdCombo) {

				final String streamVal = SessionConfigVenueComposite.this.downlinkStreamIdCombo
						.getText();

				// Change to downlink stream ID may affect the FSW input type.
				// If the stream is the command echo stream, only the CMD_ECHO
				// input type is valid.
				if (streamVal.equalsIgnoreCase("Command Echo")) {

					// If CMD_ECHO is not a choice, do not change
					if (SessionConfigVenueComposite.this.inputTypeComboSet
							.contains(TelemetryInputType.CMD_ECHO.toString())) {
						SessionConfigShellUtil
								.safeSetText(
										SessionConfigVenueComposite.this.fswInputTypeCombo,
										TelemetryInputType.CMD_ECHO
												.toString());
					}

				} else {
					// Default the input type to the default for the venue
					// for any other stream ID than command echo.
					// Not sure this is the right thing to do but
					// can't find anything wrong yet, either.
					final VenueType vt = getCurrentVenue();
					SessionConfigShellUtil.safeSetText(
							SessionConfigVenueComposite.this.fswInputTypeCombo,
                                                       connectProps.getDefaultSourceFormat(vt,
                                                                                           sseFlag.isApplicationSse())
                                                                   .toString());
				}

				// Set the default FSW/SSE host and port information for the
				// current configuration because each stream may
				// have a separate port.
				restoreFswDefaultsForVenueAndConnection();
				restoreSseDefaultsForVenueAndConnection();

			} else if (event.getSource() == SessionConfigVenueComposite.this.testbedNameCombo) {

				// Set the default FSW/SSE host and port information for the
				// current configuration because each testbed may have
				// different ports.
				restoreFswDefaultsForVenueAndConnection();
				restoreSseDefaultsForVenueAndConnection();
			} else if(event.getSource() == SessionConfigVenueComposite.this.fswInputTypeCombo) {
				restoreFswDefaultsForVenueAndConnection();
			}
		}
	}
}
