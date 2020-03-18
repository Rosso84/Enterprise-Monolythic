package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.PollenDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Level
import application.enums.Role
import application.repositories.PollenRepository
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
class PollenControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var pollenRepository: PollenRepository

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
        pollenRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        pollenRepository.deleteAll()
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

        val level = Level.HIGH
        val type = "pollenType"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating Animal
        val createdFoodUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = createdFoodUrl.split("/")[3].toLong()

        //confirm by checking if e.g chosen type is correct
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens/$id")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type))
    }

    /**@Updates a existing pollen registered*/
    @Test
    fun testUpdate() {
        val idList = createDefaultParentAndReturnIds()

        val level = Level.HIGH
        val type = "Grass"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating
        val createdUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = createdUrl.split("/")[3].toLong()

        val newType = "Hazel"
        val newLevel = Level.HIGH
        val newNotes = "som other notes"

        //update with new and should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(newType, newLevel, newNotes, time, calendarId))
                .put("calendars/$calendarId/pollens/$id")
                .then()
                .statusCode(204)
    }

    /**@Tests if several created and all data is received with getAll*/
    @Test
    fun testGetAll() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val level1 = Level.HIGH
        val level2 = Level.LOW
        val level3 = Level.MODERATE

        val type1 = "pollentype1"
        val type2 = "pollentype2"
        val type3 = "pollentype3"

        val time = LocalDateTime.now()

        //Creating
        val createFirstPollenUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id1 = createFirstPollenUrl.split("/")[3].toLong()

        //Verify its created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens/$id1")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type1))

        //Creating second
        val createdSecondPollenUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type2, level2, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id2 = createdSecondPollenUrl.split("/")[3].toLong()

        //Verify second is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens/$id2")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type2))
                .body("data.level", CoreMatchers.equalTo(level2.name))

        //Creating third
        val createdThirdPollenUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type3, level3, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id3 = createdThirdPollenUrl.split("/")[3].toLong()

        //Verify third is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens/$id3")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type3))
                .body("data.level", CoreMatchers.equalTo(level3.name))

        //get all pollen attached to a calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 3 animals
        Assertions.assertEquals(numberOfRegistered, 3)
    }


    /** @Deletes all registered */
    @Test
    fun testDeleteById() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val level1 = Level.HIGH
        val type1 = "pollentype"
        val time = LocalDateTime.now()

        //Creating
        val createFirstUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(PollenDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/pollens")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = createFirstUrl.split("/")[3].toLong()

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/pollens")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertTrue(numberOfRegistered == 1)

        //delete 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/pollens/$id")
                .then()
                .statusCode(204)

        //delete again and should be statuskode 404
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/pollens/$id")
                .then()
                .statusCode(404)
    }
}