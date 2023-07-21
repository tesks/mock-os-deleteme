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
package jpl.gds.product.automation.hibernate.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.gui.rtviewers.AbstractGuiRealTimePanel;
import jpl.gds.product.automation.hibernate.gui.rtviewers.ActionViewerPanel;
import jpl.gds.product.automation.hibernate.gui.rtviewers.LineageViewerPanel;
import jpl.gds.product.automation.hibernate.gui.rtviewers.LogsViewerPanel;
import jpl.gds.product.automation.hibernate.gui.rtviewers.ProcessViewerPanel;
import jpl.gds.product.automation.hibernate.gui.rtviewers.StatusViewerPanel;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Main application gui for controlling and viewing information about the pdpp
 * automation.
 * 
 *
 * MPCS-8182 - 08/08/16 - Added to and updated for AMPCS
 * MPCS-8391 - 08/29/16 - Added CommandLineApp interface, used
 *          for showing help and version options
 * MPCS-8449 - 12/14/16 - Changed this class to no longer be the entry point
 * 		for chill_automation_gui. The Gui is now started from the new entry point AFTER 
 * 		parsing command line to avoid starting the Gui unnecessarily.
 */
@SuppressWarnings("serial")
public class ProductAutomationAdministrationControl extends JFrame {
	// This needs to happen right away before the cache or hibernate is set up.  The tracemanager
	// sets up the appenders for hibernate and all other things.  If this is not done there will 
	// be log4j warnings.
	private final Tracer tracer;

	private static final int MIN_X = 1350;
	private static final int MIN_Y = 800;
	
	// Menu info
	private static final String HELP = "Help";
	private static final String ABOUT = "About";
	private static final String LEGEND = "Show Legend";
	private static final String ACTION_NAME = "Name";
	private static final String VERSION_STRING = "Version 0.1";
	private static final String FRAME_HEADER = "Product Automation Control and Reporting Tool";
	
	// Tab labels.
	private static final String MONITOR_TAB = "Status Monitor View";
	private static final String LINEAGE_TAB = "Lineage View";
	private static final String PROCESS_TAB = "Process View";
	private static final String ACTION_TAB = "Action View";
	private static final String LOGS_TAB = "Logs View";
	
	
	// Custom panels.  They could all be kept as jpanels but want to be able to get stuff from them.
	private QueryInputPanel paramPanel;
	private ControlPanel controlPanel;
	
	private AbstractGuiRealTimePanel statusPanel;
	private AbstractGuiRealTimePanel lineagePanel;
	private AbstractGuiRealTimePanel processPanel;
	private AbstractGuiRealTimePanel actionPanel;
	private AbstractGuiRealTimePanel logsPanel;
	
	// Linux GTK is stupid.  I have to keep track of these
	private List<AbstractGuiRealTimePanel> tabs;
	
	private JTabbedPane tabbed;
	private JDialog legend;
	private final ApplicationContext springContext;
	
