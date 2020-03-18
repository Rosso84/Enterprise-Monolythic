package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.SleepConverter
import application.controllers.dtos.SleepDto
import application.services.SleepService
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

@Api(value = "/calendars/calendar_id/sleeps",
        description = "Handling of creating and retrieving sleeps")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class SleepController(
        private val service: SleepService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    /*Note: here we only check if wakeup date (toDate) is before startdate (fromDate) and
    * return a error it is. The existing app does allow everything else*/
    @ApiOperation("Create a sleeps")
    @PostMapping(path = ["calendars/{calendar_id}/sleeps"])
    fun createSleep(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: SleepDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.POST, "Can not create sleep registration with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create sleep registration with a specified ID, leave ID blank.")
        }

        if (dto.toTimestamp.isBefore(dto.fromTimestamp)) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.POST, "The wakeup date cannot be before the start date")
            return RestResponseFactory.userFailure(
                    "The wakeup date cannot be before the start date")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val sleepId: Long?
        try {
            sleepId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (sleepId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps",
                HttpMethod.POST, "Sleep registration created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/sleeps/$sleepId"))
    }


    @ApiOperation("Update an existing sleep-period registered")
    @PutMapping(path = ["calendars/{calendar_id}/sleeps/{sleep_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: SleepDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered Sleep-period to update")
            @PathVariable("sleep_id")
            sleepId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.PUT, "Can not create sleep registration with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create sleep registration with a specified ID, leave ID blank.")
        }

        if (dto.toTimestamp.isBefore(dto.fromTimestamp)) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.PUT, "The wakeup date cannot be before the start date")
            return RestResponseFactory.userFailure(
                    "The wakeup date cannot be before the start date")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(sleepId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.PUT, "The requested data with id '$sleepId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested data with id '$sleepId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.fromTimestamp = dto.fromTimestamp
        entity.toTimestamp = dto.toTimestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                HttpMethod.PUT, "Updated specific sleep registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific sleep-period by Id")
    @GetMapping(path = ["calendars/{calendar_id}/sleeps/{sleep_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered Sleep-period to update")
            @PathVariable("sleep_id")
            sleepId: Long
    ): ResponseEntity<WrappedResponse<SleepDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val sleep = service.get(sleepId)
        if (sleep == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.GET, "No sleep registration with such ID")
            return RestResponseFactory.notFound("No sleep registration with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                HttpMethod.GET, "Retrieved a specific sleep registration")
        return RestResponseFactory.payload(200, SleepConverter.transform(sleep))
    }


    @ApiOperation("Get all the sleeps registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/sleeps"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of sleeps")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of sleeps in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<SleepDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps",
                HttpMethod.GET, "Retrieved all sleep registrations from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a registered sleep-period with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/sleeps/{sleep_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered Sleep-period to update")
            @PathVariable("sleep_id")
            sleepId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(sleepId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                    HttpMethod.DELETE, "No sleep-period with such Id registered")
            return RestResponseFactory.notFound("No sleep-period with such Id registered")
        }

        service.deleteById(sleepId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/sleeps/$sleepId",
                HttpMethod.DELETE, "Deleted specific sleep registration")
        return RestResponseFactory.noPayload(204)
    }
}