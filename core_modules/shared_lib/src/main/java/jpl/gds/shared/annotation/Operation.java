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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method that is directly callable by a command-line app that implements a RESTful interface.
 * Methods marked with this annotation will automatically be included in a list of valid, non-dashed sub-commands.
 * 
 *
 * @since R8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {
    /** Sub-Command Name */
    String subCmd();

    /** Sub-Command Description */
    String subCmdDesc();

    /** Option Names */
    String[] optNames() default {};

    /** Names of parameters */
    String[] parmNames() default {};

    /** Parameter Types */
    String[] parmTypes() default {};

    /** Descriptions of Parameters */
    String[] parmDesc() default {};

    /** Enum class if Enum option */
    String[] enumClassName() default {};

    /** A list of allowed enumerated types */
    String[] parmAllowedValues() default {};

    /** Flag to indicate whether returned value is JSON (true = JSON, false = not JSON) */
    boolean returnsJSON() default true;

    /** Flag to indicate whether JSON should be pretty-printed. (Not used if 'returnsJSON()' is false */
    boolean prettyJSON() default true;
}
