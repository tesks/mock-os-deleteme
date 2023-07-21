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

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.tc.api.icmd.CpdServiceUriUtil;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.CpdConnectionException;
import jpl.gds.tcapp.app.gui.UplinkExecutors;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersController;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersModel;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersModel.CpdParameterType;
import jpl.gds.tcapp.app.gui.icmd.model.CpdRequestTableController;

/**
 * This class defines a shell that serves as a control panel for the CPD
 * (Command Preparation and Delivery) subsystem.
 *
 * @since AMPCS R3
 */
public class CpdControlPanel extends Dialog
{
	/** The shell of this control panel. */
	private static final AtomicReference<Shell> STATIC_SHELL =
        new AtomicReference<Shell>(null);

	/** The title */
	private static final String TITLE = "CPD Control Panel";

    /** Time to pause between failed pings */
    private static final long PING_DELAY = 5000L;

	/** The default width of the shell */
	public static final int DEFAULT_WIDTH = 650;

	/** The default height of the shell */
	public static final int DEFAULT_HEIGHT = 1000;

	/** The parent shell */
	private final Shell parentShell;

	/**
	 * The CPD request table.
     * Consists of Request Pool and Radiation List tables.
     *
     * PMD is correct that this looks like it could be
     * a local variable. But I think it needs to stick around and thus needs
     * to be referenced somewhere.
	 */
    @SuppressWarnings("PMD.SingularField")
	private CpdRequestTable requestTable = null;

	/** The data model that stores the current values of the CPD parameters */
	private final CpdParametersModel cpdParamModel;

	/** The header manager that is responsible for the control panel header */
	private HeaderManager headerManager;

	/** The control panel header */
	private CpdHeaderView header;

	private ApplicationContext appContext;
	
	private CpdServiceUriUtil cpdUriUtil;

	/**
	 * Constructor.
     *
     * @param parent Parent shell
	 */
	public CpdControlPanel(final ApplicationContext appContext, final Shell parent) {
		super(new IShellProvider() {
			@Override
			public Shell getShell() {
				return null;
			}
		});
		this.appContext = appContext;
		this.cpdUriUtil = new CpdServiceUriUtil(this.appContext);
		
		this.setShellStyle(SWT.TITLE | SWT.RESIZE);
		this.setBlockOnOpen(false);
		this.parentShell = parent;
		/*  shell is null, pass parent shell instead */
		this.cpdParamModel = new CpdParametersModel(appContext, parentShell);
	}

	/**
	 * Get the main shell
	 *
	 * @return Shell
	 */
	public static Shell getStaticShell()
    {
		return STATIC_SHELL.get();
	}

	/**
	 * Create the contents of the control panel.
     *
     * @param parent Parent composite
     *
     * @return New Control
	 */
	@Override
    protected Control createContents(final Composite parent)
    {
        STATIC_SHELL.set(getShell());

		final String host = "PENDING";

		this.getShell().setText(TITLE);
		this.getShell().setSize(DEFAULT_WIDTH, this.parentShell.getSize().y);
		this.getShell().setLocation(
				this.parentShell.getLocation().x + this.parentShell.getSize().x
						+ 1, this.parentShell.getLocation().y);

		final GridLayout gl = new GridLayout();
		parent.setLayout(gl);

		header = new CpdHeaderView(appContext.getBean(AccessControlParameters.class), parent, SWT.NONE);
		header.setHostText(host);
		header.setContentProvider(cpdParamModel);
		header.setInput(new CpdParameterType[] { CpdParameterType.CONNECTION,
				CpdParameterType.ROLE });
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		header.getControl().setLayoutData(gd);
		
		final ICpdClient cpdClient = appContext.getBean(ICpdClient.class);

		UplinkExecutors.genericExecutor.execute(
        new Runnable()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run()
            {
                // Ping the CPD server

                while (true)
                {
                    try
                    {
                        if (cpdClient.pingCommandService())
                        {
                            performSuccessfulPingActions(cpdUriUtil, header);

                            // Exit loop, we're done
                            break;
                        }
                    }
                    catch (final CpdConnectionException cce)
                    {
                        // This is the exception we get
                        // if the client does not exist
                        // (like for a bad host or port).
                        // There's nothing to do except retry.
                    }
                    catch (final Exception e)
                    {
                        // This exception is not expected at all

                        showPingErrorDialog(e);

                        // Exit loop, we give up
                        break;
                    }

                    // Wait a while and try again.
                    // We get here if the ping returned false or if
                    // we got a CpdConnectionException.

                    SleepUtilities.checkedSleep(PING_DELAY);
                }
            }
        });

