FROM openjdk:17-jdk-slim

# 작업 디렉토리를 /app으로 설정
WORKDIR /app

# 빌드된 JAR 파일을 /app 디렉토리에 복사
COPY build/libs/*.jar app.jar

# Google Application Credentials 파일을 /app 디렉토리에 복사
COPY src/main/resources/StableFurnaceProject1.json /app/StableFurnaceProject1.json

# Google API 인증을 위한 환경 변수 설정
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/StableFurnaceProject1.json

# 애플리케이션이 사용하는 포트 8080 노출
EXPOSE 8080

# 컨테이너 시작 시 JAR 파일 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
