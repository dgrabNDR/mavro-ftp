package main.java.com.pulldocuments;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.java.com.salesforce.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class PullDocuments extends HttpServlet{
	private SalesforceConnector sc;
	Map<String,String> params = new HashMap<String,String>();
	
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws  IOException {
		Gson gson = new Gson();
		Map<String,String> parameters = new HashMap<String,String>();		
		String paramStr = getBody(req);
		System.out.println("Incoming Request => "+paramStr);
		
		// get params from request body
		parameters = (HashMap<String,String>) gson.fromJson(paramStr, params.getClass());
		parameters.putAll(CredentialManager.getLogin());
		this.params = parameters;
		System.out.println("parsing request body parameters...");	
		
		// login to salesforce and pull attachment
		sc = new SalesforceConnector(params.get("Username"),params.get("Password"),params.get("environment"));
		ArrayList<SObject> attachments = new ArrayList<SObject>();		
		try {	
			System.out.println("logging into salesforce...");
			sc.login();
			attachments = query(params.get("attId"));
			System.out.println("queried "+attachments.size()+" attachments");	
		} catch (ConnectionException e1) {
			e1.printStackTrace();
		}
		/*
		ArrayList<SObject> encryptedSObjs = new ArrayList<SObject>();
		ArrayList<File> encryptedFiles = new ArrayList<File>();
		
		// encrypt file
		System.out.println("encrypting attachment...");
		for(SObject so : attachments){			
			//EncryptFile.writeToFile((String)so.getField("Name"), (String)so.getField("Body"));
			try {
				EncryptFile ef = new EncryptFile();
				File suchEncrypt = ef.encrypt(base64ToByte((String)so.getField("Body")),(String)so.getField("Name"));							
				encryptedSObjs.add(fileToSObj((String)so.getField("ParentId"),(String)so.getField("Name")+".pgp",suchEncrypt));
				encryptedFiles.add(suchEncrypt);
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		// add attachment to report in salesforce
		System.out.println("adding salesforce attachment...");
		try {
			sc.create(encryptedSObjs);
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
	*/
		// upload files to ida ftp		
		UploadFile uf = new UploadFile();
		uf.start(params, null);
		try{
			uf.upload();
		} finally {
			System.out.println("did it work? "+uf.res == null);
		}
	}
	
	private SObject fileToSObj(String pId, String fileName, File theFile){
		SObject sObj = new SObject("Attachment");
		sObj.setField("ParentId", pId);
		sObj.setField("Name", fileName);
		byte[] body = null;
		try {
			body = Files.readAllBytes(theFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sObj.setField("Body", body);
		return sObj;
	}
	
	private File newFile(String fileName, byte[] body) throws FileNotFoundException, IOException{
		File theFile = new File(fileName);
		if(theFile.createNewFile()) {
        	FileOutputStream fos = new FileOutputStream(theFile);
        	fos.write(body);
        	fos.close();
        }
		return theFile;
	}
	
	private String getBody(HttpServletRequest req) throws IOException{
		BufferedReader br = req.getReader();
		StringBuilder sb = new StringBuilder();  
		String str;
	    while( (str = br.readLine()) != null ){
	        sb.append(str);
		 } 
	    return sb.toString().isEmpty() ? "{}" : sb.toString();
	}
	
	private ArrayList<SObject> query(String ids) throws ConnectionException{
		System.out.println("querying salesforce...");
		String idFilter;
		if(ids.contains(",")){
			String[] idParts = ids.split(",");
			idFilter = " Id IN(";
			for(String attId : idParts){
				if(idFilter == " Id IN("){
					idFilter = idFilter + "'" + attId + "'";
				}else {
					idFilter = idFilter + ",'" + attId + "'";
				}
			}
			idFilter = idFilter + ")";
		} else {
			idFilter = " Id = '"+ids+"'";
		}
		String soql = "SELECT Id, Name, ParentId, Body FROM Attachment WHERE "+idFilter;
		return sc.query(soql);
	}
	
	public byte[] base64ToByte(String data) throws Exception {
		return Base64.decodeBase64(data.getBytes());
	}
}
