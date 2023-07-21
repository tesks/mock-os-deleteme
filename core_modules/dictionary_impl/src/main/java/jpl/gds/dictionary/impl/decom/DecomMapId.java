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
package jpl.gds.dictionary.impl.decom;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import jpl.gds.dictionary.api.decom.IDecomMapId;

/**
 * This class is a simple IDecomMapId implementation that consists
 * of a namespace of arbitrary amounts of colon-separated statements
 * and a local name with no colons in it.
 *
 */
public class DecomMapId implements IDecomMapId {
	private String namespace;
	private String localname;

	/**
	 * Create a new instance consisting of the given namespace and localname
	 * @param namespace the namespace portion of the decom map id
	 * @param localName the local name portion of the decom map id
	 */
	public DecomMapId(String namespace, String localName) {
		this.namespace = namespace;
		this.localname = localName;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getLocalName() {
		return localname;
	}

	@Override
	public String[] getNamespaceElements() {
		return namespace.split(":");
	}
	@Override
	public String getFullId() {
		return String.format("%s:%s", namespace, localname);
	}


	@Override
	public IDecomMapId resolveReference(String ref) {
		int index = ref.lastIndexOf(":");
		if (index == -1) {
			return new DecomMapId(namespace, ref);
		}
		return new DecomMapId(ref.substring(0, index), ref.substring(index + 1));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DecomMapId)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		DecomMapId other = (DecomMapId) obj;
		return new EqualsBuilder()
			.append(namespace, other.getNamespace())
			.append(localname, other.getLocalName())
			.isEquals();
		
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(namespace)
				.append(localname)
				.toHashCode();
	}
}