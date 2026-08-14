# ---------- Stage 1 : Build ----------
# Maven 3.9 avec JDK 21 (version du projet : java.version = 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Docker layer : copier pom.xml seul et résoudre les dépendances d'abord
# -> tant que pom.xml est inchangé, cette étape est mise en cache
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copier le reste des sources puis builder le jar
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2 : Runtime ----------
# JRE léger (sans JDK) pour réduire la taille de l'image finale
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Ne copier que le jar buildé au stage 1
COPY --from=build /app/target/rag-mcp-assistant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Les secrets et la config DB sont injectés via variables d'environnement (docker-compose)
ENTRYPOINT ["java", "-jar", "app.jar"]
