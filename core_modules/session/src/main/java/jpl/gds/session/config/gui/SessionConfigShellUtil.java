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
package jpl.gds.session.config.gui;

import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.common.config.connection.HostNameValidator;
import jpl.gds.session.config.SessionKeyValidator;
import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.util.HostPortUtility;

/**
 * Contains static convenience methods and constants for use by the collection
 * of GUI classes that make up the session configuration GUI window.
 * 
 *
 */
public class SessionConfigShellUtil {
	/**
	 * Default font face.
	 */
	public static final String DEFAULT_FACE = "Helvetica";
	/**
	 * Default font size.
	 */
	public static final int DEFAULT_FONT_SIZE = 14;

	/**
	 * Default label style.
	 */
	public static final int LABEL_STYLE = SWT.LEFT;

	/**
	 * Default style for short (single-line) text.
	 */
	public static final int SHORT_TEXT_STYLE = SWT.SINGLE | SWT.BORDER;
	/**
	 * Default style for long (multi-line) text.
	 */
	public static final int LONG_TEXT_STYLE = SWT.MULTI | SWT.BORDER
			| SWT.V_SCROLL | SWT.WRAP;
	/**
	 * Default style for combo boxes.
	 */
	public static final int COMBO_STYLE = SWT.DROP_DOWN | SWT.BORDER
			| SWT.READ_ONLY;
	/**
	 * Default style for group composites.
	 */
	public static final int GROUP_STYLE = SWT.BORDER | SWT.SHADOW_ETCHED_IN;
	/**
	 * Left column label start offset.
	 */
	public static final int LEFT_COLUMN_LABEL_START = 0;
	/**
	 * Left column input start offset.
	 */
	public static final int LEFT_COLUMN_INPUT_START = 16;
	/**
	 * Right column label start offset.
	 */
	public static final int RIGHT_COLUMN_LABEL_START = 47;
	/**
	 * Right column input start offset.
	 */
	public static final int RIGHT_COLUMN_INPUT_START = 66;
	/**
	 * Short field size offset.
	 */
	public static final int SHORT_FIELD_SIZE = 30;
	/**
	 * Long field size offset.
	 */
	public static final int LONG_FIELD_SIZE = 60;

	/**
	 * Private constructor to enforce static nature.
	 */
	private SessionConfigShellUtil() {}
	
	/**
	 * Gets and validates a port number from a port text field. Verifies the
	 * value is a valid integer and is within the range of valid port numbers.
	 * If it is not, displays an error dialog to the user, sets focus to the
	 * Text control, and returns null. Otherwise, returns the parsed port
	 * number.
	 * 
	 * @param text
	 *            the Text control to read
	 * @param type
	 *            type of port field; used in the displayed error message
	 * @param parent
	 *            the parent Shell for any error dialog
	 * 
	 * @return port as an Integer or null
	 */
	public static Integer getAndValidatePortText(final Text text,
			final String type, final Shell parent) {

		final String portString = text.getText().trim();

		int port = 0;

		try {
			port = Integer.parseInt(portString);
		} catch (final NumberFormatException nfe) {
			SWTUtilities.showErrorDialog(parent, "Bad " + type + " port",
					"The specified " + type
							+ " port is not a valid integer port value");

			text.setFocus();

			return null;
		}

		if ((port < 0) || (port > HostPortUtility.MAX_PORT_NUMBER)) {
			SWTUtilities.showErrorDialog(parent, "Bad " + type + " port",
					"The specified " + type
							+ " port is not a valid port value. "
							+ "Valid ports are in the range 0-"
							+ HostPortUtility.MAX_PORT_NUMBER);

			text.setFocus();

			return null;
		}

		return port;
	}

