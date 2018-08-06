package cn.seisys.rpf.StompConfig;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 检查握手请求和响应, 对WebSocketHandler传递属性
 */
public class HandleShakeInterceptors implements HandshakeInterceptor {

	/**
     * 在握手之前执行该方法, 继续握手返回true, 中断握手返回false.
     * 通过attributes参数设置WebSocketSession的属性
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
        //保存客户端标识
        attributes.put("name", "8888");
        return true;
    }

    /**
     * 在握手之后执行该方法. 无论是否握手成功都指明了响应状态码和相应头.
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
