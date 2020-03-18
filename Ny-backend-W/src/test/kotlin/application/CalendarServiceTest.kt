package application

import application.controllers.dtos.CalendarDto
import application.entities.UserEntity
import application.enums.Role
import application.repositories.CalendarRepository
import application.services.CalendarService
import application.services.UserService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension

/*Created by Roosbeh Moradi*/

@ExtendWith(SpringExtension::class)
@SpringBootTest(
        classes = [(Application::class)],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalendarServiceTest {

    @Autowired
    private lateinit var repo: CalendarService

    @Autowired
    private lateinit var jpaRepo: CalendarRepository

    @Autowired
    private lateinit var userRepo: UserService

    @BeforeEach
    fun init() {
        jpaRepo.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        userRepo.deleteAll()
    }


    /**@Tests If a calendar is created and verifies that calendar (user) is correct owner.
     * @Tests If an CalendarAntity can be queried by Id and ParentId*/
    @Test
    fun testCreateCalenderAndVerifyParent() {

        val firstName = "Rambo"

        val user = UserEntity("roo@mail.no", firstName, "last",
                "123456", "123456", setOf(Role.ROLE_BRUKER))

        val parentId = userRepo.createUser(user)

        val calendarName = "myCalendar"

        val calendarDto = CalendarDto(calendarName, parentId)

        val calendarId = repo.createCalendar(calendarDto)

        //Note here we test getById() as well
        val createdCalendar = repo.getById(calendarId!!)

        if (createdCalendar != null) {
            assertEquals(createdCalendar.calendarName, calendarName)
            assertEquals(createdCalendar.parent_id, parentId)
        }

    }

    /**@Tests If a calendar (users) owns right amount of calendars*/
    @Test
    fun testFindAllCalendarsByParent() {

        val firstName1 = "Rambo1"

        val user = UserEntity("roo@mail.no", firstName1, "last",
                "123456", "123456", setOf(Role.ROLE_BRUKER))


        val parentId = userRepo.createUser(user)

        val calendarName1 = "myCalendar1"
        val calendarName2 = "myCalendar2"
        val calendarName3 = "myCalendar3"

        val calendarDto1 = CalendarDto(calendarName1, parentId)
        val calendarDto2 = CalendarDto(calendarName2, parentId)
        val calendarDto3 = CalendarDto(calendarName3, parentId)

        repo.createCalendar(calendarDto1)
        repo.createCalendar(calendarDto2)
        repo.createCalendar(calendarDto3)

        val offset = 1
        val limit = 3

        val pageable = PageRequest.of(offset, limit)

        val pageCalendars = repo.getAllCalendarsByParent(parentId!!, pageable)

        val allCalendarsOfThisUser = pageCalendars.totalElements

        assertEquals(allCalendarsOfThisUser, 3)


    }

    /**@Tests If a given Calendar is deleted by calendarId and parentId*/
    @Test
    fun testDeleteById() {

        val user = UserEntity("roo1@mail.no", "foo", "last",
                "123456", "123456", setOf(Role.ROLE_BRUKER))

        val parentId = userRepo.createUser(user)
        val calendarDto = CalendarDto("MyCalendar", parentId)
        val calId = repo.createCalendar(calendarDto)

        val offset = 0
        val limit = 5

        val pageable = PageRequest.of(offset, limit)
        val pageCalendars = repo.getAllCalendarsByParent(parentId!!, pageable)
        val listOfCalendars = pageCalendars.totalElements

        assertEquals(listOfCalendars, 1) //1 user has 1 calendar registered

        repo.deleteById(calId!!)

        val newPageCal = repo.getAllCalendarsByParent(parentId, pageable)
        val newListOfCalendars = newPageCal.totalElements

        assertEquals(newListOfCalendars, 0) //confirm calendar is deleted
    }

}