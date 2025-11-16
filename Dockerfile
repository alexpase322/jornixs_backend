# --- Etapa 1: Construcción (Usando Maven para compilar el .war) ---
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Etapa 2: Imagen Final (Un servidor Tomcat listo) ---
# Usamos una imagen oficial de Tomcat que ya incluye Java
FROM tomcat:10.1-jdk17-temurin

# Elimina la aplicación de ejemplo que viene con Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia tu .war (asegúrate de que el nombre coincida)
# Al renombrarlo a ROOT.war, haces que sea la aplicación por defecto del servidor.
COPY --from=build /app/target/chronotrack-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat se ejecuta en el puerto 8080 por defecto
EXPOSE 8080

# El comando de inicio ya está definido en la imagen base de Tomcat
CMD ["catalina.sh", "run"]