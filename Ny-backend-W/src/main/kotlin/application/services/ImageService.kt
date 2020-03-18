package application.services

import application.controllers.converters.ImageConverter
import application.controllers.dtos.ImageDTO
import application.entities.CalendarEntity
import application.entities.ImageEntity
import application.repositories.ImageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.stream.Collectors


@Service
@Transactional
class ImageService(

        private val imageRepository: ImageRepository,
        private val calendarService: CalendarService
) {

    //Hack to avoid InvalidKeyException: Illegal Key Size normally fixed by JCE Unlimited Strength Policy
    init {
        val field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted")
        field.isAccessible = true

        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        field.set(null, false)
    }

    private val phrase = KeyGenerators.string().generateKey()
    private var salt = KeyGenerators.string().generateKey()
    private val encryptor = Encryptors.standard(phrase, salt)

    fun createImage(image: ImageDTO): Long? {
        try {
            val parent = calendarService.findById(image.calendar_id!!)
            if (imageExistByNameAndParent(image.fileName, parent!!)) {
                return null
            }

            val encryptedData = encryptor.encrypt(image.data)
            val newImage = ImageEntity(image.fileName, image.fileType, encryptedData, image.timestamp, parent)

            return imageRepository.save(newImage).id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getAllByCalendarId(calendarId: Long, pageable: Pageable): Page<ImageDTO> {
        val page = imageRepository.findAllByCalendarId(calendarId, pageable)
        val imageDtoList = page.get()
                .map { ImageConverter.transform(it) }
                .collect(Collectors.toList())
        for (image in imageDtoList) {
            image.data = encryptor.decrypt(image.data)
        }
        return PageImpl<ImageDTO>(imageDtoList as List<ImageDTO>, pageable, page.totalElements)
    }

    fun decryptImage(data: ByteArray): ByteArray {
        return encryptor.decrypt(data)
    }

    fun findByImageId(id: Long): ImageEntity? {
        return imageRepository.findById(id).orElse(null)
    }

    fun imageExistById(id: Long): Boolean {
        return imageRepository.existsById(id)
    }

    fun imageExistByNameAndParent(fileName: String, parent: CalendarEntity): Boolean {
        return imageRepository.existsByFileNameAndCalendar(fileName, parent)
    }

    fun deleteByImageId(id: Long) {
        imageRepository.deleteById(id)
    }

    fun deleteAllByUserId(userId: Long) {
        imageRepository.deleteAllByCalendarId(userId)
    }

}