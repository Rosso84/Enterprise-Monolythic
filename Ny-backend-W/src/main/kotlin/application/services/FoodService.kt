package application.services

import application.controllers.converters.FoodConverter
import application.controllers.dtos.FoodDto
import application.entities.FoodEntity
import application.repositories.FoodRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors

@Service
@Transactional
class FoodService(
        private val foodRepo: FoodRepository,
        private val calendarService: CalendarService
) {

    fun createFood(food: FoodDto): Long? {
        try {
            val parent = calendarService.findById(food.calendar_id!!)
            if (foodRepo.existsByTypeAndCalendar(food.type!!, parent!!)) {
                return null
            }

            val newFood = FoodEntity(food.type!!, food.level!!,
                    food.note!!, food.timestamp, parent)

            return foodRepo.save(newFood).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun update(entity: FoodEntity) {

        val parent = calendarService.findById(entity.calendar.id!!)

        val new = FoodEntity(entity.type, entity.level,
                entity.note, entity.timestamp, parent!!)

        foodRepo.save(new)
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<FoodDto> {
        val page = foodRepo.findAllByCalendarId(calendarId, pageable)
        return PageImpl<FoodDto>(page.get()
                .map { FoodConverter.transform(it) }
                .collect(Collectors.toList()), pageable, page.totalElements
        )
    }

    fun getFood(id: Long): FoodEntity? {
        return foodRepo.findById(id).orElse(null)
    }

    fun existsById(id: Long): Boolean {
        return foodRepo.existsById(id)
    }

    fun deleteById(id: Long) {
        return foodRepo.deleteById(id)
    }
}