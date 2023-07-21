package jpl.gds.tcapp.app.gui.fault.util;

import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.CommandParseException;

/**
 * Helper class for common Command/Command Argument GUI operations
 * 
 *
 */
public class CommandArgumentGuiHelper {
	private CommandArgumentGuiHelper() {}

	/**
	 * For the currently selected command, find the position of the selected command
	 * argument.
	 *
	 * @param the
	 *            SWT event
	 * @return command argument index if found
	 */
	public static int getSelectedCmdArgument(TypedEvent me, Control[] argValues) {
		int position = -1;

		for (int i = 0; i < argValues.length; ++i) {
			// ENUM command arguments are Combo objects
			if (argValues[i] instanceof Combo) { 
				if (me.getSource().equals((Combo) argValues[i])) {
					position = i;
					break;
				}
			} else if (argValues[i] instanceof Text) { 
				// non-ENUM arguments are Text objects
				if (me.getSource().equals((Text) argValues[i])) {
					position = i;
					break;
				}
			}
		}
		return position;
	}

	/**
	 * Uses UplinkInputParser to parse and set the NON-REPEAT supplied command argument 
	 * 
	 * @param appContext the ApplicationContext used to create the command
	 * @param position command argument position 
	 * @param value command argument value to set 
	 * @param cmd command to update 
	 */
	public static void parseCmdArgument(final ApplicationContext appContext, Integer position, String value, IFlightCommand cmd) {
		try {
			UplinkInputParser.parseAndSetGuiArgument(appContext, cmd, position, value);
		} catch (NullPointerException | CommandParseException | IllegalStateException | IllegalArgumentException e) {
			TraceManager.getDefaultTracer()
					.warn("Unexpected exception parsing command: Attempted to set command argument " 
			                + position + ", value=" + value
					        + " - " + (e.getCause() != null ? e.getCause() : e.getMessage()));
			cmd.clearArgumentValue(position);
		}
	}
	
	/**
	 * Uses UplinkInputParser to parse and set the supplied REPEAT command argument 
	 * 
	 * @param appContext the ApplicationContext used to create the command
	 * @param index repeat argument index
	 * @param subIndex position of the repeat command argument to update
	 * @param value repeat command argument value to set 
	 * @param cmd command to update 
	 */
	public static void parseCmdArgument(final ApplicationContext appContext, Integer index, Integer subIndex, String value, IFlightCommand cmd) {
		try {
			UplinkInputParser.parseAndSetCommandArgument(appContext, cmd, index, subIndex, value);
		} catch (NullPointerException | CommandParseException | IllegalStateException | IllegalArgumentException e) {
			TraceManager.getDefaultTracer()
					.warn("Unexpected exception parsing command: Attempted to set command argument " 
			                + index + ",subIndex=" + subIndex + ", value=" + value
					        + " - " + (e.getCause() != null ? e.getCause() : e.getMessage()));
		}
	}

	/**
     * 'Search' algorithm for user typing in the command argument ENUM drop-down.
     * Remove all enums from the drop-down that don't start with the current user input. 
     * 
     * @param appContext the ApplicationContext used for the command
     * @param commandArg the Command argument Combo item (ENUM) to update
     * @param cmd the selected IFlightCommand
     * @param argPosition the selected command argument position
     *
     * @return whether or not the typed text is a valid enum for this command
     *         argument
     *         
     * 09/21/18 - moved core logic to updateAndSearcCmdArg(Combo, IFlightCommand, int, int)
     */
	public static boolean updateAndSearchCmdArg(final ApplicationContext appContext, Combo commandArg, IFlightCommand cmd, int argPosition) {
	    return updateAndSearchCmdArg(appContext, commandArg, cmd, argPosition, -1);
	}
	
