package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.SymptomConverter
import application.controllers.dtos.SymptomDto
import application.services.SymptomService
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


@Api(value = "/calendars/calendar_id/symptoms",
        description = "Handling of creating and retrieving symptoms")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class SymptomController(
        private val service: SymptomService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a symptom")
    @PostMapping(path = ["calendars/{calendar_id}/symptoms"])
    fun createSymptom(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: SymptomDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/symptoms",
                    HttpMethod.POST, "Can not create symptom with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create symptom with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val symptomId: Long?
        try {
            symptomId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (symptomId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/symptoms",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms",
                HttpMethod.POST, "Created new symptom registration")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/symptoms/$symptomId"))
    }


    @ApiOperation("Update an existing symptom registered")
    @PutMapping(path = ["calendars/{calendar_id}/symptoms/{symptom_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: SymptomDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered symptom to update")
            @PathVariable("symptom_id")
            symptomId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.PUT, "Can not create symptom with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update a symptom with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(symptomId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.PUT, "The requested symptom with id '$symptomId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested symptom with id '$symptomId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.type = dto.type!!
        entity.level = dto.level!!
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                HttpMethod.PUT, "Updated specific symptom registration")
        return RestResponseFactory.noPayload(204)
    }

    @ApiOperation("Get a specific symptom type by Id")
    @GetMapping(path = ["/calendars/{calendar_id}/symptoms/{symptom_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered symptom to update")
            @PathVariable("symptom_id")
            symptomId: Long
    ): ResponseEntity<WrappedResponse<SymptomDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val symptom = service.get(symptomId)
        if (symptom == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.GET, "No symptom with such ID")
            return RestResponseFactory.notFound("No symptom with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                HttpMethod.GET, "Retrieved specific symptom registration")
        return RestResponseFactory.payload(200, SymptomConverter.transform(symptom))
    }


    @ApiOperation("Get all the symptoms registered in a specific calendar")
    @GetMapping(path = ["/calendars/{calendar_id}/symptoms"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of symptoms")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of symptoms in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<SymptomDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms",
                HttpMethod.GET, "Retrieved all symptom registration under a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))

    }


    @ApiOperation("Delete a registered symptom with a specific id")
    @DeleteMapping(path = ["/calendars/{calendar_id}/symptoms/{symptom_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered symptom to update")
            @PathVariable("symptom_id")
            symptomId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(symptomId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                    HttpMethod.DELETE, "No symptom with such Id registered")
            return RestResponseFactory.notFound("No symptom with such Id registered")
        }

        service.deleteById(symptomId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/symptoms/$symptomId",
                HttpMethod.DELETE, "Deleted specific symptom registration")
        return RestResponseFactory.noPayload(204)
    }
}