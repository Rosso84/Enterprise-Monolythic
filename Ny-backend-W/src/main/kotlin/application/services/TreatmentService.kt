package application.services

import application.controllers.converters.TreatmentConverter
import application.controllers.dtos.TreatmentDto
import application.entities.TreatmentEntity
import application.repositories.TreatmentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class TreatmentService(
        private val treatmentRepo: TreatmentRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: TreatmentDto): Long? {
        try {
            val parent = calendarService.findById(dto.calendar_id!!)
            if (treatmentRepo.existsByTypeAndCalendar(dto.type!!, parent!!)) {
                return null
            }

            val new = TreatmentEntity(dto.type!!, dto.level!!,
                    dto.note!!, dto.timestamp, parent)

            return treatmentRepo.save(new).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun update(entity: TreatmentEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val new = TreatmentEntity(entity.type, entity.level,
                entity.note, entity.timestamp, parent!!)

        treatmentRepo.save(new)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<TreatmentDto> {
        val page = treatmentRepo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<TreatmentDto>(page.get()
                .map { TreatmentConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun get(id: Long): TreatmentEntity? {
        return treatmentRepo.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return treatmentRepo.existsById(id)
    }

    fun deleteById(id: Long) {
        return treatmentRepo.deleteById(id)
    }
}