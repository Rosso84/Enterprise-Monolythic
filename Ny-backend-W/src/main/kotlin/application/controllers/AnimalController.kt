package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.AnimalConverter
import application.controllers.dtos.AnimalDto
import application.services.AnimalService
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

@Api(value = "calendars/calendar_id/animals", description = "Handling of creating and retrieving animals")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class AnimalController(
        private val service: AnimalService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create an animal that has been contacted with")
    @PostMapping(path = ["/calendars/{calendar_id}/animals"])
    fun createAnimal(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create animal registration with")
            @RequestBody
            dto: AnimalDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/animals",
                    HttpMethod.POST, "Can not create an animal with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create an animal with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/animals",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val animalId: Long?
        try {
            animalId = service.createAnimal(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/animals",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/animals",
                HttpMethod.POST, "Animal registration created")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/animals/$animalId"))
    }


    @ApiOperation("Update an existing animal registered")
    @PutMapping(path = ["/calendars/{calendar_id}/animals/{animal_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to update animal registration with")
            @RequestBody
            dto: AnimalDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered animal to update")
            @PathVariable("animal_id")
            animalId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.PUT, "Cannot update an animal with a specific ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Cannot update an animal with a specific ID, leave ID blank.")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.getAnimal(animalId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.PUT, "The requested animal with id '$animalId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested animal with id '$animalId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.calendar.id = dto.calendar_id
        entity.type = dto.type!!
        entity.level = dto.level!!
        entity.note = dto.note!!
        entity.timestamp = dto.timestamp

        service.updateAnimal(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                HttpMethod.PUT, "Animal registration updated")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific animal by Id")
    @GetMapping(path = ["/calendars/{calendar_id}/animals/{animal_id}"])
    fun getAnimalById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered animal to retrieve")
            @PathVariable("animal_id")
            animalId: Long
    ): ResponseEntity<WrappedResponse<AnimalDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val animal = service.getAnimal(animalId)
        if (animal == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.GET, "No animal with such ID")
            return RestResponseFactory.notFound("No animal with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                HttpMethod.GET, "Retrieved specific animal registration")
        return RestResponseFactory.payload(200, AnimalConverter.transform(animal))
    }


    @ApiOperation("Get all the animals registered in a specific calendar")
    @GetMapping(path = ["/calendars/{calendar_id}/animals"])
    fun getAllAnimalsByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of animals")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of animals in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<AnimalDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/animals",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/animals",
                HttpMethod.GET, "Retrieved all animal registration from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a registered animal with a specific id")
    @DeleteMapping(path = ["/calendars/{calendar_id}/animals/{animal_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered animal to delete")
            @PathVariable("animal_id")
            animalId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(animalId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                    HttpMethod.DELETE, "No animal with such Id registered")
            return RestResponseFactory.notFound("No animal with such Id registered")
        }

        service.deleteById(animalId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/animals/$animalId",
                HttpMethod.DELETE, "The specific animal registration was deleted")
        return RestResponseFactory.noPayload(204)
    }
}