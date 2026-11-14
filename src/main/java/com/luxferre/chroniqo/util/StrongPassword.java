package com.luxferre.chroniqo.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation constraint that enforces the {@link PasswordValidator} policy.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@Documented
@Constraint(validatedBy = StrongPasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not satisfy the password policy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
