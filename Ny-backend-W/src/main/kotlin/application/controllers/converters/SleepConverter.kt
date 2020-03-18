package application.controllers.converters

import application.controllers.dtos.SleepDto
import application.entities.SleepEntity

object SleepConverter {

    fun transform(entity: SleepEntity): SleepDto {

        return SleepDto(
                fromTimestamp = entity.fromTimestamp,
                toTimestamp = entity.toTimestamp,
                calendar_id = entity.calendar.id,
                id = entity.id?.toString()
        )
    }

    fun transform(entities: Iterable<SleepEntity>): List<SleepDto> {
        return entities.map { transform(it) }
    }
}