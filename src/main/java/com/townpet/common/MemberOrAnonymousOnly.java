package com.townpet.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/** Allows public guest flows and regular members, but never staff-only accounts. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAnonymous() or hasRole('MEMBER')")
public @interface MemberOrAnonymousOnly {}
