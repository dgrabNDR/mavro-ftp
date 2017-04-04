package main.java.com.pulldocuments;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SFTPConnector {
	public Map<String,String> params = new HashMap<String,String>();
	public Boolean res;
	public ChannelSftp sftpChannel;
	public Session session;

	
	public void start(Map<String,String> p){
		System.out.println("starting connector...");
		params = p;
	}
	
	public void connect(){
		
		try{
			URL connection = new URL("http://checkip.amazonaws.com/");
			URLConnection con = connection.openConnection();
			String str = null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			str = reader.readLine();
			System.out.println("ip before fixie: "+str);
			
			// sets static ip addresses 
			System.out.println("connecting to fixie...");
			URL fixie = new URL("http://"+System.getenv("FIXIE_SOCKS_HOST"));
			String fixieInfo = fixie.getUserInfo();
			String[] fixieUserInfo = fixieInfo.split(":");
			String fixieUser = fixieUserInfo[0];
			String fixiePassword = fixieUserInfo[1];
			System.out.println("fixie u: "+fixieUser+" / p: "+fixiePassword);
			System.setProperty("socksProxyHost", fixie.getHost());
			Authenticator.setDefault(new ProxyAuthenticator(fixieUser, fixiePassword));

			connection = new URL("http://checkip.amazonaws.com/");
			con = connection.openConnection();
			str = null;
			reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			str = reader.readLine();
			System.out.println("ip after fixie: "+str);
			
			JSch jsch = new JSch();
	        session = null;
	        try {
	        	System.out.println("connecting to ftp...");
	            session = jsch.getSession(params.get("ftpuser"), params.get("ftphost"), 22);
	            Properties prop = new Properties();
	            prop.setProperty("StrictHostKeyChecking", "no");
	            session.setConfig(prop);
	            session.setPassword(params.get("ftppass"));	            
	            System.out.println("user: "+params.get("ftpuser")+" / pass: "+params.get("ftppass")+" / host: "+params.get("ftphost"));
	            System.out.println("connecting to session...");
	            session.connect();
	            Channel channel = session.openChannel("sftp");
	            System.out.println("connecting to channel...");
	            channel.connect();
	            sftpChannel = (ChannelSftp) channel;	            
	            System.out.println("connected!");
	        } catch (JSchException e) {
	            e.printStackTrace();  
	        }
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void disconnect(){
		System.out.println("disconnecting...");
		sftpChannel.exit();
        session.disconnect();
	}

	private class ProxyAuthenticator extends Authenticator {
	  private final PasswordAuthentication passwordAuthentication;
	  private ProxyAuthenticator(String user, String password) {
	    passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
	  }

	  @Override
	  protected PasswordAuthentication getPasswordAuthentication() {
	    return passwordAuthentication;
	  }
	}
}
