package application.entities

import application.custom_constraints.Encrypted
import application.enums.Role
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "USERS")
class UserEntity(

        @get:NotBlank
        var email: String,

        @get:NotBlank
        var firstName: String,

        @get:NotBlank
        var lastName: String,

        @Encrypted
        var password: String,

        @Encrypted
        var pin: String,

        @get:ElementCollection
        @get:CollectionTable(name = "USER_ROLES", joinColumns = [JoinColumn(name = "USER_ID")])
        @get:NotNull
        var roles: Set<Role> = setOf(),

        @get:NotNull
        var enabled: Boolean = true,

        @get:OneToMany(targetEntity = CalendarEntity::class, mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
        var calendars: MutableList<CalendarEntity> = mutableListOf(),

        @get:NotNull
        @get:ElementCollection(fetch = FetchType.EAGER)
        @get:CollectionTable(name = "ACCESSIBLE_CALENDARS", joinColumns = [JoinColumn(name = "USER_ID")])
        var accessibleCalendars: MutableList<Long> = mutableListOf(),

        @get:Id
        @get:GeneratedValue
        @get:Column(name = "USER_ID")
        var id: Long? = null

)
