package application.controllers.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Sleep period details")
data class SleepDto(

        @ApiModelProperty("Timestamp of sleep start")
        var fromTimestamp: LocalDateTime,

        @ApiModelProperty("Timestamp of sleep end")
        var toTimestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of this Sleep-period registered")
        var id: String? = null

)