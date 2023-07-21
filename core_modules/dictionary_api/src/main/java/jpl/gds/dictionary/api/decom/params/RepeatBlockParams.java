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
import jpl.gds.dictionary.api.decom.types.IRepeatBlockDefinition;
import jpl.gds.dictionary.api.decom.types.IRepeatBlockDefinition.LengthType;


/**
 * Parameter builder class for creating {@link IRepeatBlockDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class RepeatBlockParams extends StatementContainerParams {
	
	private int bitLength = 0;
	private LengthType lengthType = LengthType.ABSENT;

	/**
	 * Set the maximum length of the repeat block.  Also
	 * sets the length type.
	 * @param length the maximum length, in bits, of repeat block data
	 */
	public void setMaxLength(int length) {
		bitLength = length;
		lengthType = LengthType.MAX;
	}
	
	/**
	 * Sets the absolute length of the repeat bock.  Also sets the 
	 * length type.
	 * @param length the absolute length, in bits, of repeat block data.
	 */
	public void setAbsoluteLength(int length) {
		bitLength = length;
		lengthType = LengthType.ABSOLUTE;
	}

	/**
	 * @see IRepeatBlockDefinition#getLength()
	 * @return the length, in bits, defined for the repeat block (may be maximum or aboslute)
	 */
	public int getBitLength() {
		return bitLength;
	}

	/**
	 * @see IRepeatBlockDefinition#getLengthType()
	 * @return the length type defined for the repeat block
	 */
	public LengthType getLengthType() {
		return lengthType;
	}

	@Override
	public void reset() {
		super.reset();
		bitLength = 0;
		lengthType = LengthType.ABSENT;
	}
	
}
