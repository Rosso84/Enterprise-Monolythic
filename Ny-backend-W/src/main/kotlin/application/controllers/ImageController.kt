package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.ImageConverter
import application.controllers.dtos.ImageDTO
import application.services.ImageService
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

@RestController
@Api(value = "calendars/calendar_id/images", description = "RestAPI for users")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class ImageController(
        private val service: ImageService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Save an image")
    @PostMapping(path = ["calendars/{calendar_id}/images"])
    fun createImage(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Data to create a humor registration with")
            @RequestBody
            dto: ImageDTO
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logError(customUser, "/calendars/$calendarId/images",
                    HttpMethod.POST, "Can not create an image with a specific id, leave id blank")
            return RestResponseFactory.userFailure("Can not create an image with a specific id, leave id blank")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/images",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val id: Long?
        try {
            id = service.createImage(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logInfo(customUser, "/calendars/$calendarId/images",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(400)
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/images",
                HttpMethod.POST, "New image created")
        return RestResponseFactory.created(URI.create("calendars/$calendarId/images/$id"))
    }


    @ApiOperation("Get a single image by imageId")
    @GetMapping(path = ["/calendars/{calendar_id}/images/{image_id}"])
    fun getImageById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of a image")
            @PathVariable("image_id")
            imageId: Long
    ): ResponseEntity<WrappedResponse<ImageDTO>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/images/$imageId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val image = service.findByImageId(imageId)
        if (image == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/images/$imageId",
                    HttpMethod.GET, "No image with such Id")
            return RestResponseFactory.notFound("No image with such Id")
        }

        image.data = service.decryptImage(image.data)
        myLogger.logInfo(customUser, "/calendars/$calendarId/images/$imageId",
                HttpMethod.GET, "Retrieved specific image")
        return RestResponseFactory.payload(200, ImageConverter.transform(image))
    }


    @ApiOperation("Get all the images registered in a specific calendar")
    @GetMapping(path = ["/calendars/{calendar_id}/images"])
    fun getAllImagesByCalendar(
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
    ): ResponseEntity<WrappedResponse<Page<ImageDTO>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/images",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logError(customUser, "/calendars/$calendarId/images",
                HttpMethod.GET, "Retrieved all images under a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a specific image")
    @DeleteMapping(path = ["calendars/{calendar_id}/images/{image_id}"])
    fun deleteImage(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of a image")
            @PathVariable("image_id")
            imageId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/images/$imageId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.imageExistById(imageId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/images/$imageId",
                    HttpMethod.DELETE, "No image with such Id")
            return RestResponseFactory.notFound("No image with such Id")
        }

        service.deleteByImageId(imageId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/images/$imageId",
                HttpMethod.DELETE, "Deleted specific image")
        return RestResponseFactory.noPayload(204)
    }
}