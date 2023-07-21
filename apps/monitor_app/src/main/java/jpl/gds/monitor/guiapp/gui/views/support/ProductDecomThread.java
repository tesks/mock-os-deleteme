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
package jpl.gds.monitor.guiapp.gui.views.support;

import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.product.api.DpViewAppConstants;
import jpl.gds.product.api.decom.IProductDecom;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.*;
import jpl.gds.shared.swt.ProgressBarShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Timer;

/**
 * ProductDecomThread is a Runnable support class used to spawn chill_dp_view on a data
 * product file in chill_monitor. It is generally used by the Product Status view.
 *
 */
public class ProductDecomThread implements Runnable {
	
	/**
	 * Enum to define chill_dp_view launch options.
	 *
	 */
	public enum LaunchType {
		/**
		 * Decom product to text and save to file
		 */
		TEXT_VIEW_TO_FILE,
		
		/**
		 * Decom product to text and display in GUI window
		 */
		TEXT_VIEW_TO_WINDOW,
		
		/**
		 *  Launch DPO viewers only
		 */
		LAUNCH_DPO_VIEWERS,
		
		/**
		 * Launch product viewer only
		 */
		LAUNCH_PRODUCT_VIEWER
	}
	
	private final int maxDecomDisplayLines;
	private final String dpScriptName;
    private static final String GDS_DIRECTORY = GdsSystemProperties.getGdsDirectory();
    
	private final LaunchType type;
    private final String decomFile;
    private final String saveFile;
    private ProgressBarShell progressShell;
    private Timer progressTimer;
    private final Shell parent;
    
    private final Tracer trace;
	
    /**
     * Constructor.
     * 
     * @param appContext
     *            The current application context
     * @param type
     *            the type of dp_view launch desired, one of the LaunchType
     *            enums
     * @param decomFilename
     *            full path to data product file
     * @param saveFilename
     *            full path to decom save file, or null if no save requested
     * @param parent
     *            parent SWT Shell object
     */
    public ProductDecomThread(final ApplicationContext appContext, 
    		final LaunchType type, final String decomFilename, final String saveFilename, final Shell parent) {
    	this.type = type;
    	decomFile = decomFilename;
    	saveFile = saveFilename;
    	this.parent = parent;
    	maxDecomDisplayLines = appContext.getBean(MonitorGuiProperties.class).getProductMaxViewLines();
    	dpScriptName = appContext.getBean(MonitorGuiProperties.class).getProductViewScriptName();
        trace = TraceManager.getTracer(appContext, Loggers.PRODUCT_DECOM);
    }
    
    /**
     * Starts the progress bar for an upcoming launch operation. The progress bar
     * will be destroyed automatically when the launch completes.
     */
    public void startProgress() {
    	progressShell = null;
    	progressShell = new ProgressBarShell(parent);

    	progressShell.getShell().setText("Processing Data Product...");
    	progressShell.getProgressLabel().setText("Processing data product...");
    	progressShell.getProgressBar().setMinimum(0);
    	progressShell.getProgressBar().setMaximum(100);
    	progressShell.getProgressBar().setSelection(0);
    	progressShell.getShell().setSize(600,50);
    	final Point p = parent.getLocation();
    	progressShell.getShell().setLocation(p);

    	progressShell.open();
    	if (progressTimer != null) {
    		progressTimer.cancel();
    	}

    	progressTimer = new Timer();
    	SWTUtilities.startProgressBarUpdate(progressShell, progressTimer, 100, 5);
    }
    
    /**
     * {@inheritDoc}
	 * @see java.lang.Runnable#run()
     */
    @Override
	public void run() {
    	try {
    		switch (type) {
    		case TEXT_VIEW_TO_FILE:
    			launchProductTextToFile();
    			break;
    		case TEXT_VIEW_TO_WINDOW:
    			launchProductTextToWindow();
    			break;
    		case LAUNCH_DPO_VIEWERS:
    			launchDpoViewers();
    			break;
    		case LAUNCH_PRODUCT_VIEWER:
    			launchProductViewer();
    			break;
    		}
    	} catch (final Exception e) {
    		e.printStackTrace();
    		notifyComplete(e.toString());
    	}
    }

    private void notifyComplete(final String text) {
    	
    	SWTUtilities.runInDisplayThread(new Runnable() {
    		/**
    		 * @see java.lang.Runnable#run()
    		 */
    		@Override
			public void run() {
    			progressTimer.cancel();
    			progressTimer = null;
    			progressShell.dispose();
    			progressShell = null;

    			if (text != null) {
    				SWTUtilities.showErrorDialog(parent, "Product Processing Error", text);
    			}
    		}
    	});
    }
    
	private void launchProductTextToFile() {

		final File gdsDir = new File(GDS_DIRECTORY);
		final String fullPath = gdsDir.getAbsolutePath();

		final String scriptName = fullPath + "/bin/" + dpScriptName;

		try {
			final FileLineHandler stdoutFile = new FileLineHandler(new File(saveFile));
			final FileLineHandler stderrFile = new FileLineHandler(new File(saveFile));

			final ProcessLauncher launcher = new ProcessLauncher();
			launcher.setErrorHandler(stderrFile);
			launcher.setOutputHandler(stdoutFile);
			final String[] args = new String[] {scriptName, 
					"--" + DpViewAppConstants.IGNORE_CHECKSUM_LONG,
					decomFile};

			;
            if (!launcher.launch(args, GdsSystemProperties.getSystemProperty("user.dir"))) {
				notifyComplete("Unable to launch " + dpScriptName + " process");
				return;
			}
			final int status = launcher.waitForExit();
			if (status == IProductDecom.NO_PRODUCT_DEF) {
			    notifyComplete(dpScriptName + " could not produce text because no product definition (dictionary) could be found for the selected data product");
			} else if (status != IProductDecom.SUCCESS) {
				notifyComplete(dpScriptName + " process returned an error code: " + status);
			} else {
		        notifyComplete(null);
			}
			launcher.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
			notifyComplete("Unable to launch " + dpScriptName + " process");
		}
	}
	
