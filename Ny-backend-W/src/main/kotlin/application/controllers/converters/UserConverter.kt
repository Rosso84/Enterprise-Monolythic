package application.controllers.converters

import application.controllers.dtos.UserDTO
import application.entities.UserEntity

object UserConverter {

    fun transform(userEntity: UserEntity): UserDTO {

        return UserDTO(
                email = userEntity.email,
                firstName = userEntity.firstName,
                lastName = userEntity.lastName,
                password = userEntity.password,
                pin = userEntity.pin,
                roles = userEntity.roles,
                enabled = userEntity.enabled,
                calendars = userEntity.calendars.map { it.name }.toMutableList(),
                accessibleCalendars = userEntity.accessibleCalendars,
                id = userEntity.id?.toString()
        )
    }
}