FROM openjdk:21
ENV TZ=GMT+8
CMD ["sh","-c","cd /moe-backend && java -jar -XX:MaxRAM=100M -Xss512k moe-backend-main.jar --spring.profiles.active=prod"]