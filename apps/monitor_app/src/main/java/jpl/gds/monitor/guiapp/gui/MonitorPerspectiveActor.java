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
package jpl.gds.monitor.guiapp.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.DisplayType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.PerspectiveCounters;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.gui.PerspectiveActor;
import jpl.gds.perspective.gui.PerspectiveShell;
import jpl.gds.perspective.message.ChangePerspectiveMessage;
import jpl.gds.perspective.message.MergePerspectiveMessage;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * MonitorPerspectiveActor is an implementation of PerspectiveListener that performs
 * the actions necessary to save or update the user's perspective, including methods
 * specific to chill_monitor.
 */
public class MonitorPerspectiveActor extends PerspectiveActor implements MonitorPerspectiveListener {
	
    /**
     * Creates an instance of PerspectiveActor.
     * 
     * @param appContext the current application context
     * @param appConfig the ApplicationConfiguration object for the current
     *            application
     * @param shell the perspective shell for the current object
     * @param useOwnHeartbeat true if this PerspectiveActor should start its own
     *        client heartbeat in order to detect message service outages; false if not
     *
     */
    public MonitorPerspectiveActor(final ApplicationContext appContext, final ApplicationConfiguration appConfig,
            final PerspectiveShell shell, final boolean useOwnHeartbeat) {
        super(appContext, appConfig, shell, useOwnHeartbeat);
       
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener#editCalled(org.eclipse.swt.widgets.Shell)
     */
    @Override
    public boolean editCalled(final Shell parent) {
        if (!parent.isDisposed()) {
            try {
                final PerspectiveEditorShell editor = new PerspectiveEditorShell(
                        appContext, parent, this.appConfig);
                editor.open();
                while (!editor.getShell().isDisposed()) {
                    parent.getDisplay().readAndDispatch();
                }
                return editor.isReload();
            } catch (final IOException e) {
                e.printStackTrace();
                tracer.error("I/O Error editing perspective");
            }
        }
        return false;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener#loadCalled(org.eclipse.swt.widgets.Shell)
     */
    @Override
    public boolean loadCalled(final Shell parent) {
        String dir = this.appConfig.getConfigPath();
        if (parent != null) {
            dir = this.swt.displayStickyFileChooser(true, parent,
                    "PerspectiveActor", GdsSystemProperties.getUserConfigDir());
        }

        // Change the perspective
        if (dir != null) {
            return loadCalled(parent, dir);
        }
        return false;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener#loadCalled(org.eclipse.swt.widgets.Shell, java.lang.String)
     */
    @Override
    public boolean loadCalled(final Shell parent, final String dir) {

        // Change the perspective
        if (dir != null) {
            if (!PerspectiveConfiguration.perspectiveExists(appContext.getBean(PerspectiveProperties.class), dir)) {
                SWTUtilities.showErrorDialog(parent,
                        "Error loading perspective", "Directory " + dir
                                + " does not contain an AMPCS perspective.");
                return false;
            }
            this.appConfig.setConfigPath(dir);
            this.persConfig.setConfigPath(dir);
            final ChangePerspectiveMessage message = new ChangePerspectiveMessage();
            message.setLocation(dir);
            message.setApplicationId(this.appConfig.getApplicationId());
            try {
                publishMessage(message);
            } catch (final MessageServiceException e) {
                SWTUtilities
                        .showErrorDialog(
                                parent,
                                "Error loading perspective",
                                "The user interface configuration could not be properly loaded due to a message service problem.");
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener#mergeCalled(org.eclipse.swt.widgets.Shell)
     */
    @Override
    public boolean mergeCalled(final Shell parent) {
        // Popup merge dialog with root at ~user/CHILL
        String dir = this.appConfig.getConfigPath();
        if (parent != null) {
            final SWTUtilities swt = new SWTUtilities();
            dir = swt.displayStickyFileChooser(true, parent,
                    "PerspectiveActor", GdsSystemProperties.getUserConfigDir());
        }
        // Merge the perspective
        if (dir != null) {
            return mergeCalled(parent, dir);
        }
        return false;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener#mergeCalled(org.eclipse.swt.widgets.Shell, java.lang.String)
     */
    @Override
    public boolean mergeCalled(final Shell parent, final String dir) {

        // Merge the perspective
        if (dir != null) {
            if (!PerspectiveConfiguration.perspectiveExists(appContext.getBean(PerspectiveProperties.class), dir)) {
                SWTUtilities.showErrorDialog(parent,
                        "Error loading perspective", "Directory " + dir
                                + " does not contain an MPCS perspective.");
                return false;
            }
            final MergePerspectiveMessage message = new MergePerspectiveMessage();
            message.setLocation(dir);
            message.setApplicationId(this.appConfig.getApplicationId());
            mergePerspective(dir);
            try {
                publishMessage(message);
            } catch (final MessageServiceException e) {
                SWTUtilities
                        .showErrorDialog(
                                parent,
                                "Error merging perspective",
                                "The user interface configuration could not be properly loaded due to a message service problem.");
                return false;
            }
            return true;
        }
        return false;
    }

    private void mergePerspective(final String dir) {
        final ApplicationConfiguration newAppConfig = new ApplicationConfiguration(appContext);
        newAppConfig.setConfigPath(dir);

        final PerspectiveCounters oldCounters = appContext.getBean(PerspectiveCounters.class)
                .getCopy();

        newAppConfig.load(appConfig.getConfigFilename());

        appContext.getBean(PerspectiveCounters.class).addCopy(oldCounters);

        DisplayConfiguration display = newAppConfig
                .getDisplayConfig(DisplayType.MESSAGE);
        if (display == null) {
            SWTUtilities
                    .showMessageDialog(this.perspectiveShell.getShell(),
                            "Not merging perspective",
                            "The chosen perspective does not contain any monitor views.");
            return;
        }

        try {
            tracer.debug(this.perspectiveShell.getTitle()
                    + " merging perspective");
            final List<IViewConfiguration> views = display.getViewConfigs();
            final ArrayList<IViewConfiguration> viewListCopy = new ArrayList<IViewConfiguration>(
                    views);

            if (views == null || views.isEmpty()) {
                SWTUtilities
                        .showMessageDialog(this.perspectiveShell.getShell(),
                                "Not merging perspective",
                                "The chosen perspective does not contain any monitor views.");
                return;
            }
            display = this.appConfig.getDisplayConfig(DisplayType.MESSAGE);
            for (final IViewConfiguration vc : viewListCopy) {
                vc.setMerged(true);
                display.addViewConfiguration(vc);
            }

        } catch (final Exception e) {
            tracer.error(this.perspectiveShell.getTitle()
                    + " error merging perspective");
            SWTUtilities.showErrorDialog(this.perspectiveShell.getShell(),
                    "Error merging perspective",
                    "Unable to merge user perspective: " + e.toString());
        }
    }
   
}
