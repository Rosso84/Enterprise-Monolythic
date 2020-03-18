package application.controllers.converters

import application.controllers.dtos.SymptomDto
import application.entities.SymptomEntity

object SymptomConverter {

    fun transform(food: SymptomEntity): SymptomDto {

        return SymptomDto(
                type = food.type,
                level = food.level,
                note = food.note,
                timestamp = food.timestamp,
                calendar_id = food.calendar.id,
                id = food.id.toString()
        )
    }

    fun transform(entities: Iterable<SymptomEntity>): List<SymptomDto> {
        return entities.map { transform(it) }
    }
}