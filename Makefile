GEOIPJAVA_SOURCE = GeoIPJava-1.2.4/source
NETSERV_CORE_JAVA = ../../../core/java
CP = $(NETSERV_CORE_JAVA)/ext-jars/servlet-api-2.5-20081211.jar:$(NETSERV_CORE_JAVA)/ext-jars/jetty-6.1.24.jar:$(NETSERV_CORE_JAVA)/ext-jars/jetty-util-6.1.24.jar:classes

.PHONY: default
default: activestreaming-server.jar

activecdn-server.jar: \
    classes/com/maxmind/geoip/Location.class \
    classes/com/maxmind/geoip/DatabaseInfo.class \
    classes/com/maxmind/geoip/regionName.class \
    classes/com/maxmind/geoip/timeZone.class \
    classes/com/maxmind/geoip/Country.class \
    classes/com/maxmind/geoip/Region.class \
    classes/com/maxmind/geoip/LookupService.class \
    classes/NetServ/apps/ActiveStreaming/server/IPAddressLocation.class \
    classes/NetServ/apps/ActiveStreaming/server/ContentSingleton.class \
    classes/NetServ/apps/ActiveStreaming/server/ContentServlet.class
	jar cf activestreaming-server.jar -C classes NetServ/apps/ActiveStreaming -C classes com/maxmind/geoip

classes/%.class: src/%.java
	@mkdir -p classes
	javac -encoding UTF8 -d classes -sourcepath src -cp $(CP) $<

classes/%.class: $(GEOIPJAVA_SOURCE)/%.java
	@mkdir -p classes
	javac -encoding UTF8 -d classes -sourcepath $(GEOIPJAVA_SOURCE) -cp $(CP) $<

.PHONY: tags
tags:
	ctags -R .

.PHONY: clean
clean:
	rm -rf classes javadoc .DS_Store *~ tags a.out core 
	rm -f activecdn-server.jar
	find src -name "*~" -exec rm -f '{}' \;

.PHONY: all
all: clean default tags
