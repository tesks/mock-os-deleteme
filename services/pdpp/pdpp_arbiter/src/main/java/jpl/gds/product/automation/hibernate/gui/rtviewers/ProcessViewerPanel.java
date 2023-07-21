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

import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.gui.UserInputInterface;
import jpl.gds.product.automation.hibernate.gui.models.ProcessTableModel;
import jpl.gds.product.automation.hibernate.gui.renderers.ProcessTableCellRenderer;

/**
 * Viewing panel to display PDPP automation process information. For each action
 * type displays process information.
 * 
 */
@SuppressWarnings("serial")
public class ProcessViewerPanel extends AbstractGuiRealTimePanel {
	private JTable processTable;

	/**
	 * Constructor that ties the panel to the user interface
	 * 
	 * @param userInput
	 *            the display's user input panel
	 */
	public ProcessViewerPanel(UserInputInterface userInput, IFswToDictionaryMapper mapper) {
		super(userInput);
		
		init(mapper);
	}
	
	private void init(IFswToDictionaryMapper mapper) {
		// Include file types not valid, so disable it.
		export.setVisible(false);
		includeFilePath.setVisible(false);
		
		processTable = new JTable(new ProcessTableModel(mapper));
		processTable.setShowGrid(true);
		processTable.setGridColor(Color.BLACK);
		processTable.setAutoCreateRowSorter(true);
		processTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		processTable.getTableHeader().setReorderingAllowed(false);

		setTableCellRenderer(processTable, new ProcessTableCellRenderer());
		processTable.setRowHeight(ROW_HEIGHT);
		enableAdjuster(processTable);

		doPanelLayout();
	}
	
	private void doPanelLayout() {
		addContent(new JScrollPane(processTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), 
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
	
	private ProcessTableModel getModel() {
		return (ProcessTableModel) processTable.getModel();
	}
	
	@Override
	public void clearContents() {
		getModel().clearData();
	}

	@Override
	public void exportContents() {
		// Not applicable for this view.
	}

	@Override
	public void removeContents() {
		getModel().removeRows(processTable.getSelectedRows());
	}

	@Override
	public void executeQuery() {
		Thread t = new Thread() {
			public void run() {
				setRunning();
				
				startProgress();
				final Collection<ProductAutomationProcess> processes = getQueryPanel().getProcesses();
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						// This is a very small amount of data, so going to just clear it out.
						getModel().clearData();
						getModel().addRows(processes);
						
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
		// Not applicable for this view.
	}

	@Override
	public void excludeFilePaths() {
		// Not applicable for this view.
	}
}
