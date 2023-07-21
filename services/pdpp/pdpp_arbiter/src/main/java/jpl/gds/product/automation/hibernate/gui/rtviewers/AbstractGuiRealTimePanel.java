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
package jpl.gds.product.automation.hibernate.gui.rtviewers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.table.TableCellRenderer;

import jpl.gds.product.automation.hibernate.gui.IconFactory;
import jpl.gds.product.automation.hibernate.gui.UserInputInterface;
import jpl.gds.product.automation.hibernate.gui.models.TableColumnAdjuster;

/**
 * Abstract real time gui panel.  Has buttons to execute, turn real time on, clear the contents, 
 * remove a row, export as well as show the full path.  All buttons are protected so if you need 
 * to disable you can do it directly.  
 * 
 * The content pain is a panel and set up with a gridbag layout.  
 * 
 * All of the buttons have default action listeners set up to call the respective abstract 
 * methods.  Any threads or invoke laters will have to be handled in the abstract method 
 * implementations.
 * 
 * For the execute and the realtime timers, checks the value of running and will do a no-op
 * if this is true.  This will prevent a back log of execution threads from running the same 
 * query many times on top of each other.  Use the setters to toggle these values.  It is up to 
 * the implementor of the subclasses to set this value when a query is being executed.
 * 
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractGuiRealTimePanel extends JPanel implements GuiRealTime {
	// Button Tooltips
	private static final String CLEAR_TT = "Clear all data from the table";
	private static final String DELETE_TT = "Remove selected rows from the table";
	private static final String RT_TT = "If enabled statuses will be updated in real time";
	private static final String EXECUTE_TT = "Execute a query with the paramters in the parameter window";
	private static final String FILE_PATH_TT = "If checked the full file path will be displayed";
	private static final String EXPORT_TT = "Exports the contents of the table to a file.  Not supported in MPCS G8.";
	
	private static final int TIMER_DELAY_MS = 2000;

	/** starting height of table cells */
	protected static final int ROW_HEIGHT = 20;

	private JPanel contentPanel;
	
	/** Clear button */
	protected JButton clear;
	/** Delete button */
	protected JButton delete;
	/** Execute button */
	protected JButton execute;
	/** Export button */
	protected JButton export;
	/** Toggle file path button - Show full file path or file name only */
	protected JToggleButton includeFilePath;
	/** Toggle real-time button - Update data in real time or not */
	protected JToggleButton realTime;
	/** timer used for updating displayed real time results */
	protected Timer rtTimer;
	/**  */
	protected UserInputInterface userInput;
	/** Tool bar above the results pane */
	protected JToolBar topToolBar;
	/** Tool bar below the results pane */
	protected JToolBar bottomToolBar;
	/** Progress bar located in the bottom tool bar */
	protected JProgressBar progress;
	/** Adjusts the width of columns when triggered */
	protected TableColumnAdjuster adjuster;
	
	private boolean running;
	
	/**
	 * constructor for a results panel tied to the given user input
	 * 
	 * @param userInput
	 *            the user input panel in the GUI
	 */
	public AbstractGuiRealTimePanel(UserInputInterface userInput) {
		super(new GridBagLayout());
		
		this.userInput = userInput;
		
		init();
	}
	
	private void init() {
		running = false;
		
		// Not a standard variable for all tables.  Must set to use them.
		adjuster = null;
		progress = new JProgressBar();
		
		clear = new JButton(IconFactory.clear());
		clear.setToolTipText(CLEAR_TT);
		clear.setFocusable(false);
		
		export = new JButton(IconFactory.export());
		export.setToolTipText(EXPORT_TT);
		export.setFocusable(false);
		
		delete = new JButton(IconFactory.remove());
		delete.setToolTipText(DELETE_TT);
		delete.setFocusable(false);
		
		execute = new JButton(IconFactory.execute());
		execute.setToolTipText(EXECUTE_TT);
		execute.setFocusable(false);
		
		includeFilePath = new JToggleButton(IconFactory.includeFilePath());
		includeFilePath.setToolTipText(FILE_PATH_TT);
		includeFilePath.setFocusable(false);
		
		realTime = new JToggleButton(IconFactory.rtStart());
		realTime.setToolTipText(RT_TT);
		realTime.setFocusable(false);
		
		contentPanel = new JPanel(new GridBagLayout());
		
		addActionListeners();
		doPanelLayout();
	}
	
	/**
	 * Get the user input panel
	 * @return the UserInputInterface used to configure the results in this panel
	 */
	protected UserInputInterface getQueryPanel() {
		return userInput;
	}
	
	/**
	 * Get if this panel is running in real time
	 * @return TRUE if running in real time, FALSE if not
	 */
	protected boolean isRunning() {
		return running;
	}
	
	/**
	 * Start this panel to update in real time
	 */
	protected void setRunning() {
		running = true;
	}
	
	/**
	 * Stop this panel from updating in real time
	 */
	protected void setNotRunning() {
		running = false;
	}
	
	/**
	 * Adds the listeners that will change the icon for the toggle buttons.
	 */
	private void addActionListeners() {
		rtTimer = new Timer(TIMER_DELAY_MS, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!isRunning()) {
					executeQuery();
				}
			}
		});
		
		includeFilePath.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (includeFilePath.isSelected()) {
					includeFilePaths();
					includeFilePath.setIcon(IconFactory.excludeFilePath());
				} else {
					excludeFilePaths();
					includeFilePath.setIcon(IconFactory.includeFilePath());
				}
			}
		});
		
		realTime.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (realTime.isSelected()) {
					startTimer();
					realTime.setIcon(IconFactory.rtStop());
					execute.setEnabled(false);
				} else {
					stopTimer();
					realTime.setIcon(IconFactory.rtStart());
					execute.setEnabled(true);
				}
			}
		});	
		
		clear.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				clearContents();
			}
		});
		
		delete.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				removeContents();
			}
		});		
		
		execute.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				executeQuery();
			}
		});
		
		export.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				exportContents();
			}
		});
	}
	
	private void doPanelLayout() {
		// Since all the buttons are icons, going to add a tool bar to the top.  
		topToolBar = new JToolBar(JToolBar.HORIZONTAL);
		topToolBar.setFloatable(false);
		topToolBar.setLayout(new GridBagLayout());
		topToolBar.setBorder(BorderFactory.createEtchedBorder());
		
		int gridx = 0;
		int gridy = 0;
		
		// Add all the buttons to the toolbar.
		topToolBar.add(realTime, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
			
		topToolBar.add(execute, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		topToolBar.add(Box.createHorizontalGlue(), 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		

		topToolBar.add(includeFilePath, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		topToolBar.add(delete, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		

		topToolBar.add(clear, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));				
		
		// Add the tool bar to the main panel.
		gridx = 0;
		
		add(topToolBar, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		add(contentPanel,
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					1, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(20, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));				

		bottomToolBar = new JToolBar(JToolBar.HORIZONTAL);
		bottomToolBar.setFloatable(false);
		bottomToolBar.setLayout(new GridBagLayout());
		bottomToolBar.setBorder(BorderFactory.createEtchedBorder());
		
		gridx = 0;
		
		add(bottomToolBar, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.SOUTH, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(5, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		gridx = 0;
		gridy = 0;

		bottomToolBar.add(progress, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		bottomToolBar.add(export, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.EAST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 0, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
	}
	
	public void setClearListener(ActionListener al) {
		clear.addActionListener(al);
	}

	public void setRemoveListener(ActionListener al) {
		delete.addActionListener(al);
	}
	
	public void setExecuteListener(ActionListener al) {
		execute.addActionListener(al);
	}
	
	public void setExportListener(ActionListener al) {
		export.addActionListener(al);
	}
	
	public void setFilePathListener(ActionListener al) {
		includeFilePath.addActionListener(al);
	}
	
	protected void doAdjust() {
		if (adjuster != null) {
			adjuster.adjustColumns();
		}
	}
	
	/**
	 * Enables the adjuster for the given table.  Sets a size listener to call the deal blkadf.
	 * 
	 * @param table
	 */
	protected void enableAdjuster(JTable table) {
		adjuster = new TableColumnAdjuster(table);
		
		getContentPanel().addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// Don't care
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				adjuster.adjustColumns();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// Don't care
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				// Don't care
			}
		});
	}
	
	/**
	 * Get the panel containing the results table
	 * 
	 * @return the JPanel containing the results table
	 */
	protected JPanel getContentPanel() {
		return contentPanel;
	}
	
	/**
	 * Get the timer used for controlling the frequency of real time updates.
	 * 
	 * @return the real time Timer
	 */
	protected Timer getTimer() {
		return rtTimer;
	}
	
	/**
     * Adds the specified component to the end of the content panel container.
     * Also notifies the layout manager to add the component to
     * the content panel's layout using the specified constraints object.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the content panel has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     *
     * @param     cmp the component to be added
     * @param     constraint an object expressing
     *                  layout constraints for this component
     * @exception NullPointerException if {@code comp} is {@code null}
     * 
     * @see  java.awt.Container#add()
     */
	protected void addContent(Component cmp, GridBagConstraints constraint) {
		getContentPanel().add(cmp, constraint);
	}

	/**
	 * Get the tool bar located just above the content panel. It contains the
	 * controls for updating the content panel
	 * 
	 * @return the JToolBar located above the content panel
	 */
	protected JToolBar getTopToolbar() {
		return topToolBar;
	}
	
	/**
	 * Get the tool bar located under the displayed results.
	 * 
	 * @return the JToolBar located at the bottom of the display
	 */
	protected JToolBar getBottomToolbar() {
		return bottomToolBar;
	}
	
	/**
	 * Starts the timer used for controlling the frequency of real time updates.
	 * Checks if the real time button is enabled and not already running.
	 */
	public void startTimer() {
		if (realTime.isEnabled() && realTime.isSelected() && !rtTimer.isRunning()) {
			rtTimer.start();
		}
	}
	
	/**
	 * Stops the real time timer.
	 */
	public void stopTimer() {
		if (realTime.isEnabled() && rtTimer.isRunning()) {
			rtTimer.stop();
		}
	}
	
	/**
	 * Sets the progress bar to working.
	 */
	public void startProgress() {
		progress.setIndeterminate(true);
	}
	
	/**
	 * Stops the progress bar.
	 */
	public void stopProgress() {
		progress.setIndeterminate(false);
	}
	
	/**
	 * Not all implementers will use tables, but for those that do this is a
	 * convenience method for setting the cell renderer. This will set all of
	 * the columns to use the same instance of the renderer.
	 * 
	 * @param table
	 *            The table to be rendered
	 * @param renderer
	 *            the renderer to display the table data.
	 */
	protected void setTableCellRenderer(JTable table, TableCellRenderer renderer) {
		for (int column = 0; column < table.getColumnCount(); column++) {
			table.getColumnModel().getColumn(column).setCellRenderer(renderer);
		}
	}
	/**
	 * Clear button action method.
	 */
	abstract public void clearContents();
	
	/**
	 * Export button action method.
	 */
	abstract public void exportContents();
	
	/**
	 * Remove button action method.
	 */
	abstract public void removeContents();
	
	/**
	 * Execute button action method.
	 */
	abstract public void executeQuery();
	
	/**
	 * When the include file path button is set to include, runs this method.
	 */
	abstract public void includeFilePaths();
	
	/**
	 * When the include file path button is set to exclude, runs this method.
	 */
	abstract public void excludeFilePaths();
}
