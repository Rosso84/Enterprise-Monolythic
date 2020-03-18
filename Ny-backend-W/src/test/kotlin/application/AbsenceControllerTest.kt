package application

import application.controllers.dtos.AbsenceDto
import application.controllers.dtos.CalendarDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.AbsenceRepository
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
class AbsenceControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var absenceRepository: AbsenceRepository

    @Autowired
    private lateinit var userService: UserService

    @BeforeAll
    fun setUp() {
        // RestAssured configs shared by all the tests
        RestAssured.baseURI = "https://localhost/"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @BeforeEach
    fun reset() {
        absenceRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        absenceRepository.deleteAll()
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

        //userID = 0  calendarId = 1 dateId = 2
        val idList = createDefaultParentAndReturnIds()


        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val note = "Reason for absence"
        val time = LocalDateTime.now()

        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(note, time, calendarId))
                .post("calendars/$calendarId/absences")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //confirm by checking if e.g chosen note is equal
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences/$id")
                .then()
                .statusCode(200)
                .body("data.note", CoreMatchers.equalTo(note))
    }


    /** @Deletes a registered Absence by id*/
    @Test
    fun testDeleteById() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val note = "Reason for absence"
        val time = LocalDateTime.now()

        //Creating measurement
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(note, time, calendarId))
                .post("calendars/$calendarId/absences")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(numberOfRegistered, 1)

        //delete  and 204 statuskode returns
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/absences/$id")
                .then()
                .statusCode(204)

        //delete again and should be statuskode 404
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/absences/$id")
                .then()
                .statusCode(404)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has no registered left
        Assertions.assertTrue(newNumberOfRegistered == 0)
    }


    @Test
    fun testUpdateWithPut() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val note = "Reason for absence"
        val newNote = "Another reason for absence"
        val time = LocalDateTime.now()

        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(note, time, calendarId))
                .post("calendars/$calendarId/absences")
                .then()
                .statusCode(201)
                .extract().header("location")

        val absenceId = location.split("/")[3]

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(numberOfRegistered, 1)

        //updating
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(newNote, time, calendarId))
                .put("calendars/$calendarId/absences/$absenceId")
                .then()
                .statusCode(204)
                .extract().header("location")

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has still 1 registered
        Assertions.assertEquals(newNumberOfRegistered, 1)

        //confirm note has been replaced with new
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences/$absenceId")
                .then()
                .statusCode(200)
                .body("data.note", CoreMatchers.equalTo(newNote))
    }


    @Test
    fun testGetAll() {
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

        //CREATING CALENDAR
        val createdCalenderUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("testCalendar", userId.toLong()))
                .post("calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        //GETTING THE CALENDAR ID
        val calendarId = createdCalenderUrl.split("/")[1].toLong()

        val time = LocalDateTime.now()
        val note = "Reason"

        //CREATING ABSENCES
        for (i in 0L..4L) {
            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(AbsenceDto(note + i.toString(), time.plusDays(i), calendarId))
                    .post("calendars/$calendarId/absences")
                    .then()
                    .statusCode(201)
        }

        //extract the number  of created
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/absences")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm 5 is created
        Assertions.assertEquals(5, numberOfRegistered)
    }


    //TODO:  fails
    /*
    @Test
    fun testCreateInTwoDatesAndDeleteOne() {
        //userID = 0  calendarId = 1 dateId = 2 cookie = 3
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[3]

        val note1 = "Reason for absence"
        val note2 = "Another reason for absence"

        val timestamp = LocalTime.now()

        val date1 = idList[2].toLong()
        val date2 = LocalDate.now().minusDays(2.toLong())

        //register absence in first date
        val firstAbsenseLocation = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(note1, timestamp, date1))
                .post("users/$userId/calendars/$calendarId/dates/$date1/absenses")
                .then()
                .statusCode(201)
                .extract().header("location")


        //CREATING a second date
        val createdSecondDateUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(DateDTO(date2, calendarId))
                .post("users/$userId/calendars/$calendarId/dates")
                .then()
                .statusCode(201)
                .extract().header("location")

        val secondDateId = createdSecondDateUrl.split("/")[5]

        //register absence in secondDate
        val secondAbsenseLocation = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AbsenceDto(note2, timestamp, date1))
                .post("users/$userId/calendars/$calendarId/dates/$secondDateId/absenses")
                .then()
                .statusCode(201)
                .extract().header("location")

        val secondAbsenseId = secondAbsenseLocation.split("/")[7]

        //Delete second
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("users/$userId/calendars/$calendarId/dates/$secondDateId/absenses/$secondAbsenseId")
                .then()
                .statusCode(204)


        //confirm absence is removed in second date
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("users/$userId/calendars/$calendarId/dates/$secondDateId/absenses")
                .then()
                .statusCode(200)
                .body("data.totalElements", CoreMatchers.equalTo(0))


        //confirm first still exists
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("users/$userId/calendars/$calendarId/dates/$date1/absenses")
                .then()
                .statusCode(200)
                .body("data.totalElements", CoreMatchers.equalTo(1))
}

*/


}