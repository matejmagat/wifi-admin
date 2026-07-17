plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	// generates Java source classes from WSDL files using Apache CXF
	id("com.github.bjornvester.wsdl2java") version "2.0.2"
}

group = "com.ht-rnd"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

wsdl2java {
	wsdlDir.set(layout.projectDirectory.dir("../wsdl"))
}

repositories {
	mavenCentral()
}

dependencies {
	// REST API
	implementation("org.springframework.boot:spring-boot-starter-web")

	// SOAP client
	implementation("org.springframework.ws:spring-ws-core")
	implementation("wsdl4j:wsdl4j")

	// XML/JAXB
	implementation("org.glassfish.jaxb:jaxb-runtime")

	// Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Testing
	implementation("com.fasterxml.jackson.core:jackson-databind")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.ws:spring-ws-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
