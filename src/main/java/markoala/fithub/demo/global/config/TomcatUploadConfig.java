package markoala.fithub.demo.global.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 내장 서버의 대용량 파일 업로드를 위한 설정.
 * application.yaml만으로는 Tomcat connector의 maxPostSize/maxSwallowSize가
 * 적용되지 않는 경우가 있어, 코드로 직접 설정합니다.
 */
@Configuration
public class TomcatUploadConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // maxPostSize: -1 = 무제한
            connector.setMaxPostSize(-1);

            // maxSwallowSize: -1 = 무제한 (업로드 초과 시 연결 끊김 방지)
            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setMaxSwallowSize(-1);
            }
        });
    }
}
