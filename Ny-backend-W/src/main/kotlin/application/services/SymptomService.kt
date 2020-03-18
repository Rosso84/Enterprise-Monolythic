package application.services

import application.controllers.converters.SymptomConverter
import application.controllers.dtos.SymptomDto
import application.entities.SymptomEntity
import application.repositories.SymptomRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors


@Service
@Transactional
class SymptomService(
        private val symptomRepo: SymptomRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: SymptomDto): Long? {
        try {
            val parent = calendarService.findById(dto.calendar_id!!)
            if (symptomRepo.existsByTypeAndCalendar(dto.type!!, parent!!)) {
                return null
            }

            val new = SymptomEntity(dto.type!!, dto.level!!,
                    dto.note!!, dto.timestamp, parent)

            return symptomRepo.save(new).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun update(entity: SymptomEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val new = SymptomEntity(entity.type, entity.level,
                entity.note, entity.timestamp, parent!!)

        symptomRepo.save(new)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<SymptomDto> {
        val page = symptomRepo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<SymptomDto>(page.get()
                .map { SymptomConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun get(id: Long): SymptomEntity? {
        return symptomRepo.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return symptomRepo.existsById(id)
    }

    fun deleteById(id: Long) {
        return symptomRepo.deleteById(id)
    }
}