    /**
     * Gets and validates the bootstrapLad session ID text field.
     * 
     * If the text field is not a number range list, displays an error dialog to the user, drops the mouse focus
     * into the offending text field, and returns null.
     * 
     * If the text field is blank, no sessions are specified. Returns empty string.
     * Otherwise, returns the boostrapLad id list from the text field, trimmed.
     * 
     * @param text
     *            the Text control to read
     * @param parent
     *            the parent Shell for any error dialog
     * @return bootstrapLad ids or null
     */
    public static String getAndValidateLadIds(final Text text, final Shell parent) {
        final String value = text.getText().trim();
        if (value == null || value.isEmpty()) {
            // no bootstrapIds provided is OK
            return "";
        }
        try {
        		CliUtility.expandCsvRangeLong(value);
        }
        catch (final ParseException e) {
            SWTUtilities.showErrorDialog(parent, "Bad " + text, "You must enter a valid " + text + " to proceed. "
                    + ExceptionTools.getMessage(e));
            text.setFocus();
            return null;
        }

        return value;
    }

	/**
	 * Gets and validates a host name from a text field. Verifies the value is
	 * not empty. If the host entered is invalid, displays an error dialog to
	 * the user, drops the mouse focus into the offending text field, and
	 * returns null. Otherwise, returns the host name value from the text field,
	 * trimmed.
	 * 
	 * @param text
	 *            the Text control to read
	 * @param type
	 *            type of host
	 * @param parent
	 *            the parent Shell for any error dialog
	 * 
	 * @return Host name or null
	 */
	public static String getAndValidateHostText(final Text text,
			final String type, final Shell parent) {

		final String value = text.getText().trim();

		if (value.isEmpty()) {
			SWTUtilities.showErrorDialog(parent, "Bad " + type,
					"You must enter a valid " + type + " host to proceed");

			text.setFocus();

			return null;
		}

		return value;
	}

	/**
	 * Sorts the given array of strings, in alphabetically descending order, and
	 * returns a new array. The original array is untouched.
	 * 
	 * @param items
	 *            array of Strings to sort
	 * @return a new sorted array
	 */
	public static String[] reverseSort(final String[] items) {

		final String[] temp = new String[items.length];
		System.arraycopy(items, 0, temp, 0, items.length);
		Arrays.sort(temp);
		final String[] newItems = new String[temp.length];
		int index = temp.length - 1;
		for (final String item : temp) {
			newItems[index--] = item;
		}
		return newItems;
	}

	/**
	 * Initializes a combo box with values "true" and "false" and sets the value
	 * of the combo to "true".
	 * 
	 * @param combo
	 *            the Combo to initialize
	 */
	public static void initBooleanCombo(final Combo combo) {

		combo.add(Boolean.TRUE.toString());
		combo.add(Boolean.FALSE.toString());
		combo.setText(Boolean.TRUE.toString());
	}

	/**
	 * Safe setter method for a Text widget. Sets the value of the Text control
	 * to the specified input value, or to the empty string if the input value
	 * is null.
	 * 
	 * @param widget
	 *            the Text control to update
	 * @param value
	 *            the String value to set in the control
	 */
	public static void safeSetText(final Text widget, final String value) {

		if (value == null) {
			widget.setText("");
		} else {
			widget.setText(value.trim());
		}
	}

	/**
	 * Safe setter method for a Combo widget. Sets the value of the Text control
	 * to the specified input value, or to the empty string if the input value
	 * is null.
	 * 
	 * @param widget
	 *            the Text control to update
	 * @param value
	 *            the String value to set in the control
	 */
	public static void safeSetText(final Combo widget, final String value) {

		if (value == null) {
			widget.select(0);
		} else {
			widget.setText(value.trim());
		}
	}

	/**
	 * Validate a string as a session key,
	 * 
	 * @param s
	 *            String to validate
	 * 
	 * @return Error message or null if OK
	 */
	public static String validateDatabaseSessionKey(final String s) {

		final SessionKeyValidator v = new SessionKeyValidator();

		return v.isValid(s);
	}

	/**
	 * Validate a string as a session host,
	 * 
	 * @param s
	 *            Host name to validate
	 * 
	 * @return Error message or null if OK
	 */
	public static String validateDatabaseSessionHost(final String s) {

		final HostNameValidator v = new HostNameValidator();

		return v.isValid(s);
	}

}
