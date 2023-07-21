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
 * Annotation that marks an interface or class as implementable/extendable by
 * customer-supplied Java classes. These interfaces cannot be changed without
 * proper change board review. For interfaces that are visible to customers, but
 * not intended to be implemented by customer classes, use @CustomerAccessible.
 * 
 *
 * @since R8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomerExtensible {
    /**
     * Defaults to false (assumes interfaces are mutable -- which is unfortunately the negative case
     */
    boolean immutable();
}
