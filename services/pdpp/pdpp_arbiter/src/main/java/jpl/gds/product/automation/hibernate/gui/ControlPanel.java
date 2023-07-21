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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.shared.util.HostPortUtility;


/**
 * Control panel for the pdpp that allows the user to start and stop the arbiter, enable and 
 * disable individual PDPP's as well as enter global input paramters for gui operation.  
 * 
 * MPCS-8182 - 08/08/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel {
	/**
	 * MPCS-6637 -  9/2014 - Removing the arbiter table.
	 */
	private static final int OK_TO_UPDATE_OPTION = 0;
	
	// Labels
	private static final String TITLE = "PDPP Admin Control Panel";
	
	private static final String TITLE_D = "Arbiter Control Update Warning";
	private static final String MESSAGE_D = "Proceed with update?";
	
	// Names of stuff so they can be found.
	private static final String ACTION_TOGGLE_NAME = "enabeldToggle";
	
	private static final String MNEMONIC_NAME = "mnemonic";
	
	private static final String RF_TT = "Refreshes all the information on the page to the current settings";
	private static final String UD_TT = "Takes all user input and makes the changes permenant";
	private static final String PENDING_IND_TT = "Updates are pending.";
	
	private static final Color START_ENABLE_COLOR = Color.decode("#C2FFC2");
	private static final Color STOP_DISABLE_COLOR = Color.decode("#FFE0E0");
	
	private JLabel updatesPending;
	private JButton refresh;
	private JButton update;
	private JLabel host;
	private JPanel actionPanel;
	
	// Action for each action rows toggle button.
	ActionListener actionToggleAction;

	private ProductAutomationClassMapDAO classMapDao;
	
	/**
	 * Base constructor for the control panel
	 */

	public ControlPanel(ProductAutomationClassMapDAO classMapDao) {
		super();
		this.classMapDao = classMapDao;

		init();
	}
	
	/**
	 * MPCS-6637 -  9/2014 - Removing the arbiter table.
	 */
	
	/**
	 * MPCS-6637 -  9/2014 - Removing the arbiter table.  Updates should be 
	 */
	
	private String getHost() {
		String hostMachine;
		
		hostMachine = HostPortUtility.getLocalHostName();
		
		return hostMachine;
	}
	
	private void init() {
		setLayout(new GridBagLayout());
		
		Font bold = new Font("Seriff", Font.BOLD, 12);
		host = new JLabel(getHost());
		host.setFont(bold);
		
		actionPanel = new JPanel(new GridBagLayout());
		
		refresh = new JButton(IconFactory.refresh());
		refresh.setToolTipText(RF_TT);
		refresh.setFocusable(false);
		refresh.setBorderPainted(false);
		
		update = new JButton(IconFactory.controlUpdate());
		update.setToolTipText(UD_TT);
		update.setFocusable(false);
		update.setBorderPainted(false);
		
		updatesPending = new JLabel(IconFactory.controlNoUpdatePending());
		updatesPending.setToolTipText(PENDING_IND_TT);
		
		actionToggleAction = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pendingUpdates()) {
					updatesPending.setIcon(IconFactory.controlUpdatePending());
				} else {
					updatesPending.setIcon(IconFactory.controlNoUpdatePending());
				}
			}
		};
		
		addListeners();
		doPanelLayout();		
		refresh();
	}
	
	private void doPanelLayout() {
		int gridx = 0;
		int gridy = 0;

		add(new JLabel(TITLE), 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.NORTH, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		gridx = 0;
		
		add(host,
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 10, 0, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));				
		
		gridx = 0;
		
		add(new JSeparator(JSeparator.HORIZONTAL),
				new GridBagConstraints(gridx, //gridx, 
						++gridy, // gridy, 
						GridBagConstraints.REMAINDER, // gridwidth, 
						1, // gridheight, 
						1, // weightx, 
						0, // weighty, 
						GridBagConstraints.WEST, // anchor, 
						GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
						new Insets(5, 5, 0, 5), // insets - 
						0, // ipadx, 
						0 // ipady
					)				
				);
		
		gridx = 0;
		
		add(actionPanel, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		

		// Add a tool bar.  This will be at the bottom...?
		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		tb.setFloatable(false);
		tb.setLayout(new GridBagLayout());
		tb.setBorder(BorderFactory.createEtchedBorder());
		
		add(tb, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		add(Box.createVerticalGlue(), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					1, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.VERTICAL, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		// Add the stuff to the tool bar.
		gridx = 0;
		gridy = 0;
		tb.add(update, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));			
		
		tb.add(this.updatesPending, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 5, 5, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		tb.add(Box.createHorizontalGlue(), 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.EAST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(5, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));			
		
		tb.add(refresh, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.EAST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));					
	}

	private void addListeners() {
		refresh.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		
		update.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				update();
				refresh();
			}
		});
	}
	
	/**
	 * Does a query of the db for classmpas and updates the panel.  
	 */
	private void updateActions() {
		int gridy = 0;

		try {
			Collection<ProductAutomationClassMap> cmaps = classMapDao.getClassMaps();
			int lastIndex = cmaps.size() - 1;
			for (ProductAutomationClassMap cm : classMapDao.getClassMaps()) {
				addAction(gridy, cm, gridy == lastIndex ? 5 : 0);
				gridy++;
			}
		} catch (Exception e) {
			// TODO should be a log or whatever??  Print the errors there.
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a single class map line deal.
	 * 
	 * @param gridy
	 * @param cm
	 * @param bottomInset
	 */
	private void addAction(int gridy, ProductAutomationClassMap cm, int bottomInset) {
		JPanel cp = new JPanel(new GridBagLayout());
		cp.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		boolean currentlyEnabled = cm.getEnabled() == ProductAutomationClassMapDAO.Abled.ENABLED.value();

		JLabel nm = new JLabel(cm.getMnemonic());
		nm.setName(MNEMONIC_NAME);
		
		JLabel ai = new JLabel(IconFactory.getActionIcon(cm.getMnemonic()));
		
		int gx = 0;
		int gy = 0;

		cp.add(ai, 
				new GridBagConstraints(gx, //gridx, 
					gy, // gridy, 
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
		
		cp.add(nm, 
				new GridBagConstraints(++gx, //gridx, 
					gy, // gridy, 
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

		// Add glue to keep these bad boys on the right.
		cp.add(Box.createHorizontalGlue(), 
				new GridBagConstraints(++gx, //gridx, 
					gy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		// Set the default button based on the value of the deal.  If it is currently enabled, 
		// then the icon should be to disable it, or vice versa.
		
		final JToggleButton enable = new JToggleButton(IconFactory.controlActionEnabled());
		enable.setFocusable(false);
		enable.setBorderPainted(false);
		enable.setContentAreaFilled(false);
		enable.setName(ACTION_TOGGLE_NAME);
		enable.setSelected(currentlyEnabled);
		enable.setEnabled(!currentlyEnabled);
		
		final JToggleButton disable = new JToggleButton(IconFactory.controlActionDisabled());
		disable.setFocusable(false);
		disable.setBorderPainted(false);
		disable.setContentAreaFilled(false);
		disable.setSelected(!currentlyEnabled);
		disable.setEnabled(currentlyEnabled);
		
		ActionListener al = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource().equals(disable)) {
					enable.setEnabled(true);
					disable.setEnabled(false);
					enable.setSelected(false);
				} else if (e.getSource().equals(enable)) {
					enable.setEnabled(false);
					disable.setEnabled(true);
					disable.setSelected(false);
				}
				
				updatesPendingUpdate();
 			}
		};
		
		disable.addActionListener(al);
		enable.addActionListener(al);
		
		cp.add(enable, 
				new GridBagConstraints(++gx, //gridx, 
					gy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.EAST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		cp.add(disable, 
				new GridBagConstraints(++gx, //gridx, 
					gy, // gridy, 
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
		
		// If it is the last index, need to add a pad to the bottom of the inset.
		// Set the background color based on the current state of the icon.
		cp.setBackground(currentlyEnabled ? START_ENABLE_COLOR : STOP_DISABLE_COLOR);
		
		actionPanel.add(cp, 
				new GridBagConstraints(0, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					new Insets(5, 5, bottomInset, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));			
	}
	
	/**
	 * Gets all the current values and populates the buttons and indicators in
	 * the panel. This is safe to call anywhere. Uses a new thread and invoke
	 * later call.
	 */
	private void refresh() {
//		setArbiterRelatedIcons();
		
		// Deal with all the actions.
		actionPanel.removeAll();
		
		updateActions();
		updatesPending.setIcon(IconFactory.controlNoUpdatePending());
		
		revalidate();
	}
	
	private void updatesPendingUpdate() {
		boolean pending = pendingUpdates();
		Icon icon = pending ?
				IconFactory.controlUpdatePending() : 
				IconFactory.controlNoUpdatePending();
		String tt = pending ? PENDING_IND_TT : "";
		
		updatesPending.setIcon(icon);
		updatesPending.setToolTipText(tt);
		
		revalidate();
	}
	
	private HashMap<String, Boolean> getActionPanelState() {
		HashMap<String, Boolean> states = new HashMap<String, Boolean>();
		
		for (Component cmp : actionPanel.getComponents()) {
			if (!(cmp instanceof JPanel)) {
				continue;
			}
			String mnemonic = null;
			Boolean enabled = null;
			
			
			for (Component c : ((JPanel) cmp).getComponents()) {
				if (MNEMONIC_NAME.equals(c.getName())) {
					mnemonic = ((JLabel) c).getText();
				} else if (ACTION_TOGGLE_NAME.equals(c.getName())) {
					enabled = ((JToggleButton) c).isSelected();
				}
			}
			
			if (mnemonic != null && enabled != null) {
				states.put(mnemonic, enabled);
			}
		}
		
		return states;
	}
	
	/**
	 * Checks all the editable stuff in this widget to see if updates are
	 * needed.
	 * 
	 * @return true if there are updates pending, false if not
	 */
	private boolean pendingUpdates() {
		// Cycle through all the stuff in the action panel to get the toggle buttons.
		boolean pending = false;
		
		for (Boolean en : this.getActionPanelState().values()) {
			pending = pending || en;
		}
		
		return pending;
	}
	
	/**
	 * Creates an option dialogue for the user to update.
	 * 
	 * @return true if the user wants to complete the update, false if not
	 */
	private boolean continueWithUpdate() {
		int n = JOptionPane.showConfirmDialog(this, 
				MESSAGE_D,
				TITLE_D,
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.WARNING_MESSAGE);
		
		return n == OK_TO_UPDATE_OPTION;
	}
	
	/**
	 * Takes all the updates from the page and makes them permanent. Does a user
	 * input dialogue before making update.
	 */
	private void update() {
		if (continueWithUpdate()) {
			// Start the arbiter if need be.
			
			HashMap<String, Boolean> panelStates = getActionPanelState();
			
			for (String mnemonic : panelStates.keySet()) {
				boolean enabled = panelStates.get(mnemonic);
	
				try { 
					classMapDao.startTransaction();
	
					if (enabled) {
						classMapDao.enableClassMap(mnemonic);
					} else {
						classMapDao.disableClassMap(mnemonic);
					}
					
					classMapDao.commit();
				} catch (Exception e) {
					System.out.println("Could not update classmap blah");
					classMapDao.rollback();
				} finally {
					classMapDao.closeSession();
				}
			}
		}
	}
	
}
