package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.ImageDTO
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.ImageRepository
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

//TODO: continue here

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageControllerTest {

    @Autowired
    private lateinit var imageRepository: ImageRepository

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    private var port = 0

    @BeforeAll
    fun initialize() {
        RestAssured.baseURI = "https://localhost/"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @BeforeEach
    fun reset() {
        imageRepository.deleteAll()
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        imageRepository.deleteAll()
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
    fun testCreateImage() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(ImageDTO("test", ".jpeg", byteArrayOf('P'.toByte(), 'A'.toByte(),
                        'N'.toByte(), 'D'.toByte(), 'A'.toByte()), LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/images")
                .then()
                .statusCode(201)
    }

    @Test
    fun testGetSingle() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]

        val createdImageUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(ImageDTO("test", ".jpeg", byteArrayOf('P'.toByte(), 'A'.toByte(),
                        'N'.toByte(), 'D'.toByte(), 'A'.toByte()), LocalDateTime.now(), calendarId))
                .post("calendars/$calendarId/images")
                .then()
                .statusCode(201)
                .extract().header("location")

        val data = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get(createdImageUrl)
                .then()
                .statusCode(200)
                .extract().path<String>("data.data")

        assertEquals(String(Base64.getDecoder().decode(data)), String(byteArrayOf('P'.toByte(), 'A'.toByte(),
                'N'.toByte(), 'D'.toByte(), 'A'.toByte())))
    }

    @Test
    fun testGetAll() {
        val idList = createDefaultParentAndReturnIds()
        val userId = idList[0].toLong()
        val calendarId = idList[1].toLong()
        val cookie = idList[2]


        var numberOfCreated = 1
        while (numberOfCreated <= 5) {
            RestAssured.given().contentType(ContentType.JSON)
                    .cookie("JSESSIONID", cookie)
                    .body(ImageDTO("test$numberOfCreated", ".jpeg", byteArrayOf('P'.toByte(), 'A'.toByte(),
                            'N'.toByte(), 'D'.toByte(), 'A'.toByte()), LocalDateTime.now(), calendarId))
                    .post("calendars/$calendarId/images")
                    .then()
                    .statusCode(201)

            numberOfCreated++
        }

        //get all images attached to 1 calendar attached to 1 userID
        val numberOfImagesRegistered = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId/images")
                .then()
                .statusCode(200)
                .extract().body().path<Int>("data.numberOfElements")

        //confirm user has registered 5 images
        assertEquals(numberOfImagesRegistered, numberOfCreated - 1)
    }

    @Test
    fun testDeleteImage() {
        //TODO: implementation missing
    }
}