package org.sagebionetworks.template.cron;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.TimeToLive;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.inject.Inject;

public class ExpiredStackTeardownImpl implements ExpiredStackTeardown {

	private final CloudFormationClient cloudFormationClient;
	private final TimeToLive timeToLive;
	private final Logger logger;

	@Inject
	public ExpiredStackTeardownImpl(CloudFormationClient cloudFormationClient, TimeToLive timeToLive,
			LoggerFactory loggerFactory) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.timeToLive = timeToLive;
		this.logger = loggerFactory.getLogger(getClass());
	}

	@Override
	public void findAndDeleteExpiredStacks() {

		try {
			Set<StackStatus> deletableStatus = Set.of(StackStatus.CREATE_COMPLETE, StackStatus.UPDATE_COMPLETE,
					StackStatus.UPDATE_ROLLBACK_COMPLETE, StackStatus.DELETE_FAILED);

			// find any stack that is expired and can be deleted.
			List<Stack> toDelete = cloudFormationClient.streamOverAllStacks()
					.filter(s -> deletableStatus.contains(StackStatus.valueOf(s.getStackStatus())))
					.filter(s -> timeToLive.isTimeToLiveExpired(s.getParameters()))
					.filter(s -> s.getEnableTerminationProtection() == null
							|| Boolean.FALSE.equals(s.getEnableTerminationProtection()))
					.collect(Collectors.toList());

			toDelete.forEach(s -> {
				logger.info(String.format("Deleting stack: '%s'...", s.getStackName()));

				try {
					cloudFormationClient.deleteStack(s.getStackName());
				} catch (Exception e) {
					logger.error(String.format("Failed to delete stack: '%s'", s.getStackName()), e);
				}
			});
		} catch (Exception e) {
			logger.error("Failed: ", e);
		}
	}

}
