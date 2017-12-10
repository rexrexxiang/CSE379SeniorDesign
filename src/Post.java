import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;

public abstract class Post {
	/**
	 * Helper class for formatting string to csv file standard,
	 * this method forces the string surrounded by double quotes regardless if needed for consistency
	 * @param input - string to get formatted
	 * @return the formatted string
	 */
	public static String escapeCsv(String input) {
		String result = StringEscapeUtils.escapeCsv(input);
		return (input.length() == result.length()) ? ('"' + result + '"') : result ;
	}
	
	public abstract String toCSV();
	public abstract String toCSV(Function<String, String> t);
}
