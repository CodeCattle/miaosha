package com.seckillproject.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

//当spring容器中没有TomcatEmbeddedServletContainedFactory这个容器的时候,会把这个bean加载进来
@Component
public class WebServerConfiguration implements
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>{
    @Override
    public void customize(ConfigurableServletWebServerFactory configurableWebServerFactory) {
        //定制化Tomcat策略
        //使用对应工厂类提供给我们的接口定制化Tomcat connector
        ((TomcatServletWebServerFactory) configurableWebServerFactory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocol =(Http11NioProtocol)connector.getProtocolHandler();
                //定制化设置keepalive，如果30秒内没有连接，就自动断开连接
                protocol.setKeepAliveTimeout(30000);
                //超过10000连接断开
                protocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
