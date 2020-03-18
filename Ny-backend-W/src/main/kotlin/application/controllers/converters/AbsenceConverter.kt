package application.controllers.converters

import application.controllers.dtos.AbsenceDto
import application.entities.AbsenceEntity


object AbsenceConverter {

    fun transform(entity: AbsenceEntity): AbsenceDto {

        return AbsenceDto(
                note = entity.note,
                timestamp = entity.timestamp,
                calendar_id = entity.calendar.id,
                id = entity.id.toString()
        )
    }

    fun transform(entities: Iterable<AbsenceEntity>): List<AbsenceDto> {
        return entities.map { transform(it) }
    }
}