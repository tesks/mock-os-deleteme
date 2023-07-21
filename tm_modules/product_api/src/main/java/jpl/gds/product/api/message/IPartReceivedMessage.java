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
package jpl.gds.product.api.message;

import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.shared.message.IMessage;

/**
 * An interface to be implemented by product part received messages.
 * 
 *
 * @since R8
 */
public interface IPartReceivedMessage extends IMessage {

	/**
	 * MTAK Field count for tests
	 */
	public final int MTAK_FIELD_COUNT = 18;

	/**
	 * Retrieves the part associated with this message.
	 * 
	 * @return the IProductPartProvider
	 */
	public IProductPartProvider getPart();
}