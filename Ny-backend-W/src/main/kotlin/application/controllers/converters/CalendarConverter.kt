package application.controllers.converters

import application.controllers.dtos.CalendarDto
import application.entities.CalendarEntity

object CalendarConverter {

    fun transform(calendar: CalendarEntity): CalendarDto {

        return CalendarDto(
                id = calendar.id.toString(),
                calendarName = calendar.name,
                parent_id = calendar.parent.id,
                sharedWith = calendar.sharedWith,

                images = calendar.images,
                animals = calendar.animals,
                foods = calendar.foods,
                pollens = calendar.pollens,
                symptoms = calendar.symptoms,
                humors = calendar.humors,
                treatments = calendar.treatments,
                sleeps = calendar.sleeps,
                testResults = calendar.testResults,
                measurement = calendar.measurement,
                absence = calendar.absence,
                eczemas = calendar.eczemas
        )
    }

    fun transform(entities: Iterable<CalendarEntity>): List<CalendarDto> {
        return entities.map { transform(it) }
    }
}