package application

import application.services.UserService
import org.springframework.stereotype.Component

@Component
class AccessRules(
        private val userService: UserService
) {

    fun correctUser(user: CustomUser?, userId: Long): Boolean {
        if (user == null) return false
        return user.id == userId
    }

    fun userIsAdmin(user: CustomUser?): Boolean {
        if (user == null) return false
        return user.authorities.map { it.authority }.contains("ROLE_ADMIN")
    }

    fun isSharedWithUser(user: CustomUser?, calendarId: Long): Boolean {
        if (user == null) return false
        val entity = userService.findByUserId(user.id) ?: return false
        return entity.accessibleCalendars.contains(calendarId)
    }

    fun isOwnedByUser(user: CustomUser?, calendarId: Long): Boolean {
        if (user == null) return false
        val entity = userService.findByUserId(user.id) ?: return false
        return entity.calendars.map { it.id }.contains(calendarId)
    }

    fun userDoesNotOwnCalendars(user: CustomUser?): Boolean {
        if (user == null) return false
        return userService.findByUserId(user.id)!!.calendars.isEmpty()
    }

    fun userOwnsLessThanFourCalendars(user: CustomUser?): Boolean {
        if (user == null) return false
        return userService.findByUserId(user.id)!!.calendars.size > 4
    }
}