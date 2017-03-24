package main.java.com.pulldocuments;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

public class GenericFTPClient {
	
	private FTPSClient ftp = null;
	
	public GenericFTPClient(){
		ftp = new FTPSClient(false);
	}
	
	public GenericFTPClient(Boolean skipCertValidation){
		
		if(skipCertValidation){
			
			try {
				SSLContext sslContext = SSLContext.getInstance("TLS");
				TrustManager tm = new X509TrustManager() {
										public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
										}

										public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
										}

										public X509Certificate[] getAcceptedIssuers() {
										  return null;
										}
				};
				
				sslContext.init(null, new TrustManager[] { tm }, null);			
				ftp = new FTPSClient(false,sslContext);
			}catch (Exception e) {
				System.out.println("Error obtaining GenericFTPClient skipping certificate validation:" + e);
			}
		}else{
			ftp = new FTPSClient(false);
		}
	}
	
	public boolean connect(String host, String login, String password, int port) throws SocketException, IOException{
		System.out.println("host: "+host+", login: "+login+", pass: "+password+", port: "+port);
		ftp.connect(host);
		int reply = ftp.getReplyCode();
		Boolean res = FTPReply.isPositiveCompletion(reply) && ftp.login(login, password);
		if(res){
			System.out.println("Connected Success");
			ftp.execPBSZ(0);
            // Set data channel protection to private
            ftp.execPROT("P");
            // Enter local passive mode
		}
		return res;
	}
	
	public boolean changeDir(String remotePath) throws Exception {
		Boolean b = ftp.changeWorkingDirectory(remotePath);
		ftp.enterLocalPassiveMode();
		return b;
	}
	
	protected boolean uploadFile(InputStream is, String fileName) throws IOException {
		ftp.setFileType(FTP.BINARY_FILE_TYPE); 
		return ftp.storeFile(fileName,is);
	}
	
	public void logout() throws IOException {
		ftp.logout();
	}
	
	public void disconnect() throws IOException {
		ftp.disconnect();
	}
}
