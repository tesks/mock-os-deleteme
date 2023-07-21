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
package jpl.gds.product.impl.dictionary;

import jpl.gds.product.api.dictionary.IProductDefinitionKey;

/**
 * ReferenceProductDefinitionKey is the general class defining a key
 * for a product definition. It consists of the
 * product APID and the definition version.
 *
 *
 */
public class ReferenceProductDefinitionKey implements IProductDefinitionKey {

    private final int apid;
    private final int version;
    
    /**
     * Creates an instance of ReferenceProductDefinitionKey.
     * @param apid the product definition APID
     * @param version the product definition version
     */
    public ReferenceProductDefinitionKey(int apid, int version) {
        this.apid = apid;
        this.version = version;     
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets the APID from this key.
     * @return the APID
     */
    public int getApid() {
		return apid;
	}

    /**
     * Gets the version from this key.
     * @return the version number
     */
	public int getVersion() {
		return version;
	}

	/**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (!(that instanceof ReferenceProductDefinitionKey)) {
            return false;
        }
        return that.hashCode() == this.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return apid + ":v" + version;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinitionKey#toOutputString()
     */
    @Override
	public String toOutputString() {
        return "APID=" + apid + " XML Version=" + version;
    }
}
