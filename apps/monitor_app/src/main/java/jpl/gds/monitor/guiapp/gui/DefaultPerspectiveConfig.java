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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.log.TraceManager;

/**
 * A class that can load a list of default monitor view configurations as listed
 * in the GDS configuration file.
 * 
 */
public class DefaultPerspectiveConfig {
    private static final String IMPORTABLE_TOKEN = "import";

    private ArrayList<IViewConfiguration> viewConfigs;

    /**
     * Constructor: Creates a list of default perspective configs
     */
    public DefaultPerspectiveConfig(final ApplicationContext appContext) {
        final List<String> viewList = appContext.getBean(MonitorGuiProperties.class).getDefaultViewSet();

        if (viewList == null || viewList.isEmpty()) {
            return;
        }

        for (final String ref : viewList) {
            final ViewReference vr = new ViewReference();
            if (ref.startsWith(File.separator)) {
                vr.setPath(ref);
            } else {
                vr.setName(ref);
            }
            IViewConfiguration vc = vr.parse(appContext);
            if (vc == null) {
                TraceManager.getDefaultTracer(appContext).warn("Unable to load default view: " + ref);
                continue;
            }
            if (this.viewConfigs == null) {
                this.viewConfigs = new ArrayList<IViewConfiguration>();
            }
            if (ref.toLowerCase().indexOf(IMPORTABLE_TOKEN) != -1) {
                vc.setViewReference(null);
            }
            if (!vc.getViewType().isStandaloneWindow()) {
                final SingleWindowViewConfiguration winConfig = new SingleWindowViewConfiguration(appContext);
                winConfig.addViewConfiguration(vc);
                vc = winConfig;
            }
            this.viewConfigs.add(vc);
        }
    }

    /**
     * Gets the list of loaded view configurations. If none were found by the
     * constructor, then the return value will be null.
     * 
     * @return List of loaded ViewConfigurations, or null if none loaded
     */
    public ArrayList<IViewConfiguration> getViewConfigs() {
        return this.viewConfigs;
    }
}
