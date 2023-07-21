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
 * The DecomHandler class is used to define a handler to be executed when a
 * particular object is decommutated. This object is used by dictionaries
 * that can attach decom handlers to specific dictionary objects, such as 
 * telemetry packets or data products.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 *
 */
public class DecomHandler {

	/**
	 * Enumeration that defines type of decom handlers.
	 *
	 */
    public enum DecomHandlerType {
    	/** Handler is a java class */
        INTERNAL_JAVA_CLASS, 
        /** Handler is a script or application */
        EXTERNAL_PROGRAM
    };

    private DecomHandlerType type;
    private boolean wait;
    private String handlerName;

    /**
     * Retrieves the handler type.
     * 
     * @return the type
     */
    public DecomHandlerType getType() {
        return this.type;
    }

    /**
     * Sets the handler type.
     * 
     * @param type the type to set
     */
    public void setType(DecomHandlerType type) {
        this.type = type;
    }

    /**
     * Retrieves the wait flag. This indicates whether the
     * application invoking the handler should wait for its
     * completion or may continue without waiting.
     * 
     * @return true if the invoker should wait; false if not
     */
    public boolean isWait() {
        return this.wait;
    }

    /**
     * Sets the wait flag. This indicates whether the
     * application invoking the handler should wait for its
     * completion or may continue without waiting.
     * 
     * @param wait true if the invoker should wait; false if not
     */
    public void setWait(boolean wait) {
        this.wait = wait;
    }

    /**
     * Retrieves the handler name. For external handlers, this
     * is the path to a script.  For internal handlers, it is a java
     * class name.
     * 
     * @return the handlerName
     */
    public String getHandlerName() {
        return this.handlerName;
    }

    /**
     * Sets the handler name. For external handlers, this
     * is the path to a script.  For internal handlers, it is a java
     * class name.
     * 
     * @param handlerName the name to set
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    /**
     * @{inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type.toString() + ": " + handlerName + ", wait=" + wait;
    }
}
