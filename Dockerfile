# Base image có JDK 17
FROM eclipse-temurin:17-jdk

# Thư mục làm việc trong container
WORKDIR /app

# Copy toàn bộ source code vào container
COPY . .

# Cấp quyền chạy cho Maven Wrapper
RUN chmod +x ./mvnw

# Build ứng dụng (bỏ qua test để nhanh hơn)
RUN ./mvnw clean package -DskipTests

# Câu lệnh khởi động ứng dụng
CMD ["java", "-jar", "target/MyApp-0.0.1-SNAPSHOT.jar"]
