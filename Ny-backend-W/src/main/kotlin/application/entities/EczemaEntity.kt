package application.entities

import application.enums.BodyPart
import application.enums.BodyPortion
import application.enums.BodySide
import application.enums.Level
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


@Entity
@Table(name = "ECZEMA")
class EczemaEntity(

        type: String,
        level: Level,
        note: String,
        timestamp: LocalDateTime,
        calendar: CalendarEntity,

        @get:NotNull
        var bodySide: BodySide,

        @get:NotNull
        var bodyPortion: BodyPortion,

        @get:NotNull
        var bodyPart: BodyPart,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "ECZEMA_ID")
        var id: Long? = null

) : Common(type, level, note, timestamp, calendar)
