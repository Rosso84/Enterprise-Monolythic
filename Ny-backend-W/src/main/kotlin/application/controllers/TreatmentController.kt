package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.TreatmentConverter
import application.controllers.dtos.TreatmentDto
import application.services.TreatmentService
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

@Api(value = "calendars/calendar_id/treatments",
        description = "Handling of creating and retrieving treatments")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class TreatmentController(
        private val service: TreatmentService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a treatment")
    @PostMapping(path = ["calendars/{calendar_id}/treatments"])
    fun createTreatment(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create treatment registration with")
            @RequestBody
            dto: TreatmentDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/treatments",
                    HttpMethod.POST, "Can not create treatment with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create treatment with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/treatments",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val treatmentId: Long?
        try {
            treatmentId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/treatments",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (treatmentId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/treatments",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/treatments",
                HttpMethod.POST, "Created new treatment registration")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/treatments/$treatmentId"))
    }

    @ApiOperation("Update an existing treatment registered")
    @PutMapping(path = ["calendars/{calendar_id}/treatments/{treatment_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create treatment registration with")
            @RequestBody
            dto: TreatmentDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered treatment to update")
            @PathVariable("treatment_id")
            treatmentId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.PUT, "Cannot update a treatment with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update a treatment with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(treatmentId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.PUT, "The requested treatment with id '$treatmentId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested treatment with id '$treatmentId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.type = dto.type!!
        entity.level = dto.level!!
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                HttpMethod.PUT, "Updated specific treatment registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific treatment by Id")
    @GetMapping(path = ["calendars/{calendar_id}/treatments/{treatment_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered treatment to update")
            @PathVariable("treatment_id")
            treatmentId: Long
    ): ResponseEntity<WrappedResponse<TreatmentDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val treatment = service.get(treatmentId)
        if (treatment == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.GET, "No treatment registration with such ID")
            return RestResponseFactory.notFound("No treatment registration with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                HttpMethod.GET, "Retrieved specific treatment registration")
        return RestResponseFactory.payload(200, TreatmentConverter.transform(treatment))
    }


    @ApiOperation("Get all the treatments registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/treatments"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of treatments")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of treatments in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<TreatmentDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/treatments",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/treatments",
                HttpMethod.GET, "Retrieved all treatments from a single calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))

    }


    @ApiOperation("Delete a registered treatments with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/treatments/{treatment_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered treatment to update")
            @PathVariable("treatment_id")
            treatmentId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(treatmentId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                    HttpMethod.DELETE, "No treatments with such Id registered")
            return RestResponseFactory.notFound("No treatments with such Id registered")
        }

        service.deleteById(treatmentId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/treatments/$treatmentId",
                HttpMethod.DELETE, "Deleted specific treatment")
        return RestResponseFactory.noPayload(204)
    }
}