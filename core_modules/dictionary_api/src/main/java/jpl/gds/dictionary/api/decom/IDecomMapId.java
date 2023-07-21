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
package jpl.gds.dictionary.api.decom;

/**
 * This interface represents a ID that uniquely 
 * identifies a decom map within an IDecomDictionary
 * instance. IDs consist of a namespace string and a local name.
 * The local name of a decom map must uniquely identify a decom map
 * in within a namespace.
 * 
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 *
 */
public interface IDecomMapId {

	/**
	 * Get the namespace this key is associated with.
	 * More than one key can be associated with each
	 * decom map namespace.
	 * @return the namespace portion of the ID
	 */
	public String getNamespace();
	
	/**
	 * Get the local / simple name for the key.
	 * This uniquely identifies a map within a
	 * namespace.
	 * @return the local name portion of the ID
	 */
	public String getLocalName();
	
	/**
	 * Get the full string representation of the ID,
	 * which includes the namespace concatenated with
	 * the local name.
	 * @return the full decom map ID string
	 */
	public String getFullId();
	
	/**
	 * Resolve a reference to another decom map.  Some
	 * reference strings may lack the full context needed 
	 * to construct a unique decom map ID, e.g., one without
	 * a namespace. In such a case, the IDecomMapId receiver
	 * instance will fill in necessary context from itself.
	 * @param ref the full or partial string referring to some
	 * 		  decom map
	 * @return a new IDecomMapId
	 */
	public IDecomMapId resolveReference(String ref);

	/**
	 * Get an array of separate elements of the namespace.
	 * Each namespace can break down into smaller constituent parts,
	 * which may be needed to assemble a decom map file path from an
	 * ID or some other operation.
	 * @return the array of namespace elements
	 */
	String[] getNamespaceElements();
}
