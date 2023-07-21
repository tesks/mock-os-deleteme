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
package jpl.gds.dictionary.api.apid;

/**
 * This enumeration defines the valid classifications of secondary header
 * types that may occur within packets of a specific APID. <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * SecondaryHeaderType is an enumeration that defines all the valid classifications
 * for the secondary headers contained within packets labeled with an APID. As such, the
 * SecondaryHeaderType must be set on all IApidDefinition objects created by an
 * ApidDictionary implementation.  If arriving packets for a given APID do not have a secondary
 * header,  then the secondary header type will be ignored.
 *
 * @see IApidDefinition
 */
public enum SecondaryHeaderType {
	
	/**
	 * Secondary header contains only a timestamp.
	 */
	TIME,
	
	/** Secondary header contains fields other than or in addition to a timestamp,
	 * necessitating custom handling.
	 */
	CUSTOM 
}
