package eu.anasta.bm.notifier.receiver;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

public class HttpServer {
	Thread serverThread;
	Server server ;
	public void launch() {
		serverThread = new Thread("UniqueInstance-PortListenerThread") {

			{
				setDaemon(true);
			}

			@Override
			public void run() {
				try {
					server = new Server();
					SelectChannelConnector http= new SelectChannelConnector();
			        http.setHost("localhost");
			        http.setPort(51985);
			        http.setMaxIdleTime(30000);
					server.setConnectors(new Connector[]{ http});
					server.setHandler(new BmInterneHandler());
					server.start();
					server.join();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		/* On démarre le Thread. */
		serverThread.start();

	}
	
	public void stop(){
	if (server!=null){
		try {
			server.stop();
		} catch (Exception e) {
			
			e.printStackTrace();
			server.destroy();
		}
	}
}
}
