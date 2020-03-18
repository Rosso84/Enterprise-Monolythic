package application.services

import application.controllers.converters.PollenConverter
import application.controllers.dtos.PollenDto
import application.entities.PollenEntity
import application.repositories.PollenRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors


@Service
@Transactional
class PollenService(
        private val pollenRepo: PollenRepository,
        private val calendarService: CalendarService
) {

    fun create(pollen: PollenDto): Long? {
        try {
            val parent = calendarService.findById(pollen.calendar_id!!)
            if (pollenRepo.existsByTypeAndCalendar(pollen.type!!, parent!!)) {
                return null
            }

            val newPollen = PollenEntity(pollen.type!!, pollen.level!!,
                    pollen.note!!, pollen.timestamp, parent)

            return pollenRepo.save(newPollen).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun update(entity: PollenEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val new = PollenEntity(entity.type, entity.level,
                entity.note, entity.timestamp, parent!!)

        pollenRepo.save(new)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<PollenDto> {
        val page = pollenRepo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<PollenDto>(page.get()
                .map { PollenConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun getPollen(id: Long): PollenEntity? {
        return pollenRepo.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return pollenRepo.existsById(id)
    }

    fun deleteById(id: Long) {
        return pollenRepo.deleteById(id)
    }
}
