Raiden JPA
=========

A really fast JPA subset implementation to be used in development enviroments. 

STOP using mocks to improve your tests velocity, START using RaidenJPA.

Hibernate vs Raiden JPA
=========

(55 sec) https://www.youtube.com/watch?v=BPrVga6dRRA

Is it ready to use?
=========

Currently, there are already two (private) projects using it. Although it is not complete, you can try it in your project without fear, because its intention is not to go to production, but to help you in your development environment. You can see issues in GitHub.

Do you want help to put it on your project? Send me an email: andreitognolo@gmail.com

Getting Started
=========
    
Dependency:

    <dependency>
        <groupId>org.raidenjpa</groupId>
        <artifactId>raidenjpa-core</artifactId>
        <version>0.0.4</version>
    </dependency>
    
In your code:

    // As soon as possible, it will be able to use <provider> in persistence.xml
    EntityManagerFactory emf = new RaidenEntityManagerFactory();

Development
=========

Build Status (by Travis):

[![Build Status](https://travis-ci.org/andreitognolo/raidenjpa.png)](http://travis-ci.org/andreitognolo/raidenjpa)

## @Before

You need Java 7+ and maven 3+
