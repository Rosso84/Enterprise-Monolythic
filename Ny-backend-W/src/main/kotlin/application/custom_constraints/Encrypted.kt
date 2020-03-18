package application.custom_constraints

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass


@MustBeDocumented
@Constraint(validatedBy = [EncryptedConstraintValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Encrypted(
        val message: String = "Does not seem to be encrypted",
        val groups: Array<KClass<*>> = [],
        val payload: Array<KClass<out Payload>> = []
)