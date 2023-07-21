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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import org.springframework.context.ApplicationContext;

/**
 * Parameter panel that allows the user to specify that search parameters for single execution
 * queries or for real time monitoring.  
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS
 */
@SuppressWarnings("serial") // Javadoc says this will not be supported.
public class QueryInputPanel extends JPanel implements UserInputInterface {
	private static final List<String> STATUSES = Arrays.asList(
			ProductAutomationStatusDAO.Status.CATEGORIZED.toString(),
			ProductAutomationStatusDAO.Status.UNCATEGORIZED.toString(),
            ProductAutomationStatusDAO.Status.IGNORED.toString(),
            ProductAutomationStatusDAO.Status.REASSIGNED.toString(),
            ProductAutomationStatusDAO.Status.STARTED.toString(),
            ProductAutomationStatusDAO.Status.TIMEOUT.toString(),
            ProductAutomationStatusDAO.Status.COMPLETED.toString(),
            ProductAutomationStatusDAO.Status.UNKNOWN_COMPLETE.toString(),
            ProductAutomationStatusDAO.Status.FAILED.toString(),
            ProductAutomationStatusDAO.Status.COMPLETE_PRE_PB.toString());
	
	private static final List<String> STAT_TT = Arrays.asList(
			"Product needed processing and that processing type was assigned",
			"Product was added to automation and the type of processing for the product needs to be found",
			"Legacy status for completed_cd",
			"Processing action was not completed in alloted time.  The product was reassigned to a different processor to do the required processing",
			"Product processing was started",
			"Product action assignement has passed the alloted time.  Process is marked to be reassigned",
			"Product is done with all processing and was marked completed",
			"Products action was completed but no status was found. This is an anomolous finished state",
			"Product failed processing at some stage",
			"Product was extracted by a previous process and is not being extracted by automation" 
			);
	
	private static final String LOGS_TEXT_ARBITER = "Arbiter Logs";
	private static final String LOGS_TEXT_PROCESS = "Process Logs";
	private static final String LOGS_TEXT_PRODUCT = "Product Logs";
	
	private enum LOG_LEVELS {
		FATAL,
		ERROR,
		WARN,
		USER,
		INFO,
		DEBUG,
		TRACE,
		UNKNOWN
	}
	
	private static final List<String> LOGS_TEXTS = Arrays.asList(LOGS_TEXT_ARBITER, LOGS_TEXT_PROCESS, LOGS_TEXT_PRODUCT);
	
	private static final List<String> LOGS_TEXTS_TT = Arrays.asList(
			"Include arbiter logs",
			"Include process logs", 
			"Include logs assigned to specific products");
	
	private static final List<String> COMPLETED = ProductAutomationUserDAO.FINISHED_STATUSES;


	private static final List<String> PENDING = ProductAutomationUserDAO.PENDING_STATUSES;
	
	private static final Object[] TTYPES = new Object[] {"Days", "Hours", "Minutes", "Seconds"};
	private static final int COLUMNS = 10;
	
	// Time conversion constants
	private static final int SECONDS_MS = 1000;
	private static final int MINUTES_MS = 60 * SECONDS_MS;
	private static final int HOURS_MS = 60 * MINUTES_MS;
	private static final int DAYS_MS = 24 * HOURS_MS;
	
	private int[] CONVERSION_FACTORS = new int[] {DAYS_MS, HOURS_MS, MINUTES_MS, SECONDS_MS};
	
	// JLable strings.
	private static final String ST = "Start Time";
	private static final String ET = "End Time";
	private static final String PRODUCT = "Product";
	private static final String PASS = "Pass Number";
	private static final String SID = "Session ID";
	private static final String HOST = "Session Host";
	private static final String TIME_TYPE_LABEL = "Time Type";
	private static final String APID = "APID";
	
	private static final String LATEST = "Latest";
	private static final String COMPLETED_STATUS = "Completed";
	private static final String PENDING_STATUS = "Pending";
	private static final String CUSTOM = "Custom";
	private static final String TFN = "Delta";
	private static final String DOCUMENT_PARENT = "parent";
	
	// Time types
	private static final String EVENT_TIME = "Event Time";
	private static final String PRODUCT_TIME = "Product Time";
	
	// More action stuff.
	private static final String COMPLETED_ACTIONS = "Completed Actions";
	private static final String PENDING_ACTIONS = "Pending Actions";

	private static final String COMPLETED_ACTIONS_TT = "Include completed actions";
	private static final String PENDING_ACTIONS_TT = "Include pending actions";
	
	// tool tip strings
	private static final String PRODUCT_TT = "Search string for a product.  Does not have to be the full product path";
	private static final String SESSION_TT = "Filters all queries based on the session ID";
	private static final String SESSION_HOST_TT = "Search string for a session host";
	private static final String PASS_TT = "Filter query results based on pass number";
	private static final String LATEST_TT = "Only get the most recent status for any products";
	private static final String COMPLETED_TT = "Only return query results for products in a completed state of processing";
	private static final String PENDING_TT = "Only return query results for products in a non-complete state of processing";
	private static final String USER_TT = "Specify the status types for query";
	private static final String TFN_TT = "Query products from the current time - Delta";
	private static final String TIME_TYPE_TT = "Specify the time type for the query.  Event time will look at the PDPP processing event time.  Product time will look at the sclk value for the product.";
	private static final String APID_TT = "Filter based on product apid number.";
	
