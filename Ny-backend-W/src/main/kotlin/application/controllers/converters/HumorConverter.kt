package application.controllers.converters

import application.controllers.dtos.HumorDto
import application.entities.HumorEntity

object HumorConverter {
    fun transform(entity: HumorEntity): HumorDto {

        return HumorDto(
                id = entity.id.toString(),
                calendar_id = entity.calendar.id,
                type = entity.type,
                level = entity.level,
                note = entity.note,
                timestamp = entity.timestamp
        )
    }

    fun transform(entities: Iterable<HumorEntity>): List<HumorDto> {
        return entities.map { transform(it) }
    }
}