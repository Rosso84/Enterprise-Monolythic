package application.controllers.converters


import application.controllers.dtos.MeasurementDto
import application.entities.MeasurementEntity


object MeasurementConverter {

    fun transform(entity: MeasurementEntity): MeasurementDto {

        return MeasurementDto(
                weightGrams = entity.weightGrams,
                weightKilos = entity.weightKilos,
                heightCm = entity.heightCm,
                timestamp = entity.timestamp,
                calendar_id = entity.calendar.id,
                id = entity.id.toString()

        )
    }

    fun transform(entities: Iterable<MeasurementEntity>): List<MeasurementDto> {
        return entities.map { transform(it) }
    }
}
