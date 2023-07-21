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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import org.springframework.context.ApplicationContext;

/**
 * A panel that show the breakdown of the query results for each panel.  If it is a logs table, just
 * clears out the list because they are not covered.  
 * 
 * Status, lineage use the same icons except.  Actions and processes use the same except for the 
 * downlink icon. Status and lineage keep counts for pending, completed and failed.  Actions keep cound
 * for pending, completed.  Processes have counts for running, not running.
 * 
 * This keeps everything very general.  There are at most 4 columns, one for the type and 3 for
 * count types.  Just replace the text with the proper stuff.
 * 
 * MPCS-8182 - 08/08/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class MetricsPanel extends JPanel {
	private static final List<String> STATUS_HEADERS = Arrays.asList("Type", "Completed", "Pending", "Failed");
	private static final List<String> ACTION_HEADERS = Arrays.asList("Type", "Completed", "Pending");
	private static final List<String> PROCESS_HEADERS = Arrays.asList("Type", "Running", "Not Running");
	private static final String DOWNLINK = "downlink";

	private List<String> statusTypes;
	private List<String> actionTypes;
	
	private static enum VIEW_TYPE {
		STATUS,
		ACTION,
		PROCESS,
		LOG,
		NOT_INIT
	}

	// The lookup is the row number which gives a list with the column jlabels in the list.
	private HashMap<Integer, HashMap<Integer, JLabel>> counts;
	private VIEW_TYPE viewType;
	
	// Interim count hashes for all stuff.  Only have one based on the type, so needs to get set
	// up each time.
	private HashMap<String, List<Integer>> tempCounts;

	protected final ApplicationContext appContext;
	
	/**
	 * Default constructor. Creates the metrics panel with no data.
	 * @param appContext
	 */
	public MetricsPanel(ApplicationContext appContext) {
		super(new GridBagLayout());
		this.appContext = appContext;
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		init();
	}
	
	private void init() {
		counts = new HashMap<Integer, HashMap<Integer,JLabel>>();
		tempCounts = new HashMap<String, List<Integer>>();

		ProductAutomationProperties properties = appContext.getBean(ProductAutomationProperties.class);
		actionTypes = properties.getCheckOrder();

		statusTypes = actionTypes;
		statusTypes.add(DOWNLINK);

		viewType = VIEW_TYPE.NOT_INIT;
		
		doPanelLayout();
	}
	
	private void doPanelLayout() {
		switch (viewType) {
			case STATUS:
				doSetup(STATUS_HEADERS, statusTypes);
				break;
			case ACTION:
				doSetup(ACTION_HEADERS, actionTypes);
				break;
			case PROCESS:
				doSetup(PROCESS_HEADERS, actionTypes);
				break;
			case LOG:
				disableCounts();
				break;
			case NOT_INIT:
				// Need to do the initial initialization.
				// Initialize the counts deal.
				doSetup(STATUS_HEADERS, statusTypes);
				viewType = VIEW_TYPE.STATUS;
			default:
				// Do nothing
				break;
		}
		
		revalidate();
	}
	
	/**
	 * Clears all the counts and disables???
	 */
	private void disableCounts() {
		removeAll();
	}
	
	/**
	 * Clears out all of the maps so that they can be set up again with the proper headers and counters.
	 */
	private void rebuildAllMaps(List<String> colHeaders, List<String> rHeaders) {
		// Remove everything from the layout
		removeAll();

		// Just rebuild the counts map.
		counts.clear();
		
		for (int row = 1; row <= rHeaders.size(); row++) {
			counts.put(row, new HashMap<Integer, JLabel>());
		}
	}
	
	/**
	 * Only run at startup.  Sets up all of the labels that will be used for all different tables.
	 * Assumes that the startup state will be in the status mode.  Will create the status view
	 * first and set the viewType to status.  
	 */
	private void doSetup(List<String> colHeaders, List<String> rHeaders) {
		rebuildAllMaps(colHeaders, rHeaders);
		
		for (String rh : rHeaders) {
			// Not all have 3 columns so just ignore the third if you don't need it.
			tempCounts.put(rh, Arrays.asList(0, 0, 0));
		}
		
		// Got all wrapped up and confused.  Going to do this in stages.  First, the column headers.  Then the
		// row headers.  Then the counts.

		// Column headers
		for (int col = 0; col < colHeaders.size(); col++){
			String header = colHeaders.get(col);
			JLabel hl = new JLabel(header);
			
			Insets inset = col == colHeaders.size() - 1 ? new Insets(10, 10, 0, 10) : new Insets(10, 10, 0, 0);
			add(hl, 
					new GridBagConstraints(col, //gridx, 
						0, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.CENTER, // anchor, 
						0, // fill - no fill for text controls.
						inset, // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		}

		// row headers
		for (int row = 1; row <= rHeaders.size(); row++){
			String head = rHeaders.get(row-1);
			JLabel hl = new JLabel(IconFactory.getActionIcon(head));
			hl.setToolTipText(head);
			
				add(hl, 
						new GridBagConstraints(0, //gridx, 
							row, // gridy, 
							1, // gridwidth, 
							1, // gridheight, 
							0, // weightx, 
							0, // weighty, 
							GridBagConstraints.CENTER, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(10, 10, 0, 0), // insets - 
							0, // ipadx, 
							0 // ipady
						));						
				// Add the counts for this row.
				
				for (int col = 1; col < colHeaders.size(); col++) {
					JLabel cl = new JLabel("0");
					counts.get(row).put(col, cl);
					
					add(cl, 
							new GridBagConstraints(col, //gridx, 
								row, // gridy, 
								1, // gridwidth, 
								1, // gridheight, 
								0, // weightx, 
								0, // weighty, 
								GridBagConstraints.CENTER, // anchor, 
								0, // fill - no fill for text controls.
								new Insets(10, 10, 0, 0), // insets - 
								0, // ipadx, 
								0 // ipady
							));		
				}
		}
		
		add(Box.createGlue(), 
				new GridBagConstraints(0, //gridx, 
					7, // gridy, 
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
	
	private void resetTempCount(List<String> rHeaders) {
		tempCounts.clear();
		
		for (String rh : rHeaders) {
			tempCounts.put(rh, Arrays.asList(0, 0, 0));
		}
	}
	
	private void doUpdate(final List<String> valueTypes) {
		// Update the values based on the values in the temp counts.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (String t : valueTypes) {
					List<Integer> values = tempCounts.get(t);
		
					for (int col = 1 ; col < values.size() + 1; col++) {
						String nv = String.valueOf(values.get(col-1));
		
						HashMap<Integer, JLabel> rowStuff = counts.get(valueTypes.indexOf(t) + 1);
						
						if (rowStuff.containsKey(col)) {
							rowStuff.get(col).setText(nv);
						}
					}
				}
				
				revalidate();
			}
		});
	}
	
	/**
	 * Count the number of failed, completed, and pending action for each action
	 * type in the supplied list of products or statuses. If statuses are
	 * supplied the related product is retrieved and its most recent status is
	 * used.
	 * 
	 * 
	 * @param results
	 *            a collection of either ProductAutomationProduct or
	 *            ProductAutomationStatus objects
	 */
	public void processStatusProducts(Collection<?> results) {
		if (!viewType.equals(VIEW_TYPE.STATUS)) {
			viewType = VIEW_TYPE.STATUS;
			doPanelLayout();
		} 

		resetTempCount(statusTypes);
		// Need to keep track of counts for all, so set up a list for that...?
		
		for (Object o : results) {
			ProductAutomationProduct product;
			
			if (o instanceof ProductAutomationProduct) {
				product = (ProductAutomationProduct) o;
			} else {
				product = ((ProductAutomationStatus) o).getProduct();
			}
			
			// Just get the last action.
			SortedSet<ProductAutomationAction> actions = product.getActions();
			SortedSet<ProductAutomationStatus> statuses = product.getStatuses();
			
			ProductAutomationAction action;
			
			if (actions.isEmpty()) {
				action = null;
			} else {
				action = actions.last();
			}
			
			ProductAutomationStatus status;
			
			if (statuses.isEmpty()) {
				status = null;
			} else {
				status = statuses.last();
			}
			
			String mn;
			
			if (action == null) {
				mn = DOWNLINK;
			} else {
				mn = action.getActionName().getMnemonic();
			}
			
			String statusName;
			
			if (status == null) {
				// Should never happen.
				statusName = null;
			} else {
				statusName = status.getStatusName();
			}
			
			if (ProductAutomationStatusDAO.Status.FAILED.toString().equals(statusName)) {
				tempCounts.get(mn).set(2, tempCounts.get(mn).get(2) + 1);
			} else if (ProductAutomationUserDAO.FINISHED_STATUSES_ALL.contains(statusName)) {
				tempCounts.get(mn).set(0, tempCounts.get(mn).get(0) + 1);
			} else {
				tempCounts.get(mn).set(1, tempCounts.get(mn).get(1) + 1);
			}  
		}
		
		doUpdate(statusTypes);
	}
	
	/**
	 * Count the number of pending and completed actions identified in the supplied argument
	 * 
	 * @param results a collection of ProductAutomationAction objects to be counted
	 */
	public void processActions(Collection<ProductAutomationAction> results) {
		if (!viewType.equals(VIEW_TYPE.ACTION)) {
			viewType = VIEW_TYPE.ACTION;
			doPanelLayout();
		} 
		
		resetTempCount(actionTypes);
		
		for (ProductAutomationAction action : results) {
			String nm = action.getActionName().getMnemonic();

			int val;
			if (action.getCompletedTime() == null) {
				val = tempCounts.get(nm).get(1) + 1;
				tempCounts.get(nm).set(1, val);
			} else {
				val = tempCounts.get(nm).get(0) + 1;
				tempCounts.get(nm).set(0, val);
			}
		}
		
		doUpdate(actionTypes);
	}
	
	/**
	 * Count the number of active and inactive processes identified in the
	 * supplied argument
	 * 
	 * @param results
	 *            a collection of ProductAutomationProcess objects to be counted
	 */
	public void processProcesses(Collection<ProductAutomationProcess> results) {
		if (!viewType.equals(VIEW_TYPE.PROCESS)) {
			viewType = VIEW_TYPE.PROCESS;
			doPanelLayout();
		}
		
		resetTempCount(actionTypes);

		for (ProductAutomationProcess proc : results) {
			String nm = proc.getAction().getMnemonic();
			
			int val;
			if (proc.getShutDownTime() == null) {
				val = tempCounts.get(nm).get(0) + 1;
				tempCounts.get(nm).set(0, val);
			} else {
				val = tempCounts.get(nm).get(1) + 1;
				tempCounts.get(nm).set(1, val);
			}
		}
		
		doUpdate(actionTypes);
	}
	
	/**
	 * Log counts are not supported.  Just zeros out and disables all stuff.
	 * @param results a collection of log objects to be counted
	 */
	public void processLogs(Collection<?> results) {
		if (!viewType.equals(VIEW_TYPE.LOG)) {
			viewType = VIEW_TYPE.LOG;
			doPanelLayout();
		} else {
			// Do nothing.  Log counts not enabled.
		}
	}
}
