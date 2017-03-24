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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import main.java.com.salesforce.SalesforceConnector;

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
			
			System.out.println("setting up GenericFTPClient...");
			GenericFTPClient sftp = new GenericFTPClient();
			System.out.println(sftp);
			sftp.connect(params.get("ftphost"), params.get("ftpuser"), params.get("ftppass"), 22);
			System.out.println("Change folder status:"+sftp.changeDir(params.get("ftpfolder")));

			File att = lstAtt.get(0);
			byte[] fileBytes = readFile(att);
		      
			InputStream is1 = new ByteArrayInputStream(fileBytes);
			
			res = sftp.uploadFile(is1, (String) att.getName());

			if(res){
				System.out.println("upload successful");
			}
			System.out.println(res);

			sftp.logout();
		}
		catch(Exception ex){
			ex.printStackTrace();
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
