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

import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.icmd.CpdResponse;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.ICmdException;
import jpl.gds.tcapp.app.gui.UplinkExecutors;
import jpl.gds.tcapp.app.gui.icmd.ICpdRequestChangeListener;

/**
 * A controller class responds to user events on the CPD request tables in the
 * GUI and takes the appropriate action.
 *
 * @since AMPCS R3
 */
public class CpdRequestTableController implements ICpdRequestChangeListener {
	/** Logger */
	private static final Tracer logger = TraceManager.getDefaultTracer();


	/** The client class used to communicate with CPD */
	private final ICpdClient client;
	
	private final Shell parentShell;

	/**
     * Constructor
     * 
     * @param cpdClient
     *            the CPD Client
     * @param parent
     *            the SWT GUI Shell
     */
	public CpdRequestTableController(final ICpdClient cpdClient, final Shell parent) {
	    this.client = cpdClient;		
		this.parentShell = parent;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void onRequestDelete(final String requestId,
			final CommandUserRole requestRole) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final CpdResponse resp = CpdRequestTableController.this.client
							.deleteRadiationRequest(requestId, requestRole,
									false);

					if (!resp.isSuccessful()) {
						SWTUtilities.safeAsyncExec(parentShell.getDisplay(),
								"CPD Control Panel: Delete Request",
								new Runnable() {

									@Override
									public void run() {
										SWTUtilities.showErrorDialog(
												parentShell,
												"Error deleting request on CPD server",
												"Unable to delete request");

										logger.error("CPD Request Delete Failed: "
												+ resp.getDiagnosticMessage());
									}
								});
					}
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(), "CPD Control Panel: Delete Request",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
											parentShell,
											"Error deleting request on CPD server",
											e.getMessage());

									logger.error("CPD Request Delete Failed: "
											+ e.getMessage());
								}
							});
				}
			}
		});
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void onRequestFlush(final CommandUserRole rolePool) {
		UplinkExecutors.genericExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final CpdResponse resp = CpdRequestTableController.this.client
							.flushRequests(rolePool, false);

					if (!resp.isSuccessful()) {
						SWTUtilities.safeAsyncExec(parentShell.getDisplay(),
								"CPD Control Panel: Flush Requests",
								new Runnable() {

									@Override
									public void run() {
										final String rolePoolMessage = rolePool == null ? ""
												: "from " + rolePool.toString()
														+ " role pool";
										SWTUtilities.showErrorDialog(
										        parentShell,
												"Error flushing requests",
												"Unable to flush requests "
														+ rolePoolMessage
														+ " on CPD server");

										logger.error("CPD Request Flush Failed: "
												+ resp.getDiagnosticMessage());
									}
								});
					}
				} catch (final ICmdException e) {
					SWTUtilities.safeAsyncExec(parentShell
							.getDisplay(), "CPD Control Panel: Flush Requests",
							new Runnable() {

								@Override
								public void run() {
									SWTUtilities.showErrorDialog(
									        parentShell,
											"Error flushing requests on CPD server",
											e.getMessage());

									logger.error("CPD Request Flush Failed: "
											+ e.getMessage());
								}
							});
				}
			}
		});
	}
}
