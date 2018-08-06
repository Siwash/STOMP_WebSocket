package cn.seisys.rpf.StompConfig;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * ��������������Ӧ, ��WebSocketHandler��������
 */
public class HandleShakeInterceptors implements HandshakeInterceptor {

	/**
     * ������֮ǰִ�и÷���, �������ַ���true, �ж����ַ���false.
     * ͨ��attributes��������WebSocketSession������
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */
	
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String name= ((ServletServerHttpRequest) request).getServletRequest().getParameter("name");
        System.out.println("======================Interceptor" + name);
        //����ͻ��˱�ʶ
        attributes.put("name", "8888");
        return true;
    }

    /**
     * ������֮��ִ�и÷���. �����Ƿ����ֳɹ���ָ������Ӧ״̬�����Ӧͷ.
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param exception
     */
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {

    }
    
}
