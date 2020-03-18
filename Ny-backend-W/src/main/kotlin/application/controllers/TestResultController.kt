package application.controllers

import application.AccessRules
import application.CustomUser
import application.MyLogger
import application.controllers.converters.TestResultConverter
import application.controllers.dtos.TestResultDto
import application.services.TestResultService
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

@Api(value = "/calendars/calendar_id/testResults",
        description = "Handling of creating and retrieving test-results")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
@Validated
@CrossOrigin(origins = ["http://localhost:8080"])
class TestResultController(
        private val service: TestResultService,
        private val accessRules: AccessRules,
        private val myLogger: MyLogger
) {

    @ApiOperation("Create a test-result registration")
    @PostMapping(path = ["calendars/{calendar_id}/testResults"])
    fun createRegistration(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a test-result registration with")
            @RequestBody
            dto: TestResultDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.POST, "Can not create test-result with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create test-result with a specified ID, leave ID blank.")
        }

        if (dto.test.isBlank() || dto.refValue.isBlank() || dto.value.isBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.POST, "The fields other than id cannot be empty or blank")
            return RestResponseFactory.userFailure(
                    "The fields other than id cannot be empty or blank")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.POST, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val resultId: Long?
        try {
            resultId = service.create(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            myLogger.logError(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.POST, e.localizedMessage)
            return RestResponseFactory.noPayload(500)
        }

        if (resultId == null) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.POST, "Could not create due to input errors. Same data can not be created twice")
            return RestResponseFactory.userFailure("Could not create due to input errors. " +
                    "Same data can not be created twice")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/testResults",
                HttpMethod.POST, "Created test-result registration")
        return RestResponseFactory.created(
                URI.create("calendars/$calendarId/testResults/$resultId"))
    }

    @ApiOperation("Update an existing test result registered")
    @PutMapping(path = ["calendars/{calendar_id}/testResults/{testResult_id}"])
    fun update(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("Data to create a test-result registration with")
            @RequestBody
            dto: TestResultDto,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered test result to update")
            @PathVariable("testResult_id")
            testResultId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!dto.id.isNullOrBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.PUT, "Can not create test-result with a specified ID, leave ID blank.")
            return RestResponseFactory.userFailure(
                    "Can not create test-result with a specified ID, leave ID blank.")
        }

        if (dto.test.isBlank() || dto.refValue.isBlank() || dto.value.isBlank()) {
            myLogger.logWarning(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.PUT, "The fields other than id cannot be empty or blank")
            return RestResponseFactory.userFailure(
                    "The fields other than id cannot be empty or blank")
        }

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.PUT, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val entity = service.get(testResultId)
        if (entity == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.PUT, "The requested data with id '$testResultId' was not found. This PUT operation will not create it.")
            return RestResponseFactory.notFound(
                    "The requested data with id '$testResultId' was not found " +
                            "This PUT operation will not create it.")
        }

        entity.test = dto.test
        entity.refValue = dto.refValue
        entity.value = dto.value
        entity.timestamp = dto.timestamp
        entity.calendar.id = dto.calendar_id

        service.update(entity)
        myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                HttpMethod.PUT, "Updated specific test-result registration")
        return RestResponseFactory.noPayload(204)
    }


    @ApiOperation("Get a specific test result by Id")
    @GetMapping(path = ["calendars/{calendar_id}/testResults/{testResult_id}"])
    fun getById(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered test result to retrieve")
            @PathVariable("testResult_id")
            testResultId: Long
    ): ResponseEntity<WrappedResponse<TestResultDto>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val registered = service.get(testResultId)
        if (registered == null) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.GET, "No test result with such ID")
            return RestResponseFactory.notFound("No test result with such ID")
        }

        myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                HttpMethod.GET, "Retrieved specific test-result")
        return RestResponseFactory.payload(200, TestResultConverter.transform(registered))
    }


    @ApiOperation("Get all the test results registered in a specific calendar")
    @GetMapping(path = ["calendars/{calendar_id}/testResults"])
    fun getAllByCalendar(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Offset in the list of test-results")
            @RequestParam("offset", defaultValue = "0")
            offset: String,
            @ApiParam("Limit of test-results in a single retrieved page")
            @RequestParam("limit", defaultValue = "10")
            limit: String
    ): ResponseEntity<WrappedResponse<Page<TestResultDto>>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/testResults",
                    HttpMethod.GET, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        val pageable = PageRequest.of(offset.toInt(), limit.toInt())
        myLogger.logInfo(customUser, "/calendars/$calendarId/testResults",
                HttpMethod.GET, "Retrieved all test-results from a specific calendar")
        return RestResponseFactory.payload(200, service.getAllByCalendarId(calendarId, pageable))
    }


    @ApiOperation("Delete a registered test results with a specific id")
    @DeleteMapping(path = ["calendars/{calendar_id}/testResults/{testResult_id}"])
    fun delete(
            @AuthenticationPrincipal
            @ApiIgnore
            customUser: CustomUser?,
            @ApiParam("The id of the calendar")
            @PathVariable("calendar_id")
            calendarId: Long,
            @ApiParam("Id of the registered test result to delete")
            @PathVariable("testResult_id")
            testResultId: Long
    ): ResponseEntity<WrappedResponse<Void>> {

        if (!accessRules.isSharedWithUser(customUser, calendarId) && !accessRules.isOwnedByUser(customUser, calendarId)) {
            myLogger.logError(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.DELETE, "Unauthorized! This user does not have access to this calendar")
            return RestResponseFactory.unauthorized("Unauthorized! This user does not have access to this calendar")
        }

        if (!service.existsById(testResultId)) {
            myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                    HttpMethod.DELETE, "No test-result with such Id registered")
            return RestResponseFactory.notFound("No test-result with such Id registered")
        }

        service.deleteById(testResultId)
        myLogger.logInfo(customUser, "/calendars/$calendarId/testResults/$testResultId",
                HttpMethod.DELETE, "Deleted specific test-result")
        return RestResponseFactory.noPayload(204)
    }
}