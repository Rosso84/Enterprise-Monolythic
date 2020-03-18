package application.entities


import application.enums.Level
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@MappedSuperclass
abstract class Common(

        @get:NotBlank
        @get:Size(max = 256)
        var type: String,

        @get:NotNull
        var level: Level,

        @get:Size(max = 512)
        var note: String,

        @get:NotNull
        var timestamp: LocalDateTime,

        @get:ManyToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity
)
