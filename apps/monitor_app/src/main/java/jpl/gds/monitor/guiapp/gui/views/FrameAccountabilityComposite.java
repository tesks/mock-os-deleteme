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
package jpl.gds.monitor.guiapp.gui.views;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.FrameAccountabilityPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.BadFrameShell;
import jpl.gds.monitor.perspective.view.FrameAccountabilityViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICommandMessage;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.FrameSummaryRecord;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameSequenceAnomalyMessage;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.frame.ILossOfSyncMessage;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;

/**
 * This class implements the frame accountability view in the monitor.
 * 
 */
public class FrameAccountabilityComposite implements View, GeneralMessageListener {

	/**
	 * Frame accountability composite title
	 */
	public static final String TITLE = "Frame Accountability";
    private final Tracer                                   trace;

//	private static final String PACKET_VCID_PROPERTY = "packetExtract.vcids";

	private static final String RED_IMAGE = "jpl/gds/monitor/gui/flag_red.gif";
	private static final String GREEN_IMAGE = "jpl/gds/monitor/gui/flag_green.gif";
	private static final String YELLOW_IMAGE = "jpl/gds/monitor/gui/flag_orange.gif";
	private static final String BLUE_IMAGE = "jpl/gds/monitor/gui/flag_blue.gif";

	private static volatile Image redImage;
	private static volatile Image greenImage;
	private static volatile Image blueImage;
	private static volatile Image yellowImage;

	private final FrameAccountabilityViewConfiguration viewConfig;
	private FrameAccountabilityPreferencesShell prefShell;
	private Composite parent;
	private Composite mainComposite;
	private Tree tree;
	private TreeItemSelectionListener treeListener;
	private final HashMap<Integer,VirtualChannelTreeNode> vcMap = new HashMap<Integer, VirtualChannelTreeNode>();
	private final DateFormat format = TimeUtility.getFormatter();
	private final List<IFrameEventMessage> badFrames = new ArrayList<IFrameEventMessage>();
	private MenuItem badMenuItem;
	private MenuItem reportMenuItem;
	private final SWTUtilities util = new SWTUtilities();
    private Font dataFont;
    private Color foreground;
    private Color background;
    private final Semaphore syncFlag = new Semaphore(1);
	private final ApplicationContext appContext;
    
	/**
	 * Constructor
	 * 
	 * Create a FrameAccountabilityComposite with the given view configuration.
	 * @param appContext the current application context
	 * 
	 * @param config
	 *            the FrameAccountabilityViewConfiguration containing view settings
	 */
	public FrameAccountabilityComposite(final ApplicationContext appContext, final IViewConfiguration config) {
		this.appContext = appContext;
        trace = TraceManager.getDefaultTracer(appContext);
		viewConfig = (FrameAccountabilityViewConfiguration) config;
	}

