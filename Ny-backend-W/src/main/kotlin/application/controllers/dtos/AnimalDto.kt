package application.controllers.dtos

import application.enums.Level
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Animal details")
data class AnimalDto(

        @ApiModelProperty("Type of animal")
        var type: String? = null,

        @ApiModelProperty("How much/level of contact")
        var level: Level? = null,

        @ApiModelProperty("Note for description")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the Calendar")
        var id: String? = null

)
