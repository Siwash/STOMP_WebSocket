package cn.seisys.rpf.StompConfig;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketStompConfig extends AbstractWebSocketMessageBrokerConfigurer{

	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/stomp")
		.setHandshakeHandler(new DefaultHandshakeHandler() {
			@Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                  //将客户端标识封装为Principal对象，从而让服务端能通过getName()方法找到指定客户端
                  Object o = attributes.get("name");
                  return new FastPrincipal(o.toString());
            }
      })
      //添加socket拦截器，用于从请求中获取客户端标识参数
		.addInterceptors(new HandleShakeInterceptors()).withSockJS();
		
	}
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		//客户端发送消息的请求前缀
		registry.setApplicationDestinationPrefixes("/app");
        //客户端订阅消息的请求前缀，topic一般用于广播推送，queue用于点对点推送
		registry.enableSimpleBroker("/topic", "/queue");
        //服务端通知客户端的前缀，可以不设置，默认为user
		registry.setUserDestinationPrefix("/user");
		/*	如果是用自己的消息中间件，则按照下面的去配置，删除上面的配置
		 *	 registry.enableStompBrokerRelay("/topic", "/queue")
			.setRelayHost("rabbit.someotherserver")
			.setRelayPort(62623)
			.setClientLogin("marcopolo")
			.setClientPasscode("letmein01");
			registry.setApplicationDestinationPrefixes("/app", "/foo");
		 * */
		

	}
//定义一个自己的权限验证类
	class FastPrincipal implements Principal {

	    private final String name;

	    public FastPrincipal(String name) {
	        this.name = name;
	    }

	    public String getName() {
	        return name;
	    }
	}
}