	// Process option labels and tt's.
	private static final String RUNNING_PROCESSES = "Running Processes Only";
	private static final String RUNNING_PROCESSES_TT = "If checked will only find processes that are currently running";

	private static final String UNLIMITED = "Unlimited";
	private static final List<String> MAX_QUERY_OPTIONS = Arrays.asList("10", "50", "100", "500", "1000", "2000", "5000", "10000", UNLIMITED);
	
	private static final String MAX_QUERY_OPTIONS_TT = "Specify the maximum query results for query time optimization.  If 'Unlimited' is selected, this can greatly impact performance.";
	private static final String MAX_QUERY_OPTIONS_LABEL = "Query Limit";
	private static final String ASCENDING = "Ascending";
	private static final String ASCENDING_TT = "If results are greater than the max cut off the earlier results";
	private static final String DESCENDING = "Descending";
	private static final String DESCENDING_TT = "If results are greater than the max cut off the later results";
	
	private DatePanel startTime;
	private DatePanel endTime;
	private JComboBox<Object> timeType;
	private JTextArea product;
	private JTextField pass;
	private JTextField sid;
	private JTextField host;
	private JTextField tfn;
	private JTextField apid;
	private JComboBox<Object> tfnType;
	private JRadioButton latest;
	private JRadioButton pending;
	private JRadioButton completed;
	private JRadioButton userSelect;	
	private JComboBox<Object> maxQueryResults;
	private JRadioButton ascending;
	private JRadioButton descending;
	private MetricsPanel metrics;

	// Need to have an input for the process id for logs.
	private JLabel logProcessHeader;
	private JTextField logProcess;
	
	// In place of a list box selector deal.
	private HashMap<String, JCheckBox> statusCheckBoxes;
	private HashMap<String, JCheckBox> logsTypesCheckBoxes;
	private HashMap<String, JCheckBox> logsLevelCheckBoxes;
	private HashMap<String, JCheckBox> actionsCheckBoxes;
	
	// process options.
	private JCheckBox runningProcesses;
	
	// More action options
	private JCheckBox completedActions;
	private JCheckBox pendingActions;

	private ProductAutomationUserDAO userDao;
	private ProductAutomationClassMapDAO classMapDao;

	protected final ApplicationContext appContext;

	/**
	 * Creates the query input panel with all of the necessary fields
	 */
	public QueryInputPanel(ProductAutomationUserDAO userDao, ProductAutomationClassMapDAO classMapDao, ApplicationContext appContext) {
		super(new GridBagLayout());
		
		this.userDao = userDao;
		this.classMapDao = classMapDao;
		this.appContext = appContext;
		
		init();
	}
	
