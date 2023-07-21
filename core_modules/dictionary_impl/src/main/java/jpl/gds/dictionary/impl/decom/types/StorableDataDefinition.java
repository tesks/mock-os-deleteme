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

import jpl.gds.dictionary.api.decom.params.StorableDataParams;
import jpl.gds.dictionary.api.decom.types.IStorableDataDefinition;

/**
 * Base class for decom data that can be stored as a variable for later reference.
 *
 */
public abstract class StorableDataDefinition extends ChannelizableDataDefinition implements IStorableDataDefinition {
	
	private final boolean shouldStore;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public StorableDataDefinition(StorableDataParams params) {
		super(params);
		this.shouldStore = params.shouldStore();
	}

	@Override
	public boolean shouldStore() {
		return this.shouldStore;
	}

	@Override
	public String toString() {
		return super.toString() + ", is_variable=" + shouldStore;
	}
}