	/**
     * {@inheritDoc}
	 * 
	 * @see jpl.gds.perspective.view.View#getMainControl()
	 */
	@Override
	public Control getMainControl() {
		return mainComposite;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#updateViewConfig()
	 */
	@Override
	public void updateViewConfig() {	
	}

	private static void createImages() {
		if (redImage == null) {
			redImage = SWTUtilities.createImage(Display.getCurrent(), RED_IMAGE);
			greenImage = SWTUtilities.createImage(Display.getCurrent(), GREEN_IMAGE);
			blueImage = SWTUtilities.createImage(Display.getCurrent(), BLUE_IMAGE);
			yellowImage = SWTUtilities.createImage(Display.getCurrent(), YELLOW_IMAGE);
		}
		
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void init(final Composite parent) {
		this.parent = parent;
        createImages();
		createGui();
		final GeneralMessageDistributor gdm = appContext.getBean(GeneralMessageDistributor.class);
		gdm.addDataListener(this, TmServiceMessageType.BadTelemetryFrame);
		gdm.addDataListener(this, TmServiceMessageType.InSync);
		gdm.addDataListener(this, TmServiceMessageType.LossOfSync);
		gdm.addDataListener(this, TmServiceMessageType.FrameSequenceAnomaly);
		gdm.addDataListener(this, TmServiceMessageType.OutOfSyncData);
		gdm.addDataListener(this, TmServiceMessageType.TelemetryFrameSummary);
		gdm.addDataListener(this, CommandMessageType.FlightSoftwareCommand);
		gdm.addDataListener(this, CommandMessageType.SseCommand);
		gdm.addDataListener(this, CommandMessageType.HardwareCommand);

	}

	/**
	 * Sets the font background color and foreground color for the tree
	 */
	protected void updateTreeFontAndColors() {
		if (dataFont != null && !dataFont.isDisposed()) {
			dataFont.dispose();
			dataFont = null;
		}
		dataFont = ChillFontCreator.getFont(viewConfig.getDataFont());
		tree.setFont(dataFont);
		
		if (foreground != null && !foreground.isDisposed()) {
			foreground.dispose();
			foreground = null;
		}
		foreground = ChillColorCreator.getColor(viewConfig.getForegroundColor());
		tree.setForeground(foreground);
		
		if (background != null && !background.isDisposed()) {
			background.dispose();
			background = null;
		}
		background = ChillColorCreator.getColor(viewConfig.getBackgroundColor());
		tree.setBackground(background);
	}
	
	private void createGui() {
		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 5;
		mainComposite.setLayout(shellLayout);

		tree = new Tree(mainComposite, SWT.SINGLE);
		final FormData treeFd = new FormData();
		createVcNodes();

		treeFd.top = new FormAttachment(0,5);
		treeFd.left = new FormAttachment(0,5);
		treeFd.right = new FormAttachment(100,-5);
		treeFd.bottom = new FormAttachment(100);
		tree.setLayoutData(treeFd); 

		updateTreeFontAndColors();

		final Menu viewMenu = new Menu(parent);
		tree.setMenu(viewMenu);
		final MenuItem prefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		prefMenuItem.setText("Preferences...");

		new MenuItem(viewMenu, SWT.SEPARATOR);
		final MenuItem expandItem = new MenuItem(viewMenu, SWT.PUSH);
		expandItem.setText("Expand All");
		final MenuItem collapseItem = new MenuItem(viewMenu, SWT.PUSH);
		collapseItem.setText("Collapse All");

		new MenuItem(viewMenu, SWT.SEPARATOR);
		badMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		badMenuItem.setText("View Bad Frames for VC...");

		reportMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		reportMenuItem.setText("Save Report for VC...");

		final MenuItem clearMenuItem =  new MenuItem(viewMenu, SWT.PUSH);	     
		clearMenuItem.setText("Clear Data");

		clearMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					clearView();
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in clear menu item handling " + ex.toString());
				}
			}
		});

		expandItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final TreeItem[] items = tree.getItems();
					for (int i = 0; i < items.length; i++) {
						items[i].setExpanded(true);
					}
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in expand menu item handling " + ex.toString());
				}
			}
		});

		collapseItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {

					final TreeItem[] items = tree.getItems();
					for (int i = 0; i < items.length; i++) {
						items[i].setExpanded(false);
					}
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in collapse menu item handling " + ex.toString());
				}
			}
		});

		badMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {

					final TreeItem[] selection = tree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final TreeItem item = selection[0];
					final TreeNode node = (TreeNode)item.getData("node");
					if (node.type != TreeNodeType.VC) {
						return;
					}
					final int vcid = ((VirtualChannelTreeNode)node).vcid;
					final List<IFrameEventMessage> vcidBadFrames = new ArrayList<IFrameEventMessage>();

					for (final IFrameEventMessage m: badFrames) {
						if (m.getFrameInfo().getVcid() == vcid) {
							vcidBadFrames.add(m);
						}
					}
					if (vcidBadFrames.size() == 0) {
						SWTUtilities.showMessageDialog(mainComposite.getShell(), "No Bad Frames", "There are no bad frames in the current history.");
						return;
					}
                    final BadFrameShell shell = new BadFrameShell(mainComposite.getShell(),
                                                                  appContext.getBean(SseContextFlag.class));
					shell.setBadFrameList(vcidBadFrames);
					badMenuItem.setEnabled(false);
					shell.open();
					shell.getShell().addDisposeListener(new DisposeListener() {

						@Override
						public void widgetDisposed(
								final DisposeEvent event) {
							badMenuItem.setEnabled(true);
						}
					});
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in view bad frames menu item handling " + ex.toString());
				}
			}
		});

		reportMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {

					final TreeItem[] selection = tree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final TreeItem item = selection[0];
					final TreeNode node = (TreeNode)item.getData("node");
					if (node.type != TreeNodeType.VC) {
						return;
					}
					if (node.getTreeItem().getItemCount() == 0) { 	
						SWTUtilities.showMessageDialog(mainComposite.getShell(), "No VC Information", "There are no events in the current history for the selected VC.");
						return;
					}
					final String filename = util.displayStickyFileSaver(mainComposite.getShell(), "FrameAccountabilityComposite",null, "framereport.txt");
					if (filename == null) {
						return;
					}
					writeVcToFile(filename, (VirtualChannelTreeNode)node);

				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in view bad frames menu item handling " + ex.toString());
				}
			}
		});

		prefMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {

					prefShell = new FrameAccountabilityPreferencesShell(
							appContext, mainComposite.getShell());
					prefShell
					.setValuesFromViewConfiguration(viewConfig);
					prefShell.getShell()
					.addDisposeListener(new DisposeListener() {

						@Override
						public void widgetDisposed(
								final DisposeEvent event) {
							try {

								if (!prefShell
										.wasCanceled()) {
									prefShell
									.getValuesIntoViewConfiguration(viewConfig);
									updateTreeFontAndColors();
								}
							} catch (final Exception ex) {
								ex.printStackTrace();
								trace
								.error("Error handling preference window closure "
										+ ex.toString());
							} finally {
								prefShell = null;
								prefMenuItem.setEnabled(true);
							}
						}
					});
					prefMenuItem.setEnabled(false);
					prefShell.open();
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error handling preferrences menu item "
							+ ex.toString());
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}

		});

		treeListener = new TreeItemSelectionListener();
		tree.addListener(SWT.Selection, treeListener);
	}

	private void createVcNodes() {
		final List<Integer> vcidList = appContext.getBean(MissionProperties.class).getAllDownlinkVcids();
		if (vcidList == null) {
			new VirtualChannelTreeNode(0);
		} else {
			for (final Integer vcid : vcidList) {
				new VirtualChannelTreeNode(vcid);
			}
		}
	}

	/**
     * {@inheritDoc}
	 * 
	 * @see jpl.gds.perspective.view.View#getViewConfig()
	 */
	@Override
	public IViewConfiguration getViewConfig() {
		return viewConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}

	/**
     * {@inheritDoc}
	 * 
	 * @see jpl.gds.perspective.view.View#clearView()
	 */
	@Override
	public void clearView() {
	    try {
	        syncFlag.acquire();

	        final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
	        for (final Iterator<VirtualChannelTreeNode> i = vcs.iterator(); i.hasNext();) {
	        	final VirtualChannelTreeNode vc = i.next();
	            if (vc.isExpected()) {
	                vc.node.removeAll();
	                vc.node.setImage(blueImage);
	            } else {
	                i.remove();	
	                vc.node.dispose();
	                vc.node = null;
	            }
	        }
	        badFrames.clear();
	    } catch (final InterruptedException e) {
	    	e.printStackTrace();
            trace.error("Error clearing frame accountability view " + e.toString());

	    } finally {
	    	syncFlag.release();
	    }
	}

	/**
	 * Listens for selections made in the tree
	 *
	 */
	private class TreeItemSelectionListener implements Listener {
		@Override
		public void handleEvent(final Event e) {
			try {
				if (tree.getSelectionCount() == 0) {

				} else {
					final TreeItem[] selection = tree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final TreeItem item = selection[0];
					final TreeNode node = (TreeNode)item.getData("node");

					if (node != null && node.type == TreeNodeType.VC) {
						badMenuItem.setEnabled(true);
						reportMenuItem.setEnabled(true);
					} else {
						badMenuItem.setEnabled(false);
						reportMenuItem.setEnabled(false);
					}
				}
			} catch ( final Exception eE ) {
				TraceManager.getDefaultTracer().error( "tree Listener failed to catch exception in ViewEditorShell" );

				eE.printStackTrace();
			}
		}
	}

	/**
	 * Enumeration of all the tree node types
	 *
	 */
	private enum TreeNodeType {
		VC,
		IN_SYNC,
		LOSS_OF_SYNC,
		OUT_OF_SYNC,
		BAD_FRAME,
		FRAME_SEQ_ANOMALY,
		IN_SYNC_FRAMES,
		COMMAND;
	}

	/**
	 * Represents a single node in the tree. Stores the start event time, 
	 * end event time, tree node type, child node and parent node
	 *
	 */
	private class TreeNode {
		protected TreeItem node;
		private long startEventTime;
		private long endEventTime;
		private final TreeNodeType type;
		protected TreeNode parent;

		public TreeNode(final TreeNodeType treeType, final String label, final TreeNode parent, final int position) {
			this.parent = parent;
			if (parent == null) {
				if (position == -1) {
					node = new TreeItem(tree, SWT.NONE);
				} else {
					node = new TreeItem(tree, SWT.NONE, position);
				}
			} else {
				if (position == -1) {
					node = new TreeItem(parent.getTreeItem(), SWT.NONE);
				} else {
					node = new TreeItem(parent.getTreeItem(), SWT.NONE, position);					
				}
			}
			node.setText(label);
			node.setData("node", this);
			type = treeType;
		}

		public TreeItem getTreeItem() {
			return node;
		}

        public void setStartEventTime(final IAccurateDateTime time) {
			startEventTime = time.getTime();
			endEventTime = startEventTime;
			node.setData("startEventTime", startEventTime);
			node.setData("endEventTime", endEventTime);
		}

        public void setEndEventTime(final IAccurateDateTime time) {
			endEventTime = time.getTime();
			node.setData("endEventTime", endEventTime);
		}
	}

	/**
	 * Represents a virtual channel tree node. Stores the Virtual Channel ID.
	 *
	 */
	private class VirtualChannelTreeNode extends TreeNode {
		private final int vcid;
		private final boolean expected;

		public VirtualChannelTreeNode(final int vcid) {
			this(vcid, true);
		}

		public VirtualChannelTreeNode(final int vcid, final boolean isExpected) {
			super(TreeNodeType.VC, "VC " + vcid + " Frame Events" + (isExpected ? "" : " (Unexpected VC)"), null, -1);
			this.vcid = vcid;
	
			vcMap.put(this.vcid, this);
			expected = isExpected;
			if (expected) {
				node.setImage(blueImage);
			} else {
				node.setImage(yellowImage);
			}
		}

		public boolean isExpected() {
			return expected;
		}
	}

	/**
	 * Represents an In Sync tree node
	 *
	 */
	private class InSyncTreeNode extends TreeNode {

		public InSyncTreeNode(final TreeNode parent, final IFrameEventMessage message, final int position) {
			super(TreeNodeType.IN_SYNC, message.getEventTimeString() + ": In Sync", parent, position);
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(message.getOneLineSummary());
			setStartEventTime(message.getEventTime());
			node.setImage(greenImage);
			this.parent.node.setImage(greenImage);
		}
	}

	/**
	 * Represents a loss of sync tree node
	 *
	 */
	private class LossOfSyncTreeNode extends TreeNode {

		public LossOfSyncTreeNode(final TreeNode parent, final ILossOfSyncMessage message, final int position) {
			super(TreeNodeType.LOSS_OF_SYNC, message.getEventTimeString() + ": Loss Of Sync", parent, position);
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(message.getOneLineSummary());
			setStartEventTime(message.getEventTime());
			node.setImage(redImage);
			this.parent.node.setImage(redImage);
		}
	}

	/**
	 * Represents a frame sequence anomaly tree node
	 *
	 */
	private class FrameSeqAnomalyTreeNode extends TreeNode {

		public FrameSeqAnomalyTreeNode(final TreeNode parent, final IFrameSequenceAnomalyMessage message, final int position) {
			super(TreeNodeType.FRAME_SEQ_ANOMALY, "", parent, position);
			switch (message.getLogType()) {
			case TF_GAP:
				node.setText(message.getEventTimeString() + ": Frame Gap at VCFC " + message.getExpectedVcfc());
				break;

			case TF_REGRESSION:
				node.setText(message.getEventTimeString() + ": Frame Regression at VCFC " + message.getExpectedVcfc());
				break;

			case TF_REPEAT:
				node.setText(message.getEventTimeString() + ": Frame Repeat at VCFC " + message.getExpectedVcfc());
				break;
			}
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(message.getOneLineSummary());	
			setStartEventTime(message.getEventTime());
			node.setImage(yellowImage);
		}
	}

	/**
	 * Represents an in sync frame tree node. Stores the latest summary.
	 *
	 */
	private class InSyncFramesTreeNode extends TreeNode {

		private FrameSummaryRecord latestSum;

        public InSyncFramesTreeNode(final TreeNode parent, final int position, final List<FrameSummaryRecord> sums,
                final IAccurateDateTime eventTime, final int vcid) {
			super(TreeNodeType.IN_SYNC_FRAMES, "", parent, position);
			setStartEventTime(eventTime);
			latestSum = findLatestSummary(sums);
			node.setImage(greenImage);
			node.setText(format.format(eventTime) + ": In Sync Frames through VCFC " + latestSum.getSequenceCount());
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText("Last VCFC=" + latestSum.getSequenceCount() + ", Last ERT=" + latestSum.getLastErtStr() + ", Last Frame Type=" + latestSum.getFrameType());
		}

        public void addFrameSummary(final List<FrameSummaryRecord> sums, final IAccurateDateTime eventTime) {
			final FrameSummaryRecord newSum = findLatestSummary(sums);
			if (latestSum.getSequenceCount() == newSum.getSequenceCount()) {
				return;
			}
			latestSum = newSum;
			setEndEventTime(eventTime);
			node.setText(format.format(eventTime) + ": In Sync Frames through VCFC " + latestSum.getSequenceCount());
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText("Last VCFC=" + latestSum.getSequenceCount() + ", Last ERT=" + latestSum.getLastErtStr() + ", Last Frame Type=" + latestSum.getFrameType());        	
		}

		private FrameSummaryRecord findLatestSummary(final List<FrameSummaryRecord> sums) {
			IAccurateDateTime latestErt;
			FrameSummaryRecord candidate = latestSum;
			long latestVcfc = 0;
			if (latestSum == null) {
				latestErt = new AccurateDateTime(0);
			} else {
				latestErt = latestSum.getLastErt();
				latestVcfc = latestSum.getSequenceCount();
			}
			for (final FrameSummaryRecord sum : sums) {
				if (sum.getLastErt().getTime() > latestErt.getTime() ||
						sum.getSequenceCount() > latestVcfc) {
					candidate = sum;
				}
			}
			return candidate;
		}
	}

	/**
	 * Represents an out of sync tree node. Stores the byte count and the 
	 * first out of sync message.
	 *
	 */
	private class OutOfSyncTreeNode extends TreeNode {
		private long byteCount;
		private final IOutOfSyncDataMessage firstMessage;

		public OutOfSyncTreeNode(final TreeNode parent, final IOutOfSyncDataMessage m, final int vcid, final int position) {
			super(TreeNodeType.OUT_OF_SYNC, "", parent, position);
			firstMessage = m;
			node.setImage(yellowImage);
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(m.getOneLineSummary());	
			byteCount = m.getOutOfSyncBytesLength();
			node.setText(m.getEventTimeString() + ": Out of Sync Data (" + byteCount + " bytes)");
			setStartEventTime(m.getEventTime());
		}

		public void addMessage(final IOutOfSyncDataMessage m, final int vcid) {
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(m.getOneLineSummary());	
			byteCount += m.getOutOfSyncBytesLength();
			node.setText(firstMessage.getEventTimeString() + ": Out of Sync Data (" + byteCount + " bytes)");
			setEndEventTime(m.getEventTime());
		}
	}

	/**
	 * Represents a bad frame tree node.
	 *
	 */
	private class BadFrameTreeNode extends TreeNode {

		public BadFrameTreeNode(final TreeNode parent, final IFrameEventMessage message, final int position) {
			super(TreeNodeType.BAD_FRAME, message.getStationInfo().getErtString() + ": Bad Frame at VCFC " + message.getFrameInfo().getSeqCount(), parent, position);
			node.setImage(redImage);
			final TreeItem textNode = new TreeItem(node, SWT.NONE);
			textNode.setText(message.getOneLineSummary());	
			setStartEventTime(message.getEventTime());
		}
	}
	
	/**
	 * Represents a command tree node
	 *
	 */
	private class CommandTreeNode extends TreeNode {
		public CommandTreeNode(final TreeNode parent, final ICommandMessage m, final int position) {
			super(TreeNodeType.COMMAND, "", parent, position);
			if (m.isType(CommandMessageType.FlightSoftwareCommand)) {
				node.setText(m.getEventTimeString() + ": FSW Command " + m.getCommandString());
			} else if (m.isType(CommandMessageType.SseCommand)) {
				node.setText(m.getEventTimeString() + ": SSE Command " + m.getCommandString());
			} else if (m.isType(CommandMessageType.HardwareCommand)) {
				node.setText(m.getEventTimeString() + ": HW Command " + m.getCommandString());
			}
			node.setImage(greenImage);
			this.setStartEventTime(m.getEventTime());
			this.setEndEventTime(m.getEventTime());
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
	 */
	@Override
	public void messageReceived(final IMessage[] m) {
		if (m.length == 0) {
			return;
		}
		for (int i = 0; i < m.length; i++) {
			displayMessage(m[i]);
		}
	}

	/**
	 * Handles incoming frame messages if the frame composite is not disposed
	 * 
	 * @param msg frame message that will be processed
	 */
	public void displayMessage(final IMessage msg) {
		if (parent.isDisposed()) {
			return;
		}
		parent.getDisplay().asyncExec(new Runnable () {
			
			@Override
            public String toString() {
				return "FrameAccountabilityComposite.displayMessage.Runnable";
			}
			
			@Override
			public void run () {
			    if (mainComposite.isDisposed()) {
			        return;
			    }
			    try { 
			        syncFlag.acquire();
			    } catch (final InterruptedException e) {}
				try {
					trace.trace("frame accountability view is processing message"); 
					if (msg instanceof ILossOfSyncMessage) {
						handleLossOfSyncMessage((ILossOfSyncMessage)msg);
					} else if (msg instanceof IFrameSequenceAnomalyMessage) {
						handleFrameSeqAnomalyMessage((IFrameSequenceAnomalyMessage)msg);
					} else if (msg instanceof IFrameEventMessage) {
					    if (((IFrameEventMessage)msg).getLogType().equals(LogMessageType.INVALID_TF)) {
					        handleBadFrameMessage((IFrameEventMessage)msg);
					    } else if (((IFrameEventMessage)msg).getLogType().equals(LogMessageType.IN_SYNC)) {
					        handleInSyncMessage((IFrameEventMessage)msg);
					    }
					} else if (msg instanceof IOutOfSyncDataMessage){
						handleOutOfSyncBytesMessage((IOutOfSyncDataMessage)msg);
					} else if (msg instanceof IFrameSummaryMessage){
						handleFrameSyncSumMessage((IFrameSummaryMessage)msg);
					} else {
						handleCommandMessage((ICommandMessage)msg);
					}
					
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
				    syncFlag.release();
				}
			}
		});
	}

	private void handleCommandMessage(final ICommandMessage msg) {
		if (!viewConfig.isOverlayCommands()) {
			return;
		}
	
		final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
		for (final VirtualChannelTreeNode vc : vcs) {
		    new CommandTreeNode(vc, msg, findTreeInsertPosition(msg.getEventTime(), vc.vcid));
		}
	}

	private void handleInSyncMessage(final IFrameEventMessage m) {
	    final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
	    for (final VirtualChannelTreeNode vc : vcs) {
	        new InSyncTreeNode(vc, m, findTreeInsertPosition(m.getEventTime(), vc.vcid));
	    }
	}

	private void handleLossOfSyncMessage(final ILossOfSyncMessage m) {
		final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
		for (final VirtualChannelTreeNode vc : vcs) {
			new LossOfSyncTreeNode(vc, m, findTreeInsertPosition(m.getEventTime(), vc.vcid));		
		}
	}

	private void handleFrameSeqAnomalyMessage(final IFrameSequenceAnomalyMessage m) {
	
		VirtualChannelTreeNode vcNode = vcMap.get(m.getFrameInfo().getVcid());
		if (vcNode == null) {
			vcNode = new VirtualChannelTreeNode(m.getFrameInfo().getVcid(), false);
		}
		new FrameSeqAnomalyTreeNode(vcNode, m, findTreeInsertPosition(m.getEventTime(), vcNode.vcid));
	}

	private void handleBadFrameMessage(final IFrameEventMessage m) {
	  
		VirtualChannelTreeNode vcNode = vcMap.get(m.getFrameInfo().getVcid());
		if (vcNode == null) {
			vcNode = new VirtualChannelTreeNode(m.getFrameInfo().getVcid(), false);
		}
		new BadFrameTreeNode(vcNode, m, findTreeInsertPosition(m.getEventTime(), vcNode.vcid));
		badFrames.add(m);
	}

	private void handleOutOfSyncBytesMessage(final IOutOfSyncDataMessage m) {
	  
		final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
		for (final VirtualChannelTreeNode vc : vcs) {
			final OutOfSyncTreeNode node = findExistingOutOfSyncNode(m.getEventTime(), vc.vcid); 
			if (node == null) {
				new OutOfSyncTreeNode(vc, m, vc.vcid, findTreeInsertPosition(m.getEventTime(), vc.vcid));		
			} else {
				node.addMessage(m, vc.vcid);
			}
		}
	}

	private void handleFrameSyncSumMessage(final IFrameSummaryMessage m) {
	 
		final Map<String, FrameSummaryRecord> map = m.getFrameSummaryMap();
		if (map == null) {
			return;
		}
		final Collection<FrameSummaryRecord> sums = map.values();
		for (final FrameSummaryRecord sum : sums) {
			VirtualChannelTreeNode vcNode = vcMap.get((int)sum.getVcid());
			if (vcNode == null) {
				vcNode = new VirtualChannelTreeNode((int)sum.getVcid(), false);
			}
			final InSyncFramesTreeNode isNode = findExistingInSyncFramesNode(m.getEventTime(), vcNode.vcid);
			final List<FrameSummaryRecord> sumsForVcid = getFrameSummariesForVcid(map, vcNode.vcid);
			if (sumsForVcid == null) {
				continue;
			}
			if (isNode == null) {
				new InSyncFramesTreeNode(vcNode, findTreeInsertPosition(m.getEventTime(), vcNode.vcid), sumsForVcid, m.getEventTime(), vcNode.vcid);
			} else {
				isNode.addFrameSummary(sumsForVcid, m.getEventTime()); 
			}			
		}
	}

	private List<FrameSummaryRecord> getFrameSummariesForVcid(final Map<String, FrameSummaryRecord> sums, final int vcid) {
		final ArrayList<FrameSummaryRecord> result = new ArrayList<FrameSummaryRecord>();
		final Collection<FrameSummaryRecord> sumCol = sums.values();
		for (final FrameSummaryRecord sum: sumCol) {
			if (sum.getVcid() == vcid) {
				result.add(sum);
			}
		}
		if (result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}

    private int findTreeInsertPosition(final IAccurateDateTime forTime, final int vcid) {

	    final Collection<VirtualChannelTreeNode> vcs = vcMap.values();
	    for (final VirtualChannelTreeNode vc : vcs) {
	        if (vc.vcid != vcid) {
	            continue;
	        }
	        final TreeItem[] items = vc.getTreeItem().getItems();
	        if (items.length == 0) {
	            return -1;
	        }
	        final long checkTime = forTime.getTime();
	        for (int i = items.length - 1; i >= 0; i--) {
	            final Long endTime = (Long)items[i].getData("endEventTime");
	            if (checkTime >= endTime) {
	                return i + 1;
	            } 
	        }
	    }
	    return 0;
	}

    private OutOfSyncTreeNode findExistingOutOfSyncNode(final IAccurateDateTime forTime, final int vcid) {

		// Find the tree location where the node would go if it was a new 
		// out of sync node, based upon event time
		final int initialPos = findTreeInsertPosition(forTime, vcid);

		// If the tree is empty, we require a new node, not an existing one; return null
		if (initialPos == -1) {
			return null;
		}

		final long checkTime = forTime.getTime();

		VirtualChannelTreeNode vcNode = null;
		final Collection<VirtualChannelTreeNode> vcs = vcMap.values();

		for (final VirtualChannelTreeNode vc : vcs) {
			if (vc.vcid == vcid) {
				vcNode = vc;
				break;
			}
		}
		if (vcNode == null) {
			return null;
		}
		final TreeItem[] items = vcNode.getTreeItem().getItems();

		// If the insert position is 0, and there is an out of sync node there, see
		// if this new data should be attached to it
		if (initialPos == 0 && items.length != 0) {
			final TreeItem item = items[0];
			final TreeNode node = (TreeNode)item.getData("node");
			if (node.type == TreeNodeType.OUT_OF_SYNC) {

				// If time for the new node is less than the start time of the existing node,
				// we need a new node; return null
				if (checkTime < node.startEventTime) {
					return null;
				}
				// Otherwise, we want to attach to this out of sync node
				return (OutOfSyncTreeNode)node;
			}
		}

		// Otherwise, check to see if the previous node was an out of sync node. If
		// so, we want to attach to it.
		final TreeItem item = items[initialPos - 1];
		final TreeNode node = (TreeNode) item.getData("node");

		if (node.type == TreeNodeType.OUT_OF_SYNC) {
			return (OutOfSyncTreeNode) node;
		}

		return null;
	}

    private InSyncFramesTreeNode findExistingInSyncFramesNode(final IAccurateDateTime forTime, final int vcid) {

		// Find the tree location where the node would go if it was a new 
		// out of sync node, based upon event time
		final int initialPos = findTreeInsertPosition(forTime, vcid);

		// If the tree is empty, we require a new node, not an existing one; return null
		if (initialPos == -1) {
			return null;
		}

		final long checkTime = forTime.getTime();

		VirtualChannelTreeNode vcNode = null;

		final Collection<VirtualChannelTreeNode> vcs = vcMap.values();

		for (final VirtualChannelTreeNode vc : vcs) {
			if (vc.vcid == vcid) {
				vcNode = vc;
				break;
			}
		}
		if (vcNode == null) {
			return null;
		}
		final TreeItem[] items = vcNode.getTreeItem().getItems();

		// If the insert position is 0, and there is an in sync frames node there, see
		// if this new data should be attached to it
		if (initialPos == 0 && items.length != 0) {
			final TreeItem item = items[0];
			final TreeNode node = (TreeNode)item.getData("node");
			if (node.type == TreeNodeType.IN_SYNC_FRAMES) {

				// If time for the new node is less than the start time of the existing node,
				// we need a new node; return null
				if (checkTime < node.startEventTime) {
					return null;
				}
				// Otherwise, we want to attach to this out of sync node
				return (InSyncFramesTreeNode)node;
			}
		}

		// Otherwise, check to see if the previous node was an in sync frames node. If
		// so, we want to attach to it.
		final TreeItem item = items[initialPos - 1];
		final TreeNode node = (TreeNode) item.getData("node");

		if (node.type == TreeNodeType.IN_SYNC_FRAMES) {
			return (InSyncFramesTreeNode) node;
		}

		return null;
	}

	private void writeVcToFile(final String filename, final VirtualChannelTreeNode node) {
		try {
			final FileOutputStream fw = new FileOutputStream(filename);
			writeNode(node.getTreeItem(), 1, fw);
			fw.close();
		} catch (final Exception e) {
			e.printStackTrace();
			SWTUtilities.showErrorDialog(mainComposite.getShell(), "Error Saving Report", 
					"There was an error saving the VC report: " + e.toString());
		}
	}

	private void writeNode(final TreeItem node, final int level, final OutputStream stream) throws IOException {
		for (int i = 1; i < level; i++) {
			stream.write("   ".getBytes());
		}
		final String text = node.getText();
		stream.write(text.getBytes());
		stream.write("\n".getBytes());
		final TreeItem[] children = node.getItems();
		if (children == null || children.length == 0) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			writeNode(children[i], level + 1, stream);
		}
	}
}
