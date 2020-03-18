package application.controllers.dtos

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

class ImageDTO(

        @ApiModelProperty("The calendarName of an image")
        var fileName: String,

        @ApiModelProperty("The filetype of an image")
        var fileType: String,

        @ApiModelProperty("The actual image, encrypted")
        var data: ByteArray,

        @ApiModelProperty("Timestamp of the day")
        var timestamp: LocalDateTime,

        @ApiModelProperty("The id of the connected calendar")
        var calendar_id: Long? = null,

        @ApiModelProperty("The id of an image")
        var id: String? = null
)