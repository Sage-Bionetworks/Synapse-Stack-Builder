package org.sagebionetworks.template.repo.queues;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SnsAndSqsConfigTest {

	@Test
	public void testSerializationAndDeserlaization() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();

		//create original
		QueueConfig queueConfig = new QueueConfig("queueNameEcksDee",  Collections.singleton("TEST_TOPIC"), 42 , 24, 62);
		SnsAndSqsConfig original = new SnsAndSqsConfig( Collections.singleton("TEST_TOPIC"), Collections.singletonList(queueConfig));

		//write original to json
		StringWriter originalStringWriter = new StringWriter();
		objectMapper.writeValue(originalStringWriter, original);

		//deserialize from json into object
		SnsAndSqsConfig deseralized = objectMapper.readValue(originalStringWriter.toString(), SnsAndSqsConfig.class);

		//compare original json to json of
		assertEquals(original, deseralized);
	}

	//TODO: test convertToDesciptor

}
