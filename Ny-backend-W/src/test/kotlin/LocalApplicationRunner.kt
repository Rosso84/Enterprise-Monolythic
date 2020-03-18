import application.Application
import org.springframework.boot.SpringApplication

class LocalApplicationRunner

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}