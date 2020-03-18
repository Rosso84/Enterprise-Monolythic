package application.repositories

import application.entities.CalendarEntity
import application.entities.SymptomEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SymptomRepository : JpaRepository<SymptomEntity, Long> {

    fun existsByTypeAndCalendar(Type: String, parent: CalendarEntity): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<SymptomEntity>

}