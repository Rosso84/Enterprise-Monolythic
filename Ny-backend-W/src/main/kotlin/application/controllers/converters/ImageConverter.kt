package application.controllers.converters

import application.controllers.dtos.ImageDTO
import application.entities.ImageEntity

object ImageConverter {

    fun transform(imageEntity: ImageEntity): ImageDTO {

        return ImageDTO(
                fileName = imageEntity.fileName,
                fileType = imageEntity.fileType,
                data = imageEntity.data,
                timestamp = imageEntity.timestamp,
                calendar_id = imageEntity.calendar.id,
                id = imageEntity.id?.toString()
        )
    }
}