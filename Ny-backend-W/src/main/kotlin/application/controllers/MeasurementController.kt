package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.MeasurementConverter
import application.controllers.dtos.MeasurementDto
import application.services.MeasurementService
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


@Api(value = "/calendars/calendar_id/measurements",
        description = "Handling of creating and retrieving weight and height")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class MeasurementController(
        private val service: MeasurementService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create weight and height")
    @PostMapping(path = ["calendars/{calendar_id}/measurements"])
    fun createWeightHeight(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create measurement registration with")
            @RequestBody
            dto: MeasurementDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.POST, "Can not create measurement registration with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create measurement registration with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (service.existsByParentId(calendarId)) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.POST, "Cannot create more than one measurement per day, use update instead")
            return RestResponseFactory.userFailure("Cannot create more than one measurement per day, use update instead")
        }

        val measurementId: Long?
        try {
            measurementId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }
        if (measurementId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/measurements",
                HttpMethod.POST, "Created Weight-Height registration")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/measurements/$measurementId"))
    }

    @ApiOperation("Update an existing measurement registered")
    @PutMapping(path = ["calendars/{calendar_id}/measurements/{measurement_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create treatment registration with")
            @RequestBody
            dto: MeasurementDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("The id of the measurement")
            @PathVariable("measurement_id")
            measurementId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.PUT, "Can not create measurement registration with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create measurement registration with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(measurementId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.PUT, "The requested measurement with id '$measurementId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested measurement with id '$measurementId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.weightGrams = dto.weightGrams!!
        entity.weightKilos = dto.weightKilos!!
        entity.heightCm = dto.heightCm!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                HttpMethod.PUT, "Updated specific measurement registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific measurement by Id")
    @GetMapping(path = ["calendars/{calendar_id}/measurements/{measurement_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("The id of the WeightHeight")
            @PathVariable("measurement_id")
            measurementId: Long
    ): ResponseEntity<WrappedResponse<MeasurementDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val measurement = service.get(measurementId)
        if (measurement == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.GET, "No measurement with such ID: '$measurementId'")
            return RestResponseFactory.notFound("No measurement with such ID: '$measurementId'")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                HttpMethod.GET, "Retrieved specific measurement registration")
        return RestResponseFactory.payload(200, MeasurementConverter.transform(measurement))
    }


    @ApiOperation("Get all the measurements registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/measurements"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of measurements")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of measurements in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<MeasurementDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/measurements",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/measurements",
                HttpMethod.GET, "Retrieved all measurement registrations from a calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a  measurement with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/measurements/{measurement_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("The id of the WeightHeight")
            @PathVariable("measurement_id")
            measurementId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(measurementId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                    HttpMethod.DELETE, "No measurement with such Id: '$measurementId' registered")
            return RestResponseFactory.notFound("No measurement with such Id: '$measurementId' registered")
        }

        service.deleteById(measurementId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/measurements/$measurementId",
                HttpMethod.DELETE, "Deleted specific measurement registration")
        return RestResponseFactory.noPayload(204)
    }
}
