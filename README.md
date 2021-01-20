### About
This a backend system for a register app using Swagger as frontend. Here I focused on testing and architecture (a userentity should be connected to a calender which has registered  different entities). Ones a user deletes his/her account then cascade should happen to all sensitive information connected to a user. 

### How to run
- Production: Run the Application.kt file or run mvn spring-boot:run from root folder in a commandline interface

- Testing: Run the LocalApplicationRunner or run mvn spring-boot:run -Dspring.profiles=swagger from root folder in a commandline terminal

- Open Swaggerurl on a browser
