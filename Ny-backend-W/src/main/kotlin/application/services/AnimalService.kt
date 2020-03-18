package application.services

import application.controllers.converters.AnimalConverter
import application.controllers.dtos.AnimalDto
import application.entities.AnimalEntity
import application.repositories.AnimalRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class AnimalService(
        private val animalRepository: AnimalRepository,
        private val calendarService: CalendarService
) {

    fun createAnimal(animal: AnimalDto): Long? {
        try {
            val parent = calendarService.findById(animal.calendar_id!!)
            if (animalRepository.existsByTypeAndCalendar(animal.type!!, parent!!)) {
                return null
            }

            val newAnimal = AnimalEntity(animal.type!!, animal.level!!,
                    animal.note!!, animal.timestamp, parent)

            return animalRepository.save(newAnimal).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun updateAnimal(entity: AnimalEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val newAnimal = AnimalEntity(entity.type, entity.level,
                entity.note, entity.timestamp, parent!!)

        animalRepository.save(newAnimal)
    }


    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<AnimalDto> {
        val page = animalRepository.findAllByCalendarId(calendarId, pageable)
        return PageImpl<AnimalDto>(page.get()
                .map { AnimalConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun getAnimal(id: Long): AnimalEntity? {
        val animal = animalRepository.findById(id).orElse(null)
        if (animal != null) {
            return AnimalEntity(
                    animal.type,
                    animal.level,
                    animal.note,
                    animal.timestamp,
                    animal.calendar
            )
        }
        return animal
    }

    fun existsById(id: Long): Boolean {
        return animalRepository.existsById(id)
    }

    fun deleteById(id: Long) {
        return animalRepository.deleteById(id)
    }
}
