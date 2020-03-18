package application

import application.controllers.dtos.*
import application.enums.Level
import application.enums.Role
import application.repositories.FoodRepository
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
class FoodControllerTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var foodRepository: FoodRepository

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
        foodRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        foodRepository.deleteAll()
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
        val type = "Apple"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        //Creating Animal
        val createdFoodUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val foodId = createdFoodUrl.split("/")[3].toLong()

        //confirm by checking if e.g chosen foodtype is correct
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods/$foodId")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type))
    }

    /**@Tests if several created is created and all data is received with getAll*/
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

        val type1 = "apple"
        val type2 = "broccoli"
        val type3 = "nuts"

        val time = LocalDateTime.now()


        //Creating
        val createFirstFoodUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(AnimalDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val foodId1 = createFirstFoodUrl.split("/")[3].toLong()

        //Verify  is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods/$foodId1")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type1))

        //Creating second
        val createdSecondAnimalUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(type2, level2, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val foodId2 = createdSecondAnimalUrl.split("/")[3].toLong()

        //Verify second is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods/$foodId2")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type2))
                .body("data.level", CoreMatchers.equalTo(level2.name))

        //Creating third
        val createdThirdFoodUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(type3, level3, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val foodId3 = createdThirdFoodUrl.split("/")[3].toLong()

        //Verify third is created
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods/$foodId3")
                .then()
                .statusCode(200)
                .body("data.type", CoreMatchers.equalTo(type3))
                .body("data.level", CoreMatchers.equalTo(level3.name))

        //get all food is attached to 1 date, calendar and 1 userID
        val numberOfRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 3 animals
        assertEquals(numberOfRegistered, 3)
    }


    /**@Updates a existing food registered*/
    @Test
    fun testUpdate() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()

        val level = Level.HIGH
        val type = "tomatoes"
        val time = LocalDateTime.now()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        //Creating Animal
        val createdFoodlUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(type, level, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val id = createdFoodlUrl.split("/")[3].toLong()

        val newType = "oranges"
        val newLevel = Level.HIGH
        val newNotes = "som other notes"

        //update with new and should return 204
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(newType, newLevel, newNotes, time, calendarId))
                .put("calendars/$calendarId/foods/$id")
                .then()
                .statusCode(204)
    }

    /** @Deletes all registered food*/
    @Test
    fun testDeleteById() {
        //userID = 0  calendarId = 1 cookie = 2
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val level1 = Level.HIGH
        val type1 = "Carrot"
        val time = LocalDateTime.now()

        //Creating Animal
        val createFirstFoodUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(FoodDto(type1, level1, "some notes", time, calendarId))
                .post("calendars/$calendarId/foods")
                .then()
                .statusCode(201)
                .extract().header("location")

        val foodId = createFirstFoodUrl.split("/")[3].toLong()

        //get all animals attached to 1 calendar attachd to 1 userID
        val numberOfFoodsRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/foods")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 1
        assertTrue(numberOfFoodsRegistered == 1)

        //delete 204 statuskode
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/foods/$foodId")
                .then()
                .statusCode(204)

        //delete again with statuskode 404 , should not exist anymore
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("calendars/$calendarId/foods/$foodId")
                .then()
                .statusCode(404)
    }
}