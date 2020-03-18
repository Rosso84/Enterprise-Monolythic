package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.PollenConverter
import application.controllers.dtos.PollenDto
import application.services.PollenService
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


@Api(value = "/calendars/calendar_id/pollens",
        description = "Handling of creating and retrieving pollen")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class PollenController(
        private val service: PollenService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a pollen registration")
    @PostMapping(path = ["/calendars/{calendar_id}/pollens"])
    fun createPollen(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: PollenDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/pollens",
                    HttpMethod.POST, "Can not create pollen with a specified ID, leave ID blank")
            return RestResponseFactory.userFailure(
                    "Can not create pollen with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/pollens",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pollenId: Long?
        try {
            pollenId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/pollens",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (pollenId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/pollens",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/pollens",
                HttpMethod.POST, "Created pollen registration")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/pollens/$pollenId"))
    }


    @ApiOperation("Update an existing pollen registered")
    @PutMapping(path = ["calendars/{calendar_id}/pollens/{pollen_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a pollen registration with")
            @RequestBody
            dto: PollenDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered pollen to update")
            @PathVariable("pollen_id")
            pollenId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.PUT, "Cannot update a pollen with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update a pollen with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.getPollen(pollenId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.PUT, "The requested pollen with id '$pollenId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested pollen with id '$pollenId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.type = dto.type!!
        entity.level = dto.level!!
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                HttpMethod.PUT, "Updated specific pollen registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific pollen type by Id")
    @GetMapping(path = ["calendars/{calendar_id}/pollens/{pollen_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered pollen to update")
            @PathVariable("pollen_id")
            pollenId: Long
    ): ResponseEntity<WrappedResponse<PollenDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pollen = service.getPollen(pollenId)
        if (pollen == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.GET, "No pollenType with such ID")
            return RestResponseFactory.notFound("No pollenType with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                HttpMethod.GET, "Retrieved specific pollen registration")
        return RestResponseFactory.payload(200, PollenConverter.transform(pollen))
    }


    @ApiOperation("Get all the pollen registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/pollens"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of pollens")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of pollen in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<PollenDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/pollens",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/pollens",
                HttpMethod.GET, "Retrieved all pollen registrations from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))

    }


    @ApiOperation("Delete a registered pollen with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/pollens/{pollen_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered pollen to update")
            @PathVariable("pollen_id")
            pollenId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(pollenId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                    HttpMethod.DELETE, "No pollen with such Id registered")
            return RestResponseFactory.notFound("No pollen with such Id registered")
        }

        service.deleteById(pollenId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/pollens/$pollenId",
                HttpMethod.DELETE, "Deleted specific pollen registration")
        return RestResponseFactory.noPayload(204)
    }
}