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


import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.gui.AncestorMap;
import jpl.gds.product.automation.hibernate.gui.IconFactory;
import jpl.gds.product.automation.hibernate.gui.UserInputInterface;
import jpl.gds.product.automation.hibernate.gui.models.LineageCustomTable;

/**
 * Display mode will pick between top and and bottom up.  
 * 
 */
@SuppressWarnings("serial")
public class LineageViewerPanel extends AbstractGuiRealTimePanel {
	LineageCustomTable lineageViewer;
	
	private JToggleButton displayMode;

	/**
	 * Constructor that ties the panel to the user interface
	 * 
	 * @param userInput
	 *            the display's user input panel
	 */
	public LineageViewerPanel(UserInputInterface userInput, AncestorMap ancestorMap) {
		super(userInput);
		init(ancestorMap);
	}
	
	private void init(AncestorMap ancestorMap) {
		export.setEnabled(false);
		delete.setEnabled(false);
		realTime.setEnabled(false);
		
		lineageViewer = new LineageCustomTable(ancestorMap);
		
		// Create the display mode.  This will be a toggle button to change the view from bottom
		// up to top down.  The icon show will be the type that is currently used. Default mode
		// is top down, which means to top of each lineage tree will be the top most ancestor of the product.
		displayMode = new JToggleButton(IconFactory.topDown());
		displayMode.setFocusable(false);
		
		doPanelLayout();
		addActionListeners();
	}

	private void addActionListeners() {
		displayMode.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// First, set the icon based on the deal.
				displayMode.setIcon(isBottomUp() ? 
						IconFactory.bottomUp() :
						IconFactory.topDown()
						);
				
				// Next, need to set the value in the table.
				lineageViewer.setBottomUp(isBottomUp());
			}
		});
	}
	
	private boolean isBottomUp() {
		return displayMode.isSelected();
	}
	
	private void doPanelLayout() {
		addContent(lineageViewer,
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
		
		/**
		 * Get the bottom tool bar and add the displayMode button to it.
		 */
		getBottomToolbar().remove(export);
		
		getBottomToolbar().add(displayMode,
				new GridBagConstraints(1, //gridx, 
						0, // gridy, 
						1, // gridwidth, 
						1, // gridheight, 
						1, // weightx, 
						0, // weighty, 
						GridBagConstraints.EAST, // anchor, 
						0, // fill - no fill for text controls.
						new Insets(5, 0, 5, 5), // insets - 
						0, // ipadx, 
						0 // ipady
					));
	}

	@Override
	public void clearContents() {
		lineageViewer.clear();
	}

	@Override
	public void exportContents() {
		// Not supported
	}

	@Override
	public void removeContents() {
		// Currently not supported.
	}

	@Override
	public void executeQuery() {
		Thread t = new Thread() {
			public void run() {
				setRunning();
				
				startProgress();
				final Collection<ProductAutomationProduct> products = userInput.getProducts(isBottomUp());
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						lineageViewer.addProducts(products);
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
		lineageViewer.useFullPaths();
	}

	@Override
	public void excludeFilePaths() {
		lineageViewer.useShortPaths();
	}
}