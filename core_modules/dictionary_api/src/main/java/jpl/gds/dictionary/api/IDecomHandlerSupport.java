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
package jpl.gds.dictionary.api;


/**
 * The IDecomHandlerSupport interface is implemented by dictionary definition
 * that classes that support attachment of DecomHandlers. Attachment of a
 * DecomHandler to a dictionary object implies there is an associated binary
 * object to be decommutated at runtime. <p>
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * 
 * @see DecomHandler
 * 
 *
 */
public interface IDecomHandlerSupport {

    /**
     * Indicates whether this object has an internal (Java class) handler to be
     * executed by the application that is decommutating the object.
     * 
     * @return true if there is an internal handler
     */
    public boolean hasInternalHandler();

    /**
     * Indicates whether this object has an external (executable command line)
     * handler to be spawned by the application that is decommutating the
     * object.
     * 
     * @return true if there is an external handler
     */
    public boolean hasExternalHandler();

    /**
     * Gets the internal handler. An internal handler is
     * a handler with type DecomHandler.DecomHandlerType.INTERNAL_JAVA_CLASS.
     * 
     * @return the DecomHandler, or null if none defined
     */
    public DecomHandler getInternalHandler();

    /**
     * Gets the external handler. An external handler is
     * a handler with type DecomHandler.DecomHandlerType.EXTERNAL_PROGRAM.
     * 
     * @return the DecomHandler, or null if none defined
     */
    public DecomHandler getExternalHandler();

    /**
     * Sets the internal handler. An internal handler is
     * a handler with type DecomHandler.DecomHandlerType.INTERNAL_JAVA_CLASS.
     * 
     * @param handler the DecomHandler to set
     */
    public void setInternalHandler(DecomHandler handler);

    /**
     * Sets the external handler. An external handler is
     * a handler with type DecomHandler.DecomHandlerType.EXTERNAL_PROGRAM.
     * 
     * @param handler the DecomHandler to set
     */
    public void setExternalHandler(DecomHandler handler);
}
