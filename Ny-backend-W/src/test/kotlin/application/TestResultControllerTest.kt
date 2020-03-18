package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.TestResultDto
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.TestResultRepository
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest(
        classes = [(Application::class)],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestResultControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var repository: TestResultRepository

    @Autowired
    private lateinit var userService: UserService

    @BeforeAll
    fun setUp() {
        RestAssured.baseURI = "https://localhost/"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @BeforeEach
    fun reset() {
        repository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        repository.deleteAll()
        userService.deleteAll()
    }


    private fun createDefaultParentAndReturnIds(): ArrayList<String> {
        val idList: ArrayList<String> = ArrayList()
        // CREATING USER
        val createdUserUrl = RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar",
                        "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("users")
                .then()
                .statusCode(201)
                .extract().header("location")

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        //GETTING THE USER ID
        val userId = createdUserUrl.split("/")[1]

        idList.add(userId)

        //CREATING CALENDAR
        val createdCalenderUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("testCalendar", userId.toLong()))
                .post("calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        //GETTING THE CALENDAR ID
        val calendarId = createdCalenderUrl.split("/")[1]

        idList.add(calendarId)
        idList.add(cookie)

        if (idList.isNullOrEmpty()) {
            println("---------No ids in idList of User or calendar created------------")
        }
        return idList
    }

    @Test
    fun testCreateAndGetByID() {
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val test = "Bloddtest"
        val refValue = "D-vitamine-units"
        val value = "60"

        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(TestResultDto(test, refValue, value, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/testResults")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults/$id")
                .then()
                .statusCode(200)
                .body("data.test", CoreMatchers.equalTo(test))
                .body("data.value", CoreMatchers.equalTo(value))
                .body("data.refValue", CoreMatchers.equalTo(refValue))
    }

    @Test
    fun testCreateWithEmptyFields() {
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val test = "Bloddtest"
        val refValue = "D-vitamine-units"
        val value = " "

        //Creating with empty String return a 400
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(TestResultDto(test, refValue, value, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/testResults")
                .then()
                .statusCode(400)
                .extract().header("location")
    }

    /**@Updates an existing registered*/
    @Test
    fun testUpdateWithPut() {
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val test = "Blodtest"
        val refValue = "C-vitamine-units"
        val value = "40"
        val newValue2 = "60"

        //Creating with first value
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(TestResultDto(test, refValue, value, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/testResults")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //update with new value2 and should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(TestResultDto(test, refValue, newValue2, LocalDateTime.now(), calendarId))
                .put("calendars/$calendarId/testResults/$id")
                .then()
                .statusCode(204)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has still 1 registered
        Assertions.assertEquals(newNumberOfRegistered, 1)

        //confirm it has been replaced with new
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults/$id")
                .then()
                .statusCode(200)
                .body("data.value", CoreMatchers.equalTo(newValue2))
    }


    @Test
    fun testDeleteById() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val test = "Bloddtest"
        val refValue = "C-vitamine-units"
        val value = "40"

        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(TestResultDto(test, refValue, value, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/testResults")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(1, numberOfRegistered)

        //delete and return 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/testResults/$id")
                .then()
                .statusCode(204)

        //delete again and should be statuscode 404
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/testResults/$id")
                .then()
                .statusCode(404)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has no registered left
        Assertions.assertTrue(newNumberOfRegistered == 0)
    }


    @Test
    fun testGetAll() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val test = "Bloodtest"
        val refValue = "C-vitamine-units"
        val value = "40"

        //Creating multiple
        var numberOfCreated = 0
        while (numberOfCreated <= 5) {

            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(TestResultDto(test, refValue, value + numberOfCreated.toString(), LocalDateTime.now(), calendarId))
                    .post("calendars/$calendarId/testResults")
                    .then()
                    .statusCode(201)
                    .extract().header("location")

            numberOfCreated++
        }

        //extract the number  of created
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/testResults")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm 5 is created
        Assertions.assertEquals(numberOfRegistered, numberOfCreated)
    }
}