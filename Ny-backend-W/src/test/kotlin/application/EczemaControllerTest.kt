package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.EczemaDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.*
import application.repositories.EczemaRepository
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class EczemaControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var eczemaRepository: EczemaRepository

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
        eczemaRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        eczemaRepository.deleteAll()
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
    fun testCreateAndGetEczemaByID() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()

        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        //Creating Eczema
        val createdEczemaUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem", Level.MODERATE, "eggwrhrhwr", LocalDateTime.now(), BodySide.BACK,
                        BodyPortion.MIDDLE, BodyPart.LOWER_SPINE, calendarId))
                .post("calendars/$calendarId/eczemas")
                .then()
                .statusCode(201)
                .extract().header("location")

        val eczemaId = createdEczemaUrl.split("/")[3].toLong()

        //confirm by checking if eczema type is correct using getById
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas/$eczemaId")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo("Eksem"))
    }

    /**@Tests if several created is received with getAll*/
    @Test
    fun testGetAll() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        //Creating Eczema
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem", Level.MODERATE, "eggwrhrhwr", LocalDateTime.now(), BodySide.BACK,
                        BodyPortion.MIDDLE, BodyPart.LOWER_SPINE, calendarId))
                .post("calendars/$calendarId/eczemas")
                .then()
                .statusCode(201)
                .extract().header("location")

        //Creating Eczema
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem2", Level.MODERATE, "eggwrhrhwr", LocalDateTime.now(), BodySide.BACK,
                        BodyPortion.MIDDLE, BodyPart.MIDDLE_SPINE, calendarId))
                .post("calendars/$calendarId/eczemas")
                .then()
                .statusCode(201)
                .extract().header("location")

        val numberOfEczemasRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        assertEquals(numberOfEczemasRegistered, 2)
    }

    /** @Deletes all animals registered */
    @Test
    fun testDeleteById() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val createdEczemaUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem2", Level.MODERATE, "eggwrhrhwr", LocalDateTime.now(), BodySide.BACK,
                        BodyPortion.MIDDLE, BodyPart.MIDDLE_SPINE, calendarId))
                .post("calendars/$calendarId/eczemas")
                .then()
                .statusCode(201)
                .extract().header("location")

        val eczemaId = createdEczemaUrl.split("/")[3].toLong()

        val numberOfEczemasRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1 eczema
        assertTrue(numberOfEczemasRegistered == 1)

        //delete 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/eczemas/$eczemaId")
                .then()
                .statusCode(204)

        //delete again statuskode 404 , should not exist anymore
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/eczemas/$eczemaId")
                .then()
                .statusCode(404)
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
        val bodysideBack = BodySide.BACK
        val bodysideFront = BodySide.FRONT
        val time = LocalDateTime.now()


        //Creating
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem", Level.MODERATE, note, time,
                        bodysideBack, BodyPortion.MIDDLE, BodyPart.LOWER_SPINE, calendarId))
                .post("calendars/$calendarId/eczemas")
                .then()
                .statusCode(201)
                .extract().header("location")

        val eczemaId = location.split("/")[3]

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        assertEquals(numberOfRegistered, 1)

        //update with e.g newNote and bodysideFront
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(EczemaDto("Eksem", Level.MODERATE, newNote, time,
                        bodysideFront, BodyPortion.MIDDLE, BodyPart.LOWER_SPINE, calendarId))
                .put("calendars/$calendarId/eczemas/$eczemaId")
                .then()
                .statusCode(204)
                .extract().header("location")

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has still 1 registered
        assertEquals(newNumberOfRegistered, 1)

        //confirm note has been replaced with new
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/eczemas/$eczemaId")
                .then()
                .statusCode(200)
                .body("data.note", CoreMatchers.equalTo(newNote))
                .body("data.bodySide", CoreMatchers.equalTo(bodysideFront.name))
    }
}