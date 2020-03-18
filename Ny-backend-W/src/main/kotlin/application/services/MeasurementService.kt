package application.services

import application.controllers.converters.MeasurementConverter
import application.controllers.dtos.MeasurementDto
import application.entities.MeasurementEntity
import application.repositories.MeasurementRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors


@Service
@Transactional
class MeasurementService(
        private val repo: MeasurementRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: MeasurementDto): Long? {
        return try {
            val parent = calendarService.findById(dto.calendar_id!!)

            val newMeasurement = MeasurementEntity(
                    dto.weightGrams!!, dto.weightKilos!!, dto.heightCm!!, dto.timestamp, parent!!)

            repo.save(newMeasurement).id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun update(entity: MeasurementEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val newMeasurement = MeasurementEntity(
                entity.weightGrams, entity.weightKilos, entity.heightCm, entity.timestamp, parent!!)

        repo.save(newMeasurement)
    }

    fun get(id: Long): MeasurementEntity? {
        return repo.findById(id).orElse(null)
    }

    fun existsByParentId(parentId: Long): Boolean {
        return repo.existsByCalendarId(parentId)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<MeasurementDto> {
        val page = repo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<MeasurementDto>(page.get()
                .map { MeasurementConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun existsById(id: Long): Boolean {
        return repo.existsById(id)
    }

    fun deleteById(id: Long) {
        return repo.deleteById(id)
    }

    fun deleteAll() {
        repo.deleteAll()
    }
}


