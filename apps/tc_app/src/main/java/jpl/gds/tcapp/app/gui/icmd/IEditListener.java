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
package jpl.gds.tcapp.app.gui.icmd;

/**
 * Interface that provides methods to call when an edit event occurs
 *
 * @since AMPCS R3
 */
public interface IEditListener {
	/**
	 * Called when an edit event occurs
	 * @param event the edit event
	 */
	public void onEdit(EditEvent event);
	
	/**
	 * A class representing an edit event
	 *
	 */
	public class EditEvent {
		/** The new value after edit */
		private String newValue;
		
		/**
		 * Constructor
		 * @param newValue the new value after edit
		 */
		public EditEvent(String newValue) {
			this.newValue = newValue;
		}
		
		/**
		 * Get the new value after edit
		 * @return the new value after edit
		 */
		public String getNewValue() {
			return this.newValue;
		}
	}
}
