package application.controllers.dtos

import application.enums.Level
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Food details")
data class FoodDto(

        @ApiModelProperty("Type of food")
        var type: String? = null,

        @ApiModelProperty("How much/level of intake")
        var level: Level? = null,

        @ApiModelProperty("Note for description")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the animal")
        var id: String? = null
)