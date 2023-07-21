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
package jpl.gds.tcapp.app.gui.icmd.model;

import org.eclipse.swt.widgets.Shell;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.icmd.CpdResponse;
import jpl.gds.tc.api.icmd.CpdTriggerEvent;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.ICmdException;
import jpl.gds.tcapp.app.gui.UplinkExecutors;
import jpl.gds.tcapp.app.gui.icmd.ICpdParametersChangeListener;

/**
 * A controller class responds to user changes to CPD parameters in the GUI and
 * takes the appropriate action.
 *
 * @since AMPCS R3
 */
public class CpdParametersController implements ICpdParametersChangeListener {
	/** The client class used to communicate with CPD */
	private final ICpdClient client;

	/** The data model representing the most up-to-date state of CPD parameters */
	private final CpdParametersModel model;
	
	private final Shell parentShell;

	/**
	 * Constructor
	 *
	 * @param model the data model representing CPD parameters
	 * @throws ICmdException if there is an error initializing the CpdClient
	 *             class
	 */
	public CpdParametersController(final ICpdClient cpdClient, final Shell parentShell, final CpdParametersModel model) {
	    this.parentShell = parentShell;
		this.model = model;
        this.client = cpdClient;
	}

	/**
	 * Sets the execution mode on the CPD server and afterwards perform a full
	 * refresh of this data model class to retrieve updated values.
	 *
	 * @param execMode the new ExecutionMode to set the CPD server to
	 * @param when when to set the ExecutionMode
	 * @return true if successful
	 * @throws ICmdException if there is an error setting the ExecutionMode on
	 *             the CPD server or an error performing refresh on this data
	 *             model.
	 */
	public boolean setExecutionMode(final ExecutionMode execMode, final CpdTriggerEvent when)
			throws ICmdException {
		final CpdResponse resp = this.client.setExecutionMode(execMode, when);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Sets the execution state on the CPD server and afterwards perform a full
	 * refresh of this data model class to retrieve updated values.
	 *
	 * @param execState the new ExecutionStateRequest to set the CPD server to
	 * @param when when to set the ExecutionState
	 * @return true if successful
	 * @throws ICmdException if there is an error setting the ExecutionState on
	 *             the CPD server or an error performing refresh on this data
	 *             model.
	 */
	public boolean setExecutionState(final ExecutionStateRequest execState,
			final CpdTriggerEvent when) throws ICmdException {
		final CpdResponse resp = this.client.setExecutionState(execState, when);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Sets the execution method on the CPD server and afterwards perform a full
	 * refresh of this data model class to retrieve updated values.
	 *
	 * @param execMethod the new ExecutionMethod to set the CPD server to
	 * @param when when to set the ExecutionMethod
	 * @return true if successful
	 * @throws ICmdException if there is an error setting the ExecutionMethod on
	 *             the CPD server or an error performing refresh on this data
	 *             model.
	 */
	public boolean setExecutionMethod(final ExecutionMethod execMethod,
			final CpdTriggerEvent when) throws ICmdException {
		final CpdResponse resp = this.client.setExecutionMethod(execMethod, when);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Sets the aggregation method on the CPD server and afterwards perform a
	 * full refresh of this data model class to retrieve updated values.
	 *
	 * @param aggMethod the new AggregationMethod to set the CPD server to
	 * @return true if successful
	 * @throws ICmdException if there is an error setting the AggregationMethod
	 *             on the CPD server or an error performing refresh on this data
	 *             model.
	 */
	public boolean setAggregationMethod(final AggregationMethod aggMethod)
			throws ICmdException {
		final CpdResponse resp = this.client.setAggregationMethod(aggMethod);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Connect to a station
	 *
	 * @param stationId the ID of the station to connect to
	 * @return true if connection is successful
	 * @throws ICmdException if connection is not successful
	 */
	public boolean connectToStation(final String stationId) throws ICmdException {
		final CpdResponse resp = this.client.connectToStation(stationId);
		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Disconnect from station
	 *
	 * @return true if disconnect is successful
	 * @throws ICmdException if disconnect is not successful
	 */
	public boolean disconnectFromStation() throws ICmdException {
		final CpdResponse resp = this.client.disconnectFromStation();

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Set the station bit rate on CPD
	 *
	 * @param bitRate the bit rate to tell CPD that the station is configured at
	 * @return true if setting the bit rate on CPD is successful
	 * @throws ICmdException if setting the bit rate on CPD fails
	 */
	public boolean setBitRate(final double bitRate) throws ICmdException {
		final CpdResponse resp = this.client.setBitRate(bitRate);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/**
	 * Set the preparation state on CPD
	 *
	 * @param prepState the preparation state to set CPD to
	 * @return true if successful
	 * @throws ICmdException if setting preparation state fails
	 */
	public boolean setPreparationState(final ListPreparationStateEnum prepState)
			throws ICmdException {
		final CpdResponse resp = this.client.setPreparationState(prepState);

		if (resp.isSuccessful()) {
			return true;
		} else {
			throw new ICmdException(resp.getDiagnosticMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#onExecutionModeChange
	 * (gov.nasa.jpl.icmd.schema.ExecutionMode)
	 */
	@Override
	public void onExecutionModeChange(final ExecutionMode newMode) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this.setExecutionMode(newMode,
							CpdTriggerEvent.IMMEDIATELY);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(),
							"CPD Control Panel: Set Execution Mode",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
											parentShell,
											"Error",
											"Unable to set execution mode: "
													+ e.getMessage());
								}

							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#onExecutionStateChange
	 * (gov.nasa.jpl.icmd.schema.ExecutionStateRequest)
	 */
	@Override
	public void onExecutionStateChange(final ExecutionStateRequest newState) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this.setExecutionState(newState,
							CpdTriggerEvent.IMMEDIATELY);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(),
							"CPD Control Panel: Set Execution State",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
											parentShell,
											"Error",
											"Unable to set execution state: "
													+ e.getMessage());
								}

							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#
	 * onExecutionMethodChange(gov.nasa.jpl.icmd.schema.ExecutionMethod)
	 */
	@Override
	public void onExecutionMethodChange(final ExecutionMethod newMethod) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this.setExecutionMethod(newMethod,
							CpdTriggerEvent.IMMEDIATELY);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(),
							"CPD Control Panel: Set Execution Method",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
											parentShell,
											"Error",
											"Unable to set execution method: "
													+ e.getMessage());
								}

							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#
	 * onConnectionStatusChange(boolean, java.lang.String)
	 */
	@Override
	public void onConnectionStatusChange(final boolean connect,
			final String dssId) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (connect) {
						CpdParametersController.this.connectToStation(dssId);
					} else {
						CpdParametersController.this.disconnectFromStation();
					}
				} catch (final ICmdException e) {
					final String errorMessage = connect ? "Encountered error while attempting to connect to "
							+ dssId + ": " + e.getMessage()
							: "Encountered error while attempting to disconnect: "
									+ e.getMessage();

					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(), "CPD Control Panel: Manual Control",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
									        parentShell,
											"Error", errorMessage);
								}

							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#onBitRateChange
	 * (double)
	 */
	@Override
	public void onBitRateChange(final double newBitRate) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this.setBitRate(newBitRate);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(), "CPD Control Panel: Manual Control",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
									        parentShell,
											"Unable to set bit rate",
											"Error encountered when attempting to set bit rate: "
													+ e.getMessage());
								}
							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#
	 * onPreparationStateChange
	 * (gov.nasa.jpl.icmd.schema.ListPreparationStateEnum)
	 */
	@Override
	public void onPreparationStateChange(final ListPreparationStateEnum newState) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this.setPreparationState(newState);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(),
							"CPD Control Panel: List Preparation",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
									        parentShell,
											"Unable to set list preparation state",
											"Error encountered when attempting to set list preparation state: "
													+ e.getMessage());
								}
							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tcapp.icmd.gui.ICpdParametersChangeListener#
	 * onAggregationMethodChange(gov.nasa.jpl.icmd.schema.AggregationMethod)
	 */
	@Override
	public void onAggregationMethodChange(
			final AggregationMethod newAggregationMethod) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CpdParametersController.this
							.setAggregationMethod(newAggregationMethod);
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(),
							"CPD Control Panel: List Preparation",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
									        parentShell,
											"Unable to set aggregation method",
											"Error encountered when attempting to set aggregation method: "
													+ e.getMessage());
								}
							});
				} finally {
					CpdParametersController.this.model.refresh();
				}
			}
		});
	}
}
