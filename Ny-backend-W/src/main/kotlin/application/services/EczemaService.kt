package application.services

import application.controllers.converters.EczemaConverter
import application.controllers.dtos.EczemaDto
import application.entities.EczemaEntity
import application.repositories.EczemaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class EczemaService(
        private var eczemaRepository: EczemaRepository,
        private var calendarService: CalendarService
) {

    fun createEczema(eczema: EczemaDto): Long? {
        try {
            val date = calendarService.findById(eczema.calendar_id!!)
            if (eczemaRepository.existsByBodyPartAndCalendarId(eczema.bodyPart, eczema.calendar_id!!)) {
                return null
            }

            val newEczema = EczemaEntity(eczema.type, eczema.level, eczema.note, eczema.timestamp, date!!, eczema.bodySide,
                    eczema.bodyPortion, eczema.bodyPart)

            return eczemaRepository.save(newEczema).id!!
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getEczemasFromCalendarId(calendarId: Long, pageable: Pageable): Page<EczemaDto> {
        val datePage = eczemaRepository.findAllByCalendarId(calendarId, pageable)
        return PageImpl<EczemaDto>(datePage.get()
                .map { EczemaConverter.transform(it) }
                .collect(Collectors.toList()), pageable, datePage.totalElements
        )
    }

    fun getEczema(id: Long): EczemaEntity? {
        return eczemaRepository.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return eczemaRepository.existsById(id)
    }

    fun deleteById(id: Long) {
        eczemaRepository.deleteById(id)
    }

    fun update(entity: EczemaEntity) {
        eczemaRepository.save(entity)
    }
}