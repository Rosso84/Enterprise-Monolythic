package application.controllers

import application.CustomUser
import application.CustomUserDetailsService
import application.MyLogger
import application.controllers.dtos.SignInDTO
import application.wrapped_response.RestResponseFactory
import application.wrapped_response.WrappedResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.passay.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.regex.Pattern

@RestController
@Api(description = "Api for validating a session")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
class SignInController(
        private val authenticationManager: AuthenticationManager,
        private val userDetailsService: CustomUserDetailsService,
        private val myLogger: MyLogger
) {

    @ApiOperation("Validate a session by email and password")
    @PostMapping(path = ["/signIn"])
    fun signIn(
            @ApiParam("Payload to sign in with")
            @RequestBody
            dto: SignInDTO
    ): ResponseEntity<WrappedResponse<Void>> {

        val emailPattern = Pattern.compile("^(.+)@(.+).(.+)$")
        if (!emailPattern.matcher(dto.email).find()) {
            myLogger.logWarning(null, "/signIn", HttpMethod.POST, "Email is not formatted properly")
            return RestResponseFactory.userFailure("Email is not formatted properly")
        }

        if (!checkPasswordInput(dto.password)) {
            myLogger.logWarning(null, "/signIn", HttpMethod.POST, "Password does not meet requirements." +
                    " Password must be a minimum of 8 characters, have at least one upper and lowercase letter and contain a numeric letter")
            return RestResponseFactory.userFailure("Password does not meet requirements. Password must be a " +
                    "minimum of 8 characters, have at least one upper and lowercase letter and contain a numeric letter")
        }

        val userDetails = userDetailsService.loadUserByUsername(dto.email)
        val token = UsernamePasswordAuthenticationToken(userDetails, dto.password, userDetails.authorities)

        authenticationManager.authenticate(token)

        if (token.isAuthenticated) {
            SecurityContextHolder.getContext().authentication = token
            myLogger.logInfo(null, "/signIn", HttpMethod.POST, "User logged in")
            return RestResponseFactory.noPayload(200)
        }
        myLogger.logInfo(null, "/signIn", HttpMethod.POST, "Could not sign in with provided credentials")
        return RestResponseFactory.userFailure("Could not sign in with provided credentials")
    }

    private fun checkPasswordInput(password: String): Boolean {
        val validator = PasswordValidator(Arrays.asList(
                // between 8 and 30 chars
                LengthRule(8, 30),

                // at least one upper-case character
                CharacterRule(EnglishCharacterData.UpperCase, 1),

                // at least one lower-case character
                CharacterRule(EnglishCharacterData.LowerCase, 1),

                // at least one digit character
                CharacterRule(EnglishCharacterData.Digit, 1)

        ))
        val result = validator.validate(PasswordData(password))
        return result.isValid
    }

    @GetMapping(path = ["/testResource"])
    fun resource(
            @AuthenticationPrincipal customUser: CustomUser?
    ): ResponseEntity<WrappedResponse<String>> {
        return RestResponseFactory.payload(200, "The Resource")
    }
}