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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * This class creates the information displayed in the legend that is under the
 * Help menu option.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS
 */
public class LegendDialog extends JDialog {
	/**	 Required serial version number*/
	private static final long serialVersionUID = 5481102446699738307L;
	private static final String TITLE = "Icon Legend";
	
	/**
	 * Constructor for LegendDialog. It must be placed within a frame
	 * @param parent the frame this dialogue will be placed in
	 */
	public LegendDialog(JFrame parent) {
		super(parent);
		setLocationRelativeTo(parent);
		init();
	}
	
	private void init() {
		setLayout(new GridBagLayout());
		doPanelLayout();
		pack();
	}
	
	private void doPanelLayout() {
		// Since these are static, going to just create the components as I add them.
		int gridx = 0;
		int gridy = 0;
		
		add(new JLabel(TITLE), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.CENTER, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		JLabel correct = new JLabel("Correct Action");
		correct.setIcon(IconFactory.correct());
		add(correct, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		JLabel unwrap = new JLabel("Unwrap Action");
		unwrap.setIcon(IconFactory.unwrap());
		add(unwrap, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		JLabel decompress = new JLabel("Decompress Action");
		decompress.setIcon(IconFactory.decompress());
		add(decompress, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));			
		
		add(Box.createGlue(), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.CENTER, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
	}
	
}
