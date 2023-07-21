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
package jpl.gds.eha.api.channel;

/**
 * A checked exception thrown when a derivation class cannot be loaded.
 * 
 */
public class BadDerivationClassException extends Exception {
      
    private static final long serialVersionUID = 1L;
    
    private String derivationClass;

    /**
     * Constructor.
     * 
     * @param clazz Name of class that could not be loaded
     */
    public BadDerivationClassException(String clazz) {
         super("Derivation class count not be loaded: " + clazz);
         this.derivationClass = clazz;
     }
     
    /**
     * Constructor.
     * 
     * @param clazz Name of class that could not be loaded
     * @param cause root exception
     */
     public BadDerivationClassException(String clazz, Throwable cause) {
         super("Derivation class count not be loaded: " + clazz, cause);
         this.derivationClass = clazz;
     }
     
     /**
      * Gets the name of the derivation class that caused the problem.
      * 
      * @return class name
      */
     public String getDerivationClass() {
         return this.derivationClass;
     }
}
