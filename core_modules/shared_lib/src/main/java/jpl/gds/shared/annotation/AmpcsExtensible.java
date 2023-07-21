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
 * Annotation that marks an interface as an AMPCS programmatic extension point,
 * meaning that AMPCS-developed mission adaptations may provide another
 * implementation of the interface that overrides the multimission
 * implementation. This extension capability is AMPCS-internal and is not
 * intended for use by customers. The CustomerExtenable interface should be
 * used for that purpose.
 * 
 *
 * @since R8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AmpcsExtensible
{
    /**
     * Defaults to false (assumes interfaces are mutable -- which is unfortunately the negative case
     */
    boolean immutable();
}
