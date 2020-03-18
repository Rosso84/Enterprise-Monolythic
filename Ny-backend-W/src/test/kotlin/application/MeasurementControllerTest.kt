package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.MeasurementDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.MeasurementRepository
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
class MeasurementControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var weightRepository: MeasurementRepository

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
        weightRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        weightRepository.deleteAll()
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

        val weightKgs = 40
        val weightGrams = 40
        val heightCm = 110
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating measurement
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(weightGrams, weightKgs, heightCm, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/measurements")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //confirm by checking if e.g chosen height is correct
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/measurements/$id")
                .then()
                .statusCode(200)
                .body("data.heightCm", CoreMatchers.equalTo(heightCm))
    }

    /**@Updates a existing measurement registered*/
    @Test
    fun testUpdate() {
        val idList = createDefaultParentAndReturnIds()

        val weightKgs = 40
        val weightGrams = 40
        val heightCm = 110
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        //Creating measurement
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(weightGrams, weightKgs, heightCm, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/measurements")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        val newWeightKgs = 60
        val newWeightGrams = 50
        val newHeightCm = 120

        //update with new and should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(newWeightGrams, newWeightKgs, newHeightCm, LocalDateTime.now(), calendarId))
                .put("calendars/$calendarId/measurements/$id")
                .then()
                .statusCode(204)
    }


    /** @Deletes all registered */
    @Test
    fun testDeleteById() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val weightKgs = 40
        val weightGrams = 40
        val heightCm = 110

        //Creating measurement
        val location = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(weightGrams, weightKgs, heightCm, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/measurements")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = location.split("/")[3].toLong()

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/measurements")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(numberOfRegistered, 1)

        //delete 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/measurements/$id")
                .then()
                .statusCode(204)

        //delete again and should be statuskode 404
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/measurements/$id")
                .then()
                .statusCode(404)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/measurements")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has no registered left
        Assertions.assertTrue(newNumberOfRegistered == 0)
    }


    @Test
    fun testCreateTwiceAndSizeIsOne() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val weightKilos = 40
        val weightGrams = 40
        val heightCm = 110

        //Creating measurement
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(weightGrams, weightKilos, heightCm, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/measurements")
                .then()
                .statusCode(201)

        //get all attached to 1 calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/measurements")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        Assertions.assertEquals(numberOfRegistered, 1)

        val newWeigtKilo = 67
        //create again
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(MeasurementDto(weightGrams, newWeigtKilo, heightCm, LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/measurements")
                .then()
                .statusCode(400)

        val newNumberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/measurements")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has still 1 registered
        Assertions.assertTrue(newNumberOfRegistered == 1)
    }
}