package application

import application.controllers.dtos.AnimalDto
import application.controllers.dtos.CalendarDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Level
import application.enums.Role
import application.repositories.AnimalRepository
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
class AnimalControllerTest {


    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var animalRepository: AnimalRepository

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
        animalRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        animalRepository.deleteAll()
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
    fun testCreateAndGetAnimalByID() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()

        val level = Level.HIGH
        val type = "horse"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating Animal
        val createdAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId = createdAnimalUrl.split("/")[3].toLong()

        //confirm by checking if e.g chosen animal type is correct using getById
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals/$animalId")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type))
    }

    /**@Tests if several created animals is created and all data is received with getAll*/
    @Test
    fun testGetAll() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val level1 = Level.HIGH
        val level2 = Level.LOW
        val level3 = Level.MODERATE

        val type1 = "horse"
        val type2 = "Dog"
        val type3 = "Cat"

        val time = LocalDateTime.now()

        //Creating Animal
        val createFirstAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId1 = createFirstAnimalUrl.split("/")[3].toLong()

        //Verify animal is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals/$animalId1")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type1))

        //Creating second Animal
        val createdSecondAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type2, level2, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId2 = createdSecondAnimalUrl.split("/")[3].toLong()

        //Verify second animal is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals/$animalId2")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type2))
                .body("data.level", CoreMatchers.equalTo(level2.name))

        //Creating third Animal
        val createdThirdAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type3, level3, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId3 = createdThirdAnimalUrl.split("/")[3].toLong()

        //Verify third animal is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals/$animalId3")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type3))
                .body("data.level", CoreMatchers.equalTo(level3.name))

        //get all animals attached to 1 calendar attachd to 1 userID
        val numberOfAnimalsRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 3 animals
        assertEquals(numberOfAnimalsRegistered, 3)
    }

    /**@Updates an existing animal registered*/
    @Test
    fun testUpdate() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()

        val level = Level.HIGH
        val type = "horse"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating Animal
        val createdAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId = createdAnimalUrl.split("/")[3].toLong()

        val newType = "Dog"
        val newLevel = Level.HIGH
        val newNotes = "som other notes"

        //update new animal should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(newType, newLevel, newNotes, time, calendarId))
                .put("calendars/$calendarId/animals/$animalId")
                .then()
                .statusCode(204)
    }


    /** @Deletes all animals registered */
    @Test
    fun testDeleteById() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val level1 = Level.HIGH
        val type1 = "horse"
        val time = LocalDateTime.now()

        //Creating Animal
        val createFirstAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/animals")
                .then()
                .statusCode(201)
                .extract().header("location")

        val animalId = createFirstAnimalUrl.split("/")[3].toLong()

        //get all animals attached to 1 calendar attached to 1 userID
        val numberOfAnimalsRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/animals")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1 animal
        assertTrue(numberOfAnimalsRegistered == 1)

        //delete 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/animals/$animalId")
                .then()
                .statusCode(204)

        //delete again statuskode 404 , should not exist anymore
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/animals/$animalId")
                .then()
                .statusCode(404)
    }
}