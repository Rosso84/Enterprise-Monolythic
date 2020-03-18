package application.controllers.converters

import application.controllers.dtos.FoodDto
import application.entities.FoodEntity

object FoodConverter {

    fun transform(food: FoodEntity): FoodDto {

        return FoodDto(
                id = food.id.toString(),
                calendar_id = food.calendar.id,
                type = food.type,
                level = food.level,
                note = food.note,
                timestamp = food.timestamp
        )
    }

    fun transform(entities: Iterable<FoodEntity>): List<FoodDto> {
        return entities.map { transform(it) }
    }
}