	/**
	 * Initializes all of the panel components and then calls the doPanelLayout
	 * method to set up the layout
	 */
	protected void init() {
		logProcessHeader = new JLabel("Process Number");
		logProcess = new JTextField(10);
		
		maxQueryResults = new JComboBox<Object>(MAX_QUERY_OPTIONS.toArray());
		maxQueryResults.setToolTipText(MAX_QUERY_OPTIONS_TT);
		
		metrics = new MetricsPanel(appContext);

		startTime = new DatePanel();
		endTime = new DatePanel();
		
		timeType = new JComboBox<Object>(new Object[] {EVENT_TIME, PRODUCT_TIME});
		timeType.setFocusable(false);
		timeType.setToolTipText(TIME_TYPE_TT);
		
		tfn = new JTextField(COLUMNS/2);
		tfn.setToolTipText(TFN_TT);
		
		tfnType = new JComboBox<Object>(TTYPES);
		tfnType.setSelectedIndex(1);
		
		pass = new JTextField(COLUMNS/2);
		pass.setToolTipText(PASS_TT);

		product = new JTextArea();
		product.setLineWrap(true);
		product.setBorder(pass.getBorder());
		
		product.setToolTipText(PRODUCT_TT);		
		
		sid = new JTextField(COLUMNS/2);
		sid.setToolTipText(SESSION_TT);
		
		host = new JTextField(COLUMNS);
		host.setToolTipText(SESSION_HOST_TT);
		
		apid = new JTextField(COLUMNS/2);
		apid.setToolTipText(APID_TT);
		
		latest = new JRadioButton(LATEST);
		latest.setToolTipText(LATEST_TT);
		latest.setSelected(true);
		
		completed = new JRadioButton(COMPLETED_STATUS);
		completed.setToolTipText(COMPLETED_TT);
		completed.setFocusable(false);
		
		pending = new JRadioButton(PENDING_STATUS);
		pending.setToolTipText(PENDING_TT);
		pending.setFocusable(false);
		
		userSelect = new JRadioButton(CUSTOM);
		userSelect.setToolTipText(USER_TT);
		userSelect.setFocusable(false);
		
		completedActions = new JCheckBox(COMPLETED_ACTIONS);
		completedActions.setToolTipText(COMPLETED_ACTIONS_TT);
		completedActions.setFocusable(false);
		completedActions.setSelected(true);
		
		pendingActions = new JCheckBox(PENDING_ACTIONS);
		pendingActions.setToolTipText(PENDING_ACTIONS_TT);
		pendingActions.setFocusable(false);
		pendingActions.setSelected(true);
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(completed);
		bg.add(pending);
		bg.add(userSelect);
		
		ascending = new JRadioButton(ASCENDING);
		ascending.setToolTipText(ASCENDING_TT);
		ascending.setFocusable(false);
		ascending.setSelected(true);
		
		descending = new JRadioButton(DESCENDING);
		descending.setToolTipText(DESCENDING_TT);
		descending.setFocusable(false);
		
		ButtonGroup bg1 = new ButtonGroup();
		bg1.add(ascending);
		bg1.add(descending);
		
		// Creating a hashmap of radio buttons to act much like a list box.
		statusCheckBoxes = new HashMap<String, JCheckBox>();
		
		for (int index = 0; index < STATUSES.size(); index++) {
			String stat = STATUSES.get(index);
			String tt = STAT_TT.get(index);

			JCheckBox s = new JCheckBox(stat);
			s.setToolTipText(tt);
			s.setFocusable(false);
			
			statusCheckBoxes.put(stat, s);
		}
		
		logsTypesCheckBoxes = new HashMap<String, JCheckBox>();
		
		for (int index = 0; index < LOGS_TEXTS.size(); index++) {
			String head = LOGS_TEXTS.get(index);
			String tt = LOGS_TEXTS_TT.get(index);
			
			JCheckBox l = new JCheckBox(head);
			l.setToolTipText(tt);
			l.setFocusable(false);
			l.setSelected(true);
			
			logsTypesCheckBoxes.put(head, l);
		}
		
		logsLevelCheckBoxes = new HashMap<String, JCheckBox>();
		
		for (LOG_LEVELS level : LOG_LEVELS.values()) {
			JCheckBox lb = new JCheckBox(level.toString());
			lb.setFocusable(false);
			lb.setSelected(true);
			
			logsLevelCheckBoxes.put(level.toString(), lb);
		}
		
		actionsCheckBoxes = new HashMap<String, JCheckBox>();
		
		for (ProductAutomationClassMap nm : classMapDao.getClassMaps()) {
			String name = nm.getMnemonic();
			
			JCheckBox a = new JCheckBox(name);
			a.setFocusable(false);
			a.setSelected(true);
			
			actionsCheckBoxes.put(nm.getMnemonic(), a);
		}

		runningProcesses = new JCheckBox(RUNNING_PROCESSES);
		runningProcesses.setToolTipText(RUNNING_PROCESSES_TT);
		runningProcesses.setFocusable(false);
		runningProcesses.setSelected(true);
		
		addListeners();
		doPanelLayout();
		
		// Default start window is for statuses.  Enable those first.
		enableStatusOptions();
	}
	
