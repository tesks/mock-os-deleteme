package jpl.gds.tcapp.app.gui.fault.util;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.tc.api.command.IFlightCommand;

/**
 * This is the base abstract command argument GUI control class for all arguments. This class
 * primarily functions to hold the IFlightCommand and associated indecies to the ICommandArgument,
 * to get the current value, and some of the base metadata.
 *
 */
public abstract class AbstractCommandArgumentGuiControl implements ICommandArgumentGuiControl {

	IFlightCommand cmd;
	int argIndex = -1;
	int argSubIndex = -1;
	
	@Override
	public void setArgument(final IFlightCommand cmd, final int argIndex) {
		this.cmd = cmd;
		this.argIndex = argIndex;

	}
	
	@Override
	public void setArgument(final IFlightCommand cmd, final int argIndex, final int argSubIndex) {
		this.cmd = cmd;
		this.argIndex = argIndex;
		this.argSubIndex = argSubIndex;
	}
	
	//helper function for use in the subclasses to get the definition
	ICommandArgumentDefinition getDefinition() {
		if(argSubIndex < 0) {
			return this.cmd.getArgumentDefinition(argIndex);
		}
		
		return this.cmd.getArgumentDefinition(argIndex, argSubIndex);
	}
	
	//helper function for use in the subclasses to get the current value
	String getArgumentValue() {
		if(argSubIndex < 0) {
			return this.cmd.getArgumentValue(argIndex);
		}
		
		return this.cmd.getArgumentValue(argIndex, argSubIndex);
	}
	
	//helper function for use in the subclasses to get the display name
	String getDisplayName() {
		if(argSubIndex < 0) {
			return this.cmd.getArgumentDisplayName(argIndex);
		}
		
		return this.cmd.getArgumentDisplayName(argIndex, argSubIndex);
	}

}
