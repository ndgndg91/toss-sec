FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# 빌드된 bootJar 복사
COPY build/libs/toss-dca-trader-0.0.1-SNAPSHOT.jar app.jar

# JVM 옵션 (giri-skills 최적화 가이드 반영: Generational ZGC 사용)
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70.0"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
