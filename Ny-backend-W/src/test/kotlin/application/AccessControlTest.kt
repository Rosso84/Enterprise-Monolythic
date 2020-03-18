package application

import application.controllers.dtos.SignInDTO
import application.controllers.dtos.UserDTO
import application.enums.Role
import application.services.UserService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessControlTest {

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    private var port = 0

    @BeforeEach
    fun clean() {
        userService.deleteAll()
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
    fun testSingleUserAccess() {
        val createdUserUrl = RestAssured.given().contentType(ContentType.JSON)
                .body(UserDTO("test@foo.no", "Foo", "Bar", "Foobar58", "1234", setOf(Role.ROLE_BRUKER)))
                .post("/users")
                .then()
                .statusCode(201)
                .extract().header("location")

        val userId = createdUserUrl.split("/")[1]

        RestAssured.given().accept(ContentType.JSON)
                .get("/users/$userId")
                .then()
                .statusCode(401)

        val cookie = RestAssured.given().contentType(ContentType.JSON)
                .body(SignInDTO("test@foo.no", "Foobar58"))
                .post("/signIn")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", cookie)
                .get("/users/$userId")
                .then()
                .statusCode(200)
    }
}