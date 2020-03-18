package application.controllers.dtos

import application.enums.Level
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime


@ApiModel("Symptom details")
data class SymptomDto(

        @ApiModelProperty("Type of symptom")
        var type: String? = null,

        @ApiModelProperty("how severe?")
        var level: Level? = null,

        @ApiModelProperty("Note for description")
        var note: String? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the registered symptom")
        var id: String? = null
)