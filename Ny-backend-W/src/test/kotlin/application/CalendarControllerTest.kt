package application

import application.controllers.dtos.*
import application.entities.UserEntity
import application.enums.Level
import application.enums.Role
import application.services.AbsenceService
import application.services.CalendarService
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
class CalendarControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var calendarService: CalendarService

    @Autowired
    private lateinit var absenceService: AbsenceService


    @BeforeAll
    fun setUp() {
        // RestAssured configs shared by all the tests
        RestAssured.baseURI = "https://localhost"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @BeforeEach
    fun reset() {
        absenceService.deleteAll()
        calendarService.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        absenceService.deleteAll()
        calendarService.deleteAll()
        userService.deleteAll()
    }


    private fun createCalendars(userId: Long, cookie: String): ArrayList<String> {

        val listOfLocations: ArrayList<String> = ArrayList()

        //Creating multiple
        var numberOfCreated = 0
        while (numberOfCreated <= 3) {

            val calendar = RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(CalendarDto("MyCalendarName$numberOfCreated", userId))
                    .post("/calendars")
                    .then()
                    .statusCode(201)
                    .extract().header("location")

            listOfLocations.add(calendar)
            numberOfCreated++
        }

        return listOfLocations

    }

    /**@Creates new and verifies if exists.*/
    @Test
    fun testCreate() {

        val calendarName = "MinKalender"

        val parentId = userService.createUser(UserEntity("test@moo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))!!

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val currentSize = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto(calendarName, parentId))
                .post("/calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .basePath("")
                .get(location)
                .then()
                .statusCode(200)
                .body("data.calendarName", CoreMatchers.equalTo(calendarName))

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .body("data.numberOfElements", CoreMatchers.equalTo(currentSize + 1))

    }

    @Test
    fun testGetSingle() {
        //new user
        val parentId = userService.createUser(UserEntity("test@moo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_FAMILIE)))!!

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("Meowie", parentId))
                .post("/calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        val calendarId = location.split("/")[1].toLong()

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars/$calendarId")
                .then()
                .statusCode(200)
                .body("data.calendarName", CoreMatchers.equalTo("Meowie"))
    }


    /** @Deletes all Calendars by id*/
    @Test
    fun testDeleteAllCalendarsById() {

        //new user
        val parentId = userService.createUser(UserEntity("test@moo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_FAMILIE)))!!

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        //confirm has no calendars
        val sizeOfCalendars = given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        assertEquals(0, sizeOfCalendars)

        //creating 4 new calendars
        val calendarList = createCalendars(parentId, cookie)

        val newSizeOfCalendars = given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm 4 calendars are registered
        assertEquals(4, newSizeOfCalendars)
        assertFalse(newSizeOfCalendars == 0)

        for (calendar in calendarList) {
            //delete all
            RestAssured.given().accept(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .delete(calendar)
                    .then()
                    .statusCode(204)
        }

        //should now be no calendars
        val calendarsLeft = given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm everything is gone
        assertEquals(0, calendarsLeft)
    }

    /** @Deletes a Calendar by a given Id*/
    @Test
    fun testDoubleDeleteCalendarById() {

        val parentId = userService.createUser(UserEntity("test@moo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))!!

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val name = "MyCalender"

        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto(name, parentId))
                .post("/calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete(location)
                .then()
                .statusCode(204)

        //delete again, should now be 401
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete(location)
                .then()
                .statusCode(401)
    }

    @Test
    fun testGetAllWithSharedCalendar() {
        //new user
        val userId = userService.createUser(UserEntity("test@moo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_FAMILIE)))!!

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        //creating 3 new calendars
        createCalendars(userId, cookie)

        val userId2 = userService.createUser(UserEntity("test@foo.no", "Foo", "Bar",
                "Foobar58", "1234", setOf(Role.ROLE_FAMILIE)))!!

        val cookie2 = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@foo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie2)
                .body(CalendarDto("Maroi", userId2))
                .post("/calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        val calendarId = location.split("/")[1].toLong()

        //Add access
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie2)
                .queryParam("shareWith", userId)
                .post("/calendars/$calendarId")
                .then()
                .statusCode(204)

        val sizeOfCalendars = given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        assertEquals(5, sizeOfCalendars)

        //Remove access
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie2)
                .queryParam("shareWith", userId)
                .post("/calendars/$calendarId")
                .then()
                .statusCode(204)

        val sizeOfCalendars2 = given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        assertEquals(4, sizeOfCalendars2)
    }

    @Test
    fun getAllTest() {
        // CREATING USER
        val createdUserUrl = RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("users")
                .then()
                .statusCode(201)
                .extract().header("location")

        //GETTING THE USER ID
        val userId = createdUserUrl.split("/")[1].toLong()

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        //CREATING CALENDAR
        val createdCalenderUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("testCalendar", userId))
                .post("/calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        //GETTING THE CALENDAR ID
        val calendarId = createdCalenderUrl.split("/")[1].toLong()

        val note = "Reason"
        val time = LocalDateTime.now()

        //CREATING ABSENCES
        for (i in 0L..4L) {
            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(AbsenceDto(note + i.toString(), time.plusDays(i), calendarId))
                    .post("/calendars/$calendarId/absences")
                    .then()
                    .statusCode(201)
        }

        val levels = arrayOf(Level.HIGH, Level.LOW, Level.MODERATE)

        val types = arrayOf("Cat", "Mouse", "Dog")

        //CREATING ANIMALS
        for (i in 0..2) {
            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(AnimalDto(types[i], levels[i], "some notes", time.plusDays(i.toLong())))
                    .post("calendars/$calendarId/animals")
                    .then()
                    .statusCode(201)
        }

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/calendars")
                .then()
                .statusCode(200)
                .body("data.totalElements", CoreMatchers.equalTo(1))
    }
}

