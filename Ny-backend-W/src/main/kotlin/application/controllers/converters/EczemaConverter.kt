package application.controllers.converters

import application.controllers.dtos.EczemaDto
import application.entities.EczemaEntity

object EczemaConverter {

    fun transform(eczema: EczemaEntity): EczemaDto {

        return EczemaDto(
                id = eczema.id.toString(),
                calendar_id = eczema.calendar.id,
                type = eczema.type,
                level = eczema.level,
                note = eczema.note,
                timestamp = eczema.timestamp,
                bodySide = eczema.bodySide,
                bodyPortion = eczema.bodyPortion,
                bodyPart = eczema.bodyPart
        )
    }

    fun transform(entities: Iterable<EczemaEntity>): List<EczemaDto> {
        return entities.map { transform(it) }
    }

}