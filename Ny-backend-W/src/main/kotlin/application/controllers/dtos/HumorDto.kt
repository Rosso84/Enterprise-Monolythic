package application.controllers.dtos

import application.enums.Level
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime


@ApiModel("Humor details")
data class HumorDto(

        @ApiModelProperty("Type of humor")
        var type: String? = null,

        @ApiModelProperty("level of how strong")
        var level: Level? = null,

        @ApiModelProperty("Note for description")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of this humor")
        var id: String? = null

)
