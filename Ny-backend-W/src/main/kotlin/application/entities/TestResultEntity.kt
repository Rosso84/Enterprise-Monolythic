package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "TEST_RESULT")
class TestResultEntity(

        @get:NotBlank
        var test: String,

        @get:NotBlank
        var refValue: String,

        @get:NotBlank
        var value: String,

        @get:NotNull
        var timestamp: LocalDateTime,

        @get:ManyToOne
        @get:NotNull
        @get:JsonIgnore
        @get: JoinColumn(name = "CALENDAR_ID", nullable = false)
        var calendar: CalendarEntity,

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "TEST_RESULT_ID")
        var id: Long? = null
)