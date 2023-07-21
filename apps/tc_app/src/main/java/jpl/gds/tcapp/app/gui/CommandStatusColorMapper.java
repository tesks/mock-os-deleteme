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
package jpl.gds.tcapp.app.gui;

import java.util.EnumMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import jpl.gds.tc.api.CommandStatusType;

/**
 * Centralized color mapping class that stores an enumeration map which maps iCmd 
 * status types to a specified RGB value. This will be used in the transmit 
 * history composite and the request list viewer.
 * 
 *
 */
public class CommandStatusColorMapper {
	
	private final EnumMap<CommandStatusType, Color> commandColors = 
		new EnumMap<CommandStatusType, Color>(CommandStatusType.class);
	
	private final Color blue = new Color(Display.getCurrent(), 0, 255, 255);
	private final Color lightGreen = new Color(
			Display.getCurrent(), 144, 238, 144);
	private final Color green = new Color(Display.getCurrent(), 0, 255, 0);
	private final Color orange = new Color(Display.getCurrent(), 255, 165, 0);
	private final Color brown = new Color(Display.getCurrent(), 210, 180, 140);
	private final Color honeydew = new Color(
			Display.getCurrent(), 204, 255, 102);
	private final Color red = new Color(Display.getCurrent(), 255, 0, 0);
	private final Color orchid = new Color(
			Display.getCurrent(), 224, 102, 255);
	private final Color gray = new Color(Display.getCurrent(), 190, 190, 190);
	
	/**
	 * Constructor: Maps the command status types to the designated color and 
	 * stores it in an enumeration map
	 */
	public CommandStatusColorMapper() {
		//blue
		commandColors.put(CommandStatusType.Requested, blue);
		commandColors.put(CommandStatusType.Submitted, blue);
		
		//light green
		commandColors.put(CommandStatusType.Radiating, lightGreen);
		
		//green
		commandColors.put(CommandStatusType.Radiated, green);
		commandColors.put(CommandStatusType.Received, green);
		
		//orange
		commandColors.put(CommandStatusType.Failed, orange);
		
		//brown
		commandColors.put(CommandStatusType.Deleted, brown);

		//honeydew
		commandColors.put(CommandStatusType.Awaiting_Confirmation, honeydew);

		//red
		commandColors.put(CommandStatusType.Corrupted, red);
		commandColors.put(CommandStatusType.Windows_Expired, red);
		commandColors.put(CommandStatusType.Rad_Attempts_Exceeded, red);
		commandColors.put(CommandStatusType.Send_Failure, red);
		
		//purple
		commandColors.put(CommandStatusType.UNKNOWN, orchid);
		
		//gray
		commandColors.put(CommandStatusType.Standby, gray);
		
		//white (default)
		//CommandStatusType.Ready
	}
	
	/**
	 * Gets the color enumeration map
	 * 
	 * @return enumeration that maps the status type to its respective SWT 
	 * color object
	 */
	public EnumMap<CommandStatusType, Color> getColorMap() {
		return commandColors;
	}
	
	/**
	 * Gets a color from the map based on the given status type
	 * 
	 * @param type the command status type
	 * @return SWT color object for the specified status type
	 */
	public Color getColorForStatus(final CommandStatusType type) {
		return commandColors.get(type);
	}
}
