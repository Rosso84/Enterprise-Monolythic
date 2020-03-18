package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.SleepDto
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.SleepRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@ExtendWith(SpringExtension::class)
@SpringBootTest(
        classes = [(Application::class)],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SleepControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var repository: SleepRepository

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

        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.now())
        val toTime = fromTime.plusHours(12)

        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(SleepDto(fromTime, toTime, calendarId))
                .post("calendars/$calendarId/sleeps")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //confirm by checking if e.g chosen note is equal
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps/$id")
                .then()
                .statusCode(200)
                .body("data.fromTimestamp", CoreMatchers.equalTo(fromTime.toString()))
                .body("data.toTimestamp", CoreMatchers.equalTo(toTime.toString()))
    }

    @Test
    fun testCreateWithWrongWakeupDate() {
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.now())
        val toTime = fromTime.minusHours(12)

        //Creating with wrong input wakeup date and should return a 404
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(SleepDto(fromTime, toTime, calendarId))
                .post("calendars/$calendarId/sleeps")
                .then()
                .statusCode(400)
                .extract().header("location")
    }

    /**@Updates an existing sleep-period registered*/
    @Test
    fun testUpdateWithPut() {
        //userID = 0  calendarId = 1 dateId = 2 cookie = 3
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.now())
        val toTime = fromTime.plusHours(12)


        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(SleepDto(fromTime, toTime, calendarId))
                .post("calendars/$calendarId/sleeps")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        val newToTime = LocalDateTime.now().plus(2, ChronoUnit.DAYS)
        val newFromTime = LocalDateTime.now()

        //update with new and should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(SleepDto(newFromTime, newToTime, calendarId))
                .put("calendars/$calendarId/sleeps/$id")
                .then()
                .statusCode(204)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has still 1 registered
        Assertions.assertEquals(newNumberOfRegistered, 1)

        //confirm note has been replaced with new
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps/$id")
                .then()
                .statusCode(200)
                .body("data.toTimestamp", CoreMatchers.equalTo(newToTime.toString()))
    }

    /** @Deletes a registered sleep-period by id*/
    @Test
    fun testDeleteById() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.now())
        val toTime = fromTime.plusHours(12)


        //Creating measurement
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(SleepDto(fromTime, toTime, calendarId))
                .post("calendars/$calendarId/sleeps")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(numberOfRegistered, 1)

        //delete and return 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/sleeps/$id")
                .then()
                .statusCode(204)

        //delete again and should be statuskode 404
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/sleeps/$id")
                .then()
                .statusCode(404)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps")
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

        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.now())
        val toTime = fromTime.plusHours(12)

        //Creating multiple
        var numberOfCreated = 0
        while (numberOfCreated <= 5) {
            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(SleepDto(fromTime.minus(numberOfCreated.toLong(), ChronoUnit.DAYS), toTime, calendarId))
                    .post("calendars/$calendarId/sleeps")
                    .then()
                    .statusCode(201)

            numberOfCreated++
        }

        //extract the number  of created
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/sleeps")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm 5 is created
        Assertions.assertEquals(numberOfRegistered, numberOfCreated)
    }
}