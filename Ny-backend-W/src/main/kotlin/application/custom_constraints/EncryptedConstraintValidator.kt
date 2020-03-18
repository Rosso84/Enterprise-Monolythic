package application.custom_constraints

import org.passay.*
import java.util.*
import java.util.stream.Collectors
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext


class EncryptedConstraintValidator : ConstraintValidator<Encrypted, String> {

    override fun initialize(constraintAnnotation: Encrypted?) {}

    override fun isValid(input: String, context: ConstraintValidatorContext): Boolean {
        val validator = PasswordValidator(Arrays.asList(
                // longer than 30 chars
                LengthRule(30, Integer::MAX_VALUE.get()),

                // at least one upper-case character
                CharacterRule(EnglishCharacterData.UpperCase, 1),

                // at least one lower-case character
                CharacterRule(EnglishCharacterData.LowerCase, 1),

                // at least one digit character
                CharacterRule(EnglishCharacterData.Digit, 1)

        ))
        val result = validator.validate(PasswordData(input))
        if (result.isValid) {
            return true
        }
        val messages = validator.getMessages(result)

        val messageTemplate = messages.stream()
                .collect(Collectors.joining(","))
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addConstraintViolation()
                .disableDefaultConstraintViolation()
        return false
    }
}