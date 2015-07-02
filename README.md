ddth-log2scribe
===============

Log4j & Slf4j adapter to write logs to scribe server.

Project home:
[https://github.com/DDTH/ddth-log2scribe](https://github.com/DDTH/ddth-log2scribe)

OSGi environment: ddth-log2scribe modules are packaged as an OSGi bundle.


## Installation ##

Latest release version: `0.1.0`. See [RELEASE-NOTES.md](RELEASE-NOTES.md).

Maven dependency:

```xml
<dependency>
	<groupId>com.github.ddth</groupId>
	<artifactId>ddth-log2scribe</artifactId>
	<version>0.1.0</version>
</dependency>
```

## Usage ##

### Log4j Configurations ###

```
log4j.appender.X=com.github.ddth.log2scribe.log4jappender.ScribelogAppender
log4j.appender.X.layout=org.apache.log4j.PatternLayout
log4j.appender.X.layout.conversionPattern=%-5p [%t]: %m%n
log4j.appender.X.scribeHostsAndPorts=localhost:1463
log4j.appender.X.scribeCategory=my-category-name
```

### Logback (Slf4j) Configurations ###

```xml
<appender name="SCRIBELOG"
        class="com.github.ddth.log2scribe.slf4jappender.ScribelogAppender">
    <scribeHostsAndPorts><![CDATA[localhost:1463]]></scribeHostsAndPorts>
    <scribeCategory><![CDATA[my-category-name]]></scribeCategory>
    <layout class="ch.qos.logback.classic.PatternLayout">
        <pattern><![CDATA[%date - [%level] - %class{}(%file:%line\) / %thread%n%message%n%xException%n]]></pattern>
    </layout>
</appender>
```


## License ##

See LICENSE.txt for details. Copyright (c) 2015 Thanh Ba Nguyen.

Third party libraries are distributed under their own licenses.
