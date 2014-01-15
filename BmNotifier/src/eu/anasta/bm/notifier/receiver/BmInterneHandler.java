package eu.anasta.bm.notifier.receiver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import eu.anasta.bm.notifier.login.ClientFormLogin;

public class BmInterneHandler extends AbstractHandler {
//	private String host;

//	public BmInterneHandler(String hostServer) {
//		host = hostServer;
//	}
//
//
//	public String getHost() {
//		return host;
//	}
//
//
//	public void setHost(String host) {
//		this.host = host;
//	}


	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		if ("/host".equals(target)){
			response.getWriter().println("{\"HOST\":\""+ ClientFormLogin.getInstance().getHost()+"\"}");
		}else 		if ("/hps".equals(target)){
			response.getWriter().println("{\"HPS\":\"" +ClientFormLogin.getInstance().login()+"\"}");
		}else{
			response.getWriter().println("<h1>Bonjour</h1>");
		}

	}

}
