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
package jpl.gds.dictionary.api.decom.params;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.IStringDefinition;
import jpl.gds.dictionary.api.decom.types.IStringDefinition.StringEncoding;

/**
 * Parameter builder class for creating {@link IStringDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class StringParams extends StorableDataParams {


	
	private StringEncoding encoding;
	
	private boolean isNullTerminated;
	
	private int length;

	/**
	 * @see IStringDefinition#getEncoding()
	 * @return encoding the encoding to be used to interpret data as a string
	 */
	public StringEncoding getEncoding() {
		return encoding;
	}

	/**
	 * Set the encoding of the string
	 * @param encoding the encoding to be used to interpret data as a string
	 */
	public void setEncoding(StringEncoding encoding) {
		this.encoding = encoding;
	}

	/**
	 *	@see IStringDefinition#isNullTerminated() 
	 *	@return true if the string is null terminated
	 */
	public boolean isNullTerminated() {
		return isNullTerminated;
	}


	/**
	 * Set whether or not the string is null terminated
	 * @param isNullTerminated true if the string is to be null terminated
	 */
	public void setNullTerminated(boolean isNullTerminated) {
		this.isNullTerminated = isNullTerminated;
	}

	/**
	 * @see IStringDefinition#getLength()
	 * @return the length of the string
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Set the length of the string
	 * @param length integer length. Units determined by string encoding.
	 */
	public void setLength(int length) {
		this.length = length;
	}
	
	
}
