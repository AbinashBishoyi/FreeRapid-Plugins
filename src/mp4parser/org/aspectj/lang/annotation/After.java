/*******************************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * initial implementation              Alexandre Vasseur
 *******************************************************************************/
package org.aspectj.lang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * After finally advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface After {

    /**
     * The pointcut expression where to bind the advice
     */
    String value();

    /**
     * When compiling without debug info, or when interpreting pointcuts at runtime,
     * the names of any arguments used in the advice declaration are not available.
     * Under these circumstances only, it is necessary to provide the arg names in
     * the annotation - these MUST duplicate the names used in the annotated method.
     * Format is a simple comma-separated list.
     */
    String argNames() default "";
}
