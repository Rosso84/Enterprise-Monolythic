package application

import application.controllers.dtos.CalendarDto
import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    private var port = 0

    @BeforeAll
    fun initialize() {
        RestAssured.baseURI = "https://localhost"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @BeforeEach
    fun clean() {
        userService.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        userService.deleteAll()
    }

    @Test
    fun testCreateUser() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)
    }

    @Test
    fun testFailDoubleRegistration() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)

        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(400)
    }

    @Test
    fun testGetSingle() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val createdUser = userService.findUserByEmail("test@moo.no")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/" + createdUser?.id)
                .then()
                .statusCode(200)
                .body("data.email", CoreMatchers.equalTo("test@moo.no"))
                .body("data.firstName", CoreMatchers.equalTo("Foo"))
                .body("data.lastName", CoreMatchers.equalTo("Bar"))
                .body("data.password", CoreMatchers.not("Foobar58"))
                .body("data.pin", CoreMatchers.not("1234"))
                .body("data.roles[0]", CoreMatchers.equalTo("ROLE_BRUKER"))
    }

    @Test
    fun testGetAll() {
        for (index in 0..18) {
            RestAssured.given().contentType(ContentType.JSON)
                    .body(UserDTO("test@moo$index.no", "Foo$index", "Bar$index", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                    .post("/users")
                    .then()
                    .statusCode(201)
        }

        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_ADMIN)))
                .post("/users")
                .then()
                .statusCode(201)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users")
                .then()
                .statusCode(200)
                .body("data.totalElements", CoreMatchers.equalTo(20))
                .body("data.content.size", CoreMatchers.equalTo(10))
                .body("data.content[9].firstName", CoreMatchers.equalTo("Foo9"))
    }

    @Test
    fun testDeleteUser() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_ADMIN)))
                .post("/users")
                .then()
                .statusCode(201)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val size = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users")
                .then()
                .statusCode(200)
                .extract().path<Int>("data.totalElements")

        val userId = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("data.content", UserDTO::class.java)[size - 1].id

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("/users/$userId")
                .then()
                .statusCode(204)

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users")
                .then()
                .statusCode(200)
                .body("data.totalElements", CoreMatchers.equalTo(size - 1))
    }


    @Test
    fun testUpdateUser() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val createdUser = userService.findUserByEmail("test@moo.no")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/" + createdUser?.id)
                .then()
                .statusCode(200)
                .body("data.email", CoreMatchers.equalTo("test@moo.no"))
                .body("data.firstName", CoreMatchers.equalTo("Foo"))
                .body("data.lastName", CoreMatchers.equalTo("Bar"))
                .body("data.roles[0]", CoreMatchers.equalTo("ROLE_BRUKER"))

        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(UserDTO("test@caw.no", "Caw", "Meow", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .put("/users/" + createdUser?.id)
                .then()
                .statusCode(204)

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/" + createdUser?.id)
                .then()
                .statusCode(200)
                .body("data.email", CoreMatchers.equalTo("test@caw.no"))
                .body("data.firstName", CoreMatchers.equalTo("Caw"))
                .body("data.lastName", CoreMatchers.equalTo("Meow"))
                .body("data.roles[0]", CoreMatchers.equalTo("ROLE_BRUKER"))
    }

    @Test
    fun testAddCalendarToSpecificUser() {
        val createdUserUrl = RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)
                .extract().header("location")

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val userId = createdUserUrl.split("/")[1].toLong()

        var amountOfCalendars = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/$userId")
                .then()
                .statusCode(200)
                .extract().path<Int>("data.calendars.size")

        assertEquals(0, amountOfCalendars)

        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("testCalendar", userId))
                .post("calendars")
                .then()
                .statusCode(201)

        amountOfCalendars = RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/$userId")
                .then()
                .statusCode(200)
                .extract().path<Int>("data.calendars.size")

        assertEquals(1, amountOfCalendars)
    }

    /**@Tests if when deleting a user all calendars are removed along
     * with it with cascadeType ALL annotation*/
    @Test
    fun testCascadeDeleteOnCalendar() {
        val createdUserUrl = RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@moo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)
                .extract().header("location")

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@moo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val userId = createdUserUrl.split("/")[1].toLong()

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/$userId")
                .then()
                .statusCode(200)

        val createdCalenderUrl = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .body(CalendarDto("testCalendar", userId))
                .post("calendars")
                .then()
                .statusCode(201)
                .extract().header("location")

        val calendarId = createdCalenderUrl.split("/")[1].toLong()

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId")
                .then()
                .statusCode(200)
                .body("data.calendarName", CoreMatchers.equalTo("testCalendar"))

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .delete("/users/$userId")
                .then()
                .statusCode(204)

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("calendars/$calendarId")
                .then()
                .statusCode(401)
                .body("data", CoreMatchers.equalTo(null))
    }
}