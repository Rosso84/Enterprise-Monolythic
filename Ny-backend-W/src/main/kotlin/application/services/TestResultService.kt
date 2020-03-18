package application.services

import application.controllers.converters.TestResultConverter
import application.controllers.dtos.TestResultDto
import application.entities.TestResultEntity
import application.repositories.TestResultRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class TestResultService(
        private val repo: TestResultRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: TestResultDto): Long? {
        return try {
            val parent = calendarService.findById(dto.calendar_id!!)

            val entity = TestResultEntity(
                    dto.test, dto.refValue, dto.value, dto.timestamp, parent!!)

            repo.save(entity).id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun update(entity: TestResultEntity) {
        repo.save(entity)
    }

    fun get(id: Long): TestResultEntity? {
        return repo.findById(id).orElse(null)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<TestResultDto> {
        val page = repo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<TestResultDto>(page.get()
                .map { TestResultConverter.transform(it) }
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