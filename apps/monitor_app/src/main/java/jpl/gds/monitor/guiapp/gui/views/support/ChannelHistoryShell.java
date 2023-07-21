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
/**
 * 
 */
package jpl.gds.monitor.guiapp.gui.views.support;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite;
import jpl.gds.monitor.guiapp.gui.views.BasicChannelTableComposite;
import jpl.gds.monitor.perspective.view.ChannelListViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ChillShell;


/**
 * The ChannelHistoryShell class is a specific implementation of the AbstractHistoryShell that
 * uses the BasicChannelTableComposite for the underlying table format, and a ChannelListViewConfiguration
 * as the source for view configuration information. It is used by the Channel List and Fixed views.
 */
public class ChannelHistoryShell extends AbstractHistoryShell implements ChillShell{

    
	/**
	 * Constructs a ChannelHistoryShell.
	 * 
	 * @param parent parent shell for this shell
	 * @param useLocation location for this shell on the screen
	 * @param config ChannelListViewConfiguration object from the invoking view
	 */
	public ChannelHistoryShell(ApplicationContext appContext, Shell parent, Point useLocation, ChannelListViewConfiguration config) {
		super(appContext, parent, useLocation, config);
	}


	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.support.AbstractHistoryShell#createTableComposite(org.eclipse.swt.widgets.Composite, jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public AbstractBasicTableComposite createTableComposite(Composite parent,
			IViewConfiguration config) {
		return new BasicChannelTableComposite(appContext, parent, (ChannelListViewConfiguration)viewConfig);
	}
}	