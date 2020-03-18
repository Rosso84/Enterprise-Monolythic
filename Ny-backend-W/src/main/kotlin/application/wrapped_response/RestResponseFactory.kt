package application.wrapped_response

import org.springframework.http.ResponseEntity
import java.net.URI

object RestResponseFactory {

    fun <T> notFound(message: String): ResponseEntity<WrappedResponse<T>> {

        return ResponseEntity.status(404).body(
                WrappedResponse<T>(code = 404, message = message)
                        .validated())
    }

    fun <T> userFailure(message: String, httpCode: Int = 400): ResponseEntity<WrappedResponse<T>> {

        return ResponseEntity.status(httpCode).body(
                WrappedResponse<T>(code = httpCode, message = message)
                        .validated())
    }

    fun <T> payload(httpCode: Int, data: T): ResponseEntity<WrappedResponse<T>> {

        return ResponseEntity.status(httpCode).body(
                WrappedResponse(code = httpCode, data = data)
                        .validated())
    }

    fun noPayload(httpCode: Int): ResponseEntity<WrappedResponse<Void>> {

        return ResponseEntity.status(httpCode).body(
                WrappedResponse<Void>(code = httpCode).validated())
    }

    fun created(uri: URI): ResponseEntity<WrappedResponse<Void>> {

        return ResponseEntity.created(uri).body(
                WrappedResponse<Void>(code = 201).validated())
    }

    fun <T> unauthorized(message: String = "Unauthorized"): ResponseEntity<WrappedResponse<T>> {
        return ResponseEntity.status(401).body(
                WrappedResponse<T>(code = 401, message = message).validated()
        )
    }

    fun error(message: String, httpCode: Int = 500): ResponseEntity<WrappedResponse<Void>> {
        return ResponseEntity.badRequest().body(
                WrappedResponse<Void>(code = httpCode, message = message).validated()
        )
    }
}