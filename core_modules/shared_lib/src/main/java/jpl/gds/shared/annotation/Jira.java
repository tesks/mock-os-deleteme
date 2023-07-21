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
package jpl.gds.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation that marks that code has been modified under a Jira.
 * Structured comments give the detailed information.
 *
 * To use:
 *
 * import jpl.gds.shared.annotation.Jira;
 *
 *
 */
@Target({ElementType.CONSTRUCTOR,
         ElementType.METHOD,
         ElementType.TYPE,
         ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
public @interface Jira
{
    /** Type of Jira */
    public static enum Type
    {
        /** Bug fix */
        BUG,

        /** Change request implementation */
        CHANGE_REQUEST
    }


    /** Say what was done. */
    String change();


    /** Jira under which the code was added */
    String jira();


    /** Author of the addition */
    String author();


    /** Explanation for the addition */
    String explanation() default "";


    /** Type of change */
    Type type() default Type.BUG;
}
