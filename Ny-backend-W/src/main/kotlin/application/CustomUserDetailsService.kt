package application

import application.services.UserService
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CustomUserDetailsService(
        private val userService: UserService
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {

        val user = userService.findUserByEmail(email) ?: throw UsernameNotFoundException("Could not find user with email $email")

        return CustomUser(user.email, user.password, user.enabled, true, true,
                true, AuthorityUtils.createAuthorityList(*user.roles.map { it.name }.toTypedArray()), user.id!!)
    }

}