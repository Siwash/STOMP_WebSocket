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
                  //���ͻ��˱�ʶ��װΪPrincipal���󣬴Ӷ��÷������ͨ��getName()�����ҵ�ָ���ͻ���
                  Object o = attributes.get("name");
                  return new FastPrincipal(o.toString());
            }
      })
      //���socket�����������ڴ������л�ȡ�ͻ��˱�ʶ����
		.addInterceptors(new HandleShakeInterceptors()).withSockJS();
		
	}
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		//�ͻ��˷�����Ϣ������ǰ׺
		registry.setApplicationDestinationPrefixes("/app");
        //�ͻ��˶�����Ϣ������ǰ׺��topicһ�����ڹ㲥���ͣ�queue���ڵ�Ե�����
		registry.enableSimpleBroker("/topic", "/queue");
        //�����֪ͨ�ͻ��˵�ǰ׺�����Բ����ã�Ĭ��Ϊuser
		registry.setUserDestinationPrefix("/user");
		/*	��������Լ�����Ϣ�м�������������ȥ���ã�ɾ�����������
		 *	 registry.enableStompBrokerRelay("/topic", "/queue")
			.setRelayHost("rabbit.someotherserver")
			.setRelayPort(62623)
			.setClientLogin("marcopolo")
			.setClientPasscode("letmein01");
			registry.setApplicationDestinationPrefixes("/app", "/foo");
		 * */
		

	}
//����һ���Լ���Ȩ����֤��
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
