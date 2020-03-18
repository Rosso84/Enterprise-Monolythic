package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.AbsenceConverter
import application.controllers.dtos.AbsenceDto
import application.services.AbsenceService
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

@Api(value = "calendars/calendar_id/absences",
        description = "Handling of creating and retrieving Absences")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class AbsenceController(
        private val service: AbsenceService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create Absence")
    @PostMapping(path = ["calendars/{calendar_id}/absences"])
    fun createAbsence(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create absence with")
            @RequestBody
            dto: AbsenceDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.POST, "Can not create with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val absenceId: Long?

        try {
            absenceId = service.create(dto)

        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.POST, "Server failure, could not create absence")
            return RestResponseFactory.noPayload(500)
        }
        if (absenceId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/absences",
                HttpMethod.POST, "Absence created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/absences/$absenceId"))
    }

    @ApiOperation("Update an existing absence registered")
    @PutMapping(path = ["calendars/{calendar_id}/absences/{absence_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create absence with")
            @RequestBody
            dto: AbsenceDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the Absence")
            @PathVariable("absence_id")
            absenceId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.PUT, "Cannot update an absence with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update an absence with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(absenceId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.PUT, "The requested absence with id '$absenceId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested absence with id '$absenceId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                HttpMethod.PUT, "Absence updated")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific absence by Id")
    @GetMapping(path = ["calendars/{calendar_id}/absences/{absence_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the Absence")
            @PathVariable("absence_id")
            absenceId: Long
    ): ResponseEntity<WrappedResponse<AbsenceDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val absence = service.get(absenceId)
        if (absence == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.GET, "No absence with such ID: '$absenceId'")
            return RestResponseFactory.notFound("No absence with such ID: '$absenceId'")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                HttpMethod.GET, "Retrieved specific absence")
        return RestResponseFactory.payload(200, AbsenceConverter.transform(absence))
    }


    @ApiOperation("Get all the absences registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/absences"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of absences")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of absences in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<AbsenceDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/absences",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/absences",
                HttpMethod.GET, "Retrieved all absences from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))

    }


    @ApiOperation("Delete an absence with a specific id")
    @DeleteMapping(path = ["/calendars/{calendar_id}/absences/{absence_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the Absence")
            @PathVariable("absence_id")
            absenceId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(absenceId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                    HttpMethod.DELETE, "No absence with such Id: '$absenceId' registered")
            return RestResponseFactory.notFound("No absence with such Id: '$absenceId' registered")
        }

        service.deleteById(absenceId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/absences/$absenceId",
                HttpMethod.DELETE, "Absence deleted")
        return RestResponseFactory.noPayload(204)
    }

}
