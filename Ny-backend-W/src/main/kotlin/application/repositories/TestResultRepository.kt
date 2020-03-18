package application.repositories

import application.entities.CalendarEntity
import application.entities.TestResultEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestResultRepository : JpaRepository<TestResultEntity, Long> {
    fun existsByCalendar(parentEntity: CalendarEntity): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<TestResultEntity>
}