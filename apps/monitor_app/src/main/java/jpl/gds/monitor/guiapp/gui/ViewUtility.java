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
import java.util.List;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.canvas.ViewLaunchManager;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.monitor.perspective.view.TabularViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewFactory;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * A utility class that knows how to load pre-defined views.
 */
public class ViewUtility implements ViewLaunchManager {
	/**
	 * Window pad height in pixels for Mac and Linux operating systems
	 */
	public static final int WINDOW_PAD_HEIGHT = GdsSystemProperties.isMacOs() ? 46 : 60;
	
	/**
	 * Window pad width in pixels for Mac and Linux operating systems
	 */
	public static final int WINDOW_PAD_WIDTH = GdsSystemProperties.isMacOs() ? 18 : 35;

	private final ApplicationContext appContext;

	public ViewUtility(final ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
	/**
	 * Loads the view configuration at the given view reference.
	 * 
	 * @param ref ViewReference object
	 * @param singleViewOnly indicates whether only configurations for single views 
	 * (not tabbed views) should be returned
	 * @param display the SWT device for computing font sizes
	 * @return ViewConfiguration, or null if no view can be found from the reference
	 */
	public IViewConfiguration loadViewConfiguration(final ViewReference ref, 
			final boolean singleViewOnly, final Display display) {

		final IViewConfiguration vc = ref.parse(appContext);
		if (vc == null) {
			return null;
		}
		normalizeCoordinates(vc);
		if (singleViewOnly) {
			if (vc.getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)) {
				return ((SingleWindowViewConfiguration) vc).getViews().get(0);
			} else {
				return vc;
			}
		} else {

			if (vc.getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)
					|| vc.getViewType().equals(ViewType.MESSAGE_TAB)) {
				return vc;
			} else {
				final SingleWindowViewConfiguration window = new SingleWindowViewConfiguration(appContext);
				window.addViewConfiguration(vc);
				window.setViewName(vc.getViewName() + " (" + IViewConfiguration.ID_TOKEN + ")");
				if (vc.getPreferredWidth() != 0) {
					final Point p = getViewSize(vc, display);

					window.setSize(new ChillSize(p.x + WINDOW_PAD_WIDTH, 
							p.y + WINDOW_PAD_HEIGHT));
				}

				return window;
			}
		}
	}

	/**
	 * Returns the preferred size, if any, of a view to be loaded, in pixels.
	 * @param vc ViewConfiguration the configuration for the view to get size for.
	 * @param display the SWT device to use for computing size
	 * @return Point object, giving size as x,y pixel coordinate
	 */
	public static Point getViewSize(final IViewConfiguration vc, final Display display) {
		int width = vc.getPreferredWidth();
		int height = vc.getPreferredHeight();
		if (vc.getViewType().equals(ViewType.FIXED_LAYOUT)) {
			final IFixedLayoutViewConfiguration vfc = (IFixedLayoutViewConfiguration)vc;	
			if (vfc.getCoordinateSystem().equals(CoordinateSystemType.CHARACTER)) {
				final ChillFont defFont = vc.getDataFont();
				Font font = ChillFontCreator.getFont(defFont);
				GC gc = new GC(display);
				gc.setFont(font);
				final int charWidth = SWTUtilities.getFontCharacterWidth(gc);
				final int charHeight = SWTUtilities.getFontCharacterHeight(gc);
				width = charWidth * width;
				height = charHeight * height;
				gc.dispose();
				gc = null;
				font.dispose();
				font = null;
			}
		}
		return new Point(width, height);
	}

