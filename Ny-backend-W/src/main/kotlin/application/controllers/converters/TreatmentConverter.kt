package application.controllers.converters

import application.controllers.dtos.TreatmentDto
import application.entities.TreatmentEntity

object TreatmentConverter {

    fun transform(entity: TreatmentEntity): TreatmentDto {

        return TreatmentDto(
                type = entity.type,
                level = entity.level,
                note = entity.note,
                timestamp = entity.timestamp,
                calendar_id = entity.calendar.id,
                id = entity.id.toString()
        )
    }

    fun transform(entities: Iterable<TreatmentEntity>): List<TreatmentDto> {
        return entities.map { transform(it) }
    }
}