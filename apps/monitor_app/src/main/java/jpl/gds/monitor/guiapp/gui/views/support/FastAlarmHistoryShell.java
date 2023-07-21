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
import jpl.gds.monitor.guiapp.gui.views.BasicFastAlarmTableComposite;
import jpl.gds.monitor.perspective.view.FastAlarmViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ChillShell;


/**
 * The FastAlarmHistoryShell class is a specific implementation of the AbstractHistoryShell that
 * uses the BasicFastAlarmTableComposite for the underlying table format, and a FastAlarmViewConfiguration
 * as the source for view configuration information. It is used by the Fast Alarm View
 */
public class FastAlarmHistoryShell extends AbstractHistoryShell implements ChillShell{
	/**
	 * Constructs a FastAlarmHistoryShell.
	 * 
	 * @param parent parent shell for this shell
	 * @param useLocation location for this shell on the screen
	 * @param config FastAlarmViewConfiguration object from the invoking view
	 */
	public FastAlarmHistoryShell(ApplicationContext appContext, Shell parent, Point useLocation, FastAlarmViewConfiguration config) {
		super(appContext, parent, useLocation, config);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.support.AbstractHistoryShell#createTableComposite(org.eclipse.swt.widgets.Composite, jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public AbstractBasicTableComposite createTableComposite(Composite parent,
			IViewConfiguration config) {
		return new BasicFastAlarmTableComposite(appContext, parent, (FastAlarmViewConfiguration)config);
	}
}