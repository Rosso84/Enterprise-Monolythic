package application.controllers.dtos

import application.enums.BodyPart
import application.enums.BodyPortion
import application.enums.BodySide
import application.enums.Level
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

class EczemaDto(

        @ApiModelProperty("What type of eczema one got")
        var type: String,

        @ApiModelProperty("The level of the outbreak")
        var level: Level,

        @ApiModelProperty("Place to add extra notes")
        var note: String,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("On what side of the body the outbreak is on, front or back")
        var bodySide: BodySide,

        @ApiModelProperty("On what body portion the eczema is located, left, middle or right side ")
        var bodyPortion: BodyPortion,

        @ApiModelProperty("The specific body part the eczema is on")
        var bodyPart: BodyPart,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of this specific eczema registration")
        var id: String? = null
)