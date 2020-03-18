package application

import application.wrapped_response.RestResponseFactory
import application.wrapped_response.WrappedResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class ExceptionResolver {

    @ExceptionHandler(value = [UsernameNotFoundException::class, BadCredentialsException::class, HttpMessageNotReadableException::class])
    fun handleUsernameNotFoundException(request: HttpServletRequest, exception: Exception): ResponseEntity<WrappedResponse<Void>> {
        return RestResponseFactory.userFailure(exception.message!!)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(request: HttpServletRequest, exception: Exception): ResponseEntity<WrappedResponse<Void>> {
        return RestResponseFactory.error(exception.message!!)
    }

}