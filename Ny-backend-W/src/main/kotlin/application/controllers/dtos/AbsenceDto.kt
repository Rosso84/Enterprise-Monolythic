package application.controllers.dtos

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

data class AbsenceDto(

        @ApiModelProperty("Description for reason of absence")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the registered absence")
        var id: String? = null
)