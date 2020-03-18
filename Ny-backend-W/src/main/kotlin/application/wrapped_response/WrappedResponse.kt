package application.wrapped_response

import io.swagger.annotations.ApiModelProperty

open class WrappedResponse<T>(

        @ApiModelProperty("The HTTP status code of the response")
        var code: Int? = null,

        @ApiModelProperty("The wrapped payload")
        var data: T? = null,

        @ApiModelProperty("Error message in case where was an error")
        var message: String? = null,

        @ApiModelProperty("String representing either 'success', user error ('error') or server failure ('fail')")
        var status: ResponseStatus? = null
) {

    /**
     * Method to check if "status" and "code" matches
     * Will set the "status" if missing, based on "code".
     *
     * @throws IllegalStateException if validation fails
     */
    fun validated(): WrappedResponse<T> {

        val statusCode: Int = code ?: throw IllegalStateException("Missing HTTP code")

        if (statusCode !in 100..599) {
            throw  IllegalStateException("Invalid HTTP code: $code")
        }

        if (status == null) {
            status = when (statusCode) {
                in 100..399 -> ResponseStatus.SUCCESS
                in 400..499 -> ResponseStatus.ERROR
                in 500..599 -> ResponseStatus.FAIL
                else -> throw  IllegalStateException("Invalid HTTP code: $code")
            }
        } else {
            val wrongSuccess = (status == ResponseStatus.SUCCESS && statusCode !in 100..399)
            val wrongError = (status == ResponseStatus.ERROR && statusCode !in 400..499)
            val wrongFail = (status == ResponseStatus.FAIL && statusCode !in 500..599)

            val wrong = wrongSuccess || wrongError || wrongFail
            if (wrong) {
                throw IllegalArgumentException("Status $status is not correct for HTTP code $statusCode")
            }
        }

        if (status != ResponseStatus.SUCCESS && message == null) {
            throw IllegalArgumentException("Failed response, but with no describing 'message' for it")
        }

        return this
    }
}
