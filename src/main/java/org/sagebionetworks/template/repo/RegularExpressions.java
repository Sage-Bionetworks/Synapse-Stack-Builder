package org.sagebionetworks.template.repo;

import org.apache.velocity.context.Context;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;

public class RegularExpressions {

	public static String UNSIGNED_LONG = "\\d{1,19}";

	public static String SYN_ID = "(?i)(syn)?(?-i)" + UNSIGNED_LONG;

	public static String REPO_V1_ENTITY_SYNID_WIKI_UNSIGNEDLONG = String
			.format("\\/+repo\\/v1\\/entity\\/%s\\/wiki(\\/%s)?", SYN_ID, UNSIGNED_LONG);

	public static String REPO_V1_FORUM_UNSIGNEDLONG_THREADCOUNT = String
			.format("\\/+repo\\/v1\\/forum\\/%s\\/threadcount", UNSIGNED_LONG);

	public static String REPO_V1_THREAD_UNSIGNEDLONG_REPLYCOUNT = String
			.format("\\/+repo\\/v1\\/thread\\/%s\\/replycount", UNSIGNED_LONG);

	public static String REPO_V1_TEAM_UNSIGNEDLONG_MEMBER_UNSIGNEDLONG = String
			.format("\\/+repo\\/v1\\/team\\/%s\\/member\\/%s", UNSIGNED_LONG, UNSIGNED_LONG);

	public static String REPO_V1_ENITTY_SYNID_TABLE_QUERY_ASYNC_START = String
			.format("\\/+repo\\/v1\\/entity\\/%s\\/table\\/query\\/async\\/start", SYN_ID);

	public static String REPO_V1_ENITTY_SYNID_TABLE_DOWNLOAD_CSV_ASYNC_START = String
			.format("\\/+repo\\/v1\\/entity\\/%s\\/table\\/download\\/csv\\/async\\/start", SYN_ID);

	public static String REPO_V1_ACCOUNT_UNSIGNEDLONG_EMAIL_VALIDATION = String
			.format("\\/+repo\\/v1\\/account\\/%s\\/emailValidation", UNSIGNED_LONG);
	
	public static String REPO_V1_VERIFICATION_SUBMISSION_UNSIGNEDLONG_STATE = String
			.format("\\/+repo\\/v1\\/verificationSubmission\\/%s\\/state", UNSIGNED_LONG);
	
	public static long l = Long.MAX_VALUE;

	/**
	 * Bind all regular expressions to the provided context
	 * 
	 * @param context
	 */
	public static void bindRegexToContext(Context context) {
		ValidateArgument.required(context, "context");
		context.put("regex_unsignedlong", toEscapedJSON(UNSIGNED_LONG));

		context.put("regex_synid", toEscapedJSON(SYN_ID));

		context.put("regex_repo_v1_entity_synid_wiki_unsignedlong",
				toEscapedJSON(REPO_V1_ENTITY_SYNID_WIKI_UNSIGNEDLONG));

		context.put("regex_repo_v1_forum_unsignedlong_threadcount",
				toEscapedJSON(REPO_V1_FORUM_UNSIGNEDLONG_THREADCOUNT));

		context.put("regex_repo_v1_thread_unsignedlong_replycount",
				toEscapedJSON(REPO_V1_THREAD_UNSIGNEDLONG_REPLYCOUNT));

		context.put("regex_repo_v1_team_unsignedlong_member_unsignedlong",
				toEscapedJSON(REPO_V1_TEAM_UNSIGNEDLONG_MEMBER_UNSIGNEDLONG));

		context.put("regex_repo_v1_entity_synid_table_query_async_start",
				toEscapedJSON(REPO_V1_ENITTY_SYNID_TABLE_QUERY_ASYNC_START));

		context.put("regex_repo_v1_entity_synid_table_csv_download_async_start",
				toEscapedJSON(REPO_V1_ENITTY_SYNID_TABLE_DOWNLOAD_CSV_ASYNC_START));
		
		context.put("regex_repo_v1_account_unsignedlong_email_validation", 
				toEscapedJSON(REPO_V1_ACCOUNT_UNSIGNEDLONG_EMAIL_VALIDATION));
		
		context.put("regex_repo_v1_verification_submission_unsignedlong_state", 
				toEscapedJSON(REPO_V1_VERIFICATION_SUBMISSION_UNSIGNEDLONG_STATE));
	}

	/**
	 * Generate escaped JSON for the provided string.
	 * 
	 * @param toEscape
	 * @return
	 */
	public static String toEscapedJSON(String toEscape) {
		String inQuotes = JSONObject.valueToString(toEscape);
		// remove the surrounding quotes.
		return inQuotes.substring(1, inQuotes.length() - 1);
	}

	public static String fromEscapedJSON(String toTrasnalte) {
		String json = String.format("{\"key\":\"%s\"}", toTrasnalte);
		return new JSONObject(json).getString("key");
	}
}
