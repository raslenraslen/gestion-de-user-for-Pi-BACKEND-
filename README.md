# Configuring a Microservice with Spring Boot and Config Server

This guide explains how to configure your Spring Boot microservice and connect it to a Config Server, 
while creating necessary files and folders to ensure smooth integration. 

## 1. Modify `application.yml` in Your Microservice

In your Spring Boot microservice, open the `application.yml` file and configure the application name and 
connection to the Config Server. The configuration should look like this:

```yaml
spring:
  application:
    name: %name%
  config:
    import: optional:configserver:http://localhost:8888
```
## 2. Add your Service to API Router

go to the Config-Server/src/main/resources
```yaml
        - id: %name% 
          uri: lb://%NAME% 
          predicates:
            - Path=/(path mte3ik)/**
```


11111111
