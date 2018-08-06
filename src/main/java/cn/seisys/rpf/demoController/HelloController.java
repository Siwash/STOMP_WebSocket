package cn.seisys.rpf.demoController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
@RequestMapping("/")
public class HelloController {
	@RequestMapping("index.html")
	public String hello(HttpServletRequest request,HttpServletResponse response) {
		System.out.println("=======>>hello spring");
		request.setAttribute("msg", "hello spring");
		return "index";
	}
	@RequestMapping("socket.html")
	public String toSocket(HttpServletRequest request,HttpServletResponse response) {
		System.out.println("=======>>hello Socket");
		request.setAttribute("msg", "hello spring");
		return "socket";
	}
}
