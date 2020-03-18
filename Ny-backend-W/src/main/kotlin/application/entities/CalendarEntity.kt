package application.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "CALENDARS")
class CalendarEntity(

        @get:NotBlank
        var name: String,

        @get:ManyToOne
        @get:JoinColumn(name = "user_id", nullable = false)
        @get:JsonIgnore
        var parent: UserEntity,

        @get:NotNull
        @get:ElementCollection
        @get:CollectionTable(name = "USERS_WITH_REMOTE_ACCESS", joinColumns = [JoinColumn(name = "CALENDAR_ID")])
        var sharedWith: MutableList<Long> = mutableListOf(),

        @get:OneToOne(targetEntity = MeasurementEntity::class, mappedBy = "calendar")
        var measurement: MeasurementEntity? = null,

        @get:OneToOne(targetEntity = AbsenceEntity::class, mappedBy = "calendar")
        var absence: AbsenceEntity? = null,

        @get:OneToMany(targetEntity = AnimalEntity::class, mappedBy = "calendar")
        var animals: MutableList<AnimalEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = FoodEntity::class, mappedBy = "calendar")
        var foods: MutableList<FoodEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = ImageEntity::class, mappedBy = "calendar")
        var images: MutableList<ImageEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = PollenEntity::class, mappedBy = "calendar")
        var pollens: MutableList<PollenEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = SymptomEntity::class, mappedBy = "calendar")
        var symptoms: MutableList<SymptomEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = HumorEntity::class, mappedBy = "calendar")
        var humors: MutableList<HumorEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = TreatmentEntity::class, mappedBy = "calendar")
        var treatments: MutableList<TreatmentEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = SleepEntity::class, mappedBy = "calendar")
        var sleeps: MutableList<SleepEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = TestResultEntity::class, mappedBy = "calendar")
        var testResults: MutableList<TestResultEntity> = mutableListOf(),

        @get:OneToMany(targetEntity = EczemaEntity::class, mappedBy = "calendar")
        var eczemas: Set<EczemaEntity> = hashSetOf(),

        @get:Id @get:GeneratedValue
        @get:Column(name = "CALENDAR_ID")
        var id: Long? = null
)
