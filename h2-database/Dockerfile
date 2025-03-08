FROM eclipse-temurin:21
# Port to expose for web console
EXPOSE 9081
# Port to expose for tcp connections
EXPOSE 9082

COPY h2-2.1.210.jar h2.jar
CMD  java -cp h2.jar org.h2.tools.Server -web -webDaemon -webAllowOthers -webPort 9081 -tcp -tcpAllowOthers -tcpPort 9082 -ifNotExists