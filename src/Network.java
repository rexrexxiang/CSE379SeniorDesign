import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.*;

public class Network {

	private static final String MS_KEY = "Enter Here";
	private static final String GOOGLE_KEY = "Enter Here";
	private static final String WECHAT_KEY = "Enter Here";
	private static final int URL_MAX_LENGTH = 1800;
	
	
	/**
	 * This method is used to get the wechat data with only the uid given.
	 * This method will use the sogou wechat webpage to get an url of an article the account posted,
	 * and from the article page it will collect the biz of the account, and use it to gather data from the api,
	 * for further detail, please see getWeChatData(String uid, String biz)
	 * 
	 * Few cases where this method will not find the biz of the account:
	 * - there's no account will such uid
	 * - the account is a service account and not a subscription account
	 * - the account has not posted recently and there's no article link on its profile page (can manually get biz)
	 * 
	 * @param uid - the wechat account id
	 * @throws Exception - any IOException encountered in the process
	 */
	public static void getWeChatData(String uid) throws Exception {
		NetworkResult nr = httpGet("http://weixin.sogou.com/weixin?type=1&s_from=input&query=" + uid);
		if(nr.getStatus() < 200 || nr.getStatus() >= 300) {
			System.out.println("WeChat: network error " + nr.getStatus());
			return;
		}
		String profileHTML = nr.getResult();
		int index = profileHTML.indexOf("http://mp.weixin.qq.com/s?src=");
		String postUrl = "";
		if(index < 0) {
			int index2 = profileHTML.indexOf("http://mp.weixin.qq.com/profile?src=");
			if(index2 < 0) {
				System.out.println("WeChat: no account named \"" + uid + "\" is found, check if it is a subscription account");
				return;
			}
			NetworkResult nr2 = httpGet(profileHTML.substring(index2, profileHTML.indexOf("\">", index2)).replace("&amp;", "&"));
			if(nr2.getStatus() < 200 || nr2.getStatus() >= 300) {
				System.out.println("WeChat: network error " + nr2.getStatus());
				return;
			}
			String historyHTML = nr2.getResult();
			int index3 = historyHTML.indexOf("\"content_url\":");
			if(index3 < 0) {
				System.out.println("WeChat: no post in account \"" + uid + "\" is found");
				return;
			}
			index3 = historyHTML.indexOf("/s?", index3);
			postUrl = "http://mp.weixin.qq.com" + historyHTML.substring(index3, historyHTML.indexOf("\",", index3)).replace("&amp;", "&");
			
		} else {
			postUrl = profileHTML.substring(index, profileHTML.indexOf("\">", index)).replace("&amp;", "&");
		}
		NetworkResult nr2 = httpGet(postUrl);
		
		if(nr2.getStatus() < 200 || nr2.getStatus() >= 300) {
			System.out.println("WeChat: network error " + nr2.getStatus());
			return;
		}
		String postHTML = nr2.getResult();
				
		int index2 = postHTML.indexOf("var biz = \"\"||\"");
		if(index2 < 0) {
			System.out.println("WeChat: cannot found biz for account \"" + uid + "\"");
			return;
		} 
		String biz = postHTML.substring(index2 + "var biz = \"\"||\"".length(), postHTML.indexOf("\";", index2));
		getWeChatData(uid, biz);
	}
	
	
	/**
	 * This method uses the biz of the wechat account to collect wechat posts from the api.
	 * The api call will retry itself if there's an internal server error
	 * Any other error the api returns will get displayed and the method will quit
	 * If the collection of all the post in the account is successful, it will write a file named
	 * C_[uid]_.csv in the WeChat subfolder.
	 * 
	 * @param uid - the wechat account id
	 * @param biz - the biz of the wechat account
	 * @throws Exception - any IOException encountered in the process
	 */
	public static void getWeChatData(String uid, String biz) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String pageToken = "";
		Set<WeChatPost> posts = new HashSet<>(200);
		int size = 0;
		collection:
		while(true) {
			String path = "https://api01.bitspaceman.com/post/weixinpro?" + "biz=" + biz + (pageToken.equals("") ? "" :("&pageToken=" + pageToken)) + "&apikey=" + WECHAT_KEY;
	
			NetworkResult res = null;
			int retryCounter = 0;
			try {
				while(true) {
					System.out.println(uid + ": token \"" + pageToken + "\" (" + size + " posts collected)");
					res = httpsGet(path);
					if((res.getStatus() >= 500 && res.getStatus() < 600) || res.getStatus() == 429) {
						if(retryCounter >= 10) {
							System.out.println("Retry limit reached, stop collection.");
							break collection;
						}
						retryCounter++;
						System.out.println("WeChat api: " + res.getStatus() + ", retrying...");
                        System.out.flush();
						Thread.sleep(2500);
						continue;
					} else if(res.getResult() == null) {
						System.out.println("WeChat api: " + res.getStatus());
                        System.out.flush();
						return;
					}
					break;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			String json = res.getResult();
			JsonObject response = new JsonParser().parse(json).getAsJsonObject();
			if(response.get("retcode").getAsString().equals("000000")) {
				JsonArray postsdata = response.getAsJsonArray("data");
				for(JsonElement e:postsdata) {
					JsonObject o = e.getAsJsonObject();
					WeChatPost p = new WeChatPost();
					p.id = o.get("id").getAsString();
					p.title = o.get("title").getAsString();
					p.content = o.get("content").getAsString();
					p.numViews = o.get("viewCount").getAsInt();
					try { 
						p.numLikes = o.get("likeCount").getAsInt(); 
					} catch (Exception ex) { 
						p.numLikes = 0; 
					}
					p.numComments = o.get("comments").isJsonNull() ? 0 : o.getAsJsonArray("comments").size();
					p.numImages = o.get("imageUrls").isJsonNull() ? 0 :o.getAsJsonArray("imageUrls").size();
					p.numVideos = o.get("videoUrls").isJsonNull() ? 0 : o.getAsJsonArray("videoUrls").size();
					p.postTime = sdf.parse(o.get("publishDateStr").getAsString());
					if(p.numComments > 0) {
						for(JsonElement e2: o.getAsJsonArray("comments")) {
							JsonObject o2 = e2.getAsJsonObject();
							WeChatPost.Comment c = p.new Comment();
							c.user = o2.get("commenterScreenName").getAsString();
							c.content = o2.get("content").getAsString().replace("\n", " ");
							c.postTime = sdf.parse(o.get("publishDateStr").getAsString());
						}
					}
					posts.add(p);
				}
				boolean hasNext = response.get("hasNext").getAsBoolean();
				if(hasNext) {
					pageToken = response.get("pageToken").getAsString();
					size = posts.size();
				} else {
					break;
				}
			} else {
				System.out.println(uid + " page " + pageToken + ": " + response.get("message").getAsString());
                System.out.flush();
				break;
			}
		}
		
		if(posts.size() == 0) { 
			System.out.println("No data for this account.");
            System.out.flush();
			return; 
		}
		String header = "id,title,content,numViews,numLikes,numComments,numImages,numVideos,postTime,commentUser,commentTime,commentText";
		
		if(!new File("WeChat").isDirectory()) {
			new File("WeChat").mkdir();
		}
		
		File f = new File("WeChat", "C_" + uid + ".csv");
		f.createNewFile();

		try(PrintWriter out = new PrintWriter(f, "UTF-8");) {
			out.println(header);
			for(WeChatPost p:posts) {
				out.println(p.toCSV());
			}
		}
		
		System.out.println("WeChat data for " + uid + " is saved in " + f.getAbsolutePath());
        System.out.flush();
	}
	
	/**
	 * This method will read the weibo data from the csv file in the Weibo subfolder into memory
	 * and performs the translation on it, the translation is mixed with the original data and is
	 * outputted to the WeiboT folder with BT_ prefix
	 * 
	 * @param uid - the filename of the weibo file (without .csv)
	 * @throws Exception - any IOException encountered in the process
	 */
	public static void translateWeiboData(String uid) throws Exception {
		if(!new File("Weibo").isDirectory()) {
			System.out.println("Cannot find the Weibo folder.");
            System.out.flush();
			return;
		}
		File f = new File("Weibo", uid + ".csv");
		if(!f.exists()) {
			System.out.println("Cannot find the file " + uid + ".csv in the Weibo folder.");
            System.out.flush();
			return;
		}
		ArrayList<WeiboPost> posts = WeiboPost.getPostFromFile(f);
		int numPost = posts.size();
		String header = WeiboPost.getTranslationHeader();
		
		if(!new File("WeiboT").isDirectory()) {
			new File("WeiboT").mkdir();
		}
		
		try {
			int counter = 0;
			File file = new File("WeiboT","BT_" + uid + ".csv");
			file.createNewFile();

			try(PrintWriter pw = new PrintWriter(file, "UTF-8")) {
				pw.println(header);
				System.out.println("Begin translation for weibo account " + uid);
				for(WeiboPost p : posts) {
					System.out.println("Translating " + (++counter) + "/" + numPost + " posts with Google Translation...");
	                System.out.flush();
					pw.println(p.toCSV(translate(Network::google_translate_api)));
				}
			}
			System.out.println("Translation complete, data saved in " + file.getAbsolutePath());
            System.out.flush();
		} catch (Exception e) {
			System.out.println(e.getMessage());
            System.out.flush();
		}	
	}
	
	/**
	 * This method will read the wechat data from the csv file in the WeChat subfolder into memory
	 * and performs the translation on it, the translation is mixed with the original data and is
	 * outputted to the WeChatT folder with CT_ prefix
	 * 
	 * @param uid - the filename of the weibo file (without C_ and .csv)
	 * @throws Exception - any IOException encountered in the process
	 */
	public static void translateWeChatData(String uid) throws Exception {
		if(!new File("WeChat").isDirectory()) {
			System.out.println("Cannot find the WeChat folder.");
            System.out.flush();
			return;
		}
		File f = new File("WeChat", "C_" + uid + ".csv");
		if(!f.exists()) {
			System.out.println("Cannot find the file C_" + uid + ".csv in the WeChat folder.");
            System.out.flush();
			return;
		}
		
		ArrayList<WeChatPost> posts = WeChatPost.getPostFromFile(f);
		String headerT = WeChatPost.getTranslationHeader();
		int numPost = posts.size();
		
		if(!new File("WeChatT").isDirectory()) {
			new File("WeChatT").mkdir();
		}
		try {
			int counter = 0;
			File file = new File("WeChatT", "CT_" + uid + ".csv");
			file.createNewFile();

			try(PrintWriter pw = new PrintWriter(file, "UTF-8");) {
				pw.println(headerT);
				System.out.println("Begin translation for wechat account " + uid);
				for(WeChatPost p : posts) {
					System.out.println("Translating " + (++counter) + "/" + numPost + " posts with Google Translation...");
                    System.out.flush();
					pw.println(p.toCSV(translate(Network::google_translate_api)));
				}
			}
			System.out.println("Translation complete, data saved in " + file.getAbsolutePath());
            System.out.flush();
		} catch (Exception e) {
			System.out.println(e.getMessage());
            System.out.flush();
		}	
		
	}
	
	/**
	 * The helper function that returns a function that takes in a string and returns its translation with a specified api
	 * @param api - the api that the returned function is going to use to translate the input
	 * @return a function that takes in a string and returns its translation
	 */
	public static Function<String, String> translate(Function<String, String> api) {
		return (str) -> {return translate(api, str);};
	}
	
	/**
	 * This method is a helper method to translate the input string (assumed in Chinese),
	 * mainly it breaks the input string down into parts so the url request does not fail due to the length limit
	 * The method will split the string into sentences, and append them to the maximum limit of the url,
	 * then calls the translation api. If a single sentence is too long, it will split the sentence into phrases and translate them,
	 * then the method will append the result together and returns it. 
	 * 
	 * @param api - the api function is going to translate the string
	 * @param msg - the string that is going to be translated
	 * @return the translated string
	 */
	public static String translate(Function<String, String> api, String msg) {
		StringBuilder result = new StringBuilder();
		String[] lines = msg.split("(?<=(\\u3002|\\uff01|\\uff1f|\n))");
		StringBuilder query = new StringBuilder();
		for(String line : lines) {
			String utf8 = "";
			try {
				utf8 = URLEncoder.encode(line, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if(utf8.length() > URL_MAX_LENGTH) {
				String[] parts = line.split("(?<=(\\h|\\uff0c))");
				for(String part : parts) {
					try {
						result.append(api.apply(URLEncoder.encode(part, "UTF-8")));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} else if((query.length() + utf8.length()) > URL_MAX_LENGTH) {
				result.append(api.apply(query.toString()));
				query = new StringBuilder(utf8);
			} else {
				query.append(utf8);
			}
		}
		result.append(api.apply(query.toString()));
		return result.toString();
	}
	
	/**
	 * This method will perform translation on the encoded text using microsoft translation api
	 * If the server says there's an internal error, it will retry in 2.5 seconds
	 * If the server returns other errors (including 403), the method print out the error and will returned original string (decoded)
	 * 
	 * Please note that there is a chance that this method will get stuck in an infinite loop
	 * 
	 * @param query - the encoded string that is going to be translated
	 * @return the translated text, or original string if there's an error
	 */
	private static String ms_translate_api(String query) {
		NetworkResult response = null;
		// getting a token for the api access
		try {
			while(true) {
				response = httpsPost("https://api.cognitive.microsoft.com/sts/v1.0/issueToken?Subscription-Key=" + MS_KEY, null, "");
				if(response.getStatus() >= 500 && response.getStatus() < 600) {
					System.out.println("Microsoft Translate (Token): " + response.getStatus());
                    System.out.flush();
					Thread.sleep(2500);
					continue;
				} else if(response.getResult() == null) {
					System.out.println("Microsoft Translate (Token): " + response.getStatus());
                    System.out.flush();
					return URLDecoder.decode(query, "UTF-8");
				}
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		// actual translation api call
		String token = response.getResult();
		String requestUrl = "https://api.microsofttranslator.com/V2/Http.svc/Translate" +
				"?&from=zh-CN&to=en&appid=Bearer%20" + token + "&text=" + query;
		try {
			while(true) {
				response = httpsGet(requestUrl);
				if(response.getStatus() >= 500 && response.getStatus() < 600) {
					System.out.println("Microsoft Translate: " + response.getStatus());
                    System.out.flush();
					Thread.sleep(2500);
					continue;
				} else if(response.getResult() == null) {
					return URLDecoder.decode(query, "UTF-8");
				}
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		String xml = response.getResult();
		xml = StringUtils.substringBetween(xml, "<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">", "</string>");
		return StringEscapeUtils.unescapeHtml4(xml);
	}
	
	
	/**
	 * This method will perform translation on the encoded text using google translation api
	 * If the server says there's an internal error, it will retry in 2.5 seconds
	 * If the server returns 403, it means the quota of the api is reached, you can expand the daily quota in the google console
	 * If the server returns other errors (including 403), the method print out the error and will returned original string (decoded)
	 * 
	 * Please note that there is a chance that this method will get stuck in an infinite loop
	 * 
	 * @param query - the encoded string that is going to be translated
	 * @return the translated text, or original string if there's an error
	 */
	private static String google_translate_api(String query) {
		String requestUrl = "https://translation.googleapis.com/language/translate/v2" +
				"?source=zh-CN&target=en&key=" + GOOGLE_KEY + "&q=" + query;
		NetworkResult response = null;
		try {
			while(true) {
				response = httpsGet(requestUrl);
				if(response.getStatus() >= 500 && response.getStatus() < 600) {
					System.out.println("Google Translate: " + response.getStatus() + ", retrying");
                    System.out.flush();
					Thread.sleep(2500);
					continue;
				} else if(response.getResult() == null) {
					System.out.println("Google Translate: " + response.getStatus());
                    System.out.flush();
					System.out.println(requestUrl);
                    System.out.flush();
					return URLDecoder.decode(query, "UTF-8");
				}
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		String json = response.getResult();
		JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
		return StringEscapeUtils.unescapeHtml4(obj.getAsJsonObject("data").getAsJsonArray("translations").get(0).getAsJsonObject().get("translatedText").getAsString());
	}
		
	/**
	 * This method performs a synchronous https post request to the specified address.
	 * If the server returns status code between 200 and 300, the response content is stored in the result, otherwise it's null
	 * This method will handle simple server status exceptions, 
	 * all other IOException regarding Internet connectivity will be thrown to the caller
	 * 
	 * @param urlpath - the full URL (including https://) that the page is located
	 * @param header - String pairs that sets the headers of the post request, if none is needed, pass in null
	 * @param param - any content that needs to sent to the server in the post request
	 * @return the network result object that contains information about the result of the request
	 * @throws Exception - IOException regarding Internet issues
	 */
	public static NetworkResult httpsPost(String urlpath, Map<String, String> header, String param) throws Exception {
		URL url = new URL(urlpath);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		if(header != null) {
			for(Map.Entry<String, String> entry : header.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		conn.setRequestMethod("POST");
		
		conn.setDoOutput(true);
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		if(param != null) {
			out.writeBytes(param);
		}
		out.flush();
		out.close();
		
		int status = conn.getResponseCode();
		if(status >= 200 && status < 300) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return new NetworkResult(status, response.toString());
		} else {
			return new NetworkResult(status, null);
		}
	}

	/**
	 * This method performs a synchronous https get request to the specified address.
	 * If the server returns status code between 200 and 300, the response content is stored in the result, otherwise it's null
	 * This method will handle simple server status exceptions, 
	 * all other IOException regarding Internet connectivity will be thrown to the caller
	 * 
	 * @param urlpath - the full URL (including https://) that the page is located
	 * @return the network result object that contains information about the result of the request
	 * @throws Exception - IOException regarding Internet issues
	 */
	public static NetworkResult httpsGet(String urlpath) throws Exception {
		URL url = new URL(urlpath);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		
		int status = conn.getResponseCode();
		if(status >= 200 && status < 300) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	
			return new NetworkResult(status, response.toString());
		} else {
			return new NetworkResult(status, null);
		}
	}
	
	/**
	 * This method performs a synchronous http get request to the specified address.
	 * If the server returns status code between 200 and 300, the response content is stored in the result, otherwise it's null
	 * This method will handle simple server status exceptions, 
	 * all other IOException regarding Internet connectivity will be thrown to the caller
	 * 
	 * @param urlpath - the full URL that the page is located
	 * @return the network result object that contains information about the result of the request
	 * @throws Exception - IOException regarding Internet issues
	 */
	public static NetworkResult httpGet(String urlpath) throws Exception {
		URL url = new URL(urlpath);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		
		int status = conn.getResponseCode();
		if(status >= 200 && status < 300) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	
			return new NetworkResult(status, response.toString());
		} else {
			return new NetworkResult(status, null);
		}
	}
	
	/**
	 * A class that is used to provide the result of a network access
	 */
	private static class NetworkResult {
		private int status;
		private String result;
		public NetworkResult(int status, String result) {
			this.status = status;
			this.result = result;
		}
		
		/**
		 * @return the HTTP status code that returned by the server
		 */
		public int getStatus() { return status; }
		
		/**
		 * @return the content of the result returned by the server
		 */
		public String getResult() { return result; }
	}
}
