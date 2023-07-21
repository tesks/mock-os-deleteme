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
package jpl.gds.tcapp.app.gui.icmd;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.ExecutionMode;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.icmd.datastructures.CpdConnectionStatus;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersModel;

/**
 * This is a UI widget that allows the user to control CPD in manual mode.
 * 
 * @since AMPCS R3
 */
public class CpdStationConnectionView extends ContentViewer {
	private final Tracer logger;

	private static final String DISCONNECT_IMAGE_PATH = "jpl/gds/tcapp/icmd/gui/disconnect-icon.png";

	private final Group contentsGroup;
	private Composite manualModeComposite;
	private EditableItem connectionState;
	private EditableItem bitRate;
	private EditableItem executionMode;
	private final ApplicationContext appContext;

	protected List<ICpdParametersChangeListener> listeners;

	/**
	 * Constructor
	 * 
	 * @param parent the parent composite
	 * @param style the SWT style
	 */
	public CpdStationConnectionView(final ApplicationContext appContext, final Composite parent, final int style) {
		this.appContext = appContext;
        this.logger = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);
		this.listeners = new LinkedList<ICpdParametersChangeListener>();
		this.contentsGroup = new Group(parent, SWT.NONE);

		final GridLayout layout = new GridLayout();
		this.contentsGroup.setLayout(layout);

