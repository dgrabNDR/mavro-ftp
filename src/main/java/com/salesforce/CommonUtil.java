package main.java.com.salesforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;


public class CommonUtil {
	public static String convertDateToGCSString(Date date){
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		String dateFmt = fmt.format(date);
		return dateFmt;
	}
	
	public static Date convertDate(String input) throws ParseException {
	    int colon = input.lastIndexOf(":");
	    input = input.substring(0, colon) + input.substring(colon + 1, input.length());
	    //System.out.println(input);
	    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	    Date date = fmt.parse(input);
	    //System.out.println("date = " + date);
	    return date;
	}
	
	
	public static String getRequestJson(HttpServletRequest req) throws IOException{
		return extractString(req.getReader());		
	}
	
	public static String extractString(BufferedReader br) throws IOException{
		StringBuilder sb = new StringBuilder();
	    String str;
	    while( (str = br.readLine()) != null ){
	        sb.append(str);
	    } 
	    String jsonStr = sb.toString().isEmpty() ? "[]" : sb.toString();
	    return jsonStr;
		
	}
	
}
