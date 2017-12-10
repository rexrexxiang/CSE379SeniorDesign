import java.io.*;
import java.util.*;

public class Helper {

    public static void collectWeChatData(File f){
        try {
            ArrayList<String> wechat_account = new ArrayList<>();

            FileReader fileReader = new FileReader(f);

            // Always wrap FileReader in BufferedReader.
            try(BufferedReader bufferedReader = new BufferedReader(fileReader)) {
	            String line = null;
	
	            while((line = bufferedReader.readLine()) != null) {
	                wechat_account.add(line);
	            }  
            }
            for(int i = 0; i < wechat_account.size(); i++){
                Network.getWeChatData(wechat_account.get(i));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void translateWeChatData(String wechat_file){
        try {
            ArrayList<String> wechat_account = new ArrayList<String>();
 
            FileReader wechat_fileReader = new FileReader(wechat_file);
            
            String line = null;
            // Always wrap FileReader in BufferedReader.
            try(BufferedReader wechat_bufferedReader = new BufferedReader(wechat_fileReader)) {

	            while((line = wechat_bufferedReader.readLine()) != null) {
	                wechat_account.add(line);
	            }
            }
            
            for(int i = 0; i < wechat_account.size(); i++){
                Network.translateWeChatData(wechat_account.get(i));
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } 
    }
    
    public static void translateWeiboData(String weibo_file) {
        try {
            ArrayList<String> weibo_account = new ArrayList<String>();
            
            FileReader weibo_fileReader = new FileReader(weibo_file);
            
            String line = null;
            
            // Always wrap FileReader in BufferedReader.
            try(BufferedReader weibo_bufferedReader = new BufferedReader(weibo_fileReader)) {
            
	            while((line = weibo_bufferedReader.readLine()) != null) {
	                weibo_account.add(line);
	            }  
            }
            
            for(int i = 0; i < weibo_account.size(); i++) {
                Network.translateWeiboData(weibo_account.get(i));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } 
    }
}