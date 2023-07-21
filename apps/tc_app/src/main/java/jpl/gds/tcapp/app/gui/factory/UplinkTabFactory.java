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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.tc.api.ICommandObjectFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.types.Pair;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.CommandProperties.GuiTab;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.ImmediateCommandComposite;
import jpl.gds.tcapp.app.gui.SendFileLoadComposite;
import jpl.gds.tcapp.app.gui.SendRawDataComposite;
import jpl.gds.tcapp.app.gui.SendScmfComposite;
import jpl.gds.tcapp.app.gui.UplinkShell;
import jpl.gds.tcapp.app.gui.fault.CommandBuilderComposite;
import jpl.gds.tcapp.app.gui.fault.FaultInjectorComposite;
import jpl.gds.tcapp.app.gui.fault.FaultInjectorException;

/**
 * The UplinkTabFactory creates the base set of tabs for the chill_up GUI applicaiton
 * <br><br>
 * The tabs that can be created are:<br>
 * * Immediate Command<br>
 * * Command Builder<br>
 * * Send File Load<br>
 * * Send Raw Data<br>
 * * Fault Injection<br>
 * <br>
 * The presence or absence of each tab can be dictated by updating the CommandProperties
 * 
 *
 */
public class UplinkTabFactory implements IUplinkTabFactory {

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
	 * The factory function for creating an immediate command tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createCommandTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		if (appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
				GuiTab.IMMEDIATE_COMMAND)) {
			final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

			tabItem.setText("Immediate Command");

			final AbstractUplinkComposite commandTab = new ImmediateCommandComposite(appContext, tabFolder);

			final FormLayout fl = new FormLayout();

			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;

			commandTab.setLayout(fl);

			final FormData data = new FormData();

			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);

			commandTab.setLayoutData(data);

			tabItem.setControl(commandTab);

			retPair = new Pair<>(tabItem, commandTab);
		}

		return retPair;

	}

	/**
	 * The factory function for creating a command builder tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createBuilderTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.COMMAND_BUILDER)) {
			try {
				final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

				tabItem.setText("Command Builder");

				// Create with no "Exit" button
				final AbstractUplinkComposite builderTab = new CommandBuilderComposite(appContext, tabFolder, false, false);

				final FormLayout fl = new FormLayout();

				fl.spacing = 10;
				fl.marginHeight = 5;
				fl.marginWidth = 5;

				builderTab.setLayout(fl);

				final FormData data = new FormData();

				data.left = new FormAttachment(0);
				data.right = new FormAttachment(100);

				builderTab.setLayoutData(data);

				tabItem.setControl(builderTab);

				retPair = new Pair<>(tabItem, builderTab);

			} catch (final DictionaryException de) {
				TraceManager.getDefaultTracer(appContext).fatal(de.getMessage());

				SWTUtilities.showErrorDialog(upShell.getStaticShell(),
						"Command Dictionary Parsing Error", de.getMessage());
			}
		}

		return retPair;
	}

	/**
	 * The factory function for creating a send file tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createSendFileLoadTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.SEND_FILE_LOAD)) {
			final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

			tabItem.setText("Send File Load");

			final AbstractUplinkComposite sendFileLoadTab = new SendFileLoadComposite(appContext, tabFolder,
					appContext.getBean(CommandProperties.class), appContext.getBean(ICommandObjectFactory.class));

			final FormLayout fl = new FormLayout();

			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;

			sendFileLoadTab.setLayout(fl);

			final FormData data = new FormData();

			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);

			sendFileLoadTab.setLayoutData(data);

			tabItem.setControl(sendFileLoadTab);

			retPair = new Pair<>(tabItem, sendFileLoadTab);
		}

		return retPair;
	}

	/**
	 * The factory function for creating an send SCMF tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createSendScmfTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.SEND_SCMF)) {
			final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

			tabItem.setText("Send SCMF");

			final AbstractUplinkComposite sendScmfTab = new SendScmfComposite(appContext, tabFolder);

			final FormLayout fl = new FormLayout();

			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;

			sendScmfTab.setLayout(fl);

			final FormData data = new FormData();

			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);

			sendScmfTab.setLayoutData(data);

			tabItem.setControl(sendScmfTab);

			retPair = new Pair<>(tabItem, sendScmfTab);
		}

		return retPair;
	}

	/**
	 * The factory function for creating a send raw data tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createSendRawDataTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.SEND_RAW_DATA_FILE)) {
			final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

			tabItem.setText("Send Raw Data File");

			final AbstractUplinkComposite sendRawDataTab = new SendRawDataComposite(appContext, tabFolder);

			final FormLayout fl = new FormLayout();

			fl.spacing = 10;
			fl.marginHeight = 5;
			fl.marginWidth = 5;

			sendRawDataTab.setLayout(fl);

			final FormData data = new FormData();

			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);

			sendRawDataTab.setLayoutData(data);

			tabItem.setControl(sendRawDataTab);

			retPair = new Pair<>(tabItem, sendRawDataTab);
		}

		return retPair;
	}

	/**
	 * The factory function for creating a command fault injection tab
	 * @param appContext the current application context
	 * @param upShell the uplink shell in which these tabs will be displayed
	 * @param tabFolder the folder of tabs for the uplink shell supplied
	 * @return the pair of tab item and uplink composite elements to be added to the chill_up GUI
	 */
	public Pair<TabItem, AbstractUplinkComposite> createFaultInjectorTab(final ApplicationContext appContext, final UplinkShell upShell, final TabFolder tabFolder) {

		Pair<TabItem, AbstractUplinkComposite> retPair = null;

		final Tracer log = TraceManager.getDefaultTracer(appContext);

		if (!appContext.getBean(SseContextFlag.class).isApplicationSse()
				&& appContext.getBean(CommandProperties.class).isGuiTabRequired(appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
						GuiTab.FAULT_INJECTION)) {
			try {
				final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

				tabItem.setText("Fault Injection Wizard");

				final AbstractUplinkComposite faultInjectorTab = new FaultInjectorComposite(appContext, tabFolder);

				final FormLayout fl = new FormLayout();

				fl.spacing = 10;
				fl.marginHeight = 5;
				fl.marginWidth = 5;

				faultInjectorTab.setLayout(fl);

				final FormData data = new FormData();

				data.left = new FormAttachment(0);
				data.right = new FormAttachment(100);

				faultInjectorTab.setLayoutData(data);

				tabItem.setControl(faultInjectorTab);

				retPair = new Pair<>(tabItem, faultInjectorTab);

			} catch (final DictionaryException de) {
				log.fatal(de.getMessage());

				SWTUtilities.showErrorDialog(upShell.getStaticShell(),
						"Command Dictionary Parsing Error", de.getMessage());
			} catch (final FaultInjectorException fie) {
				log.fatal(fie.getMessage());

				SWTUtilities.showErrorDialog(upShell.getStaticShell(),
						"Fault injector Error", fie.getMessage());
			}
		}

		return retPair;
	}


}
