/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tcapp.app.gui.factory;

import java.util.List;

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.types.Pair;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.UplinkShell;

/**
 * The IUplinkTabFactory is an interface to be used by any factory that creates the various
 * tabs to be displayed in the chill_up GUI application
 * 
 *
 */
public interface IUplinkTabFactory {
	
	/**
	 * The factory function that gets all of the uplink tabs constructed by chill_up
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return A list of pairs of tab items and uplink composite views to be added to the uplink GUI
	 */
	public List<Pair<TabItem, AbstractUplinkComposite>> createUplinkTabs(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder);
}
