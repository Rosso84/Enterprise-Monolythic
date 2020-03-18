package application

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import javax.sql.DataSource


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class ApplicationConfig(
        private val dataSource: DataSource,
        private val customUserDetailsService: CustomUserDetailsService
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun swaggerApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
                .title("Title: RestAPI for Regit")
                .description("Description: API for mobile application RegIt")
                .version("2.0")
                .build()
    }

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/configuration/ui/**",
                "/swagger-resources/**",
                "/configuration/security/**",
                "/swagger-ui.html",
                "/webjars/**")
    }

    /**
     * Overrides the default encoding to make sure we use BCrypt to encode/hash
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    override fun userDetailsServiceBean(): UserDetailsService {
        return super.userDetailsServiceBean()
    }

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(http: HttpSecurity) {

        http.requiresChannel().anyRequest().requiresSecure()
                .and()
                .httpBasic()
                .authenticationEntryPoint(authenticationEntryPoint)
                .and()
                .logout()
                .and()
                .authorizeRequests()
                .antMatchers("/", "/csrf", "/swagger-ui.html").permitAll()
                .antMatchers("/signIn").permitAll()
                .antMatchers(HttpMethod.POST, "/users").permitAll()
                .antMatchers(HttpMethod.GET, "/users").authenticated()
                .antMatchers("/users/*").authenticated()
                .antMatchers("/calendars").authenticated()
                .antMatchers("/calendars/*").authenticated()
                .antMatchers("/calendars/**").authenticated()
                .antMatchers("/testResource").authenticated()
                .anyRequest().denyAll()
                .and()
                .csrf().disable()
    }

    @Autowired
    private lateinit var authenticationEntryPoint: MyBasicAuthenticationEntryPoint

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(customUserDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(authenticationProvider())
    }
}