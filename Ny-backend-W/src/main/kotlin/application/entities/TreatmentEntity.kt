package application.entities

import application.enums.Level
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "TREATMENT")
class TreatmentEntity(

        type: String,
        level: Level,
        note: String,
        timestamp: LocalDateTime,
        calendar: CalendarEntity,

        @get: Id
        @get: GeneratedValue
        @get:Column(name = "TREATMENT_ID")
        var id: Long? = null

) : Common(type, level, note, timestamp, calendar)