	/**
     * 'Search' algorithm for user typing in the command argument ENUM drop-down.
     * Remove all enums from the drop-down that don't start with the current user input. 
     * 
     * @param appContext the ApplicationContext used for the command
     * @param commandArg the Command argument Combo item (ENUM) to update
     * @param cmd the selected IFlightCommand
     * @param argPosition the selected command argument position
     * @param repeatArgPosition the selected command argument position within the above selected argPosition.
     *        If supplied with -1, then this argument value is ignored
     *
     * @return whether or not the typed text is a valid enum for this command
     *         argument
     */
	public static boolean updateAndSearchCmdArg(final ApplicationContext appContext, Combo commandArg, IFlightCommand cmd, int argPosition, int repeatArgPosition) {
		// At this point the selected Combo is expected to contain some text.
		String currentValue = commandArg.getText();
		if (currentValue == null || currentValue.isEmpty()) {
			return false;
		}
		currentValue = currentValue.trim();

		String[] comboSearchItems = commandArg.getItems();
		// Look through enum drop-down for a match and
		// remove any that do not start with current text entry
		int match = -1;
		for (int i = 0; i < comboSearchItems.length; ++i) {
			String term = comboSearchItems[i].trim();
			
			if (currentValue.equalsIgnoreCase(term)) {
				// Match found! User typed a valid enum
				match = i;
			} 
			// Check if text is numeric and matches enum number representation
			else if (!term.toUpperCase().contains(currentValue.toUpperCase()) && commandArg.indexOf(term) != -1 ) {
				// Remove enums that don't start with search value
				commandArg.remove(term);
			}
		}
		// Added bitvalue matching
		// There wasn't an enum match, let's try numeric
		// 
		// Added extra checks for valid binary/hex values
		// Only lookupByBitValue when the numeric is valid. Exceptions occur without this
		boolean goodInput = isValidGuiInput(currentValue);
		
		if ((match == -1 && appContext.getBean(CommandProperties.class).isAllowEnumBitValue()) && goodInput ) { 
			ICommandEnumerationValue val = null;
			
			if(repeatArgPosition >= 0) {
			    val = cmd.getArgumentDefinition(argPosition, repeatArgPosition).getEnumeration().lookupByBitValue(currentValue);
			} else {
			    val = cmd.getArgumentDefinition(argPosition).getEnumeration().lookupByBitValue(currentValue);
			}
			 
			if (val != null) { // user entered number matches the enum's bit value, match!
				match = 1;
				if (commandArg.indexOf(val.toString()) == -1) { 
				    commandArg.add(val.toString(), 0);
				}
			}
		} 
		return match != -1 ? true : false;
	}
	
	/**
	 * Helper function to determine whether or not an input string is valid GUI input. 
	 * "Valid" can mean decimal, string, hex, and binary input
	 * 
	 * @param input String input to check 
	 * 
	 * @return true if the input is valid (should be set). False otherwise
	 */
	public static boolean isValidGuiInput(String input) { 
		boolean isHexOrBinary = BinOctHexUtility.hasHexOrBinPrefix(input);
		boolean validBinary = isHexOrBinary && (BinOctHexUtility.isValidBin(input) && !BinOctHexUtility.stripBinaryPrefix(input).isEmpty());
		boolean validHex = !BinOctHexUtility.hasBinaryPrefix(input) && (isHexOrBinary && (BinOctHexUtility.isValidHex(input) && !BinOctHexUtility.stripHexPrefix(input).isEmpty()));
		
		return (!isHexOrBinary || (validBinary || validHex));
	}
	
	/**
	 * Matches the current combo text to its enum format. 
	 * This allows matching between lower/uppcase entries and selects the case 
	 * based on what the enum definition is
	 * 
	 * @param commandArg Command argument enum drop-down 
	 * 
	 * @return Non-empty string if current text matches an enum
	 */
	public static String getArgumentText(Combo commandArg) {
		// At this point the selected Combo is expected to contain some text.
		String currentValue = commandArg.getText();
		if (currentValue == null || currentValue.isEmpty()) {
			return "";
		}
		currentValue = currentValue.trim();

		for(String s: commandArg.getItems()) { 
			if (s.equalsIgnoreCase(currentValue)) { 
				return s;
			}
		}
		return currentValue;
	}
	
	/**
	 * Sets focus to allow user to keep typing by putting focus on Text field. 
	 * Also move cursor (selection) to what it was when event was triggered
	 * @param c Control item
	 * @return Point user caret
	 */
	public static Point getUserCaret(Control c) { 
		if (c == null) { 
			return new Point(0,0);
		} else if (c instanceof Combo) {
			return new Point(((Combo)c).getCaretPosition(),((Combo)c).getCaretPosition());
		} else if (c instanceof Text) { 
			return new Point(((Text)c).getCaretPosition(),((Text)c).getCaretPosition());
		} else return new Point(0,0);
	}
	
	/**
	 * Sets the caret on a Control item
	 * @param c Control item
	 * @param position Point to set
	 */
	public static void setOriginalCaret(Control c, Point position) { 
		if (c instanceof Combo) { 
			((Combo)c).forceFocus();
			((Combo)c).setSelection(position);
		} else if (c instanceof Text) { 
			((Text)c).forceFocus();
			((Text)c).setSelection(position);
		}
	}

}
