package application.controllers.converters

import application.controllers.dtos.AnimalDto
import application.entities.AnimalEntity

object AnimalConverter {

    fun transform(animal: AnimalEntity): AnimalDto {

        return AnimalDto(
                id = animal.id.toString(),
                calendar_id = animal.calendar.id,
                type = animal.type,
                level = animal.level,
                note = animal.note,
                timestamp = animal.timestamp
        )
    }

    fun transform(entities: Iterable<AnimalEntity>): List<AnimalDto> {
        return entities.map { transform(it) }
    }
}