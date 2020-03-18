package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.HumorConverter
import application.controllers.dtos.HumorDto
import application.services.HumorService
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


@Api(value = "calendars/calendar_id/humors",
        description = "Handling of creating and retrieving Humors")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class HumorController(
        private val service: HumorService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a humor")
    @PostMapping(path = ["calendars/{calendar_id}/humors"])
    fun createHumor(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a humor registration with")
            @RequestBody
            dto: HumorDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/humors",
                    HttpMethod.POST, "Can not create humor with a specified ID, please leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create humor with a specified ID, please leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/humors",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val humorId: Long?
        try {
            humorId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/humors",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (humorId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/humors",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/humors",
                HttpMethod.POST, "Humor registration created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/humors/$humorId"))
    }


    @ApiOperation("Get a specific humor type by Id")
    @GetMapping(path = ["calendars/{calendar_id}/humors/{humor_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("ID of the humors")
            @PathVariable("humor_id")
            humorId: Long
    ): ResponseEntity<WrappedResponse<HumorDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/humors/$humorId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val humor = service.getPollen(humorId)
        if (humor == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/humors/$humorId",
                    HttpMethod.GET, "No humor registration with such ID")
            return RestResponseFactory.notFound("No humor registration with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/humors/$humorId",
                HttpMethod.GET, "Retrieved specific humor registration")
        return RestResponseFactory.payload(200, HumorConverter.transform(humor))
    }


    @ApiOperation("Get all the humors registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/humors"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of humors")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of humors in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<HumorDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/humors",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/humors",
                HttpMethod.GET, "Retrieved all humor registrations from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a registered humor with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/humors/{humor_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("ID of the humors")
            @PathVariable("humor_id")
            humorId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/humors/$humorId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(humorId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/humors/$humorId",
                    HttpMethod.DELETE, "No humor with such Id registered")
            return RestResponseFactory.notFound("No humor with such Id registered")
        }

        service.deleteById(humorId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/humors/$humorId",
                HttpMethod.DELETE, "Deleted specific humor registration")
        return RestResponseFactory.noPayload(204)
    }

    //TODO: missing update api put
}