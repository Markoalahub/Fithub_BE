package markoala.fithub.demo.global.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TomcatSettingsLogger {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @EventListener
    public void onWebServerReady(WebServerInitializedEvent ev) {
        var webServer = ev.getWebServer();
        if (webServer instanceof TomcatWebServer tomcat) {
            Connector connector = tomcat.getTomcat().getConnector();
            ProtocolHandler handler = connector.getProtocolHandler();
            log.info("Connector protocol: {}", connector.getProtocol());
            if (handler instanceof AbstractHttp11Protocol<?> http) {
                try {
                    log.info("maxHttpHeaderSize = {}", http.getMaxHttpHeaderSize());
                } catch (Throwable t) {
                    log.warn("unable to read maxHttpHeaderSize", t);
                }
                try {
                    log.info("maxSwallowSize = {}", http.getMaxSwallowSize());
                } catch (Throwable t) {
                    log.warn("unable to read maxSwallowSize", t);
                }
                try {
                    // Not all Tomcat versions expose getMaxSavePostSize
                    log.info("maxSavePostSize = {}", http.getMaxSavePostSize());
                } catch (Throwable t) {
                    log.debug("getMaxSavePostSize not available", t);
                }
            }
            // connector.getMaxPostSize() available
            try {
                log.info("connector.maxPostSize(attribute) = {}", connector.getMaxPostSize());
            } catch (Throwable t) {
                log.warn("unable to read connector maxPostSize", t);
            }
        } else {
            log.warn("WebServer is not TomcatWebServer: {}", webServer.getClass());
        }
    }
}
