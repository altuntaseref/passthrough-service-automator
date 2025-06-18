# Temel imajı kullanarak build aşaması
FROM altuntasserf/microservice-base:latest AS builder

WORKDIR /app

# Önce sadece pom.xml'i kopyalayın - temel imajdaki mevcut bağımlılıkları kullanacak
COPY pom.xml .

# Mikro servise özgü ek bağımlılıkları önbelleğe alın
# Ortak bağımlılıklar zaten temel imajda olduğu için çok daha hızlı olacak
RUN mvn dependency:go-offline -B -Dmaven.repo.local=/root/.m2/repository

# Kaynak kodları kopyalayın
COPY src ./src

# Uygulamayı derleyin
RUN mvn clean package -B -DskipTests -Dmaven.repo.local=/root/.m2/repository

# Çalışma zamanı imajı
FROM openjdk:17-jdk-slim

WORKDIR /app

# Sadece JAR dosyasını kopyalayın
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]