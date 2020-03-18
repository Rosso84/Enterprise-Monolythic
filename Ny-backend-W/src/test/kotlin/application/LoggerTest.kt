package application

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

class LoggerTest {

    @Test
    fun testFormat() {
        val logger = MyLogger()
        println(logger.logError(null, "/users", HttpMethod.GET, "Test"))
    }

}