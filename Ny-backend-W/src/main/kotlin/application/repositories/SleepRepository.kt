package application.repositories

import application.entities.CalendarEntity
import application.entities.SleepEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SleepRepository : JpaRepository<SleepEntity, Long> {
    fun existsByCalendar(parentEntity: CalendarEntity): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<SleepEntity>
}