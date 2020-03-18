package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


@Entity
@Table(name = "SLEEP")
class SleepEntity(

        @get:NotNull
        var fromTimestamp: LocalDateTime,

        @get:NotNull
        var toTimestamp: LocalDateTime,

        @get:ManyToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "SLEEP_ID")
        var id: Long? = null

)