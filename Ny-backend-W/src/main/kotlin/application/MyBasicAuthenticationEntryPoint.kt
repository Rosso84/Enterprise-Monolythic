package application

import application.wrapped_response.RestResponseFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class MyBasicAuthenticationEntryPoint : BasicAuthenticationEntryPoint() {

    @Throws(IOException::class, ServletException::class)
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        response.addHeader("WWW-Authenticate", "Basic realm=$realmName")
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"

        val jsonError = RestResponseFactory.unauthorized<Void>(authException.localizedMessage)
        val jackson = jacksonObjectMapper()
        val jsonOutput = jackson.writeValueAsString(jsonError.body)

        val writer = response.writer
        writer.println(jsonOutput)
    }

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        realmName = "RegIT"
        super.afterPropertiesSet()
    }

}