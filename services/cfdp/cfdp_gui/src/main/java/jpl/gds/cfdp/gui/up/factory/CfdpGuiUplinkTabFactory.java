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
package jpl.gds.cfdp.gui.up.factory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.cfdp.gui.up.SendFileCfdpComposite;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.Pair;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.CommandProperties.GuiTab;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.UplinkShell;
import jpl.gds.tcapp.app.gui.factory.UplinkTabFactory;

/**
 * The CFDP GUI uplink tab factory creates all of the tabs that may be needed by a CFDP enabled mission
 * This includes the base tabs created by UplinkTabFactory, but also the Send File Cfdp tab.
 *
 */
public class CfdpGuiUplinkTabFactory extends UplinkTabFactory {
	
	@Override
	public List<Pair<TabItem, AbstractUplinkComposite>> createUplinkTabs(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {
				final List<Pair<TabItem, AbstractUplinkComposite>> retTabs = new ArrayList<>();
				Pair<TabItem, AbstractUplinkComposite> tmp;

				tmp = createCommandTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				tmp = createBuilderTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				tmp = createSendFileLoadTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				//the one extra tab added by CFDP
				tmp = createSendFileCfdpTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				tmp = createSendScmfTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				tmp = createSendRawDataTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				tmp = createFaultInjectorTab(appContext, upShell, tabFolder);
				if(tmp != null) {
					retTabs.add(tmp);
				}

				return retTabs;
	}
	

	/**
	 * The factory function for creating a send file via CFDP tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createSendFileCfdpTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {
		
		Pair<TabItem, AbstractUplinkComposite> retPair = null;
		
		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.SEND_FILE_CFDP)) {
			final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

			tabItem.setText("Send File CFDP");

			final AbstractUplinkComposite sendFileCfdpTab = new SendFileCfdpComposite(appContext, tabFolder);

			final FormLayout fl = new FormLayout();

			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;

			sendFileCfdpTab.setLayout(fl);

			final FormData data = new FormData();

			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);

			sendFileCfdpTab.setLayoutData(data);

			tabItem.setControl(sendFileCfdpTab);
			
			retPair = new Pair<>(tabItem, sendFileCfdpTab);
		}
		
		return retPair;
	}

}
