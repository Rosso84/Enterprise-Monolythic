package application.entities

import application.enums.Level
import java.time.LocalDateTime
import javax.persistence.*


@Entity
@Table(name = "SYMPTOM")
class SymptomEntity(

        type: String,
        level: Level,
        note: String,
        timestamp: LocalDateTime,
        calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "SYMPTOM_ID")
        var id: Long? = null

) : Common(type, level, note, timestamp, calendar)
