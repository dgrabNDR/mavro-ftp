package main.java.com.pulldocuments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

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
		
		// upload files to ida ftp		
		SFTPConnector connector = new SFTPConnector();
		connector.start(params);
		try{
			connector.connect();
			
			connector.sftpChannel.cd("/E:/Opex/Mavro");
			System.out.println("pwd: "+connector.sftpChannel.pwd());
			Vector<ChannelSftp.LsEntry> topLevel = connector.sftpChannel.ls("*");
			HashMap<String,File> mapFiles = new HashMap<String,File>();	
			// display contents of top level directory
			for(ChannelSftp.LsEntry dayFolder : topLevel){
				System.out.println("opening day folder: "+dayFolder.getFilename());
				connector.sftpChannel.cd("/E:/Opex/Mavro/"+dayFolder.getFilename());
				Vector<ChannelSftp.LsEntry> lstBatch = connector.sftpChannel.ls("*");
				// display contents of day level directory
				for(ChannelSftp.LsEntry batchFolder : lstBatch){
					if(batchFolder.getFilename().indexOf("Shortcut.lnk") == -1 && batchFolder.getFilename().indexOf("Thumbs.db") == -1){
						System.out.println("opening batch folder: "+batchFolder.getFilename());
						connector.sftpChannel.cd("/E:/Opex/Mavro/"+dayFolder.getFilename()+"/"+batchFolder.getFilename());
						Vector<ChannelSftp.LsEntry> lstFiles = connector.sftpChannel.ls("*");
						System.out.println("pulling contents of batch folder...");
						// display contents of batch level directory
						File xmlFile = null;
						Integer count = 0;
						for(ChannelSftp.LsEntry file : lstFiles){
							if(file.getFilename().indexOf(".xml") == -1){
								//System.out.println("found pdf file: "+file.getFilename());
								count++;
								InputStream is = connector.sftpChannel.get(file.getFilename());
								mapFiles.put(file.getFilename(), inputStreamToFile(is, file.getFilename()));

							} else {
								System.out.println("found xml file: "+file.getFilename());
								InputStream is = connector.sftpChannel.get(file.getFilename());
								xmlFile = inputStreamToFile(is, file.getFilename());
							}
						}
						System.out.println("pulled "+count+" pdf files");
						if(xmlFile != null){
							System.out.println("parsing xmlFile... ");
							// parse xml file
							// create salesforce records
						}
					}
				}
				// move day folder to MavroArchive
			}
			// do the things
			/*
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
			
			// create new SF attachment objects
			ArrayList<SObject> lstSObj = new ArrayList<SObject>();
			for(SObject so : attachments){			
				try {						
					lstSObj.add(fileToSObj((String)so.getField("ParentId"),(String)so.getField("Name")+".pgp",attachments));
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
			
			// add attachment to report in salesforce
			System.out.println("adding salesforce attachment...");
			try {
				sc.create(lstSObj);
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
			*/
			
			connector.disconnect();
		} catch (SftpException e){
			
		} finally {}
		
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
	
	public static File inputStreamToFile(InputStream in, String fileName) throws IOException {
		File tempFile = new File(fileName);
        tempFile.deleteOnExit();
        try {
        	FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
        } catch (IOException e){} finally {}
        return tempFile;
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