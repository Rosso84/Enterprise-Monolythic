package application.repositories

import application.entities.CalendarEntity
import application.entities.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CalendarRepository : JpaRepository<CalendarEntity, Long> {
    fun existsByNameAndParent(name: String, parent: UserEntity): Boolean
    fun findAllByParentId(parent: Long, pageable: Pageable): Page<CalendarEntity>
}

