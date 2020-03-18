package application.controllers.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("Weight and hight details")
data class MeasurementDto(

        @ApiModelProperty("The weight in grams")
        var weightGrams: Int? = null,

        @ApiModelProperty("The weight in Kilos")
        var weightKilos: Int? = null,

        @ApiModelProperty("The height in centimeters")
        var heightCm: Int? = null,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("Id of the Calendar")
        var id: String? = null

)
