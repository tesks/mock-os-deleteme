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
package jpl.gds.dictionary.api.decom.types;

import jpl.gds.dictionary.api.decom.IStatementContainer;

/**
 * Represents a case containing decom statements to invoke if a certain
 * switch condition is met.
 * 
 *
 */
public interface ICaseBlockDefinition extends IStatementContainer {

	/**
	 * Get the value that should be matched for this case's statements to be executed.
	 * The value should only be used after verifying {#link {@link #isDefault()} returns false.
	 * @return the long value associated with this case
	 */
	public long getValue();
	
	/**
	 * Indicates whether this case is a default case, meaning that its statements
	 * should be executed when no other cases belonging to the same switch are selected.
	 * @return true if this is a default case, false if not
	 */
	public boolean isDefault();

}
