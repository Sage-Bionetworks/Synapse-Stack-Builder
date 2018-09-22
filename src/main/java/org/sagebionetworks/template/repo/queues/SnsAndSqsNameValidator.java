package org.sagebionetworks.template.repo.queues;

import java.util.Collection;
import java.util.regex.Pattern;

public class SnsAndSqsNameValidator {
	private static final Pattern ALPHA_NUMERIC_MATCHER = Pattern.compile("\\w+");


	public static void validateNames(Collection<String> names){
		for(String name : names){
			validateName(name);
		}
	}

	public static void validateName(String name){
		if(!ALPHA_NUMERIC_MATCHER.matcher(name).matches()){
			throw new IllegalArgumentException("The name: "+ name +" is not alphanumeric.");
		}
	}
}
