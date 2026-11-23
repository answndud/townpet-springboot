package com.townpet.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated member id into a controller parameter of type {@link java.util.UUID}.
 *
 * <p>With {@code required = true} (the default) a request without an authenticated member is
 * rejected with 401. With {@code required = false} the parameter is {@code null} for anonymous
 * viewers; a malformed principal is still rejected with 401.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
  boolean required() default true;
}
