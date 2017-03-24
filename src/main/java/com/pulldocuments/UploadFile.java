package main.java.com.pulldocuments;

import java.io.ByteArrayInputStream; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import main.java.com.salesforce.SalesforceConnector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class UploadFile {
	public Map<String,String> params = new HashMap<String,String>();
	public ArrayList<File> lstAtt;
	public Boolean res;

	
	public void start(Map<String,String> p, ArrayList<File> files){
		System.out.println("starting uploader...");
		params = p;
		lstAtt = files;
	}
	
	public void upload(){
		
		try{
			// sets static ip addresses 
			System.out.println("Normal IP Address => "+InetAddress.getLocalHost().getHostAddress());
			System.out.println("setting up fixie...");
			URL fixie = new URL("http://"+System.getenv("FIXIE_SOCKS_HOST"));
			String fixieInfo = fixie.getUserInfo();
			System.out.println("fixieURL ==> "+fixieInfo);
			String[] fixieUserInfo = fixieInfo.split(":");
			String fixieUser = fixieUserInfo[0];
			String fixiePassword = fixieUserInfo[1];
			System.out.println("fixie u: "+fixieUser+" ,p: "+fixiePassword);
			System.setProperty("socksProxyHost", fixie.getHost());
			Authenticator.setDefault(new ProxyAuthenticator(fixieUser, fixiePassword));

			JSch jsch = new JSch();
			Session session = null;
			try {
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
			    ChannelSftp sftpChannel = (ChannelSftp) channel;
			    System.out.println("pwd: "+sftpChannel.pwd());
			    sftpChannel.exit();
			    session.disconnect();
			} catch (JSchException e) {
			    e.printStackTrace();  
			} catch (SftpException e) {
			    e.printStackTrace();
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: " + aReply);
            }
        }
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
	
	public byte[] readFile(File file) throws IOException{
		InputStream is = new FileInputStream(file);
		long length = file.length();
		byte[] bytes = new byte[(int)length];
		int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
        return bytes;
	}
	
	public byte[] base64ToByte(String data) throws Exception {
		return Base64.decodeBase64(data.getBytes());
	}
}
