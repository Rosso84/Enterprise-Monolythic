package application.services

import application.controllers.converters.CalendarConverter
import application.controllers.dtos.CalendarDto
import application.entities.CalendarEntity
import application.repositories.CalendarRepository
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
class CalendarService(
        private val calendarRepository: CalendarRepository,
        private val userRepo: UserService
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
    private val encryptor = Encryptors.text(phrase, salt)

    fun createCalendar(calendar: CalendarDto): Long? {
        try {
            val user = userRepo.findByUserIdUnencrypted(calendar.parent_id!!)

            if (calendarRepository.existsByNameAndParent(calendar.calendarName!!, user!!)) {
                return null
            }

            val encryptedName = encryptor.encrypt(calendar.calendarName)

            val newCalendar = CalendarEntity(encryptedName, user)

            return calendarRepository.save(newCalendar).id!!

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getAllCalendarsByParent(parentId: Long, pageable: Pageable): Page<CalendarDto> {
        val user = userRepo.findByUserId(parentId)!!
        val calendarPage = calendarRepository.findAllByParentId(parentId, pageable)
        val calendarDtoList = calendarPage.get()
                .map { CalendarConverter.transform(it) }
                .collect(Collectors.toList())
        for (accessibleCalendars in user.accessibleCalendars) {
            calendarDtoList.add(CalendarConverter.transform(calendarRepository.findById(accessibleCalendars).orElse(null)))
        }
        for (calendarDto in calendarDtoList) {
            if (calendarDto.calendarName == null) continue
            calendarDto.calendarName = encryptor.decrypt(calendarDto.calendarName)
        }
        return PageImpl<CalendarDto>(calendarDtoList as List<CalendarDto>, pageable, calendarPage.totalElements)
    }

    fun getById(id: Long): CalendarDto? {
        val calendar = calendarRepository.findById(id).orElse(null)
        return CalendarDto(
                calendarName = encryptor.decrypt(calendar.name),
                parent_id = calendar.parent.id,
                id = calendar.id.toString()
        )
    }

    fun findById(id: Long): CalendarEntity? {
        return calendarRepository.findById(id).orElse(null)
    }

    fun deleteById(id: Long) {
        val calendar = calendarRepository.findById(id).orElse(null)
        calendar.parent.calendars.remove(calendar)
    }

    fun deleteAll() {
        calendarRepository.deleteAll()
    }

    fun existsById(id: Long): Boolean {
        return calendarRepository.existsById(id)
    }

    fun addAccessFromUser(id: Long, userId: Long) {
        val calendar = calendarRepository.findById(id).orElse(null)
        calendar.sharedWith.add(userId)
        calendarRepository.save(calendar)
    }

    fun revokeAccessFromUser(id: Long, userId: Long) {
        val calendar = calendarRepository.findById(id).orElse(null)
        calendar.sharedWith.remove(userId)
        calendarRepository.save(calendar)
    }
}
