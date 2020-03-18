package application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class MyLogger {

    private val logger: Logger = LoggerFactory.getLogger(MyLogger::class.java)

    fun logError(customUser: CustomUser?, path: String, httpMethod: HttpMethod, message: String) {
        logger.error(genericTemplate(path, httpMethod, customUser, message))
    }

    fun logWarning(customUser: CustomUser?, path: String, httpMethod: HttpMethod, message: String) {
        logger.warn(genericTemplate(path, httpMethod, customUser, message))
    }

    fun logInfo(customUser: CustomUser?, path: String, httpMethod: HttpMethod, message: String) {
        logger.info(genericTemplate(path, httpMethod, customUser, message))
    }

    fun logDebug(customUser: CustomUser?, path: String, httpMethod: HttpMethod, message: String) {
        logger.debug(genericTemplate(path, httpMethod, customUser, message))
    }

    fun logTrace(customUser: CustomUser?, path: String, httpMethod: HttpMethod, message: String) {
        logger.trace(genericTemplate(path, httpMethod, customUser, message))
    }

    private fun genericTemplate(path: String, httpMethod: HttpMethod, customUser: CustomUser?, message: String): String {
        return "\n Timestamp: ${getTime()}\n" +
                " Path: $path\n" +
                " Http method: ${httpMethod.name}\n" +
                " UserId: ${getUserId(customUser)}\n" +
                " Roles: ${getRoles(customUser)}\n" +
                " Ip Address: ${getIp()}\n" +
                " Message: $message"
    }

    private fun getUserId(customUser: CustomUser?): String {
        if (customUser == null) return "None/Unauthorized"
        return customUser.id.toString()
    }

    private fun getRoles(customUser: CustomUser?): String {
        if (customUser == null) return "None/Unauthorized"
        return Arrays.toString(customUser.authorities.toTypedArray())
    }

    private fun getIp(): String {
        val authentication = SecurityContextHolder.getContext().authentication ?: return ""
        val details = authentication.details
        if (details is WebAuthenticationDetails) return details.remoteAddress
        return ""
    }

    private fun getTime(): String {
        return DateTimeFormatter
                .ofPattern("dd-MM-yyyy HH:mm:ss zzz")
                .withZone(ZoneId.of("Europe/Paris"))
                .format(Instant.now())
    }

}