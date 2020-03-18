package application.entities

import application.enums.Level
import java.time.LocalDateTime
import javax.persistence.*


@Entity
@Table(name = "POLLEN")
class PollenEntity(

        type: String,
        level: Level,
        note: String,
        timestamp: LocalDateTime,
        calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "POLLEN_ID")
        var id: Long? = null

) : Common(type, level, note, timestamp, calendar)
