package application.controllers.dtos

import application.entities.*
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Calendar details")
data class CalendarDto(

        @ApiModelProperty("The calendarName of the Calendar")
        var calendarName: String? = null,

        @ApiModelProperty("The user connected to this calendar")
        var parent_id: Long? = null,

        @ApiModelProperty("List of users with approved access to this calendar")
        var sharedWith: MutableList<Long> = mutableListOf(),

        @ApiModelProperty("A list of your images for this day")
        var images: MutableList<ImageEntity> = mutableListOf(),

        @ApiModelProperty("A list of animal symptoms happening this current day")
        var animals: MutableList<AnimalEntity> = mutableListOf(),

        @ApiModelProperty("A list of your allergic reactions to food for this day")
        var foods: MutableList<FoodEntity> = mutableListOf(),

        @ApiModelProperty("A list of pollen registered for this day")
        var pollens: MutableList<PollenEntity> = mutableListOf(),

        @ApiModelProperty("A list of symptoms registered for this day")
        var symptoms: MutableList<SymptomEntity> = mutableListOf(),

        @ApiModelProperty("A list of registered humors for this day")
        var humors: MutableList<HumorEntity> = mutableListOf(),

        @ApiModelProperty("A list of registered treatments for this day")
        var treatments: MutableList<TreatmentEntity> = mutableListOf(),

        @ApiModelProperty("A list of registered sleep-periods")
        var sleeps: MutableList<SleepEntity> = mutableListOf(),

        @ApiModelProperty("A list of registered TestResults")
        var testResults: MutableList<TestResultEntity> = mutableListOf(),

        @ApiModelProperty("A list of registered weight and height")
        var measurement: MeasurementEntity? = null,

        @ApiModelProperty("A registered absence")
        var absence: AbsenceEntity? = null,

        @ApiModelProperty("A set of your eczema outburst today")
        var eczemas: Set<EczemaEntity> = hashSetOf(),

        @ApiModelProperty("Id of the Calendar")
        var id: String? = null

)
