package application.repositories

import application.entities.CalendarEntity
import application.entities.HumorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HumorRepository : JpaRepository<HumorEntity, Long> {

    fun existsByTypeAndCalendar(Type: String, parent: CalendarEntity): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<HumorEntity>

}