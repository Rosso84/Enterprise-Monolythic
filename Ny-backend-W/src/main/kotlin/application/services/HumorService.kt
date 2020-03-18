package application.services

import application.controllers.converters.HumorConverter
import application.controllers.dtos.HumorDto
import application.entities.HumorEntity
import application.repositories.HumorRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors


@Service
@Transactional
class HumorService(
        private val humorRepo: HumorRepository,
        private val calendarService: CalendarService
) {

    fun create(dto: HumorDto): Long? {
        try {
            val parent = calendarService.findById(dto.calendar_id!!)
            if (humorRepo.existsByTypeAndCalendar(dto.type!!, parent!!)) {
                return null
            }

            val new = HumorEntity(dto.type!!, dto.level!!,
                    dto.note!!, dto.timestamp, parent)

            return humorRepo.save(new).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<HumorDto> {
        val page = humorRepo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<HumorDto>(page.get()
                .map { HumorConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun getPollen(id: Long): HumorEntity? {
        return humorRepo.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return humorRepo.existsById(id)
    }

    fun deleteById(id: Long) {
        return humorRepo.deleteById(id)
    }
}