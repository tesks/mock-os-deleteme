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
package jpl.gds.globallad.data.container;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

import jpl.gds.globallad.data.GlobalLadContainerException;

/**
 * Public interface used to serialize and deserialize containers.  The serialization will be done using Jackson to create
 * JSON strings to represent container and all of its children. 
 * <p>
 * NOTE:  This method of serialization of an entire global lad container is costly and perhaps should not be used.  The implementation
 * for this interface currently works, but on large data sets can take hours to complete.  Also when using this method to create 
 * backups of the global lad at a given time, it will recreate the global lad exactly how it was when the snapshot was taken.  However, it
 * will not use any updated global lad options, so it is possible that the entire tree structure has changed and this method can not 
 * handle that case.  
 * <p>
 * This interface is being marked deprecated since it does not handle the case stated above, and is really no longer
 * necessary.  All marshalling is done at the data level and not at the container level anyhow.  Future releases should probably remove
 * this all together if it is shown that it is truly unnecessary.
 */
@Deprecated
public interface IGlobalLadSerializable {
	
	/**
	 * Serializes the object using the given generator and the list of identifiers.  
	 * 
	 * @param generator
	 * @param identifiers - A list of identifiers to be used to construct the path in a map to reach this.
	 * 
	 * @throws IOException
	 */
	public void dehydrate(JsonGenerator generator, Object... identifiers) throws IOException;

	/**
	 * Recreates the map
	 * 
	 * @param container
	 * @param childContainerTypes
	 * @param childContainerIdentifiers
	 * @throws GlobalLadContainerException
	 */
	public void rehydrate(IGlobalLadContainer container, List<String> childContainerTypes, List<Object> childContainerIdentifiers) throws GlobalLadContainerException;
	
}