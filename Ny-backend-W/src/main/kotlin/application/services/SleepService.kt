package application.services

import application.controllers.converters.SleepConverter
import application.controllers.dtos.SleepDto
import application.entities.SleepEntity
import application.repositories.SleepRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class SleepService(
        private val repo: SleepRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: SleepDto): Long? {
        return try {
            val parent = calendarService.findById(dto.calendar_id!!)

            val entity = SleepEntity(
                    dto.fromTimestamp, dto.toTimestamp, parent!!)

            repo.save(entity).id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun update(entity: SleepEntity) {
        repo.save(entity)
    }

    fun get(id: Long): SleepEntity? {
        return repo.findById(id).orElse(null)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<SleepDto> {
        val page = repo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<SleepDto>(page.get()
                .map { SleepConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun existsById(id: Long): Boolean {
        return repo.existsById(id)
    }

    fun existsByParentId(id: Long): Boolean {
        val parentId = calendarService.findById(id)
        return repo.existsByCalendar(parentId!!)
    }

    fun deleteById(id: Long) {
        return repo.deleteById(id)
    }
}