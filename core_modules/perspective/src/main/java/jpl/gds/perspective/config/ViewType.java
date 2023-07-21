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
package jpl.gds.perspective.config;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.EnumeratedType;

/**
 * An enumeration of view types in a user perspective.
 */
public class ViewType extends EnumeratedType {
	private static final Tracer trace = TraceManager.getDefaultTracer();


	// --------------------------------------------------
	// Add new types here
	//  static integer values for the enumerated values
	//  must map directly to types
	/**
	 * Unknown view integer mapping
	 */
	static public final int UNKNOWN_VIEW = 0;
	
	/**
	 * Channel list view integer representation
	 */
	static public final int CHANNEL_LIST_VIEW = 1;
	
	/**
	 * Message view integer representation
	 */
	static public final int MESSAGE_VIEW = 2;
	
	/**
     * Message status view integer representation
     */
	static public final int MESSAGE_STATS_VIEW = 3;
    
    /**
     * Channel plot view integer representation
     */
	static public final int CHANNEL_CHART_VIEW = 4;
    
    /**
     * Event record message view integer representation
     */
	static public final int EVR_MESSAGE_VIEW = 5;
    
    /**
     * Product status view integer representation
     */
	static public final int PRODUCT_STATUS_VIEW = 6;
    
    /**
     * Status Message view integer representation
     */
	static public final int STATUS_MESSAGE_VIEW = 7;
    
    /**
     * Command view integer representation
     */
	static public final int COMMAND_MESSAGE_VIEW = 8;
    
    /**
     * Down message view integer representation
     */
	static public final int DOWN_MESSAGE_VIEW = 9;
    
    /**
     * Message tab view integer representation
     */
	static public final int MESSAGE_TAB_VIEW = 10;
    
    /**
     * Custom grid view integer representation
     */
	static public final int CUSTOM_GRID_VIEW = 11;
    
    /**
     * Packet watch view integer representation
     */
	static public final int PACKET_WATCH_VIEW = 12;
    
    /**
     * Frame watch view integer representation
     */
	static public final int FRAME_WATCH_VIEW = 13;
    
    /**
     * Encoding watch view integer representation
     */
	static public final int ENCODING_WATCH_VIEW = 14;
    
    /**
     * Frame accountability view integer representation
     */
	static public final int FRAME_ACCOUNTABILITY_VIEW = 15;
    
    /**
     * Fast alarm view integer representation
     */
	static public final int FAST_ALARM_VIEW = 16;
    
    /**
     * Fixed layout view integer representation
     */
	static public final int FIXED_LAYOUT_VIEW = 17;
    
    /**
     * view integer representation
     */
	public static final int SINGLE_WINDOW_VIEW = 18;
    
    /**
     * Predefined view integer representation
     */
	public static final int PREDEFINED_VIEW = 19;

	//	static string values for the enumerated values

	/**
	 * Array of all the view type names
	 */
	@SuppressWarnings({"MALICIOUS_CODE","MS_PKGPROTECT"}) 
	static public final String[] viewTypes = { 
		"Unknown",
		"Channel List", 
		"Message List", 
		"Message Statistics",
		"Channel Plot", 
		"EVR",
		"Product Status", 
		"Status Messages",
		"Command",
		"Downlink Messages",
		"Message Tab",
		"Custom Grid",
		"Packet Watch",
		"Frame Watch",
		"Encoding Watch",
		"Frame Accountability",
		"Fast Alarm",
		"Fixed Layout",
		"Single View Window",
		"Pre-Defined"
	};

	// static convenience instances
	/**
	 * Unknown view convenience instance
	 */
	public static final ViewType UNKNOWN = new ViewType(UNKNOWN_VIEW);
	
	/**
     * Channel list view convenience instance
     */
	public static final ViewType CHANNEL_LIST = new ViewType(CHANNEL_LIST_VIEW);

    
    /**
     * Message view convenience instance
     */
	public static final ViewType MESSAGE = new ViewType(MESSAGE_VIEW);
    
    /**
     * Statistics view convenience instance
     */
	public static final ViewType STATISTICS = new ViewType(MESSAGE_STATS_VIEW);
    
