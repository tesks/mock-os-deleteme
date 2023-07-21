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
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;

/**
 * Panel that deals with input from the user.  Will detect when the date is in the correct format
 * and auto fill some of the fields to make life easier.  Makes getting the date by the 
 * gui easier.
 * 
 * MPCS-8182 - 08/08/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial") 
public class DatePanel extends JPanel {
	
	private static final String[] DATE_FORMATS_RX = new String[] {"^\\d{4}-\\d{3}[ T-]\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,})?$", // Standard DOY with or without subseconds
		"^\\d{4}-\\d{3}$", // DOY with no times.  Must include the year.
		"^(?i)(sol)-\\d{4}M\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,})?$", // Standard LMST with or without subseconds.
		"^\\d{1,4}$" // LMST with only sol number.  If only a number is found, assumes it is a sol.
	};
	
	private static final String HMS = "00:00:00.000";
	private static final String DOY_SEP = "T";
	
	private static final String SOL = "SOL";
	private static final String SOL_SEP = "M";
	private static final String INVALID = "INVALID SOL NUMBER";
	private static final int COLUMNS = 20;
	
	// Used to set the size of the text field.f
	private static final Pattern[] DATE_FORMAT_PATTERNS;
	
	static {
		DATE_FORMAT_PATTERNS = new Pattern[DATE_FORMATS_RX.length];
		
		for (int index = 0; index < DATE_FORMAT_PATTERNS.length; index++) {
			DATE_FORMAT_PATTERNS[index] = Pattern.compile(DATE_FORMATS_RX[index]);
		}
	}
	
	private static final String TOOLTIP = "Enter date with one of the following formats: \nYYYY-jjjTHH:MM:SS.fff, YYYY-jjj, SOL-DDDDDMHH:MM:SS.fff." +
			"If a single number is entered, \nit is assumed to be a SOL number and the time to be 00:00:00.";
	
	private JTextField date;
	private JLabel convertedDate;
	private boolean isLst;
	
	/**
	 * Constructor for a date panel.
	 */
	public DatePanel() {
		super();
		init();
	}
		
	/**
	 * Initializes the DatePanel
	 */
	public void init() {
		date = new JTextField(COLUMNS);
		date.setToolTipText(TOOLTIP);
		
		// Get size based on the font.  Use the sample txt for this blah.
		convertedDate = new JLabel();
		setLayout(new GridBagLayout());	
		doPanelLayout();
	}
	
	/**
	 * Places the date and converted date in the display. It also sets up the
	 * listener that will update the displayed date if the input changes
	 */
	public void doPanelLayout() {
		int gridx = 0;
		int gridy = 0;

		add(date, 
				new GridBagConstraints(gridx++, //gridx, 
					gridy, // gridy, 
					2, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill, 
					new Insets(0, 0, 0, 0), // insets, 
					0, // ipadx, 
					0 // ipady
			));
		
		add(convertedDate, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill, 
					new Insets(0, 10, 0, 10), // insets, 
					0, // ipadx, 
					0 // ipady
				));
		
		DocumentListener dl = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				// Not used
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				textChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				textChanged();
			}
		};
		
		date.getDocument().addDocumentListener(dl);
	}
	
	private void textChanged() {
		Color textColor = Color.BLACK;
		String labelText = "";
		
		String dateText = date.getText();
		int fmtIndex = matchDateFormat(dateText);
				
		switch(fmtIndex) {
		case 0:
			// Standard DOY and LST input value.  Just set the string
			labelText = dateText;
			isLst = false;
			break;
		case 1:
			// Need to add the default stuff to the string.
			labelText = dateText + DOY_SEP + HMS;
			isLst = false;
			break;
		case 2:
			labelText = dateText.toUpperCase();
			isLst = true;
			break;
		case 3:
			// User was lazy.  Just gave the sol.  Format and set.
			try {
				labelText = String.format("%s-%04d%s%s", SOL, Long.parseLong(dateText), SOL_SEP, HMS);
				isLst = true;
			} catch (NumberFormatException e ) {
				// Special case.  If they enter a number that is ober the max.
				textColor = Color.RED;
				labelText = INVALID;
				isLst = false;
			}
			break;
		default:
			// Just need to set to set the color.  
			textColor = Color.RED;
			break;
		}
		
		if (!date.getForeground().equals(textColor)) {
			date.setForeground(textColor);
		}
		
		// Set the label deal.
		convertedDate.setText(labelText);
	}
	
	/**
	 * Checks the date string entered against the formats in the list. Returns
	 * the index of the format that is matched, -1 if no match.
	 * 
	 * @param dateText
	 *            the date string for which a format is being found
	 * @return the index of the format that matches the supplied date text
	 */
	public int matchDateFormat(String dateText) {
		int result = -1;
		
		for(int index = 0; index < DATE_FORMAT_PATTERNS.length; index++) {
			final Matcher m = DATE_FORMAT_PATTERNS[index].matcher(dateText);
			
			if (m.find()) {
				result = index;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Converts the converted string to a accurate date/time object and returns
	 * it. If not set or invalid will return null.
	 * 
	 * @return the date/time stored in this panel
	 */
	public IAccurateDateTime getTime() {
		String d = convertedDate.getText();
		IAccurateDateTime dt = null;
		
		if (!d.isEmpty() && !d.equals(INVALID)) {
			try {
				if (isLst) {
					// Have to convert the lst to sol and then get the scet of the value.
					dt =  (LocalSolarTimeFactory.getNewLst(d)).toScet();
				} else {
					dt = new AccurateDateTime(d);
				}
			} catch (ParseException e) {
				// TODO how to deal with logging?
				System.out.println("Format is wrong : " + d);
			}
		}
		
		return dt;
	}
	
	/**
	 * Will disable or enable the text input fields. If enabled is false will
	 * clear the text field and disable it.
	 * 
	 * @param enabled
	 *            true if input is accepted for this date panel, false if not
	 */
	public void enableInput(boolean enabled) {
		if (!enabled) {
			date.setText("");
		}
		
		date.setEnabled(enabled);
	}
}