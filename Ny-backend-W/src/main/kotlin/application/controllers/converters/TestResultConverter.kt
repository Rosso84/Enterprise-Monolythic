package application.controllers.converters

import application.controllers.dtos.TestResultDto
import application.entities.TestResultEntity

object TestResultConverter {

    fun transform(entity: TestResultEntity): TestResultDto {

        return TestResultDto(
                test = entity.test,
                refValue = entity.refValue,
                value = entity.value,
                timestamp = entity.timestamp,
                calendar_id = entity.calendar.id,
                id = entity.id.toString()
        )
    }

    fun transform(entities: Iterable<TestResultEntity>): List<TestResultDto> {
        return entities.map { transform(it) }
    }
}