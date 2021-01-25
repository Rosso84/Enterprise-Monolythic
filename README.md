### About

In a group of two we rewrote an existing backend system from javascript to a springboot application for a company who owns a mobileapplication within e-health. Their goal was to get their aplication improved within the norm of e-health that were made included:

-When a user deleted their acccount it only deleted the sensitive data. The rest was kept for analysis purposes as this was one of the main wishes for improvements by product owner

-When a user stores images it will be encrypted

-Implementation of Logging

-Endpoints which previously did not work properly before were now working

-Implementation og unit testing and end to end testing using maven libraries like Junit and RestAssured

-Swagger for physical testing endpoints and documentation.
 

### How to run
- Production: Run the Application.kt file or run mvn spring-boot:run from root folder in a commandline interface

- Testing: Run the LocalApplicationRunner or run mvn spring-boot:run -Dspring.profiles=swagger from root folder in a commandline terminal

- Open Swaggerurl on a browser
