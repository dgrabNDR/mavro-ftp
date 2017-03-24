package main.java.com.salesforce;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class CredentialManager {
	public static Map<String,String> getLogin() throws IOException{
		Map<String, String> params = new HashMap<String, String>();
		FileInputStream stream  =new FileInputStream("src/main/resources/Config.json");
		FileChannel fc = stream.getChannel();
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		/* Instead of using default, pass in a decoder. */
		String content = Charset.defaultCharset().decode(bb).toString();
		Gson gson = new Gson();
		params = (HashMap<String,String>) gson.fromJson(content, params.getClass());
		return params; 
	}
}
