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
package jpl.gds.monitor.guiapp.gui.views.tab;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.views.FixedLayoutComposite;
import jpl.gds.monitor.perspective.view.fixed.FixedLayoutViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * FixedLayoutTabItem is the tab item container for FixedLayoutComposite in the non-RCP version
 * of the monitor.  It plays a ViewTab role, so it can be added to or removed
 * from the monitor by generic view logic.
 *
 */
public class FixedLayoutTabItem extends AbstractTabItem  {
	/**
	 * Fixed layout tab item title
	 */
	public static final String TITLE = FixedLayoutComposite.TITLE;

	/**
	 * Fixed layout composite within this tab item
	 */
	protected FixedLayoutComposite fixedComp;

	/**
	 * Creates an instance of FixedLayoutTabItem.
	 * @param appContext the current application context
	 * @param config the FixedLayoutViewConfiguration object containing display settings
	 */
	public FixedLayoutTabItem(final ApplicationContext appContext, final IViewConfiguration config) {

		super(config); 
		setComposite(new FixedLayoutComposite(appContext, viewConfig));
		fixedComp = (FixedLayoutComposite)getComposite();
	}

	/**
	 * Creates an instance of FixedLayoutTabItem with a default view configuration.
	 * @param appContext the current application context
	 */
	public FixedLayoutTabItem(final ApplicationContext appContext) {
		this(appContext, new FixedLayoutViewConfiguration(appContext));
	}

	/**
	 * Creates the controls and composites for this tab display.
	 */
	@Override
	protected void createControls() {
		tab = new CTabItem(parent, SWT.NONE);
		tab.setText(viewConfig.getViewName());

		final Composite comp = new Composite(parent, SWT.NONE);
		final FormLayout form = new FormLayout();
		comp.setLayout(form);

		fixedComp.init(comp);
		final Control innerComp = fixedComp.getMainControl();
		final FormData efd = new FormData();
		efd.top = new FormAttachment(0);
		efd.right = new FormAttachment(100);
		efd.bottom = new FormAttachment(100);
		efd.left = new FormAttachment(0);
		innerComp.setLayoutData(efd);

		tab.setControl(comp);
		tab.setToolTipText("Fixed Channel View");

		tab.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					if (fixedComp != null) {
						fixedComp.getMainControl().dispose();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}      

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}
}