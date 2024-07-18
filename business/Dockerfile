FROM bellsoft/liberica-openjdk-alpine-musl:21
ENV TZ=GMT+8
COPY ./target/moe-backend-main.jar .
CMD ["sh","-c","java -jar -XX:MaxRAM=128M -Xss512k -XX:+UseG1GC moe-backend-main.jar --spring.profiles.active=prod"]