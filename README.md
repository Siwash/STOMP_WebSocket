#webSocket进阶篇
------
> * ##背景介绍
>  之前提到使用原始的websocket，实现后台消息的主动推送，详情见：[WebSocket入门](https://www.jianshu.com/p/326290d38abe)。但是这种方式过于偏向底层，需要开发人员去手动的保存用户连接到websocket中的信息，这个信息不仅仅是用户的id、name而已还要保存他们的订阅信息，因为完完全全有可能所有已连接用户需要推送的消息是不一样的，而且可能一个用户会订阅很多的推送信息，比如说：在一个新闻网页中，有的用户对军事感兴趣，有的用户对科技感兴趣，有的对开源的代码感兴趣，而有的可能对所有的都感兴趣，如果你们老板要求这些东西需要根据数据库实时更新的话，在使用原始的websocket来管理就会变成十分麻烦。

### 基于STOMP协议的WebSocket
>使用STOMP的好处在于，它完全就是一种消息队列模式，你可以使用生产者与消费者的思想来认识它，发送消息的是生产者，接收消息的是消费者。而消费者可以通过订阅不同的destination，来获得不同的推送消息，不需要开发人员去管理这些订阅与推送目的地之前的关系，spring官网就有一个简单的spring-boot的[stomp-demo](https://spring.io/guides/gs/messaging-stomp-websocket/),如果是基于springboot，大家可以根据spring上面的教程试着去写一个简单的demo。

而stomp这种协议的核心思想如下图所示，spring官网也有：
![Stomp协议消息流程](http://static.zybuluo.com/a617137379/lw3z1s7xjf7eh82hghromyuq/image_1c0im0q9t13cs1boij5h12ho1i9919.png)
>我的理解就是：stomp定义了自己的消息传输体制。首先是通过一个后台绑定的连接点endpoint来建立socket连接，然后生产者通过send方法，绑定好发送的目的地也就是destination，而topic和app(后面还会说到)则是一种消息处理手段的分支，走app/url的消息会被你设置到的MassageMapping拦截到，进行你自己定义的具体逻辑处理，而走topic/url的消息就不会被拦截，直接到Simplebroker节点中将消息推送出去。其中simplebroker是spring的一种基于内存的消息队列，你也可以使用activeMQ，rabbitMQ代替。
###本文主要是基于springMVC实现stomp协议的websocket，下面是具体的实现细节
> 首先你得有一个springMVC的项目，可以在网页上实现简单的hello world即可

####1.也是同spring官网一样写一个stomp的注册中心，配置好具体的连接点，和订阅的分支出，代码如下：
```
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

```
然后是一个自己的握手拦截器，用户验证连接是否合法，这里只是做了简单的处理，具体可根据具体的业务去改写，代码：
```
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

```
### 2.在controller层中写具体的app/拦截到url所做出的具体业务处理：
```
package cn.seisys.rpf.stompController;



import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StompController {
	@Autowired
	SimpMessagingTemplate SMT;
	
	@MessageMapping("/send")
	public void subscription(String str) throws MessagingException, UnsupportedEncodingException {
	System.err.println(str);
	SMT.convertAndSend("/topic/sub","开始推送消息了："+str);
		
	}
	
}

```
>Tips：`SimpMessagingTemplate`这个bean是当你的配置生效后，spring自动注入的bean，直接用就可以了。它可以实现注解`@sendto`或者`@sendtoUser`的所有功能，并且可以在任意地方使用(sendto系列注解必须要在controller中陪着MassageMapp使用)，用它就可以实现后台的主动推送消息。当然sendto也有它的好处，比如直接将你得pojo转json字符串发到对于的消费者那里。
###3.最后把我们的spring-mvc的xml配置一下，使我们的configuration生效，后台就搞定了，代码如下：
```
<context:annotation-config />

    <mvc:annotation-driven />
	 
    <context:component-scan base-package="你配置的路径" />
```
###4.贴出依赖的pom文件代码：
```
<dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
    
    <dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.0</version>
    <scope>compile</scope>
	</dependency>
	
	 <!-- https://mvnrepository.com/artifact/org.springframework/spring-context -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-web -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-webmvc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-websocket</artifactId>
      <version>${spring.version}</version>
      
    </dependency>
	<dependency>  
	  <groupId>org.springframework</groupId>  
	  <artifactId>spring-messaging</artifactId>  
	  <version>${spring.version}</version>
	  
	</dependency>
	<!-- https://mvnrepository.com/artifact/javax.servlet/jstl -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>jstl</artifactId>
    <version>1.2</version>
</dependency>
	
	 <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.5.3</version>
            <scope>runtime</scope>
        </dependency>
	
  </dependencies>
  <properties>
  	<maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <spring.version>4.2.5.RELEASE</spring.version>
  </properties>
```
> 到此后台代码完成，开始配置前端jsp
### 5.前端代码：
```
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>stomp</title>
 <link href="https://cdn.bootcss.com/bootstrap/4.1.1/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.bootcss.com/jquery/3.3.1/jquery.min.js"></script>
    <script src="https://cdn.bootcss.com/sockjs-client/1.1.4/sockjs.min.js"></script>
    <script src="https://cdn.bootcss.com/stomp.js/2.3.3/stomp.min.js"></script>
   
</head>
<body>
<noscript><h2 style="color: #ff0000">Seems your browser doesn't support Javascript! Websocket relies on Javascript being
    enabled. Please enable
    Javascript and reload this page!</h2></noscript>
<div id="main-content" class="container">
    <div class="row">
        <div class="col-md-6">
            <form class="form-inline">
            	<div class="form-group">
                    <label for="connect">register an user,input name:</label>
                    <input type="text" id="username" class="form-control" placeholder="Your name here...">
                    <button id="confirm" class="btn btn-default" type="submit">confirm</button>
                </div>
                <div class="form-group">
                    <label for="connect">WebSocket connection:</label>
                    <button id="connect" class="btn btn-default" type="submit">Connect</button>
                    <button id="disconnect" class="btn btn-default" type="submit" disabled="disabled">Disconnect
                    </button>
                </div>
            </form>
        </div>
        <div class="col-md-6">
            <form class="form-inline">
                <div class="form-group">
                    <label for="name">What is your name?</label>
                    <input type="text" id="name" class="form-control" placeholder="Your name here...">
                </div>
                <button id="send" class="btn btn-default" type="submit">Send</button>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <table id="conversation" class="table table-striped">
                <thead>
                <tr>
                    <th>Greetings</th>
                </tr>
                </thead>
                <tbody id="greetings">
                </tbody>
            </table>
        </div>
    </div>
</div>
<script type="text/javascript">
/**
 * 
 */
var stompClient = null;

var hostaddr = window.location.host + "<c:url value='/stomp/' />";
var url = 'ws://' + hostaddr;
function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}
var username="";
function connect() {
    var socket = new SockJS("stomp");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/sub', function (greeting) {
            showGreeting(greeting.body);
        });
        stompClient.subscribe('/user/'+username+'/topic/sub', function (greeting) {
            showGreeting(greeting.body);
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.send("/topic/message", {},$("#name").val());
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendName(); });
    $("#confirm").click(function(){username=$("#username").val();connect();});
    	
  
});
</script>
</body>
</html>
```
>  * 这个前端的demo也是直接从spring官网复制的，它的连接和发送方式和原生的websocket是完全不一样的，首先注意一下几点：
>  **1.** 通过sockJS绑定好服务器中配置的endpoint连接点，并通过stomp.over方式创建一个stompClient，完成客户端的创建。
> **2.**再通过stompClient.subscribe订阅N多个的消息地址。
> **3.**发送消息的时候也同样的通过stompClient.send方法去发送消息到指定的。destination

> 完整的代码已上传到gitHup上，欢迎[下载交流]()

##至此一个简单的基于stomp协议的websocket的spring-mvc项目已经完成。
不过问题是如果要写一个stomp的java客户端该怎么实现呢？因为原生websocket的方式已经不管用了，它也不再是一次性的连接了，而是先建立连接，再绑定订阅地址。