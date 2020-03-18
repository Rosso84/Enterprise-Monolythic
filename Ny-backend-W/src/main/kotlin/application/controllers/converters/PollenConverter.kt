package application.controllers.converters

import application.controllers.dtos.PollenDto
import application.entities.PollenEntity

object PollenConverter {

    fun transform(pollen: PollenEntity): PollenDto {

        return PollenDto(
                type = pollen.type,
                level = pollen.level,
                note = pollen.note,
                timestamp = pollen.timestamp,
                calendar_id = pollen.calendar.id,
                id = pollen.id.toString()
        )
    }

    fun transform(entities: Iterable<PollenEntity>): List<PollenDto> {
        return entities.map { transform(it) }
    }
}