package application.controllers.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Testresult details")
data class TestResultDto(

        @ApiModelProperty("The type of test")
        var test: String,

        @ApiModelProperty("The value reference")
        var refValue: String,

        @ApiModelProperty("The actual value")
        var value: String,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of this Sleep-period")
        var id: String? = null

)