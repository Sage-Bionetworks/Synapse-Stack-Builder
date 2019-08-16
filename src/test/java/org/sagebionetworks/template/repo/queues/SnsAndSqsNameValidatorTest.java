package org.sagebionetworks.template.repo.queues;

import java.util.Arrays;

import org.junit.Test;

public class SnsAndSqsNameValidatorTest {

	@Test (expected = IllegalArgumentException.class)
	public void testValidateName_nameContainsDashes(){
		SnsAndSqsNameValidator.validateName("my-dashed-name");
	}

	@Test
	public void testValidateName_nameContainsUnderscore(){
		SnsAndSqsNameValidator.validateName("my_underscored_name");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateName_nameContainsSpace(){
		SnsAndSqsNameValidator.validateName("spaces are not ok");
	}

	@Test
	public void testValidateName_nameIsAlphanumeric(){
		SnsAndSqsNameValidator.validateName("1337H4x0r");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateNames_containBadNames(){
		SnsAndSqsNameValidator.validateNames(Arrays.asList("goodName", "anotherGoodName", "nope!!!!!"));
	}

	@Test
	public void testValidateNames_containsOnlyAlphanumericNames(){
		SnsAndSqsNameValidator.validateNames(Arrays.asList("This", "is", "fine"));
	}
}
