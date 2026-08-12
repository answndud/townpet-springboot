package com.townpet.catalog.api;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Locale;

/** Validates optional content tags against the public animal community catalog. */
@Documented
@Constraint(validatedBy = ValidAnimalCommunityCodes.Validator.class)
@Target({
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.PARAMETER,
  ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAnimalCommunityCodes {
  String message() default "contains an invalid animal community code";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  final class Validator
      implements ConstraintValidator<ValidAnimalCommunityCodes, Collection<String>> {
    @Override
    public boolean isValid(
        Collection<String> codes, ConstraintValidatorContext constraintValidatorContext) {
      if (codes == null) return true;
      return codes.stream()
          .allMatch(
              code ->
                  code != null
                      && !code.isBlank()
                      && AnimalInterestCatalog.codes()
                          .contains(code.trim().toUpperCase(Locale.ROOT)));
    }
  }
}