	/**
	 * 
	 */
	public ProductAutomationAdministrationControl(final ApplicationContext appContext) {
		super(FRAME_HEADER);
		this.springContext = appContext;
        this.tracer = TraceManager.getDefaultTracer(appContext);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/**
	 * Returns the frame.  Is used to set parent of dialoges in event handlers that do not have 
	 * access to the frame. 
	 * 
	 * @return frame
	 */
	private JFrame getFrame() {
		return this;
	}

	private void createMenuBar() {
		final JMenuBar mb = new JMenuBar();
		mb.setLayout(new BorderLayout());
		
		// Add the about and the legend items.
		final JMenu menu = new JMenu(HELP);
		
		// Simple about dialogue.
		final JMenuItem about = new JMenuItem(new AbstractAction() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				JOptionPane.showMessageDialog(getFrame(), VERSION_STRING, ABOUT, JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		// Gets the name from the action.  Need to set it here.
		about.getAction().putValue(ACTION_NAME, ABOUT);
		
		// Show the help legend.  Will be a seperate frame.  Do not want it to be modal so it can 
		// stay up if the user wants to see it all the time.
		
		final JMenuItem leg = new JMenuItem(new AbstractAction() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (legend != null) {
					// Just destroy it and recreate.
					legend.dispose();
				}
				
				legend = new LegendDialog(getFrame());
				legend.setModal(false);
				legend.setVisible(true);
			}
		});
		
		leg.getAction().putValue(ACTION_NAME, LEGEND);
		
		menu.add(about);
		menu.add(leg);
		
		mb.add(menu, BorderLayout.EAST);
		setJMenuBar(mb);		
	}
	
	/**
	 * Initializes the Gui layout and panels
	 */
	public void initGui() {
		setLayout(new GridBagLayout());
		
		legend = null;
		
		final ProductAutomationUserDAO userDao = springContext.getBean(ProductAutomationUserDAO.class);
		final ProductAutomationClassMapDAO classMapDao = springContext.getBean(ProductAutomationClassMapDAO.class);
		final AncestorMap ancestorMap = springContext.getBean(AncestorMap.class);
		final IFswToDictionaryMapper mapper = springContext.getBean(IFswToDictionaryMapper.class);

		paramPanel = new QueryInputPanel(userDao, classMapDao, springContext);
		paramPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		controlPanel = new ControlPanel(classMapDao);
		controlPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		statusPanel = new StatusViewerPanel(paramPanel);
		
		lineagePanel = new LineageViewerPanel(paramPanel, ancestorMap);
		
		processPanel = new ProcessViewerPanel(paramPanel, mapper);
		actionPanel = new ActionViewerPanel(paramPanel);
		logsPanel = new LogsViewerPanel(paramPanel, springContext);

		tabs = Arrays.asList(statusPanel, lineagePanel, processPanel, actionPanel, logsPanel);
		
		createMenuBar();

		doMainLayout();
		addListeners();
	}
	
	/**
	 * Initializes Look and Feel for the Gui
	 * 
	 * @return true if successfully initialized, false otherwise
	 */
	public boolean initLookAndFeel() { 
		boolean isSet = false;
		
		try {
		    for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("GTK+".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            isSet = true;
		            break;
		        }
		    }
		} catch (final Exception e) {
			isSet = false;
		}	
		
		if (!isSet) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (final Exception e) {
				// Do nothing let the deal take over.
				tracer.warn("System look and feel error: " + e.getMessage());
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Starts the Gui 
	 */
	public void startGui() { 
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				
				final Image i = IconFactory.applicationImage();
				
				if (i != null) {
					setIconImages(Arrays.asList(i, i));
				}
				
				start();
			}
		});
	}
	
	private void enableQueryOptions(final Component cmp) {
		if (statusPanel.equals(cmp) || lineagePanel.equals(cmp)) {
			paramPanel.enableStatusOptions();
		} else if (processPanel.equals(cmp)) {
			paramPanel.enableProcessOptions();
		} else if (actionPanel.equals(cmp)) {
			paramPanel.enableActionOptions();
		} else if (logsPanel.equals(cmp)) {
			paramPanel.enableLogsOptions();
		} else {
			// Do nothing.
		}
	}
	
	private void addListeners() {
		final ChangeListener cl = new ChangeListener() {
			
			@Override
			public void stateChanged(final ChangeEvent e) {
				final Component cmp = tabbed.getSelectedComponent();
				
				for (final AbstractGuiRealTimePanel rt : tabs) {
					if (rt.equals(cmp)) {
						enableQueryOptions(rt);
						rt.startTimer();
					} else {
						rt.stopTimer();
					}
				}
			}
		};
		
		tabbed.addChangeListener(cl);
	}
	
	/**
	 * Adds all frame components to the grid.
	 */
	private void doMainLayout() {
		// Set the min size.
		setMinimumSize(new Dimension(MIN_X, MIN_Y));
		
		// gridx and gridy will be used as the position of all stuff.  Things should be added
		// in order from left -> right, top -> down and update the gridx and gridy accordingly. 
		// Will make updates and inserts easier in the future, and make it more clean.
		int gridx = 0;
		int gridy = 0;
		
		add(paramPanel, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		add(controlPanel, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		// Create a tabbed pane and add all the panels that need to go in that.
		tabbed = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbed.add(MONITOR_TAB, statusPanel);
		tabbed.add(LINEAGE_TAB, lineagePanel);
		tabbed.add(LOGS_TAB, logsPanel);
		tabbed.add(ACTION_TAB, actionPanel);
		tabbed.add(PROCESS_TAB, processPanel);
		
		gridx = 0;
		
		add(tabbed, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					1, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
	}
	
	/**
	 * Starts up the application. Sets the size and makes it visible.
	 */
	public void start() {
		// TODO temp solution.  need to make the perm.
		setLocation(1487, 879);
		pack();
		setVisible(true);
	}
	


}
