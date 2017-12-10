import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class WeChatPost extends Post {
	public String id;
	public String title;
	public String content;
	public int numViews;
	public int numLikes;
	public int numComments;
	public int numImages;
	public int numVideos;
	public Date postTime;
	public ArrayList<Comment> comments;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	

	public WeChatPost() {
		comments = new ArrayList<>();
	}
	
	public static ArrayList<WeChatPost> getPostFromFile(File file) throws Exception {
		ArrayList<WeChatPost> posts = new ArrayList<>();
		Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(getHeader().split(",")).withSkipHeaderRecord().parse(in);
		for(CSVRecord record : records) {
			WeChatPost p = new WeChatPost();
			p.id = record.get("id");
			p.title = record.get("title");
			p.content = record.get("content");
			p.numViews = Integer.parseInt(record.get("numViews"));
			p.numLikes = Integer.parseInt(record.get("numLikes"));
			p.numComments = Integer.parseInt(record.get("numComments"));
			p.numImages = Integer.parseInt(record.get("numImages"));
			p.numVideos = Integer.parseInt(record.get("numVideos"));
			p.postTime = sdf.parse(record.get("postTime"));
			if(!record.get("commentUser").equals("")) {
				String[] commentUser = record.get("commentUser").split("$");
				String[] commentTime = record.get("commentTime").split("$");
				String[] commentText = record.get("commentText").split("$");
				for(int i=0; i<commentUser.length; i++) {
					Comment c = p.new Comment();
					c.user = commentUser[i];
					c.postTime = sdf.parse(commentTime[i]);
					c.content = commentText[i];
				}
			}
			posts.add(p);
		}
		return posts;
	}
	
	public String toCSV() {
		StringBuilder buf = new StringBuilder();
		buf.append(id);
		buf.append(',');
		buf.append(escapeCsv(title));
		buf.append(',');
		buf.append(escapeCsv(content));
		buf.append(',');
		buf.append(numViews);
		buf.append(',');
		buf.append(numLikes);
		buf.append(',');
		buf.append(numComments);
		buf.append(',');
		buf.append(numImages);
		buf.append(',');
		buf.append(numVideos);
		buf.append(',');
		buf.append(sdf.format(postTime));
		buf.append(',');
		buf.append(comments.stream().map(Comment::getUser).collect(Collectors.joining("$")));
		buf.append(',');
		buf.append(comments.stream().map(Comment::getPostTime).collect(Collectors.joining("$")));
		buf.append(',');
		buf.append(escapeCsv(comments.stream().map(Comment::getContent).collect(Collectors.joining("$"))));
		
		return buf.toString();
	}
	
	public String toCSV(Function<String, String> translation) {
		StringBuilder buf = new StringBuilder();
		buf.append(id);
		buf.append(',');
		buf.append(escapeCsv(title));
		buf.append(',');
		buf.append(escapeCsv(translation.apply(title)));
		buf.append(',');
		buf.append(escapeCsv(content));
		buf.append(',');
		buf.append(escapeCsv(translation.apply(content)));
		buf.append(',');
		buf.append(numViews);
		buf.append(',');
		buf.append(numLikes);
		buf.append(',');
		buf.append(numComments);
		buf.append(',');
		buf.append(numImages);
		buf.append(',');
		buf.append(numVideos);
		buf.append(',');
		buf.append(sdf.format(postTime));
		buf.append(',');
		buf.append(comments.stream().map(Comment::getUser).collect(Collectors.joining("$")));
		buf.append(',');
		buf.append(comments.stream().map(Comment::getPostTime).collect(Collectors.joining("$")));
		buf.append(',');
		buf.append(escapeCsv(comments.stream().map(Comment::getContent).collect(Collectors.joining("$"))));
		buf.append(',');
		buf.append(escapeCsv(comments.stream().map(Comment::getContent).map((s) -> {return translation.apply(s);}).collect(Collectors.joining("$"))));
		
		return buf.toString();
	}
	
	@Override
    public int hashCode() {
        return postTime.hashCode();
	}

    @Override
    public boolean equals(Object obj) {
    	if(obj == null) { return false; }
    	if(obj instanceof WeChatPost) {return postTime.getTime() == ((WeChatPost)obj).postTime.getTime();}
    	return false;
    }
    
    public static String getHeader() {
    	return "id,title,content,numViews,numLikes,numComments,numImages,numVideos,postTime,commentUser,commentTime,commentText";
    }
    
    public static String getTranslationHeader() {
    	return "id,title,titleTranslated,content,contentTranslated,numViews,numLikes,numComments,numImages,numVideos,postTime,commentUser,commentTime,commentText,commentTextTranslated";
    }
    
	
	public class Comment {
		public Date postTime;
		public String user;
		public String content;
		
		public Comment() {
			comments.add(this);
		}
		
		public String getUser() {
			return user;
		}
		
		public String getPostTime() {
			return sdf.format(postTime);
		}
		
		public String getContent() {
			return content;
		}
		
	}
}
