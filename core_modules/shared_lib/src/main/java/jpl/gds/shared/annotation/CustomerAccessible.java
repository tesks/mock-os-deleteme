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
 * Annotation that marks a an interface as accessible by customers.
 * These interfaces cannot be changed without proper change board
 * review. This annotation does not imply that the annotated interface
 * is a customer adaptation point. For that use @CustomerAdaptable. It
 * only means that the interface is visible to customers in some way.
 *
 *
 * @since R8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomerAccessible {
    /**
     * Defaults to false (assumes interfaces are mutable -- which is unfortunately the negative case
     */
    boolean immutable();
}
