package application.repositories

import application.entities.AbsenceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AbsenceRepository : JpaRepository<AbsenceEntity, Long> {

    fun existsByCalendarId(parentId: Long): Boolean
    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<AbsenceEntity>

}