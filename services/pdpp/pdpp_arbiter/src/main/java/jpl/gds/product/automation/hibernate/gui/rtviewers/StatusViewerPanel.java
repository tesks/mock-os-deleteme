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

import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import jpl.gds.product.automation.hibernate.gui.UserInputInterface;
import jpl.gds.product.automation.hibernate.gui.models.StatusTableModel;
import jpl.gds.product.automation.hibernate.gui.renderers.StatusTableCellRenderer;

/**
 * Panel widget that holds that status table and the controls related to it. If
 * the execute button is clicked or the real time monitoring is selected, then
 * this widget will create action events and send them to any action listeners.
 * Just register an event listener to this to get the events. These events mean
 * that a query is needed so the frame, or whatever is controlling this widget
 * needs to do a query and give the data back when it is ready.
 * 
 *
 */
@SuppressWarnings("serial")
public class StatusViewerPanel extends AbstractGuiRealTimePanel {
	private JTable statusTable;

	/**
	 * Constructor that ties the panel to the user interface
	 * 
	 * @param userInput
	 *            the display's user input panel
	 */
	public StatusViewerPanel(UserInputInterface userInput) {
		super(userInput);
		
		init(); 
	}
	
	private StatusTableModel getModel() {
		return (StatusTableModel) statusTable.getModel();
	}
	
	private void init() {
		export.setEnabled(false);
		realTime.setEnabled(false);
		
		statusTable = new JTable(new StatusTableModel());
		statusTable.setShowGrid(true);
		statusTable.setGridColor(Color.BLACK);
		statusTable.setAutoCreateRowSorter(true);
		statusTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		statusTable.getTableHeader().setReorderingAllowed(false);
		
		StatusTableCellRenderer r = new StatusTableCellRenderer();

		setTableCellRenderer(statusTable, r);
		statusTable.setRowHeight(ROW_HEIGHT);
		enableAdjuster(statusTable);
		
		doPanelLayout();
	}
	
	private void doPanelLayout() {
		addContent(new JScrollPane(statusTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), 
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
		getModel().removeRows(statusTable.getSelectedRows());
	}
	
	@Override
	public void executeQuery() {
		Thread t = new Thread() {
			public void run() {
				setRunning();
				
				startProgress();
				final Collection<ProductAutomationStatus> statuses = getQueryPanel().getStatuses();
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						getModel().addRows(statuses);
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