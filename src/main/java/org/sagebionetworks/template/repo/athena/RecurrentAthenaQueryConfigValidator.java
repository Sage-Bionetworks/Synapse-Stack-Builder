package org.sagebionetworks.template.repo.athena;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.template.repo.queues.SnsAndSqsConfig;
import org.sagebionetworks.util.ValidateArgument;

public class RecurrentAthenaQueryConfigValidator {
	
	private RecurrentAthenaQueryConfig config;
	private SnsAndSqsConfig sqsConfig;

	public RecurrentAthenaQueryConfigValidator(RecurrentAthenaQueryConfig config, SnsAndSqsConfig sqsConfig) {
		this.config = config;
		this.sqsConfig = sqsConfig;
	}
	
	public RecurrentAthenaQueryConfig validate() {
		
		if (config.getQueries() == null || config.getQueries().isEmpty()) {
			return config;
		}
		
		Set<String> queryIds = new HashSet<>(config.getQueries().size());
		Set<String> availableQueues = sqsConfig.getQueueDescriptors().stream().map((q) -> q.getQueueName()).collect(Collectors.toSet());
		
		config.getQueries().forEach((query) -> {
			validate(query);
			
			if (!queryIds.add(query.getQueryName())) {
				throw new IllegalArgumentException("A query with name " + query.getQueryName() + " was already defined");
			}
			
			if (!availableQueues.contains(query.getDestinationQueue())) {
				throw new IllegalArgumentException("The query with name " + query.getQueryName() + " references the non defined queue " + query.getDestinationQueue());
			}
		});
		
		return config;
	}
	
	private void validate(RecurrentAthenaQuery query) {
		ValidateArgument.required(query.getQueryName(), "The queryName");
		ValidateArgument.required(query.getQueryPath(), "The queryPath");
		ValidateArgument.required(query.getCronExpression(), "The cronExpression");
		ValidateArgument.required(query.getDestinationQueue(), "The destinationQueue");
	}

}
