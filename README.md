### About

In a group of two we rewrote an existing backend system from javascript to a springboot application for a company who owns a mobileapplication within e-health. Their goal was to get their aplication improved within the norm of e-health. What we managed to improve in order to make it more robust and secure included:

-When a user deleted their account it only deleted the sensitive data. The rest was kept for analysis purposes as this was one of the main wishes for improvements by product owner

-When a user stores images it will be encrypted

-Implementation of Logging

-Endpoints which previously did not work properly before were now working

-Implementation of unit testing and end to end testing using maven libraries like Junit and RestAssured with 90% code coverage (no tests implemented on the previous one)

-Swagger for physical testing endpoints and documentation.

 

### How to run
- Production: Run the Application.kt file or run mvn spring-boot:run from root folder in a commandline interface

- Testing: Run the LocalApplicationRunner or run mvn spring-boot:run -Dspring.profiles=swagger from root folder in a commandline terminal

- Open Swaggerurl on a browser