    /**
     * Channel plot view convenience instance
     */
	public static final ViewType CHART = new ViewType(CHANNEL_CHART_VIEW);
    
    /**
     * Event record view convenience instance
     */
	public static final ViewType EVR = new ViewType(EVR_MESSAGE_VIEW);
    
    /**
     * Product status view convenience instance
     */
	public static final ViewType PRODUCT = new ViewType(PRODUCT_STATUS_VIEW);
    
    /**
     * Status view convenience instance
     */
	public static final ViewType STATUS = new ViewType(STATUS_MESSAGE_VIEW);
    
    /**
     * Command view convenience instance
     */
	public static final ViewType COMMAND = new ViewType(COMMAND_MESSAGE_VIEW);
    
    /**
     * Down view convenience instance
     */
	public static final ViewType DOWN_MESSAGE = new ViewType(DOWN_MESSAGE_VIEW);
    
    /**
     * Message tab view convenience instance
     */
	public static final ViewType MESSAGE_TAB = new ViewType(MESSAGE_TAB_VIEW);
    
    /**
     * Custom grid view convenience instance
     */
	public static final ViewType CUSTOM_GRID = new ViewType(CUSTOM_GRID_VIEW);
    
    /**
     * Packet watch view convenience instance
     */
	public static final ViewType PACKET_WATCH = new ViewType(PACKET_WATCH_VIEW);
    
    /**
     * Frame watch view convenience instance
     */
	public static final ViewType FRAME_WATCH = new ViewType(FRAME_WATCH_VIEW);
    
    /**
     * Encoding watch view convenience instance
     */
	public static final ViewType ENCODING_WATCH = new ViewType(ENCODING_WATCH_VIEW);
    
    /**
     * Frame accountability view convenience instance
     */
	public static final ViewType FRAME_ACCOUNTABILITY = new ViewType(FRAME_ACCOUNTABILITY_VIEW);
    
    /**
     * Fast alarm view convenience instance
     */
	public static final ViewType FAST_ALARM = new ViewType(FAST_ALARM_VIEW);
    
    /**
     * Fixed layout view convenience instance
     */
	public static final ViewType FIXED_LAYOUT = new ViewType(FIXED_LAYOUT_VIEW);
    
    /**
     * Single window view convenience instance
     */
	public static final ViewType SINGLE_VIEW_WINDOW = new ViewType(SINGLE_WINDOW_VIEW);
    
    /**
     * Predefined view convenience instance
     */
	public static final ViewType PREDEFINED = new ViewType(PREDEFINED_VIEW);

	// End of add new types
	// -------------------------------------------------
	
	/**
	 * Creates an instance of ViewType.
	 * @param intVal the integer value constant from this class
	 * @throws IllegalArgumentException if the supplied argument does 
	 * not map to a known display type 
	 */
	public ViewType(int intVal) throws IllegalArgumentException {
		super(intVal);
	}

	/**
	 * Creates an instance of ViewType.
	 * @param stringVal the string value constant from this class
	 * @throws IllegalArgumentException if the supplied argument does 
	 * not map to a known display type 
	 */
	public ViewType(String stringVal) throws IllegalArgumentException {
		super(stringVal);
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(int index) {
		if (index < 0 || index > getMaxIndex()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return (viewTypes[index]);
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex() {
		return (viewTypes.length - 1);
	}

	/**
	 * Generates an XML representation of the ViewType
	 * @return the XML string
	 */
	public String toXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<viewType> name=" + this.getValueAsString()
				+ " </viewType>");
		return buf.toString();
	}


	/**
	 * Determines if view is a standalone or tabbed window
	 * 
	 * @return true if this view type represents a standalone window view
	 */
	public boolean isStandaloneWindow() {
		return this.getValueAsInt() == MESSAGE_TAB_VIEW ||
		this.getValueAsInt() == SINGLE_WINDOW_VIEW;
	}

	/**
	 * Gets an array of all valid values for this enumeration.
	 * @return array of ViewTypes
	 */
	public static ViewType[] getAllViewTypes() {
		ViewType[] types = new ViewType[viewTypes.length];
		int i = 0;
		for (String typeStr : viewTypes) {
			types[i++] = new ViewType(typeStr);
		}

		return types;
	}
}
