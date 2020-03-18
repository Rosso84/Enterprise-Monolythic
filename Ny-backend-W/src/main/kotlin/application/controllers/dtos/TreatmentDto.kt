package application.controllers.dtos

import application.enums.Level
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Treatment details")
data class TreatmentDto(

        @ApiModelProperty("Type of Treatment")
        var type: String? = null,

        @ApiModelProperty("How much/level of Treatment")
        var level: Level? = null,

        @ApiModelProperty("Note for description")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the Treatment registered")
        var id: String? = null

)