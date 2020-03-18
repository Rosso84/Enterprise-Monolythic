package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.EczemaConverter
import application.controllers.dtos.EczemaDto
import application.services.EczemaService
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

@Api(value = "calendars/calendar_id/eczemas", description = "Handling of creating and retrieving eczemas")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class EczemaController(
        private val service: EczemaService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create an animal that has been contacted with")
    @PostMapping(path = ["/calendars/{calendar_id}/eczemas"])
    fun createEczema(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create eczema registration with")
            @RequestBody
            dto: EczemaDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/eczemas",
                    HttpMethod.POST, "Can not create eczema with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create eczema with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/eczemas",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val eczemaId: Long?
        try {
            eczemaId = service.createEczema(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas",
                HttpMethod.POST, "Eczema registration created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/eczemas/$eczemaId"))
    }


    @ApiOperation("Update a registered eczema")
    @PutMapping(path = ["calendars/{calendar_id}/eczemas/{eczema_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to update eczema registration with")
            @RequestBody
            dto: EczemaDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered eczema to update")
            @PathVariable("eczema_id")
            eczemaId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.PUT, "Can not update an eczema with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update a eczema with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }


        val entity = service.getEczema(eczemaId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.PUT, "The requested eczema with id '$eczemaId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested eczema with id '$eczemaId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.bodyPart = dto.bodyPart
        entity.bodyPortion = dto.bodyPortion
        entity.bodySide = dto.bodySide
        entity.level = dto.level
        entity.note = dto.note
        entity.timestamp = dto.timestamp
        entity.type = dto.type

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                HttpMethod.PUT, "Eczema registration updated")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific eczema by Id")
    @GetMapping(path = ["calendars/{calendar_id}/eczemas/{eczema_id}"])
    fun getEczemaById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered eczema to retrieve")
            @PathVariable("eczema_id")
            eczemaId: Long
    ): ResponseEntity<WrappedResponse<EczemaDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val eczema = service.getEczema(eczemaId)
        if (eczema == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.GET, "No eczema with such ID")
            return RestResponseFactory.notFound("No eczema with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                HttpMethod.GET, "Retrieved specific eczema registration")
        return RestResponseFactory.payload(200, EczemaConverter.transform(eczema))
    }


    @ApiOperation("Get all the eczemas registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/eczemas"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of eczemas")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of eczemas in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<EczemaDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/eczemas",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas",
                HttpMethod.GET, "Retrieved all eczema registration from a specific calendar")
        return RestResponseFactory.payload(200, service.getEczemasFromCalendarId(calendarId, pageable))

    }

    @ApiOperation("Delete a registered eczema with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/eczemas/{eczema_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered eczema to delete")
            @PathVariable("eczema_id")
            eczemaId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(eczemaId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                    HttpMethod.DELETE, "No eczema with such Id registered")
            return RestResponseFactory.notFound("No eczema with such Id registered")
        }

        service.deleteById(eczemaId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/eczemas/$eczemaId",
                HttpMethod.DELETE, "Eczema registration deleted")
        return RestResponseFactory.noPayload(204)
    }
}