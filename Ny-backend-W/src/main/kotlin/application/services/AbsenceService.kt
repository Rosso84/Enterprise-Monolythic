package application.services

import application.controllers.converters.AbsenceConverter
import application.controllers.dtos.AbsenceDto
import application.entities.AbsenceEntity
import application.repositories.AbsenceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class AbsenceService(
        private val repository: AbsenceRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: AbsenceDto): Long? {
        return try {
            val parent = calendarService.findById(dto.calendar_id!!)

            val absence = AbsenceEntity(
                    dto.note!!, dto.timestamp, parent!!)

            repository.save(absence).id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun update(entity: AbsenceEntity) {
        repository.save(entity)
    }

    fun get(id: Long): AbsenceEntity? {
        return repository.findById(id).orElse(null)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<AbsenceDto> {
        val page = repository.findAllByCalendarId(calendarId, pageable)
        return PageImpl<AbsenceDto>(page.get()
                .map { AbsenceConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun existsById(id: Long): Boolean {
        return repository.existsById(id)
    }

    fun existsByParentId(id: Long): Boolean {
        return repository.existsByCalendarId(id)
    }

    fun deleteById(id: Long) {
        return repository.deleteById(id)
    }

    fun deleteAll() {
        repository.deleteAll()
    }
}