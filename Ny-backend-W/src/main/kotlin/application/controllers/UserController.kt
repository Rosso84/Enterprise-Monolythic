package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.UserConverter
import application.controllers.dtos.UserDTO
import application.entities.UserEntity
import application.services.UserService
import application.wrapped_response.RestResponseFactory
import application.wrapped_response.WrappedResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.passay.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore
import java.net.URI
import java.util.*
import java.util.regex.Pattern


@RestController
@Api(value = "/users", description = "RestAPI for users")
@RequestMapping(path = ["/users"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class UserController(

        private val service: UserService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a User")
    @PostMapping
    fun createUser(
            @ApiParam("Method to create a user")
            @RequestBody
            dto: UserDTO
    ): ResponseEntity<WrappedResponse<Void>> {

        val errorMessage = getErrorMessageIfBaseDtoChecksFail(dto)
        if (errorMessage != null) {
            myLogger.logWarning(null, "/users", HttpMethod.POST, errorMessage)
            return RestResponseFactory.userFailure(errorMessage)
        }

        if (service.userExistByEmail(dto.email)) {
            myLogger.logWarning(null, "/users", HttpMethod.POST,
                    "Cannot create, a user with this email: ${dto.email} exists already")
            return RestResponseFactory.userFailure("Cannot create, a user with this email: ${dto.email} exists already")
        }

        val id: Long?
        try {
            id = service.createUser(UserEntity(dto.email, dto.firstName, dto.lastName, dto.password, dto.pin, dto.roles.map { it }.toSet()))
        } catch (e: Exception) {
            myLogger.logError(null, "/users", HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        myLogger.logInfo(null, "/users", HttpMethod.POST, "User created")
        return RestResponseFactory.created(URI.create("users/$id"))
    }

    @ApiOperation("Get all the users")
    @GetMapping
    fun getAllUsers(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Offset in the list of users")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of users in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<UserDTO>>> {

        if (!accessRules.userIsAdmin(customUser)) {
            myLogger.logError(customUser, "/users", HttpMethod.GET, "Unauthorized, need admin privileges")
            return RestResponseFactory.unauthorized("Unauthorized, need admin privileges")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/users", HttpMethod.GET, "Retrieved all users")
        return RestResponseFactory.payload(200, service.findAllUsers(pageable))
    }

    @ApiOperation("Get a single user by userId")
    @GetMapping(path = ["/{user_id}"])
    fun getUserById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Id of a user")
            @PathVariable("user_id")
            userId: Long
    ): ResponseEntity<WrappedResponse<UserDTO>> {

        if (!accessRules.correctUser(customUser, userId)) {
            myLogger.logError(customUser, "/users/$userId", HttpMethod.GET, "Unauthorized attempt to get")
            return RestResponseFactory.unauthorized()
        }

        val user = service.findByUserId(userId)
        if (user == null) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.GET, "No user with such Id")
            return RestResponseFactory.notFound("No user with such Id")
        }

        myLogger.logInfo(customUser, "/users/$userId", HttpMethod.GET, "Retrieved specific user")
        return RestResponseFactory.payload(200, UserConverter.transform(user))
    }

    @ApiOperation("Delete a user with a specific id")
    @DeleteMapping(path = ["/{user_id}"])
    fun deleteUser(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Id of a user")
            @PathVariable("user_id")
            userId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.correctUser(customUser, userId)) {
            myLogger.logError(customUser, "/users/$userId", HttpMethod.DELETE, "Unauthorized attempt to delete")
            return RestResponseFactory.unauthorized()
        }

        if (!service.existByUserId(userId)) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.DELETE, "No user with such Id")
            return RestResponseFactory.notFound("No user with such Id")
        }

        service.deleteByUserId(userId)
        myLogger.logInfo(customUser, "/users/$userId", HttpMethod.DELETE, "Deleted specific user")
        return RestResponseFactory.noPayload(204)
    }

    @ApiOperation("Update a specific user")
    @PutMapping(path = ["/{user_id}"])
    fun updateUser(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to have in updated user")
            @RequestBody
            dto: UserDTO,
            @ApiParam("The id of the user")
            @PathVariable("user_id")
            userId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.correctUser(customUser, userId)) {
            myLogger.logError(customUser, "/users/$userId", HttpMethod.PUT, "Unauthorized attempt to update")
            return RestResponseFactory.unauthorized()
        }

        val errorMessage = getErrorMessageIfBaseDtoChecksFail(dto)
        if (errorMessage != null) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.PUT, errorMessage)
            return RestResponseFactory.userFailure(errorMessage)
        }

        if (!dto.calendars.isNullOrEmpty()) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.PUT, "Can not update a user with new calendar info. " +
                    "To update specific calendar use PUT method from calendar Controller")
            return RestResponseFactory.userFailure("Can not update a user with new calendar info. " +
                    "To update specific calendar use PUT method from calendar Controller")
        }

        if (!dto.accessibleCalendars.isNullOrEmpty()) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.PUT,
                    "Can not manually update the id of a shared calendar")
            return RestResponseFactory.userFailure("Can not manually update the id of a shared calendar")
        }

        val entity = service.findByUserId(userId)
        if (entity == null) {
            myLogger.logWarning(customUser, "/users/$userId", HttpMethod.PUT, "The requested user " +
                    "with id '$userId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested user with id '$userId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.email = dto.email
        entity.firstName = dto.firstName
        entity.lastName = dto.lastName
        entity.password = dto.password
        entity.pin = dto.pin
        entity.enabled = dto.enabled
        entity.roles = dto.roles.map { it }.toHashSet()

        service.updateUser(entity)
        myLogger.logInfo(customUser, "/users/$userId", HttpMethod.PUT, "User updated")
        return RestResponseFactory.noPayload(204)
    }

    private fun getErrorMessageIfBaseDtoChecksFail(dto: UserDTO): String? {
        if (!dto.id.isNullOrBlank()) {
            return "Can not create a user with a specific id, leave id blank"
        }

        val emailPattern = Pattern.compile("^(.+)@(.+).(.+)$")
        if (!emailPattern.matcher(dto.email).find()) {
            return "Email is not formatted properly"
        }

        if (!passwordIsValid(dto.password)) {
            return "Password does not meet requirements. Password must be a " +
                    "minimum of 8 characters, have at least one upper and lowercase letter and contain a numeric letter"
        }

        if (dto.firstName.isBlank() || dto.lastName.isBlank()) {
            return "First and last calendarName must be provided"
        }

        if (dto.pin.length != 4) {
            return "Pin must be 4 chars in size"
        }

        if (!dto.calendars.isNullOrEmpty()) {
            return "Cannot provide calendar names manually"
        }

        if (!dto.accessibleCalendars.isNullOrEmpty()) {
            return "Cannot provide accessible calendarIds manually"
        }

        return null
    }

    private fun passwordIsValid(password: String): Boolean {
        val validator = PasswordValidator(Arrays.asList(
                // between 8 and 30 chars
                LengthRule(8, 30),

                // at least one upper-case character
                CharacterRule(EnglishCharacterData.UpperCase, 1),

                // at least one lower-case character
                CharacterRule(EnglishCharacterData.LowerCase, 1),

                // at least one digit character
                CharacterRule(EnglishCharacterData.Digit, 1)

        ))
        val result = validator.validate(PasswordData(password))
        return result.isValid
    }
}