package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "ABSENCE")
class AbsenceEntity(

        @get:Size(max = 512)
        var note: String,

        @NotNull
        var timestamp: LocalDateTime,

        @get:OneToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity,

        @get: Id @get: GeneratedValue
        @get:Column(name = "ABSENCE_ID")
        var id: Long? = null
)
