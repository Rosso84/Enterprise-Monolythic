package application.controllers.dtos

import io.swagger.annotations.ApiModelProperty

data class SignInDTO(

        @ApiModelProperty("Unique email of a user")
        var email: String,

        @ApiModelProperty("Password of a user")
        var password: String
)

