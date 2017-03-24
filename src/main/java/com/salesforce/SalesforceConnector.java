package main.java.com.salesforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class SalesforceConnector {
	private static final String CLIENTID ="3MVG9yZ.WNe6byQBrEHW_cRm._dp_BF.2h1xhv1.qUNdo9mllhb6wLTwiF1e5vhIH1eu9Ojvl0UiD6Y62FGr6";
	private static final String CLIENT_SECRET = "7740176649279426585";
	private static String ENV;
	private static String username;
	private static String password; 
	
	
	private PartnerConnection partnerConnection = null;
	
	public SalesforceConnector(String un,String pw,String env){
		System.out.println(un+"||"+pw+"||"+env);
		SalesforceConnector.ENV = env;
		SalesforceConnector.password = pw;
		SalesforceConnector.username = un;
	}
	
	public static OAuthResponse getOAuthToken() throws ClientProtocolException, IOException{
		OAuthResponse oauthres = new OAuthResponse();
		String res = "";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://"+ENV+".salesforce.com/services/oauth2/token");
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("grant_type", "password"));
		nvps.add(new BasicNameValuePair("client_id", CLIENTID));
		nvps.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		nvps.add(new BasicNameValuePair("username", username));
		nvps.add(new BasicNameValuePair("password", password));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		CloseableHttpResponse response = httpclient.execute(httpPost);

		try {
			System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();
			res = CommonUtil.extractString(new BufferedReader(new InputStreamReader(entity.getContent())));
			//System.out.println(EntityUtils.toString(entity2));
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity);
		} finally {
			response.close();
		}

		Gson gson = new Gson();

		if(!res.isEmpty()){
			System.out.println(res);
			oauthres = gson.fromJson(res, new TypeToken<OAuthResponse>(){}.getType());
		}

		return oauthres;
	}
	
	public void login() throws ClientProtocolException, IOException, ConnectionException{
		if(partnerConnection == null){
			OAuthResponse oauthres = getOAuthToken();
			ConnectorConfig config = new ConnectorConfig();
			config.setSessionId(oauthres.access_token);
			config.setServiceEndpoint(oauthres.instance_url+"/services/Soap/u/33.0");
			System.out.println("ServiceEndpoint ==>"+config.getServiceEndpoint());
			partnerConnection = new PartnerConnection(config);
		}
	}
	
	public ArrayList<SaveResult> update( ArrayList<SObject> sobjects) throws ConnectionException{
		ArrayList<SaveResult> results = new ArrayList<SaveResult>();
		ArrayList<ArrayList<SObject>> chunkedsobjects = chunk(sobjects,200);
		for(ArrayList<SObject> chunk : chunkedsobjects){
			SObject[] sobjArr = chunk.toArray(new SObject[chunk.size()]);
			System.out.println("Updating : "+sobjArr.length);
			partnerConnection.update(sobjArr);
			//results.addAll(Arrays.asList(partnerConnection.update(sobjArr)));
		}
		return results;
	}
	
	public ArrayList<SaveResult> create( ArrayList<SObject> sobjects) throws ConnectionException{
		ArrayList<SaveResult> results = new ArrayList<SaveResult>();
		ArrayList<ArrayList<SObject>> chunkedsobjects = chunk(sobjects,200);
		for(ArrayList<SObject> chunk : chunkedsobjects){
			SObject[] sobjArr = chunk.toArray(new SObject[chunk.size()]);
			System.out.println("Updating : "+sobjArr.length);
			partnerConnection.create(sobjArr);
			//results.addAll(Arrays.asList(partnerConnection.update(sobjArr)));
		}
		return results;
	}
	
	public ArrayList<SObject> query(String soql) throws ConnectionException{
		System.out.println(soql);
		ArrayList<SObject> sObjects = new ArrayList<SObject>();
		Boolean done = false;
		QueryResult qr = partnerConnection.query(soql);
		while(!done){
			SObject[] records = qr.getRecords();
			for (int i = 0; i < records.length; i++) {
				//upload attachment from here
				sObjects.add(records[i]);
			}
			if (qr.isDone()) {
                done = true;
            } else {
                qr = partnerConnection.queryMore(qr.getQueryLocator());
            }
		}
		
		return sObjects;
	}
	
	private static <T> ArrayList<ArrayList<T>> chunk(ArrayList<T> list, final int L) {
		ArrayList<ArrayList<T>> parts = new ArrayList<ArrayList<T>>();
	    final int N = list.size();
	    for (int i = 0; i < N; i += L) {
	        parts.add(new ArrayList<T>(
	            list.subList(i, Math.min(N, i + L)))
	        );
	    }
	    return parts;
	}
	
}