	private void launchProductViewer() {
		
		final File gdsDir = new File(GDS_DIRECTORY);
		final String fullPath = gdsDir.getAbsolutePath();

		final String scriptName = fullPath + "/bin/" + dpScriptName;

		try {
			final ProcessLauncher launcher = new ProcessLauncher();
			launcher.setErrorHandler(new StderrLineHandler());
			launcher.setOutputHandler(new StdoutLineHandler());
			
			final String[] args = new String[] {scriptName, 
					"--" + DpViewAppConstants.LAUNCH_PRODUCT_VIEWER_LONG,
					"--" + DpViewAppConstants.SHOW_LAUNCH_LONG,
					"--" + DpViewAppConstants.NOTEXT_OPTION_LONG,
					"--" + DpViewAppConstants.IGNORE_CHECKSUM_LONG,
					decomFile};

            if (!launcher.launch(args, GdsSystemProperties.getSystemProperty("user.dir"))) {
				notifyComplete("Unable to launch " + dpScriptName + " process");
				return;
			}
			final int status = launcher.waitForExit();
			if (status == IProductDecom.NO_PRODUCT_DEF) {
                notifyComplete(dpScriptName + " could not produce text because no product definition (dictionary) could be found for the selected data product");
            } else if (status == IProductDecom.NO_PROD_VIEWER) {
				notifyComplete("There is no product viewer defined");
			} else if (status != IProductDecom.SUCCESS) {
				notifyComplete(dpScriptName + " process returned an error code: " + status);
			} else {
				notifyComplete(null);
			}
		
		} catch (final Exception e) {
			e.printStackTrace();
			notifyComplete("Unable to launch " + dpScriptName + " process");
		}
	}
	
	private void launchDpoViewers() {

		final File gdsDir = new File(GDS_DIRECTORY);
		final String fullPath = gdsDir.getAbsolutePath();

		final String scriptName = fullPath + "/bin/" + dpScriptName;

		try {
			final ProcessLauncher launcher = new ProcessLauncher();
			launcher.setErrorHandler(new StderrLineHandler());
			launcher.setOutputHandler(new StdoutLineHandler());
			
			final String[] args = new String[] {scriptName, 
					"--" + DpViewAppConstants.LAUNCH_DPO_VIEWER_LONG, 
					"--" + DpViewAppConstants.SHOW_LAUNCH_LONG,
					"--" + DpViewAppConstants.NOTEXT_OPTION_LONG,
					"--" + DpViewAppConstants.IGNORE_CHECKSUM_LONG,
					decomFile};

            if (!launcher.launch(args, GdsSystemProperties.getSystemProperty("user.dir"))) {
				notifyComplete("Unable to launch " + dpScriptName + " process");
				return;
			}
			final int status = launcher.waitForExit();
			if (status == IProductDecom.NO_PRODUCT_DEF) {
	            notifyComplete(dpScriptName + " could not produce text because no DPO definitions (dictionary) could be found for the selected data product");
			} else if (status == IProductDecom.NO_DPO_VIEWER) {
				notifyComplete("There are no DPO viewers defined");
			} else if (status != IProductDecom.SUCCESS) {
				notifyComplete(dpScriptName + " process returned an error code: " + status);
			} else {
				notifyComplete(null);
			}
		
		} catch (final Exception e) {
			e.printStackTrace();
			notifyComplete("Unable to launch " + dpScriptName + " process");
		}
	}

	private void launchProductTextToWindow() {

		final File gdsDir = new File(GDS_DIRECTORY);
		final String fullPath = gdsDir.getAbsolutePath();

		final String scriptName = fullPath + "/bin/" + dpScriptName;

		try {
			final StringBuffer decomBuffer = new StringBuffer();
			final ProductLineHandler handler = new ProductLineHandler(decomBuffer, maxDecomDisplayLines);

			final ProcessLauncher launcher = new ProcessLauncher();
			launcher.setErrorHandler(handler);
			launcher.setOutputHandler(handler);
			final String[] args = new String[] {scriptName, "--" + DpViewAppConstants.IGNORE_CHECKSUM_LONG, decomFile};


            if (!launcher.launch(args, GdsSystemProperties.getSystemProperty("user.dir"))) {
				notifyComplete("Unable to launch " + dpScriptName + " process");
				return;
			}
			final int status = launcher.waitForExit();
			if (status == IProductDecom.NO_PRODUCT_DEF) {
			    launcher.destroy();
                notifyComplete(dpScriptName + " could not produce text because no product definition (dictionary) could be found for the selected data product");
                return;
			} else if (status != IProductDecom.SUCCESS) {
				launcher.destroy();
				notifyComplete(dpScriptName + " process returned an error code: " + status);
				return;
			}
			launcher.destroy();
			notifyComplete(null);
			SWTUtilities.runInDisplayThread(new Runnable() {
	    		/**
	    		 * @see java.lang.Runnable#run()
	    		 */
				@Override
				public void run() {
                    final TextViewShell textShell = new TextViewShell(parent, TraceManager.getDefaultTracer());
					textShell.setText(handler.getDecomText());
					textShell.open();

				}
			});
			
		} catch (final Exception e) {
			e.printStackTrace();
			notifyComplete("Unable to launch " + dpScriptName + " process");
		}
	}

}
