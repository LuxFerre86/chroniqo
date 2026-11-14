package com.luxferre.chroniqo.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator backing {@link StrongPassword}. Reports the first violated
 * password rule as the constraint violation message.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
public class StrongPasswordConstraintValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        var violation = PasswordValidator.validate(value);
        if (violation.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(violation.get()).addConstraintViolation();
        return false;
    }
}
