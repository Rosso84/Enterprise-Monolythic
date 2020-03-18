package application

import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.repositories.UserRepository
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.containsString
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

class SignInControllerTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    private var port = 0

    @BeforeEach
    fun clean() {
        userRepository.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        userService.deleteAll()
    }

    @BeforeAll
    fun initialize() {
        RestAssured.baseURI = "https://localhost"
        RestAssured.port = port
        RestAssured.useRelaxedHTTPSValidation()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun testUnauthorizedAccess() {
        RestAssured.given().accept("${ContentType.TEXT},*/*")
                .get("/resource")
                .then()
                .statusCode(401)
    }

    @Test
    fun testSignIn() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@foo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)

        RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@foo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .header("Set-Cookie", containsString("JSESSIONID"))
                .header("Set-Cookie", containsString("HttpOnly"))
    }

    @Test
    fun testSignInWithNonExistingUser() {
        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@error.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(400)
                .extract().cookie("JSESSIONID")

        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users")
                .then()
                .statusCode(401)
    }

    @Test
    fun testAccessCookie() {

        RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@foo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@foo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        val response = RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/testResource")
                .then()
                .statusCode(200)
                .extract().path<String>("data")

        assertEquals("The Resource", response)
    }
}