import java.util.ArrayList;
import java.util.function.Function;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.Reader;

import org.apache.commons.csv.*;


public class WeiboPost extends Post{
	public int num;
	public String post;
	public String date;
	public int like;
	public String like_id;
	public int share;
	public String share_id;
	public String share_content;
	public int comment;
	public String comment_id;
	public String comment_content;

	public static ArrayList<WeiboPost> getPostFromFile(File file) throws Exception {
		ArrayList<WeiboPost> posts = new ArrayList<>();
		Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(getHeader().split(",")).withSkipHeaderRecord().parse(in);
		for(CSVRecord record : records) {
			WeiboPost p = new WeiboPost();
			p.num = Integer.parseInt(record.get("Num"));
			p.post = record.get("Post");
			p.date = record.get("Date");
			p.like = Integer.parseInt(record.get("Like"));
			p.like_id = record.get("Like_ID");
			p.share = Integer.parseInt(record.get("Share"));
			p.share_id = record.get("Share_ID");
			p.share_content = record.get("Share_Content");
			p.comment = Integer.parseInt(record.get("Comment"));
			p.comment_id = record.get("Comment_ID");
			p.comment_content = record.get("Comment_Content");
			posts.add(p);
		}
		return posts;
	}
	
	public String toCSV() {
		StringBuilder buf = new StringBuilder();
		buf.append(num);
		buf.append(',');
		buf.append(escapeCsv(post));
		buf.append(',');
		buf.append(date);
		buf.append(',');
		buf.append(like);
		buf.append(',');
		buf.append(escapeCsv(like_id));
		buf.append(',');
		buf.append(share);
		buf.append(',');
		buf.append(escapeCsv(share_id));
		buf.append(',');
		buf.append(escapeCsv(share_content));
		buf.append(',');
		buf.append(comment);
		buf.append(',');
		buf.append(escapeCsv(comment_id));
		buf.append(',');
		buf.append(escapeCsv(comment_content));
		return buf.toString();
	}
	
	public String toCSV(Function<String, String> translation) {
		StringBuilder buf = new StringBuilder();
		buf.append(num);
		buf.append(',');
		buf.append(escapeCsv(post));
		buf.append(',');
		buf.append(escapeCsv(translation.apply(post)));
		buf.append(',');
		buf.append(date);
		buf.append(',');
		buf.append(like);
		buf.append(',');
		buf.append(escapeCsv(like_id));
		buf.append(',');
		buf.append(share);
		buf.append(',');
		buf.append(escapeCsv(share_id));
		buf.append(',');
		buf.append(escapeCsv(share_content));
		buf.append(',');
		buf.append(escapeCsv(translation.apply(share_content)));
		buf.append(',');
		buf.append(comment);
		buf.append(',');
		buf.append(escapeCsv(comment_id));
		buf.append(',');
		buf.append(escapeCsv(comment_content));
		buf.append(',');
		buf.append(escapeCsv(translation.apply(comment_content)));
		return buf.toString();
	}

	public static String getHeader() {
		return "Num,Post,Date,Like,Like_ID,Share,Share_ID,Share_Content,Comment,Comment_ID,Comment_Content";
	}

	public static String getTranslationHeader() {
		return "Num,Post,Post_Translated,Date,Like,Like_ID,Share,Share_ID,Share_Content,Share_Content_Translated,Comment,Comment_ID,Comment_Content,Comment_Content_Translated";
	}
}
