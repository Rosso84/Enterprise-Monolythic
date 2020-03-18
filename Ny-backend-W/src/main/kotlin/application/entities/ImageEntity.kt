package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "IMAGE")
class ImageEntity(

        @get:NotBlank
        @get:Size(max = 64)
        var fileName: String,

        @get:NotBlank
        @get:Size(max = 8)
        var fileType: String,

        @get:Lob
        @get:NotNull
        var data: ByteArray,

        @get:NotNull
        var timestamp: LocalDateTime,

        @get:ManyToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "IMAGE_ID")
        var id: Long? = null
)