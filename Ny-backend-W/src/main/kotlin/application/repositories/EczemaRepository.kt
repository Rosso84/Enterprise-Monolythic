package application.repositories

import application.entities.EczemaEntity
import application.enums.BodyPart
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EczemaRepository : JpaRepository<EczemaEntity, Long> {

    fun findAllByCalendarId(calendarId: Long, pageable: Pageable): Page<EczemaEntity>

    fun existsByBodyPartAndCalendarId(bodyPart: BodyPart, parentId: Long): Boolean

}