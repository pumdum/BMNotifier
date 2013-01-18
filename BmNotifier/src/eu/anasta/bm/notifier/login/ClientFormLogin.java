package eu.anasta.bm.notifier.login;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class ClientFormLogin {
	private static final Logger LOG = Logger.getLogger(ClientFormLogin.class);
	private String host;
	private String user;
	private String password;
	private static ClientFormLogin instance;
	private DefaultHttpClient httpclient;

	public static ClientFormLogin getInstance() {
		if (instance == null) {
			LOG.debug("instancie clientFomLoger");
			instance = new ClientFormLogin();
		}
		return instance;
	}

	public void init(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
		httpclient = new DefaultHttpClient();
	}

	public String login() {
		try {
			HttpGet httpget = new HttpGet("https://" + host + "/");

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			LOG.debug("Login form get: " + response.getStatusLine());
			EntityUtils.consume(entity);
			// HttpResponse response ;
			// HttpEntity entity ;
			if (response.getFirstHeader("BMAuth")!=null && response.getFirstHeader("BMAuth").getValue().equals("OK")) {
				List<Cookie> cookies = httpclient.getCookieStore().getCookies();
				if (cookies.isEmpty()) {
					LOG.debug("Session not enabled");
				} else {
					for (int i = 0; i < cookies.size(); i++) {
						if (cookies.get(i).getName().equals("BMHPS")) {
							LOG.debug("find cookies BMHPS");
							return cookies.get(i).getValue();
						}
					}
				}
			}
			HttpPost httpost = new HttpPost("https://" + host
					+ "/bluemind_sso_security?");

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("login", user));
			nvps.add(new BasicNameValuePair("password", password));
			nvps.add(new BasicNameValuePair("priv", "public"));
			// nvps.add(new BasicNameValuePair("storedRequestId", "public"));

			httpost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

			response = httpclient.execute(httpost);
			entity = response.getEntity();

			LOG.debug("Login form post: " + response.getStatusLine());
			EntityUtils.consume(entity);
			if (response.getFirstHeader("BMAuth")!=null && response.getFirstHeader("BMAuth").getValue().equals("OK")) {
				List<Cookie> cookies = httpclient.getCookieStore().getCookies();
				if (!cookies.isEmpty()) {
					for (int i = 0; i < cookies.size(); i++) {
						if (cookies.get(i).getName().equals("BMHPS")) {
							LOG.debug("bmhps : "+cookies.get(i).getValue());
							return cookies.get(i).getValue();
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			// TODO log error
			return null;
		}
		// finally {
		// // When HttpClient instance is no longer needed,
		// // shut down the connection manager to ensure
		// // immediate deallocation of all system resources
		// httpclient.getConnectionManager().shutdown();
		// }
	}

	public void closeFormLogin() {
		httpclient.getConnectionManager().shutdown();
	}
}
