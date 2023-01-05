# thesisPoC
Proof-of-concept tool for the thesis 'Toward automatic application redesign based on saga patterns for monolith to microservices migration'. The tool can automatically redesign a functionality of a monolithic application based on saga patterns, and finds the optimal redesign based on quality attributes of saga redesigns. As this is a Proof-of-concept, please consider it for evaluation but not for production purposes.

This thesis project was conducted at the University of Amsterdam by Maré du Preez.

Currently implemented for Spring-Boot monoliths that use FenixFramework and Spring Data ORMs.

[//]: # (### Pre-Requisites)

[//]: # ()
[//]: # (- java 8+     &#40;```java --version```&#41;)

[//]: # (- nodejs 10+  &#40;```node --version```&#41;)

[//]: # (- npm 6+      &#40;```npm --version```&#41;)

## Run manually

To run the collectors:

	cd collectors/
	see README.md for each collector

To run the backend:

	cd backend/
	mvn clean install -DskipTests
	mvn spring-boot:run
