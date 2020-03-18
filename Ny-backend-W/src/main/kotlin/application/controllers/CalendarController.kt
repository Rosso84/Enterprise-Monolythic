package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.dtos.CalendarDto
import application.services.CalendarService
import application.services.UserService
import application.wrapped_response.RestResponseFactory
import application.wrapped_response.WrappedResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

@Api(value = "calendars", description = "Handling of creating and retrieving calendars")
@RequestMapping(produces = [(MediaType.APPLICATION_JSON_VALUE)], path = ["calendars"])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class CalendarController(

        private val service: CalendarService,
        private val userService: UserService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a new Calendar")
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun create(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser,
            @ApiParam("Data to create new Calendar with")
            @RequestBody
            dto: CalendarDto
    ): ResponseEntity<WrappedResponse<Void>> {

        if (dto.id != null) {
            myLogger.logWarning(customUser, "calendars", HttpMethod.POST,
                    "Cannot specify an id when creating a new Calendar")
            return RestResponseFactory.userFailure("Cannot specify an id when creating a new Calendar")
        }

        if (dto.calendarName.isNullOrBlank()) {
            myLogger.logWarning(customUser, "calendars", HttpMethod.POST,
                    "Cannot create calendar without specifying a calendarName")
            return RestResponseFactory.userFailure("Cannot create calendar without specifying a calendarName")
        }

        if (customUser.id != dto.parent_id) {
            myLogger.logWarning(customUser, "calendars", HttpMethod.POST,
                    "Logged in userId and userId from provided json must match")
            return RestResponseFactory.userFailure("User id in url path and provided json must match")
        }

        if (customUser.authorities.map { it.authority }.contains("ROLE_BRUKER") &&
                customUser.authorities.map { it.authority }.size == 1 &&
                !accessRules.userDoesNotOwnCalendars(customUser)) {
            myLogger.logWarning(customUser, "calendars", HttpMethod.POST
                    , "A user with role ROLE_BRUKER cannot create more than one calendar")
            return RestResponseFactory.userFailure("A user with role ROLE_BRUKER cannot create more than one calendar")
        }

        if (customUser.authorities.map { it.authority }.contains("ROLE_FAMILY") &&
                customUser.authorities.map { it.authority }.size == 2 &&
                !accessRules.userOwnsLessThanFourCalendars(customUser)) {
            myLogger.logWarning(customUser, "calendars", HttpMethod.POST,
                    "A user with role FAMILY cannot create more than four calendars")
            return RestResponseFactory.userFailure("A user with role FAMILY cannot create more than four calendars")
        }

        val calendarId = service.createCalendar(dto)
        myLogger.logInfo(customUser, "calendars", HttpMethod.POST, "Calendar created")
        return RestResponseFactory.created(URI.create("calendars/$calendarId"))
    }


    @ApiOperation("Delete a calendar by id")
    @DeleteMapping(path = ["/{calendar_id}"])
    fun deleteById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser,
            @ApiParam("The id of the calendar to delete")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logWarning(customUser, "calendars/$calendarId",
                    HttpMethod.DELETE, "Unauthorized! This user does not own this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not own this calendar")
        }

        if (!service.existsById(calendarId)) {
            myLogger.logWarning(customUser, "calendars/$calendarId",
                    HttpMethod.DELETE, "The requested calendar with id '$calendarId' is not in the database")
            return RestResponseFactory.notFound(
                    "The requested calendar with id '$calendarId' is not in the database")
        }

        service.deleteById(calendarId)
        myLogger.logInfo(customUser, "calendars/$calendarId",
                HttpMethod.DELETE, "Calendar deleted")
        return RestResponseFactory.noPayload(204)
    }

    @ApiOperation("Get a specific calendar")
    @GetMapping(path = ["/{calendar_id}"])
    fun getCalendarById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<CalendarDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logWarning(customUser, "calendars/$calendarId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val calendar = service.getById(calendarId)
        if (calendar == null) {
            myLogger.logWarning(customUser, "calendars/$calendarId",
                    HttpMethod.GET, "No calendar with this id: $calendarId found")
            return RestResponseFactory.notFound("No calendar with this id: $calendarId found")
        }

        myLogger.logInfo(customUser, "calendars/$calendarId",
                HttpMethod.GET, "Retrieved specific calendar")
        return RestResponseFactory.payload(200, calendar)
    }


    @ApiOperation("Get all calendars linked to a specific user")
    @GetMapping
    fun getAll(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser,
            @ApiParam("Offset in the list of news")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of news in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<CalendarDto>>> {

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "calendars", HttpMethod.GET, "Retrieved all calendars")
        return RestResponseFactory.payload(200, service.getAllCalendarsByParent(customUser.id, pageable))
    }

    @ApiOperation("Share a calendar with a specific user")
    @PostMapping(path = ["/{calendar_id}"])
    fun shareCalendarWith(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser,
            @ApiParam("The id of the calendar to delete")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Query param on what user to share a calendar with")
            @RequestParam("shareWith")
            shareWith: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logWarning(customUser, "calendars/$calendarId",
                    HttpMethod.POST, "Unauthorized! This user does not own this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not own this calendar")
        }

        val user = userService.findByUserId(shareWith)!!

        if (user.accessibleCalendars.contains(calendarId)) {
            user.accessibleCalendars.remove(calendarId)
            userService.updateUser(user)
            service.revokeAccessFromUser(calendarId, customUser.id)
            myLogger.logInfo(customUser, "calendars/$calendarId",
                    HttpMethod.POST, "Revoked access to $calendarId from user ${customUser.id}")
            return RestResponseFactory.noPayload(204)
        }

        user.accessibleCalendars.add(calendarId)
        userService.updateUser(user)
        service.addAccessFromUser(calendarId, customUser.id)
        myLogger.logInfo(customUser, "calendars/$calendarId",
                HttpMethod.POST, "Added access to $calendarId for user ${customUser.id}")
        return RestResponseFactory.noPayload(204)
    }
}