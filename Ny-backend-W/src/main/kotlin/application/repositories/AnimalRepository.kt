package application.repositories

import application.entities.AnimalEntity
import application.entities.CalendarEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimalRepository : JpaRepository<AnimalEntity, Long> {

    fun existsByTypeAndCalendar(Type: String, parent: CalendarEntity): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<AnimalEntity>

}