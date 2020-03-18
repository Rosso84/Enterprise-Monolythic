package application.services

import application.controllers.converters.UserConverter
import application.controllers.dtos.UserDTO
import application.entities.UserEntity
import application.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.stream.Collectors

@Service
@Transactional
class UserService(
        private val userRepo: UserRepository
) {

    @Autowired
    @Lazy
    private lateinit var encoder: PasswordEncoder


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

    fun createUser(userEntity: UserEntity): Long? {

        return try {
            userEntity.password = encoder.encode(userEntity.password)
            userEntity.pin = encoder.encode(userEntity.pin)
            userEntity.email = encryptor.encrypt(userEntity.email)
            userEntity.firstName = encryptor.encrypt(userEntity.firstName)
            userEntity.lastName = encryptor.encrypt(userEntity.lastName)

            userRepo.save(userEntity).id!!

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updateUser(userEntity: UserEntity) {

        userEntity.password = encoder.encode(userEntity.password)
        userEntity.pin = encoder.encode(userEntity.pin)
        userEntity.email = encryptor.encrypt(userEntity.email)
        userEntity.firstName = encryptor.encrypt(userEntity.firstName)
        userEntity.lastName = encryptor.encrypt(userEntity.lastName)

        userRepo.save(userEntity)
    }

    fun userExistByEmail(email: String): Boolean {
        val users = userRepo.findAll()
        users.forEach {
            val decryptedEmail = encryptor.decrypt(it.email)
            if (decryptedEmail == email) {
                return true
            }
        }
        return false
    }

    fun existByUserId(id: Long): Boolean {
        return userRepo.existsById(id)
    }

    fun findUserByEmail(email: String): UserEntity? {
        val users = userRepo.findAll()
        users.forEach {
            val decryptedEmail = encryptor.decrypt(it.email)
            if (decryptedEmail == email) {
                return findByUserId(it.id!!)
            }
        }
        return null
    }

    fun deleteByUserId(id: Long) {
        return userRepo.deleteById(id)
    }

    fun deleteAll() {
        return userRepo.deleteAll()
    }

    fun findByUserId(id: Long): UserEntity? {
        val user = userRepo.findById(id).orElse(null)
        if (user != null) {
            return UserEntity(
                    encryptor.decrypt(user.email),
                    encryptor.decrypt(user.firstName),
                    encryptor.decrypt(user.lastName),
                    user.password,
                    user.pin,
                    user.roles,
                    user.enabled,
                    user.calendars,
                    user.accessibleCalendars,
                    user.id
            )
        }
        return user
    }

    fun findByUserIdUnencrypted(id: Long): UserEntity? {
        return userRepo.findById(id).orElse(null)
    }

    fun findAllUsers(pageable: Pageable): Page<UserDTO> {
        val userPage = userRepo.findAll(pageable)
        val userDto = userPage.get()
                .map { UserConverter.transform(it) }
                .collect(Collectors.toList())
        for (user in userDto) {
            user.email = encryptor.decrypt(user.email)
            user.firstName = encryptor.decrypt(user.firstName)
            user.lastName = encryptor.decrypt(user.lastName)
        }
        return PageImpl<UserDTO>(userDto as List<UserDTO>, pageable, userPage.totalElements)
    }
}