	private void addListeners() {
		// Create action listener for pass and session id.  Turns red if the values are not all integers.
		DocumentListener numberListener = new DocumentListener() {
			public void changed(DocumentEvent e) {
				JTextField tx = (JTextField) e.getDocument().getProperty(DOCUMENT_PARENT);
				Integer num = null;
				
				try {
					num = Integer.parseInt(tx.getText());
					tx.setForeground(Color.BLACK);
					
				} catch (Exception e1) {
					tx.setForeground(Color.RED);
				}
				
				// Check if the field that is entered is the tfn box.  If so and it is valid, disable / enable 
				// the other time fields.
				if (tx.equals(tfn)) {
					boolean set;
					if (tx.getText().isEmpty() || num == null) {
						set = true;
					} else {
						set = false;
					}
					startTime.enableInput(set);
					endTime.enableInput(set);
				}
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				changed(e);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				changed(e);
				
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				changed(e);
			}
		};
		
		tfn.getDocument().putProperty(DOCUMENT_PARENT, tfn);
		tfn.getDocument().addDocumentListener(numberListener);
		
		// Add the listener, and set the property to the document so that the parent component can be traced back.
		pass.getDocument().putProperty(DOCUMENT_PARENT, pass);
		pass.getDocument().addDocumentListener(numberListener);
		
		sid.getDocument().putProperty(DOCUMENT_PARENT, sid);
		sid.getDocument().addDocumentListener(numberListener);		
		
		apid.getDocument().putProperty(DOCUMENT_PARENT, apid);
		apid.getDocument().addDocumentListener(numberListener);
		
		// Create the action listener for the radio buttons.  
		ActionListener radioListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Clears the selected in the blah and selects the specified statuses for a given type.
				
				if (e.getSource().equals(completed)) {
					setStatusSelect(COMPLETED);
				} else if (e.getSource().equals(pending)) {
					setStatusSelect(PENDING);
				} else {
					clearStatusSelect();
				}
			}
		};
		
		completed.addActionListener(radioListener);
		pending.addActionListener(radioListener);
		userSelect.addActionListener(radioListener);	
		
		ActionListener rbal = new ActionListener() {
			private boolean sameElements(Collection<String> first, Collection<String> second) {
				boolean result = true;
				
				if (first.size() != second.size()) {
					result = false;
				}
				
				for (String f : first) {
					if (!second.contains(f)) {
						result = false;
						break;
					}
				}
				
				return result;
		
			}			
			@Override
			public void actionPerformed(ActionEvent e) {
				// Check what buttons are clicked and set the type accordingly.
				Collection<String> vals = getSelectedStatuses();
				
				if (sameElements(vals, COMPLETED)) {
					// Select completed
					completed.setSelected(true);
				} else if (sameElements(vals, PENDING)) {
					pending.setSelected(true);
				} else {
					userSelect.setSelected(true);
				}
			}
		};

		// For all the radio buttons set the listener.
		for (JCheckBox jb : statusCheckBoxes.values()) {
			jb.addActionListener(rbal);
		}
	}
	
	private void setStatusSelect(Collection<String> statuses) {
		for (String s : statusCheckBoxes.keySet()) {
			statusCheckBoxes.get(s).setSelected(statuses.contains(s));
		}
	}
	
	private void clearStatusSelect() {
		for (JCheckBox jb : statusCheckBoxes.values()) {
			jb.setSelected(false);
		}
	}
	
	/**
	 * Sets up the gridbag layout for this panel. There is no way to make this
	 * clean and efficient, so it will be a laborous task. If you did not write
	 * this, it may take some writing out to figure out what is going on.
	 * 
	 * All components fill to the end of the panel. Any padding that is needed
	 * should be added when this panel is added down stream.
	 */
	private void doPanelLayout() {
		// No way around it.  I have tried a number of ways to use the same constraint for different components, and it is not working out.
		// Ends up being too many steps and too ugly.  Going to just add everything with a new instance of the constraint set up for that component.
		int gridx = 0;
		int gridy = 0;
		
		add(new JLabel(ST), 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 10), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(startTime,
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					7, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
			));

		add(metrics, 
				new GridBagConstraints(8, //gridx, 
						0, // gridy, 
						GridBagConstraints.REMAINDER, // gridwidth, 
						GridBagConstraints.REMAINDER, // gridheight, 
						0, // weightx, 
						1, // weighty, 
						GridBagConstraints.NORTHEAST, // anchor, 
						GridBagConstraints.VERTICAL, // fill - no fill for text controls.
						new Insets(5, 5, 5, 5), // insets - 
						0, // ipadx, 
						0 // ipady
						));	

		
		gridx = 0;
		
		add(new JLabel(ET), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 10), // insets - 
					0, // ipadx, 
					0 // ipady
				));
			
		add(endTime,
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					7, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
	
		gridx = 0;

		add(new JLabel(TFN), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(tfn, 
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
		
		add(tfnType, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.RELATIVE, // gridWidth
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));

		gridx = 0;
		
		add(new JLabel(MAX_QUERY_OPTIONS_LABEL), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridWidth
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(maxQueryResults, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridWidth
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(ascending, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridWidth
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(descending, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.RELATIVE, // gridWidth
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		gridx = 0;
		add(new JLabel(TIME_TYPE_LABEL), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		add(timeType, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));			
		
		gridx = 0;
		add(new JLabel(PRODUCT), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
			
		add(product,
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					7, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(0, 3, 0, 10), // insets - 
					0, // ipadx, 
					0 // ipady
				));

		gridx = 0;
		
		add(new JLabel(PASS), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(pass, 
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 10), // insets - 
						0, // ipadx, 
						0 // ipady
					));
		
		add(new JLabel(SID), 
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
			
		add(sid, 
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 10), // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		
		add(new JLabel(APID), 
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 10), // insets - 
						0, // ipadx, 
						0 // ipady
					));		
		
		add(apid, 
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 10), // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		
		add(new JLabel(HOST), 
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
			
		add(host, 
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 10), // insets - 
						0, // ipadx, 
						0 // ipady
						));	
		
		// NOTE:  All of the buttons below that are added to the select panel will be added on the left hand side.  
		// When a view is selected, things will be hidden from view.
		
		// Adding buttons.  They will be in gridx 1.  If the user select is clicked, the list box will be gridx 2 on the side of the buttons.
		gridx = 0;
		
		// Can't get the layout to work correctly.  Going to add another panel that has all 
		// the status selections.
		JPanel selectPanel = new JPanel(new GridBagLayout());
		selectPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		add(selectPanel,
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						GridBagConstraints.RELATIVE, // gridwidth, 
						GridBagConstraints.REMAINDER, // gridheight, 
						0, // weightx, 
						1, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						GridBagConstraints.BOTH, // fill - no fill for text controls.
						new Insets(5, 5, 5, 5), // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		
		gridx = 0;
		gridy = 0;
		
		selectPanel.add(latest,
				new GridBagConstraints(gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 0), // insets - 
						0, // ipadx, 
						0 // ipady
					));					
		
		selectPanel.add(completed,
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 0), // insets - 
						0, // ipadx, 
						0 // ipady
					));				

		selectPanel.add(pending,
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 0), // insets - 
						0, // ipadx, 
						0 // ipady
					));		
		
		selectPanel.add(userSelect,
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						1, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 50), // insets - 
						0, // ipadx, 
						0 // ipady
					));		
		
		int gyc = 0;
		int gyp = 0;
		
		int gxc = 1;
		int gxp = 2;
		int width;
		int weightx;
		
		// Here add all the radio buttons.  Will be on the same gridx x, just incease the gridy.
		for (String s : STATUSES) {
			boolean completed = COMPLETED.contains(s) || s.equals(ProductAutomationStatusDAO.Status.COMPLETE_PRE_PB.toString());
			
			gridx = completed ? gxc : gxp;
			gridy = completed ? gyc++ : gyp++;
			width = completed ? 1 : GridBagConstraints.REMAINDER;
			weightx = completed ? 0 : 1;
			
			selectPanel.add(statusCheckBoxes.get(s),
					new GridBagConstraints(gridx, //gridx, 
							gridy, // gridy, 
							width, // gridwidth, 
							1, // gridheight, 
							weightx, // weightx, 
							0, // weighty, 
							GridBagConstraints.WEST, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(0, 0, 0, 0), // insets - 
							0, // ipadx, 
							0 // ipady
						));
		}
		
		// Add the other select boxes for logs and actions.
		gridx = 0;
		gridy = -1;
		
		for (String logOption : LOGS_TEXTS) {
			selectPanel.add(logsTypesCheckBoxes.get(logOption),
					new GridBagConstraints(gridx, //gridx, 
							++gridy, // gridy, 
							1, // gridwidth, 
							1, // gridheight, 
							0, // weightx, 
							0, // weighty, 
							GridBagConstraints.NORTHWEST, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(0, 0, 0, 50), // insets - 
							0, // ipadx, 
							0 // ipady
						));			
		}
		
		// Add the input process log deals.
		gridy = 0;
		gridx++;
		selectPanel.add(logProcessHeader,
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.CENTER, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 5, 0, 5), // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		
		selectPanel.add(logProcess,
				new GridBagConstraints(++gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 50), // insets - 
						0, // ipadx, 
						0 // ipady
					));	
		
		gridy = -1;
		gridx++;
		width = 1;
		weightx = 0;
		
		// Add the log level stuff.
		for (LOG_LEVELS level : LOG_LEVELS.values()) {
			if (level == LOG_LEVELS.INFO) {
				gridx++;
				gridy = -1;
				width = GridBagConstraints.REMAINDER;
				weightx = 1;				
			}
			
			selectPanel.add(logsLevelCheckBoxes.get(level.toString()),
					new GridBagConstraints(gridx, //gridx, 
							++gridy, // gridy, 
							width, // gridwidth, 
							1, // gridheight, 
							weightx, // weightx, 
							0, // weighty, 
							GridBagConstraints.NORTHWEST, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(0, 0, 0, 0), // insets - 
							0, // ipadx, 
							0 // ipady
						));			
		}
		
		gridx = 0;
		gridy = -1;
		
		selectPanel.add(completedActions,
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.NORTHWEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 50), // insets - 
						0, // ipadx, 
						0 // ipady
				));		
				
			selectPanel.add(pendingActions,
					new GridBagConstraints(gridx, //gridx, 
							++gridy, // gridy, 
							1, // gridwidth, 
							GridBagConstraints.REMAINDER, // gridheight, 
							0, // weightx, 
							1, // weighty, 
							GridBagConstraints.NORTHWEST, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(0, 0, 0, 50), // insets - 
							0, // ipadx, 
							0 // ipady
					));		
			
		// Going to use a little trick here.  The action types are used by the process as well, so add them in gridx = 3.
		// The layout will adjust them for the action view, but allow for something else t be seen in the process view.

		gridx++;
		gridx++;
		gridy = 0;
		for (ProductAutomationClassMap cm : classMapDao.getClassMaps()) {
			selectPanel.add(actionsCheckBoxes.get(cm.getMnemonic()),
					new GridBagConstraints(gridx, //gridx, 
							gridy++, // gridy, 
							GridBagConstraints.REMAINDER, // gridwidth, 
							1, // gridheight, 
							1, // weightx, 
							0, // weighty, 
							GridBagConstraints.NORTHWEST, // anchor, 
							0, // fill - no fill for text controls.
							new Insets(0, 0, 0, 0), // insets - 
							0, // ipadx, 
							0 // ipady
						));			
		}	

		gridx = 0;
		gridy = 0;
		
		selectPanel.add(runningProcesses,
				new GridBagConstraints(gridx, //gridx, 
						gridy, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						0, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(0, 0, 0, 20), // insets - 
						0, // ipadx, 
						0 // ipady
					));		
		
		// Add this guy to take up any slack and keep all the buttons in the north of the select panel.
		selectPanel.add(Box.createGlue(),
				new GridBagConstraints(0, //gridx, 
						20, // gridy, 
						GridBagConstraints.REMAINDER, // gridwidth, 
						1, // gridheight, 
						1, // weightx, 
						1, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						GridBagConstraints.BOTH, // fill - no fill for text controls.
						new Insets(0, 0, 0, 0), // insets - 
						0, // ipadx, 
						0 // ipady
					));				
	}

	// Getters.
	
	private Timestamp acdToTimestamp(IAccurateDateTime adc) {
		Timestamp result = null;
		
		if (adc != null) {
			result = new Timestamp(adc.getTime());
		}
		
		return result;		
	}

	private String getTextString(JTextArea ta) {
		return ta.getText().isEmpty() ? null : ta.getText();
	}

	/**
	 * Checks the value of the text field. Returns null if empty else the value.
	 * 
	 * @param tf
	 */
	private String getTextString(JTextField tf) {
		return tf.getText().isEmpty() ? null : tf.getText();
	}
	
	private Long getTextLong(JTextField tf) {
		String v = tf.getText();
		Long result = null;
		
		if (!v.isEmpty()) {
			try {
				result = Long.parseLong(v);
			} catch (NumberFormatException e) {
				// Do nothing
			}
		}
		
		return result;
	}

	private Integer getTextInteger(JTextField tf) {
		String v = tf.getText();
		Integer result = null;
		
		if (!v.isEmpty()) {
			try {
				result = Integer.parseInt(v);
			} catch (NumberFormatException e) {
				// Do nothing
			}
		}
		
		return result;
	}

	
	/**
	 * Gets the input from tfn and will convert to number of milleseconds based
	 * on the value in the tfnType. Days, hours, minutes, seconds.
	 * 
	 * @return Current time - tfn value. Null if no valid data in the field.
	 */
	private Long timeFromNowMS() {
		Long ms = null;
		
		String t = tfn.getText();
		
		try {
			ms = System.currentTimeMillis() - Long.parseLong(t) * CONVERSION_FACTORS[tfnType.getSelectedIndex()];
		} catch (NumberFormatException e) {
			// Don't care.
		}
		
		return ms;
	}	
	
	/**
	 * Returns the product string. Will be null if field is empty.
	 * 
	 * @return the text in the product field as a String
	 */
	public String getProduct() {
		return getTextString(product);
	}
	
	/**
	 * Return the pass number. Will be null if field is empty.
	 * 
	 * @return the text in the pass number field as a Long
	 */
	public Long getPassNumber() {
		return getTextLong(pass);
	}
	
	/**
	 * Return the process ID. Will be null if field is empty
	 * @return the text in the process ID field as a Long
	 */
	public Long getLogProcessId() {
		return getTextLong(logProcess);
	}
	
	/**
	 * Return the session ID. Will be null if field is empty.
	 * 
	 * @return the text in the session ID field as a Long
	 */
	public Long getSessionId() {
		return getTextLong(sid);
	}

	/**
	 * Return the host. Will be null if the field is empty.
	 * 
	 * @return the text in the host field as a String
	 */
	public String getHost() {
		return getTextString(host);
	}
	
	/**
	 * Return the apid. Will be null if the field is empty
	 * 
	 * @return the text in the APID field as an Integer.
	 */
	public Integer getApids() {
		return getTextInteger(apid);
	}
	
	/**
	 * Get if only the latest option is selected or not.
	 * 
	 * @return TRUE if the "Latest" option is selected, FALSE otherwise
	 */
	public boolean latestOnly() {
		return latest.isSelected();
	}
	
	/**
	 * Returns an array of the selected statuses.
	 * 
	 * @return a collection of the Strings representing the selected statuses
	 */
	public Collection<String> getSelectedStatuses() {
		SortedSet<String> result = new TreeSet<String>();
		
		for (String v : statusCheckBoxes.keySet()) {
			if (statusCheckBoxes.get(v).isSelected()) {
				result.add(v);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns if the "Event Time" option is selected under the "Time Type" configuration option
	 * 
	 * @return TRUE if "Event Time" is selected, FALSE if "Product Time" is selected
	 */
	public boolean isEventTime() {
		return timeType.getSelectedItem().equals(EVENT_TIME);
	}

	/**
	 * Converts lower bound time to a time stamp and returns. If the field is
	 * empty, returns null. If the time from now field has data will get the
	 * current time minus the delta.
	 * 
	 * @return a Timestamp object representing the value places in the
	 *         "Start Time" field
	 */
	public Timestamp getLowerBound() {
		Long ms = timeFromNowMS();
		
		return acdToTimestamp(ms == null ? startTime.getTime() : new AccurateDateTime(ms, new Long(0)));
	}
	
	/**
	 * Converts upper bound time to a time stamp and returns. If the field is
	 * empty, returns null. If the time from now field has valid data will
	 * return the current time.
	 * 
	 * @return a Timestamp object representing the value places in the
	 *         "End Time" field
	 */
	public Timestamp getUpperBound() {
		// Only used to see if there is a prodblem
		return acdToTimestamp(timeFromNowMS() == null ? endTime.getTime() : 
			new AccurateDateTime(System.currentTimeMillis(), new Long(0)));
	}

	/**
	 * Queries for products with the inputs from the panel.
	 * 
	 * @param bottomUp
	 *            TRUE if the results are to be returned starting with the child
	 *            followed by parent, FALSE if parent to child
	 * 
	 * @return a collection of ProductAutomationProduct objects matching the
	 *         user query parameters
	 */
	public Collection<ProductAutomationProduct> getProducts(boolean bottomUp) {
		Collection<ProductAutomationProduct> products;
		
		if (isEventTime()) {
			products = userDao.getProductsByEventTime(getProduct(), 
					getSessionId(), 
					getHost(), 
					getPassNumber(), 
					getApids(), 
					latestOnly(), 
					this.getLowerBound(), 
					getUpperBound(), 
					getSelectedStatuses(),
					getMaxResults(),
					isAscending(),
					bottomUp);
		} else {
			products = userDao.getProductsByProductTime(getProduct(), 
					getSessionId(), 
					getHost(), 
					getPassNumber(), 
					getApids(), 
					latestOnly(), 
					getLowerBound(), 
					getUpperBound(), 
					getSelectedStatuses(),
					getMaxResults(),
					isAscending(),
					bottomUp);
		}
		
		updateMetrics(products);
		
		return products;
	}
	
	private boolean runningProcessesOnly() {
		return runningProcesses.isSelected();
	}
	
	/**
	 * Queries for statuses using the user input values.
	 * 
	 * @return a collection of ProductAutomationStatus objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationStatus> getStatuses() {
		Collection<ProductAutomationStatus> statuses;
		
		if (isEventTime()) {
			statuses = userDao.getStatusesByEventTime(getProduct(), 
					getSessionId(), 
					getHost(), 
					getPassNumber(), 
					getApids(), 
					latestOnly(), 
					this.getLowerBound(), 
					getUpperBound(), 
					getSelectedStatuses(),
					getMaxResults(),
					isAscending());			
		} else {
			statuses = userDao.getStatusesByProductTime(getProduct(), 
					getSessionId(), 
					getHost(), 
					getPassNumber(), 
					getApids(), 
					latestOnly(), 
					getLowerBound(), 
					getUpperBound(), 
					getSelectedStatuses(),
					getMaxResults(),
					isAscending());
		}

		updateMetrics(statuses);
		
		return statuses;
	}
	
	private Integer getMaxResults() {
		if (maxQueryResults.getSelectedItem().equals(UNLIMITED)) {
			return null;
		} else {
			return Integer.valueOf((String) maxQueryResults.getSelectedItem());
		}
	}
	
	/**
	 * Queries for processes using the user input values
	 * 
	 * @return a collection of ProductAutomationProcess objects matching the
	 *         user query parameters
	 */
	public Collection<ProductAutomationProcess> getProcesses() {
		/**
		 * MPCS-6975  4/2105 - If all of the actions are unchecked we get an error when trying to 
		 * do the query.  Check if the actions are set and if they are not, just return the empty list.
		 */
		
		Collection<String> checked = getCheckedActions();
		
		Collection<ProductAutomationProcess> procs = checked.isEmpty() ?
				Collections.<ProductAutomationProcess>emptyList() : // Empty since no actions are checked.
				userDao.getProcesses(
				getHost(), // host, 
				getLowerBound(), // lowerBound, 
				getUpperBound(), // upperBound, 
				checked, // actionNames, 
				runningProcessesOnly(), // running, 
				getMaxResults(), 
				isAscending());
		
		metrics.processProcesses(procs);
		
		return procs;
	}

	/**
	 * Queries for actions using the user input values
	 * 
	 * @return a collection of ProductAutomationAction objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationAction> getActions() {
		Collection<ProductAutomationAction> actions;
		
		/**
		 * MPCS-6975  4/2105 - If all of the actions are unchecked we get an error when trying to 
		 * do the query.  Check if the actions are set and if they are not, just return the empty list.
		 */
		
		Collection<String> checked = getCheckedActions();
		
		actions = checked.isEmpty() ? 
				Collections.<ProductAutomationAction>emptyList() : // Empty since no actions are checked.
				userDao.getActions(
				getProduct(), 
				getHost(), 
				getSessionId(), 
				getPassNumber(), 
				getApids(), 
				getLowerBound(), 
				getUpperBound(), 
				completedActions.isSelected(),
				pendingActions.isSelected(),
				getMaxResults(),
				isAscending(),
				checked);
		
		metrics.processActions(actions);
		
		return actions;
	}
	
	private boolean isAscending() {
		return ascending.isSelected();
	}
	
	private Collection<String> getCheckedActions() {
		Collection<String> actions = new ArrayList<String>();
		
		for (JCheckBox cb : actionsCheckBoxes.values()) {
			if (cb.isSelected()) {
				actions.add(cb.getText());
			}
		}
		
		return actions;
	}

	private boolean arbiterLogs() {
		return logsTypesCheckBoxes.get(LOGS_TEXT_ARBITER).isSelected();
	}
	
	private boolean processLogs() {
		return logsTypesCheckBoxes.get(LOGS_TEXT_PROCESS).isSelected();
	}
	
	private boolean productLogs() {
		return logsTypesCheckBoxes.get(LOGS_TEXT_PRODUCT).isSelected();
	}
	
	private Collection<String> getSelectedLogLevels() {
		Collection<String> levels = new ArrayList<String>();
		
		for (String level : logsLevelCheckBoxes.keySet()) {
			if (logsLevelCheckBoxes.get(level).isSelected()) {
				levels.add(level);
			}
		}
		
		return levels;
	}
	
	/**
	 * Queries for logs using the user input values
	 * 
	 * @return a collection of ProductAtuomationLogs objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationLog> getLogs() {
		/**
		 * MPCS-6975  4/2105 - If all of the log levels are unchecked we get an error when trying to 
		 * do the query.  Check if the checked log levels is empty before running the qyery.
		 */
		Collection<String> logLevels = getSelectedLogLevels();
		
		Collection<ProductAutomationLog> logs = logLevels.isEmpty() ?
				Collections.<ProductAutomationLog>emptyList() : // Nothing is checked.
				userDao.getLogs(				
				getHost(), // host, 
				getLowerBound(), // lowerBound, 
				getUpperBound(), // upperBound, 
				logLevels, // Log levels
				getLogProcessId(),
				arbiterLogs(), // arbiter, 
				processLogs(), // process, 
				productLogs(), // productMessages);
				getMaxResults(),
				isAscending()
				);
		
		metrics.processLogs(logs);
		
		return logs;
	}
	
	private void updateMetrics(Collection<?> values) {
		metrics.processStatusProducts(values);
	}

	/**
	 * Disables the status only inputs.
	 */
	private void setStatusEnabledValues(boolean enabled) {
		for (Component cb : statusCheckBoxes.values()) {
			cb.setVisible(enabled);
		}

		completed.setVisible(enabled);
		pending.setVisible(enabled);
		userSelect.setVisible(enabled);
		latest.setVisible(enabled);
	}
	
	private void setLogsEnabledValues(boolean enabled) {
		for (JCheckBox cb : logsTypesCheckBoxes.values()) {
			cb.setVisible(enabled);
		}

		for (JCheckBox cb : logsLevelCheckBoxes.values()) {
			cb.setVisible(enabled);
		}

		logProcessHeader.setVisible(enabled);
		logProcess.setVisible(enabled);
		
		// Extra query params that need to be disabled for logs queries.
		product.setEnabled(!enabled);
		sid.setEnabled(!enabled);
		pass.setEnabled(!enabled);
		apid.setEnabled(!enabled);
		timeType.setEnabled(!enabled);
	}

	private void setActionsEnabledValues(boolean enabled) {
		completedActions.setVisible(enabled);
		
		pendingActions.setVisible(enabled);		
		
		setIndividualActions(enabled);
	}
	
	private void setProcessEnabledValues(boolean enabled) {
		runningProcesses.setVisible(enabled);
		
		setIndividualActions(enabled);
		
		// Disable the inputs that are not valid for process queries.
		product.setEnabled(!enabled);
		sid.setEnabled(!enabled);
		pass.setEnabled(!enabled);
		apid.setEnabled(!enabled);
		timeType.setEnabled(!enabled);
	}
	
	private void setIndividualActions(boolean enabled) {
		for (JCheckBox cb : actionsCheckBoxes.values()) {
			cb.setVisible(enabled);
//			cb.setSelected(enabled);
		}
	}
	
	/**
	 * Enables all of the status options and disables the other options.
	 */
	public void enableStatusOptions() {
		disableActionOptions();
		disableLogsOptions();
		disableProcessOptions();
		
		setStatusEnabledValues(true);
	}

	/**
	 * Disables the status only options.
	 */
	public void disableStatusOptions() {
		setStatusEnabledValues(false);
	}

	/**
	 * Enables all of the logs options and disables the other options
	 */
	public void enableLogsOptions() {
		disableStatusOptions();
		disableActionOptions();
		disableProcessOptions();
		
		setLogsEnabledValues(true);
	}
	
	/**
	 * Disables the logs only options.
	 */
	public void disableLogsOptions() {
		setLogsEnabledValues(false);
	}

	/**
	 * Enables all of the actions options and disables the other options
	 */
	public void enableActionOptions() {
		disableStatusOptions();
		disableLogsOptions();
		disableProcessOptions();
		
		setActionsEnabledValues(true);
	}
	
	/**
	 * Disables the action only options.
	 */
	public void disableActionOptions() {
		setActionsEnabledValues(false);
	}

	/**
	 * Enables all of the process options and disables the other options
	 */
	public void enableProcessOptions() {
		disableStatusOptions();
		disableLogsOptions();
		disableActionOptions();
		
		setProcessEnabledValues(true);
	}

	/**
	 * Disables the process only options.
	 */
	public void disableProcessOptions() {
		setProcessEnabledValues(false);
	}
}
