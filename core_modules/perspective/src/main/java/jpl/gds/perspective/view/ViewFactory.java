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
package jpl.gds.perspective.view;

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * ViewFactory manufactures View instances given either a ViewType or a
 * ViewConfiguration. Class names for View classes are stored in the
 * ViewConfiguration file.
 * 
 *
 */
public class ViewFactory {

    private static Tracer trace; 


    /**
     * Creates a View instance given a view configuration object.
     * 
     * @param vc
     *            the ViewConfiguration object
     * @param tabView
     *            true if a monitor tab view should be created, false for
     *            non-tab view
     * @return the new View, or null if an object could not be created.
     */
    public static View createView(final ApplicationContext appContext, final IViewConfiguration vc,
            final boolean tabView) {
        trace = TraceManager.getDefaultTracer(appContext);
        final ViewType vt = vc.getViewType();
        Class<?> c = null;
        if (tabView) {
            c = vc.getViewTabClass();
        } else {
            c = vc.getViewClass();
        }
        try {
            final View v = (View)ReflectionToolkit.createObject(c, new Class[] {ApplicationContext.class, 
            		IViewConfiguration.class}, new Object[] { appContext, vc });
            
            return v;
        } catch (final Exception e) {
            e.printStackTrace();
            trace.error("Unable to create view of type " + vt + ": "
                    + e.toString());
        }
        return null;
    }

    /**
     * Creates a View instance given a view type.
     * 
     * @param vt
     *            the ViewType
     * @param tabView
     *            true if a monitor tab view should be created, false for
     *            non-tab view
     * @return the new View, or null if an object could not be created.
     */
    public static View createView(final ApplicationContext appContext, final ViewType vt, final boolean tabView) {
        trace = TraceManager.getDefaultTracer(appContext);
        final IViewConfiguration vc = createViewConfig(appContext, vt);
        Class<?> c = null;
        if (tabView) {
            c = vc.getViewTabClass();
        } else {
            c = vc.getViewClass();
        }
        try {
            final View v = (View)ReflectionToolkit.createObject(c, new Class[] {ApplicationContext.class, 
                    IViewConfiguration.class}, new Object[] { appContext, vc });
            return v;
        } catch (final Exception e) {
            e.printStackTrace();
            trace.error("Unable to create view of type " + vt + ": "
                    + e.toString());
        }
        return null;
    }

    /**
     * Creates a View configuration instance given a view type.
     * 
     * @param vt
     *            the ViewType
     * @return the new ViewConfiguration, or null if an object could not be
     *         created.
     */
    public static IViewConfiguration createViewConfig(final ApplicationContext appContext, final ViewType vt) {
        trace = TraceManager.getDefaultTracer(appContext);
        try {
            final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
            final Class<?> c = vp.getViewConfigurationClass();
            final IViewConfiguration v = (IViewConfiguration) ReflectionToolkit.createObject(c, new Class[] {ApplicationContext.class}, new Object[] { appContext });
            return v;
        } catch (final Exception e) {
            e.printStackTrace();
            trace.error("Unable to create view configuration for type " + vt
                    + ": " + e.toString());
        }
        return null;
    }

    /**
     * Creates a View configuration instance given a view type, assuming the
     * view will be added as a tab in a tabbed window.
     * 
     * @param vt
     *            the ViewType
     * @return the new ViewConfiguration, or null if an object could not be
     *         created.
     */
    public static IViewConfiguration createViewConfigForTab(final ApplicationContext appContext, final ViewType vt) {
        trace = TraceManager.getDefaultTracer(appContext);
        final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
        final Class<?> c = vp.getViewConfigurationClass();
        try {
            final IViewConfiguration v = (IViewConfiguration)ReflectionToolkit.createObject(c, new Class[] {ApplicationContext.class}, new Object[] { appContext });
            final Class<?> vc = v.getViewTabClass();
            v.setViewClass(vc.getName());
            return v;
        } catch (final Exception e) {
            e.printStackTrace();
            trace.error("Unable to create tab view configuration for type "
                    + vt + ": " + e.toString());
        }
        return null;
    }
}
