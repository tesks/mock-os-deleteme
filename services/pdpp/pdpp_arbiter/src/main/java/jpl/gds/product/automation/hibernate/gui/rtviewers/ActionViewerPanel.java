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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Collection;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.gui.UserInputInterface;
import jpl.gds.product.automation.hibernate.gui.models.ActionTableModel;
import jpl.gds.product.automation.hibernate.gui.renderers.ActionTableCellRenderer;

/**
 * Panel widget that holds that action table and the controls related to it. If
 * the execute button is clicked or the real time monitoring is selected, then
 * this widget will create action events and send them to any action listeners.
 * Just register an event listener to this to get the events. These events mean
 * that a query is needed so the frame, or whatever is controlling this widget
 * needs to do a query and give the data back when it is ready.
 * 
 *
 */
@SuppressWarnings("serial")
public class ActionViewerPanel extends AbstractGuiRealTimePanel {
	
	private JTable actionTable;
	
	/**
	 * Constructor that ties the panel to the user interface
	 * 
	 * @param userInput
	 *            the display's user input panel
	 */
	public ActionViewerPanel(UserInputInterface userInput) {
		super(userInput);
		init();
	}
	
	private void init() {
		export.setEnabled(false);
		
		actionTable = new JTable(new ActionTableModel());
		actionTable.setShowGrid(true);
		actionTable.setGridColor(Color.BLACK);
		actionTable.setAutoCreateRowSorter(true);
		actionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		actionTable.getTableHeader().setReorderingAllowed(false);
		
		setTableCellRenderer(actionTable, new ActionTableCellRenderer());
		
		actionTable.setRowHeight(ROW_HEIGHT);
		enableAdjuster(actionTable);
		
		doPanelLayout();
	}
	
	private void doPanelLayout() {
		addContent(new JScrollPane(actionTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), 
				new GridBagConstraints(0, //gridx, 
					0, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					1, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(5, 5, 5, 5), // insets - 
					0, // ipadx, 
					0 // ipady
				));
	}
	
	private ActionTableModel getModel() {
		return (ActionTableModel) actionTable.getModel();
	}
	
	@Override
	public void clearContents() {
		getModel().clearData();
	}

	@Override
	public void exportContents() {
		// Not supported
	}

	@Override
	public void removeContents() {
		getModel().removeRows(actionTable.getSelectedRows());
	}

	@Override
	public void executeQuery() {
		Thread t = new Thread() {
			public void run() {
				setRunning();
				
				startProgress();
				final Collection<ProductAutomationAction> actions = getQueryPanel().getActions();
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						getModel().addRows(actions);
						doAdjust();
						stopProgress();
						setNotRunning();
					}
				});
			}
		};
		
		t.start();
	}

	@Override
	public void includeFilePaths() {
		getModel().setFullPaths(true);
		adjuster.adjustColumns();
	}

	@Override
	public void excludeFilePaths() {
		getModel().setFullPaths(false);
		adjuster.adjustColumns();
	}
}
