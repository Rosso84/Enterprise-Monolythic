package application.entities

import application.enums.Level
import java.time.LocalDateTime
import javax.persistence.*


@Entity
@Table(name = "HUMOR")
class HumorEntity(

        type: String,
        level: Level,
        note: String,
        timestamp: LocalDateTime,
        calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "HUMOR_ID")
        var id: Long? = null

) : Common(type, level, note, timestamp, calendar)
