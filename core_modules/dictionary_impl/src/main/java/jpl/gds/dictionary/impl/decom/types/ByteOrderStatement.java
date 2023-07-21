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

import java.nio.ByteOrder;

import jpl.gds.dictionary.api.decom.types.IByteOrderStatement;

/**
 * A class that implements a decom statement for byte order.
 *
 */
public class ByteOrderStatement implements IByteOrderStatement {


	private final ByteOrder order;

	/**
	 * Constructor
	 * @param order ByteOrder for this statement
	 */
	public ByteOrderStatement(ByteOrder order) {
		this.order = order;
	}

	@Override
	public ByteOrder getByteOrder() {
		return order;
	}

}
