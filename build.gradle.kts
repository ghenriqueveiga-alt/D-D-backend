import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.dd"
version = "0.0.1-SNAPSHOT"
description = "Backend de sistema de venda de itens"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	// H2 usado apenas pelo perfil "test" (banco em memória + h2-console).
	// No Spring Boot 4 a autoconfiguração do console saiu do spring-boot-autoconfigure
	// e vive no módulo spring-boot-h2console.
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.springframework.boot:spring-boot-h2console")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// ==========================================================================
// Perfis de execução — aparecem no IntelliJ em:
//   Gradle (painel lateral) → D-D-backend → Tasks → application
// Duplo clique roda; botão direito → "Run" cria uma Run Configuration salva.
//
//   ./gradlew bootRunDev   # dev  : PostgreSQL + Hibernate ddl-auto=update (8090)
//   ./gradlew bootRunTest  # test : H2 em memória + h2-console            (8091)
//   ./gradlew bootRunProd  # prod : Liquibase + PostgreSQL (validate)      (8092)
//
// Porta alternativa: -Pport=8099 (ex.: ./gradlew bootRunTest -Pport=9091)
// Obs.: dev e prod apontam para o mesmo banco (dd_store) — não rode os dois
// ao mesmo tempo.
// ==========================================================================
listOf(
	"bootRunDev" to "dev",
	"bootRunTest" to "test",
	"bootRunProd" to "prod",
).forEach { (taskName, profile) ->
	val defaultPort = when (profile) {
		"dev" -> "8090"
		"test" -> "8091"
		else -> "8092"
	}
	val port = (findProperty("port") as String?) ?: defaultPort
	tasks.register<BootRun>(taskName) {
		group = "application"
		description = "Sobe a aplicação com o perfil '$profile' na porta $port."
		classpath = sourceSets["main"].runtimeClasspath
		mainClass = "com.dd.backend.DDBackendApplication"
		args("--spring.profiles.active=$profile", "--server.port=$port")
	}
}
