package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "WEIGHT_HEIGHT")
class MeasurementEntity(

        @get:NotNull
        var weightGrams: Int,

        @get:NotNull
        var weightKilos: Int,

        @get:NotNull
        var heightCm: Int,

        @get:NotNull
        var timestamp: LocalDateTime,

        @get:ManyToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "WEIGHT_HEIGHT_ID")
        var id: Long? = null

)
