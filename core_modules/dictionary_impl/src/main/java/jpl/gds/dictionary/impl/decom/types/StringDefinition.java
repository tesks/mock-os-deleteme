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
package jpl.gds.dictionary.impl.decom.types;

import jpl.gds.dictionary.api.decom.params.StringParams;
import jpl.gds.dictionary.api.decom.types.IStringDefinition;

/**
 * Implementation class for string definitions occurring in decom maps.
 *
 */
public class StringDefinition extends StorableDataDefinition implements IStringDefinition {
	
	private final StringEncoding encoding;
	private final boolean isNullTerminated;
	private final int length;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public StringDefinition(StringParams params) {
		super(params);
		encoding = params.getEncoding();
		isNullTerminated = params.isNullTerminated();
		length = params.getLength();
		if (format == null) {
			format = "%s";
		}
	}

	@Override
	public StringEncoding getEncoding() {
		return encoding;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public boolean isNullTerminated() {
		return isNullTerminated;
	}

}
