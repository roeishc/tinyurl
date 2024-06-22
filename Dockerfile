FROM openjdk:11
COPY target/tinyurl*.jar /usr/src/tinyurl.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
COPY entrypoint.sh /opt/entrypoint.sh
# make the script executable
RUN chmod +x /opt/entrypoint.sh
# execute script
CMD ["/opt/entrypoint.sh"]