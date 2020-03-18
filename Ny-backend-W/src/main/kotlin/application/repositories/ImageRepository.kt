package application.repositories

import application.entities.CalendarEntity
import application.entities.ImageEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageRepository : JpaRepository<ImageEntity, Long> {

    fun existsByFileNameAndCalendar(fileName: String, parent: CalendarEntity): Boolean
    fun deleteAllByCalendarId(parentId: Long)
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<ImageEntity>

}