//	/**
//	 * Loads the view configuration from the given file.
//	 * 
//	 * @param filename path to the view configuration file
//	 * @param singleViewOnly indicates whether only configurations for single views 
//	 * (not tabbed views) should be returned
//	 * @param display the SWT device for computing font sizes
//	 * 
//	 * @return ViewConfiguration, or null if no view can be found from the reference
//	 */
//	public static ViewConfiguration loadViewConfiguration(String filename, boolean singleViewOnly, Display display) {
//
//		ViewReference ref = new ViewReference();
//		ref.setPath(filename);
//		return ViewUtility.loadViewConfiguration(ref, singleViewOnly, display);
//	}

	/**
	 * Loads the referenced view into the current window manager as a new window.
	 * 
	 * @param ref reference to the view to load
	 * @param mainShell Shell widget to use for error dialog display
	 */
	public void loadView(final ViewReference ref, final Shell mainShell) {
		final IViewConfiguration vc = loadViewConfiguration(ref, false, mainShell.getDisplay());
		if (vc == null) {
			SWTUtilities.showErrorDialog(mainShell, "Error Loading View", "The view could not be loaded due to an error");
			return;
		}
		final WindowManager wm = MonitorViewReferences.getInstance().getWindowManager();
		if (wm == null) {
			return;
		}
		if (vc instanceof TabularViewConfiguration) {
			wm.addViewTab((TabularViewConfiguration)vc);
		} else if (vc instanceof SingleWindowViewConfiguration) {
			wm.addView((SingleWindowViewConfiguration)vc);
		}
	}
	
	@Override
    public void loadView(final String filename, final Shell mainShell) {
	
	    final ViewReference vr = new ViewReference();
	    if (filename.startsWith(File.separator)) {
	        vr.setPath(filename);
	    } else {
	        vr.setName(filename);
	    }
	    loadView(vr, mainShell);
	}

	/**
	 * Loads the referenced view into the given tabbed window.
	 * 
	 * @param ref reference to the view to load 
	 * @param mainShell Shell widget to use for error dialog display
	 * @param tabShell the TabularViewShell to add the tab to
	 * @return true if view is loaded properly, false if loading the view fails
	 */
	public boolean loadViewAsTab(final ViewReference ref, final Shell mainShell, 
			final TabularViewShell tabShell) {
		final TabularViewConfiguration myTabConfig = tabShell.getViewConfig();

		final IViewConfiguration vc = loadViewConfiguration(ref, true, mainShell.getDisplay());

		if (vc == null) {
			SWTUtilities.showErrorDialog(mainShell, "Error Loading View", "The view could not be loaded because it cannot\nbe loaded into a tab, or because there was an error");
			return false;
		}
		final View v = ViewFactory.createView(appContext, vc, true);
		tabShell.getViews().add(v);
		myTabConfig.addViewConfiguration(vc);

		v.init(tabShell.getTabs());
		return true;
	}

	/**
	 * Loads view in the given file into the given tabbed window.
	 * 
	 * @param filename the path to the view configuration file to load
	 * @param mainShell Shell widget to use for error dialog display
	 * @param tabShell the TabularViewShell to add the tab to
	 * @return true if view is loaded properly, false if loading the view fails
	 */
	public boolean loadViewAsTab(final String filename, final Shell mainShell, final TabularViewShell tabShell) {

		final ViewReference ref = new ViewReference();
		ref.setPath(filename);

		return loadViewAsTab(ref, mainShell, tabShell);
	}

	/**
	 * Normalizes the coordinates in a ViewConfiguration so that all Fixed View coordinates 
	 * are either pixel or character based.
	 * 
	 * @param config the top-level ViewConfiguration to normalize. Children will be normalized 
	 * recursively.
	 */
	public void normalizeCoordinates(final IViewConfiguration config) {
		if (config instanceof IFixedLayoutViewConfiguration) {
			final IFixedLayoutViewConfiguration fixedConfig = (IFixedLayoutViewConfiguration)config;
			final CoordinateSystemType coordSystem = fixedConfig.getCoordinateSystem();
			final ChillFont chillFont = fixedConfig.getDataFont(); 
			Font font = ChillFontCreator.getFont(chillFont);
			GC gc = new GC(Display.getCurrent());
			gc.setFont(font);
			final int charWidth = SWTUtilities.getFontCharacterWidth(gc);
			final int charHeight = SWTUtilities.getFontCharacterHeight(gc);
			final List<IFixedFieldConfiguration> fieldConfigs = ((IFixedLayoutViewConfiguration)config).getFieldConfigs();
			for (final IFixedFieldConfiguration field: fieldConfigs) {
				if (field instanceof HeaderFieldConfiguration) {
					field.convertCoordinates(coordSystem, charWidth, charHeight);
				}
			}
			gc.dispose();
			gc = null;
			font.dispose();
			font = null;
		} else if (config instanceof IViewConfigurationContainer) {
			final List<IViewConfiguration> children = ((IViewConfigurationContainer)config).getViews();
			if (children != null) {
				for (final IViewConfiguration vc: children) {
					normalizeCoordinates(vc);
				}
			}
		}
	}
}
