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
