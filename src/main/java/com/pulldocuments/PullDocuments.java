package main.java.com.pulldocuments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import main.java.com.salesforce.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class PullDocuments extends HttpServlet{
	private SalesforceConnector sc;
	Map<String,String> params = new HashMap<String,String>();
	HashMap<String,File> mapFiles = new HashMap<String,File>();	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.getWriter().write("this is the response");
		resp.getWriter().flush();
		resp.getWriter().close();
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
		ArrayList<SObject> lstSObj = new ArrayList<SObject>();
		try{
			connector.connect();
			
			connector.sftpChannel.cd("/E:/Opex/Mavro");
			System.out.println("pwd: "+connector.sftpChannel.pwd());
			Vector<ChannelSftp.LsEntry> topLevel = connector.sftpChannel.ls("*");
			
			// display contents of top level directory
			for(ChannelSftp.LsEntry dayFolder : topLevel){
				if(dayFolder.getFilename().indexOf("Thumbs.db") > -1){
					System.out.println("deleting "+dayFolder.getFilename());
					connector.sftpChannel.rm("/E:/Opex/Mavro/"+dayFolder.getFilename());
				} else {
					System.out.println("opening day folder: "+dayFolder.getFilename());
					connector.sftpChannel.cd("/E:/Opex/Mavro/"+dayFolder.getFilename());
					Vector<ChannelSftp.LsEntry> lstBatch = connector.sftpChannel.ls("*");
					// display contents of day level directory
					for(ChannelSftp.LsEntry batchFolder : lstBatch){
						Integer count = 0;
						if(batchFolder.getFilename().indexOf("Shortcut.lnk") == -1 && batchFolder.getFilename().indexOf("Thumbs.db") == -1){
							System.out.println("opening batch folder: "+batchFolder.getFilename());
							connector.sftpChannel.cd("/E:/Opex/Mavro/"+dayFolder.getFilename()+"/"+batchFolder.getFilename());
							Vector<ChannelSftp.LsEntry> lstFiles = connector.sftpChannel.ls("*");
							System.out.println("pulling contents of batch folder...");
							// display contents of batch level directory
							File xmlFile = null;						
							for(ChannelSftp.LsEntry file : lstFiles){
								if(file.getFilename().indexOf(".xml") == -1){
									//System.out.println("found pdf file: "+file.getFilename());
									InputStream is = connector.sftpChannel.get(file.getFilename());
									mapFiles.put(file.getFilename(), inputStreamToFile(is, file.getFilename()));
									count++;
								} else {
									//System.out.println("found xml file: "+file.getFilename());
									InputStream is = connector.sftpChannel.get(file.getFilename());
									xmlFile = inputStreamToFile(is, file.getFilename());
								}
							}
							System.out.println("pulled "+count+" pdf files");
							if(xmlFile != null){
								System.out.println("parsing xmlFile... ");
								lstSObj.addAll(xmlToSObj(xmlFile));
							}
							SftpATTRS attrs=null;
							try {
							    attrs = connector.sftpChannel.stat("/E:/Opex/MavroArchive/"+dayFolder.getFilename());
							} catch (Exception e) {
							    System.out.println("/E:/Opex/MavroArchive/"+dayFolder.getFilename()+" not found");
							}
							if (attrs == null) {
							    System.out.println("Creating dir "+dayFolder.getFilename());
							    connector.sftpChannel.mkdir("/E:/Opex/MavroArchive/"+dayFolder.getFilename());
							}						
							String src = "/E:/Opex/Mavro/"+dayFolder.getFilename()+"/"+batchFolder.getFilename();
							String dest = "/E:/Opex/MavroArchive/"+dayFolder.getFilename()+"/"+batchFolder.getFilename();
							System.out.println("moving folder "+dayFolder.getFilename()+"/"+batchFolder.getFilename()+" and files to archive...");
							connector.sftpChannel.rename(src,dest);
						} else {
							System.out.println("deleting non-batch folder: "+batchFolder.getFilename());
							connector.sftpChannel.rm("/E:/Opex/Mavro/"+dayFolder.getFilename()+"/"+batchFolder.getFilename());
						}
					}
					// move day folder to MavroArchive	
					connector.sftpChannel.cd("/E:/Opex/Mavro/");
					Vector<ChannelSftp.LsEntry> lstFolder = connector.sftpChannel.ls("*");
					System.out.println("/E:/Opex/Mavro/ folder contents: "+lstFolder.size());
					for(ChannelSftp.LsEntry fld : lstFolder){
						System.out.println("fld "+fld.getFilename());
						System.out.println("day "+dayFolder.getFilename());
						if( fld.getFilename() == dayFolder.getFilename() || fld.getFilename().indexOf("Thumbs.db") > -1){
							System.out.println("deleting "+fld.getFilename()+"...");
							SftpATTRS attrs = null;
							try {
							    attrs = connector.sftpChannel.stat("/E:/Opex/Mavro/"+dayFolder.getFilename());
							} catch (Exception e) {
							    System.out.println("/E:/Opex/Mavro/"+dayFolder.getFilename()+" not found");
							}
							if(attrs != null) {
								connector.sftpChannel.rm("/E:/Opex/Mavro/"+dayFolder.getFilename());
							}
							break;
						}
					}
				}
			}
			
			if(lstSObj.size() > 0){
				System.out.println("inserting new attachment__c records...");
				ArrayList<String> idLst = new ArrayList<String>();
				try {
					System.out.println("connecting to salesforce...");
					sc = new SalesforceConnector(params.get("Username"),params.get("Password"),params.get("environment"));
					sc.login();			
					ArrayList<SaveResult> srLst = sc.create(lstSObj);				
					for(SaveResult sr : srLst){
						idLst.add(sr.getId());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// query new attachments
				ArrayList<SObject> attachments = new ArrayList<SObject>();	
				ArrayList<SObject> insertFiles = new ArrayList<SObject>();	
				try {	
					sc.login();
					attachments = query(idLst);
					System.out.println("queried "+attachments.size()+" attachments");
					for(SObject so : attachments){					
						File theFile = mapFiles.get((String)so.getField("Name"));
						byte[] body = null;
						
						body = Files.readAllBytes(theFile.toPath());					
						SObject sObj = new SObject("Attachment");
						sObj.setField("ParentId", (String)so.getField("Id"));
						sObj.setField("Name", (String)so.getField("Name"));					
						sObj.setField("Body", body);
						insertFiles.add(sObj);
					}
				} catch (ConnectionException e1) {
					e1.printStackTrace();
				}
				
				System.out.println("inserting new attachment__c child attachment records...");
				try {
					sc.login();
					sc.create(insertFiles);	
				} catch (Exception e) {
					e.printStackTrace();
				}	
			} else {
				System.out.println("No new files found.");
			}
			
			connector.disconnect();
		} catch (SftpException e){
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {}
		
	}
	
	private ArrayList<SObject> xmlToSObj(File xml) throws ParserConfigurationException, SAXException, IOException{
		ArrayList<SObject> lstSO = new ArrayList<SObject>();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		doc.getDocumentElement().normalize();
		//System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		NodeList bList = doc.getElementsByTagName("Batch");
		Element batchNode = (Element) bList.item(0);
		String batchDate = batchNode.getAttribute("ProcessDate");
		batchDate = batchDate.substring(6)+"-"+batchDate.substring(0,5);
		System.out.println("Batch Date:" + batchDate);
		
		NodeList nList = doc.getElementsByTagName("Transaction");		
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			//System.out.println("Current Element :" + nNode.getNodeName());
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String imgId =  eElement.getAttribute("ImageFile");
				SObject sObj = new SObject("Attachment__c");	
					sObj.setField("Name", imgId);
					sObj.setField("Mavro_OriginalCreditorName__c", eElement.getAttribute("OriginalCreditorName"));	
					sObj.setField("Mavro_CurrentCreditorName__c", eElement.getAttribute("CurrentCreditorName"));
					sObj.setField("Mavro_CollectionAgency__c", eElement.getAttribute("CollectionAgency"));
					sObj.setField("Mavro_DocumentType__c", eElement.getAttribute("DocumentType"));
					sObj.setField("Mavro_CustomerName__c", eElement.getAttribute("CustomerName"));
					sObj.setField("Mavro_AccountNumber__c", eElement.getAttribute("AccountNumber"));
					sObj.setField("Mavro_AccountBalance__c", eElement.getAttribute("AccountBalance"));
					sObj.setField("Mavro_NewCharges__c", eElement.getAttribute("NewCharges"));
					sObj.setField("Mavro_Offer__c", eElement.getAttribute("Offer"));
					sObj.setField("Mavro_Batch_Date__c", batchDate);
				lstSO.add(sObj);
			}
		}	
		return lstSO;
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
	
	private ArrayList<SObject> query(ArrayList<String> ids) throws ConnectionException{
		System.out.println("querying new attachment__c records...");
		String idFilter;

		idFilter = " Id IN(";
		for(String attId : ids){
			if(idFilter == " Id IN("){
				idFilter = idFilter + "'" + attId + "'";
			}else {
				idFilter = idFilter + ",'" + attId + "'";
			}
		}
		idFilter = idFilter + ")";

		String soql = "SELECT Id, Name FROM Attachment__c WHERE "+idFilter;
		return sc.query(soql);
	}
	
	public byte[] base64ToByte(String data) throws Exception {
		return Base64.decodeBase64(data.getBytes());
	}
}
