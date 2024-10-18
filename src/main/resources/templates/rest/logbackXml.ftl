<configuration scan="true" scanPeriod="30 seconds">
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>
    <property name="STACK_TRACE_COUNT" value="15"/>
    <property name="CLASS_NAME_LENGTH" value="40"/>
    <springProperty scope="context" name="app_name" source="spring.application.name"/>

    <property name="HOSTNAME" value="MAIL"/>
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>172.19.226.130:4560</destination>
        <addDefaultStatusListener>false</addDefaultStatusListener>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">

            <providers>
                <pattern>
                    <pattern>
                        {
                        "timestamp": "%date{yyyy-MM-dd'T'HH:mm:ss.SSSZZ}",
                        "level": "%level",
                        "logger": "%logger",
                        "message": "%message",
                        "thread": "%thread",
                        "exception": "%ex",
                        "mdc": "%mdc",
                        "context": "${projectName}-uat"
                        }
                    </pattern>
                </pattern>
            </providers>

        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
