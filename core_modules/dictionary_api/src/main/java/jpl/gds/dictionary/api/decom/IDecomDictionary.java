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

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IDecomDictionary represents a dictionary consisting of many
 * constituent decom maps.  Maps can be loaded lazily or eagerly
 * by the underlying implementation class.
 * 
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 * An IDecomDictionary can be used to interpret raw binary data across various types
 * of telemetry.  The IDecomDictionary is used to look up individual generic decom
 * maps by an map ID known to correspond a specific chunk of data.
 * 
 *
 */
@CustomerAccessible(immutable = false)
public interface IDecomDictionary extends IBaseDictionary {

	/**
	 * Fetch a decom map by the given ID.
	 * @param id the decom map ID uniquely identifying the desired map to return
	 * @return the IDecomMapDefinition identified by the ID, or null if none is found.
	 */
	public IDecomMapDefinition getDecomMapById(IDecomMapId id);
	
}
