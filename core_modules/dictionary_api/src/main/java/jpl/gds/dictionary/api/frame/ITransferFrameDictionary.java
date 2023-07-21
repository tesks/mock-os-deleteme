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
package jpl.gds.dictionary.api.frame;

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The ITransferFrameDictionary interface is to be implemented by all Transfer
 * Frame Dictionary adaptation classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * The Transfer Frame dictionary is used by the telemetry processing system in
 * order to identify the types and format of telemetry frames in use by a
 * mission. It defines the Control and Display Unit (CADU) for each frame by
 * supplying its length, encoding type, ASM, and other information needed to
 * process the frame. Each frame definition in the dictionary has a unique type
 * name. An appropriate dictionary parser must be used in order to create the
 * mission-specific ITransferFrameDictionary object, which MUST implement this
 * interface. ITransferFrameDictionary objects should only be created via the
 * TransferFrameDictionaryFactory. Direct creation of an
 * ITransferFrameDictionary object is a violation of multi-mission development
 * standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * 
 *
 * @see ITransferFrameDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 */
@CustomerAccessible(immutable = true)
public interface ITransferFrameDictionary extends IBaseDictionary, ITransferFrameDefinitionProvider {


    @Override
    default public boolean isLoaded() {
        return true;
    }

}