		final Composite bodyComposite = new Composite(parent, SWT.NONE);
		gd = new GridData(GridData.FILL_BOTH);
		bodyComposite.setLayoutData(gd);

		final GridLayout layout = new GridLayout();
		bodyComposite.setLayout(layout);

		CpdRequestTableController reqPoolCtrl = null;
		CpdParametersController parametersCtrl = null;

		reqPoolCtrl = new CpdRequestTableController(cpdClient, STATIC_SHELL.get());
		parametersCtrl = new CpdParametersController(cpdClient, STATIC_SHELL.get(), cpdParamModel);
		
		final CpdStationConnectionView manualCtrl = new CpdStationConnectionView(
				appContext, bodyComposite, SWT.NONE);
		manualCtrl.setContentProvider(cpdParamModel);
		manualCtrl.addCpdParametersChangeListener(parametersCtrl);
		manualCtrl.setInput(new CpdParameterType[] {
				CpdParameterType.CONNECTION, CpdParameterType.CONFIGURATION,
				CpdParameterType.BITRATE });
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		manualCtrl.getControl().setLayoutData(gd);

		final Group requestTableArea = new Group(bodyComposite, SWT.NONE);
		requestTableArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		requestTableArea.setText("Uplink Requests");

		final FillLayout fl = new FillLayout();
		fl.type = SWT.VERTICAL;
		requestTableArea.setLayout(fl);

		requestTable = new CpdRequestTable(appContext, cpdParamModel, requestTableArea, SWT.NONE);
		requestTable.addCpdRequestChangeListener(reqPoolCtrl);
		requestTable.setContentProvider(cpdParamModel);
		requestTable.addCpdParameterChangeListener(parametersCtrl);
		requestTable.setInput(CpdParameterType.CONFIGURATION);

		return parent;
	}


    /**
     * Perform actions for successful ping.
     * Set the host text in the header.
     *
     * @param header CPD header view
     */
    private static void performSuccessfulPingActions(
    		                final CpdServiceUriUtil uriUtil,
                            final CpdHeaderView header)
    {

        final String host = uriUtil.getCpdServerUrl();

        SWTUtilities.safeAsyncExec(
            getStaticShell().getDisplay(),
            "CPD Host Ping",
            new Runnable()
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run()
                {
                    header.setHostText(host);
                }
            });
    }


    /**
     * Pop up an error dialog for ping failure.
     *
     * @param e Exception causing the trouble
     */
    private static void showPingErrorDialog(final Exception e)
    {

        final String message = e.getLocalizedMessage();
        final Shell  shell   = getStaticShell();

        SWTUtilities.safeAsyncExec(
            shell.getDisplay(),
            "Error pinging CPD server",
            new Runnable()
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run()
                {
                    SWTUtilities.showErrorDialog(
                        shell,
                        "Error pinging CPD server",
                        message);
                }
            });
    }


	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Window#handleShellCloseEvent()
	 */
	@Override
	protected void handleShellCloseEvent() {
		SWTUtilities
				.showWarningDialog(
						this.getShell(),
						"Do not close CPD Control Panel",
						"You should not be closing this window.\nIf you wish to exit chill_up please close the main chill_up window.");
	}

	/**
	 * Set the header manager
	 *
	 * @param headerManager Header manager
	 */
	public void setHeaderManager(final HeaderManager headerManager) {
		this.headerManager = headerManager;
	}

	/**
	 * Get the header manager for the CPD control panel
	 *
	 * @return header manager
	 */
	public HeaderManager getHeaderManager() {
		return this.headerManager;
	}

	/**
	 * Get the CPD header view
	 *
	 * @return the CPD header view
	 */
	public CpdHeaderView getHeader() {
		return header;
	}

}