		createControls();
	}

	protected void createControls() {
		this.contentsGroup.setText("Station Connection");

		this.executionMode = new EditableItem(this.contentsGroup, SWT.NONE);
		this.executionMode.setReadOnly(true);
		this.executionMode.setLabel("Execution Mode:");

		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		this.executionMode.setLayoutData(gd);

		this.manualModeComposite = new Composite(this.contentsGroup, SWT.BORDER);

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		this.manualModeComposite.setLayoutData(gd);

		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		this.manualModeComposite.setLayout(layout);

		this.connectionState = new EditableItem(this.manualModeComposite,
				SWT.NONE, DISCONNECT_IMAGE_PATH, new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						final MessageBox confirmDialog = new MessageBox(
								CpdStationConnectionView.this.getControl()
										.getShell(), SWT.YES | SWT.NO);
						confirmDialog
								.setMessage("Are you sure you want to disconnect from the station?");
						confirmDialog.setText("Disconnect?");

						final int returnCode = confirmDialog.open();

						if (returnCode == SWT.YES) {
							CpdStationConnectionView.this.notifyListeners(
									false, null);
						}
					}
				});

		this.connectionState.setAuxiliaryButtonToolTip("Disconnect");
		this.connectionState.setLabel("Connection State:");
		
		final MissionProperties missionProps = appContext.getBean(MissionProperties.class);

		/*
		 * Replaced ConfiguredDssIds access with
		 * StationMapper access to consolidate station configuration.
		 */
		final String[] stations = missionProps.getStationMapper().getStationStrings();

		if (stations == null) {
			logger.error("No Stations Configured: Error retrieving configured uplink stations");
		} else {
			this.connectionState.setEditValues(stations);
		}

		this.connectionState.setEditLabel("Connect to:");

		this.connectionState.addEditListener(new IEditListener() {
			@Override
			public void onEdit(final EditEvent event) {
				notifyListeners(true, event.getNewValue());
			}
		});

		this.bitRate = new EditableItem(this.manualModeComposite, SWT.NONE);
		this.bitRate.setLabel("Station Bit Rate (bps):");

		final String[] bitRates = missionProps.getAllowedUplinkBitrates().toArray(new String[] {});

		if (bitRates.length == 0) {
			logger.error("No Valid Bit Rates Configured: Error retrieving configured bit rates");
		} else {
			this.bitRate.setEditValues(bitRates);
		}

		this.bitRate.setComparator(new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				try {
					final double d1 = Double.parseDouble(o1);
					final double d2 = Double.parseDouble(o2);

					if (d1 < d2) {
						return -1;
					} else if (d1 == d2) {
						return 0;
					} else {
						return 1;
					}

				} catch (final Exception e) {
					throw new ClassCastException("Unable to compare bit rates "
							+ o1 + " and " + o2);
				}
			}
		});

		this.bitRate.addEditListener(new IEditListener() {
			@Override
			public void onEdit(final EditEvent event) {
				for (final ICpdParametersChangeListener l : CpdStationConnectionView.this.listeners) {
					double bitRate = 0;
					try {
						bitRate = Double.parseDouble(event.getNewValue());
					} catch (final Exception e) {
						logger.error("Invalid bit rate: Cannot set station rate to "
								+ event.getNewValue());
						return;
					}

					l.onBitRateChange(bitRate);
				}
			}
		});

		// layout widgets
		gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		this.connectionState.setLayoutData(gd);

		gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		this.bitRate.setLayoutData(gd);
	}

	public void addCpdParametersChangeListener(
			final ICpdParametersChangeListener listener) {
		this.listeners.add(listener);
	}

	public void removeCpdParametersChangeListener(
			final ICpdParametersChangeListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public ISelection getSelection() {
		// no-op
		return null;
	}

	@Override
	public void refresh() {
		final IContentProvider provider = this.getContentProvider();

		if (provider instanceof CpdParametersModel) {
			final CpdParametersModel model = (CpdParametersModel) provider;

			final boolean stale = model.isStale();

			if (stale) {
				this.connectionState.setValueColor(this.getControl()
						.getDisplay().getSystemColor(SWT.COLOR_GRAY));
				this.bitRate.setValueColor(this.getControl().getDisplay()
						.getSystemColor(SWT.COLOR_GRAY));
				this.executionMode.setValueColor(this.getControl().getDisplay()
						.getSystemColor(SWT.COLOR_GRAY));
			} else {
				this.connectionState.setValueColor(this.getControl()
						.getDisplay().getSystemColor(SWT.COLOR_BLUE));
				this.bitRate.setValueColor(this.getControl().getDisplay()
						.getSystemColor(SWT.COLOR_BLUE));
				this.executionMode.setValueColor(this.getControl().getDisplay()
						.getSystemColor(SWT.COLOR_BLUE));
			}

			final ExecutionMode execMode = model.getExecutionMode();

			if (execMode != null) {
				this.executionMode.setValue(execMode.toString());
				if (execMode.equals(ExecutionMode.MANUAL)) {
					this.manualModeComposite.setVisible(true);
				} else {
					this.manualModeComposite.setVisible(false);
				}
			} else {
				this.executionMode.setValue("UNKNOWN");
			}

			final String bitRate = model.getBitRate() >= 0 ? Double.toString(model
					.getBitRate()) : "ERROR";
			this.bitRate.setValue(bitRate);

			final CpdConnectionStatus connectionStatus = model.getConnectionStatus();

			final String connectionStr = connectionStatus == null ? "UNKNOWN"
					: connectionStatus.toString();

			if (connectionStatus != null) {
				if (connectionStatus.isConnected()) {
					// show bitrate when connected
					this.bitRate.setVisible(true);

					// show disconnect button when not disconnected or
					// disconnecting
					this.connectionState.setEditButtonVisibility(false);
				} else if (connectionStatus.isPending()) {
					// don't show bitrate when not connected
					this.bitRate.setVisible(false);
					
					// while pending, we need a way to disconnect (stop waiting
					// for connection)
					this.connectionState.setEditButtonVisibility(false);
				} else { // not connected
					// don't show bitrate when not connected
					this.bitRate.setVisible(false);

					// don't show disconnect button when disconnected or
					// disconnecting
					this.connectionState.setEditButtonVisibility(true);
				}
			}

			this.connectionState.setValue(connectionStr);

			this.manualModeComposite.layout();

		} else {
			return;
		}

		this.contentsGroup.layout();
	}

	@Override
	public void setSelection(final ISelection selection, final boolean reveal) {
		// no-op

	}

	private void notifyListeners(final boolean connect, final String dssId) {
		for (final ICpdParametersChangeListener l : this.listeners) {
			l.onConnectionStatusChange(connect, dssId);
		}
	}

	@Override
	public Control getControl() {
		return this.contentsGroup;
	}
}
