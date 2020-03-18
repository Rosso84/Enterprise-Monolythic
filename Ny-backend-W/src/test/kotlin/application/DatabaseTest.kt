package application

import application.entities.UserEntity
import application.enums.Role
import application.repositories.UserRepository
import application.services.UserService
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {

    @Autowired
    private lateinit var userHandler: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder


    @BeforeEach
    fun clean() {
        userRepository.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        userRepository.deleteAll()
    }

    @Test
    fun testCreateUser() {

        val email = "test@test.no"
        val firstName = "foo"
        val lastName = "bar"
        val pwd = "bar"
        val pin = "1234"
        val role = Role.ROLE_BRUKER

        val id = userHandler.createUser(UserEntity(email, firstName, lastName, pwd, pin, setOf(role)))

        assertTrue(id != null)

        val user = userRepository.findById(id!!).get()

        assertNotEquals(pwd, user.password)

        assertTrue(passwordEncoder.matches(pwd, user.password))
    }
}