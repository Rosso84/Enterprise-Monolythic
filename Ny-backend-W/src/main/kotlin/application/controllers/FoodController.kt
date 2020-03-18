package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.FoodConverter
import application.controllers.dtos.FoodDto
import application.services.FoodService
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

@Api(value = "calendars/calendar_id/foods", description = "Handling of creating and retrieving food")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class FoodController(
        private val service: FoodService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a registration of specific consumed food")
    @PostMapping(path = ["calendars/{calendar_id}/foods"])
    fun createFood(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create food registration with")
            @RequestBody
            dto: FoodDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/foods",
                    HttpMethod.POST, "Can not create a food registration with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create a food registration with a specified ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/foods",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val foodId: Long?
        try {
            foodId = service.createFood(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logInfo(customUser, "/calendars/$calendarId/foods",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/foods",
                HttpMethod.POST, "Food registration created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/foods/$foodId"))
    }


    @ApiOperation("Update an existing food registered")
    @PutMapping(path = ["calendars/{calendar_id}/foods/{food_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to update food registration with")
            @RequestBody
            dto: FoodDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered food to update")
            @PathVariable("food_id")
            foodId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.PUT, "Cannot update a food with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update a food with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.getFood(foodId)
        if (entity == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.PUT, "The requested food with id '$foodId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested food with id '$foodId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.type = dto.type!!
        entity.level = dto.level!!
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.update(entity)
        myLogger.logWarning(customUser, "/calendars/$calendarId/foods/$foodId",
                HttpMethod.PUT, "Updated specific food registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific foodType by Id")
    @GetMapping(path = ["calendars/{calendar_id}/foods/{food_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered food to retrieve")
            @PathVariable("food_id")
            foodId: Long
    ): ResponseEntity<WrappedResponse<FoodDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val food = service.getFood(foodId)
        if (food == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.GET, "No food registration with such ID")
            return RestResponseFactory.notFound("No food registration with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/foods/$foodId",
                HttpMethod.GET, "Retrieved a specific food registration")
        return RestResponseFactory.payload(200, FoodConverter.transform(food))
    }


    @ApiOperation("Get all the food registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/foods"])
    fun getAllFoodByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of food")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of food in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<FoodDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/foods",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/foods",
                HttpMethod.GET, "Retrieved all food registrations under a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))

    }


    @ApiOperation("Delete a registered food with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/foods/{food_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered food to delete")
            @PathVariable("food_id")
            foodId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(foodId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/foods/$foodId",
                    HttpMethod.DELETE, "No food with such Id registered")
            return RestResponseFactory.notFound("No food with such Id registered")
        }

        service.deleteById(foodId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/foods/$foodId",
                HttpMethod.DELETE, "Deleted specific food registration")
        return RestResponseFactory.noPayload(204)
    }
}