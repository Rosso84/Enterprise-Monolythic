package application

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class CustomUser(
        username: String,
        password: String,
        enabled: Boolean,
        accountNonExpired: Boolean,
        credentialsNonExpired: Boolean,
        accountNonLocked: Boolean, authorities: Collection<GrantedAuthority>,
        var id: Long

) : User(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities)