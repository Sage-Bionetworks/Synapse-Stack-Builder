package org.sagebionetworks.template.repo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.RdsClientWrapper;

/**
 * Shared by every builder that renders an AWS::RDS::DBSubnetGroup.
 */
public class VpcSubnetUtils {

	/**
	 * The private subnets of a color that the databases of a stack may use.
	 * <p>
	 * A DB subnet group that includes an availability zone where one of the DB instance classes cannot
	 * be placed lets RDS pick that zone for a Multi-AZ standby. RDS then abandons the conversion and
	 * returns the instance to 'available', recording the reason only as an RDS event, so CloudFormation
	 * reports success while the database stays single-AZ (PLFM-9965). Keep only the zones that offer
	 * every given DB instance class and are not excluded, and fail the build when too few remain.
	 *
	 * @param excludedZones availability zone names to leave out even though RDS lists the classes as
	 *                      orderable there.
	 * @return the subnets that support all of the instance classes, always at least
	 *         {@link Constants#RDS_MINIMUM_SUBNET_COUNT} of them, as the comma separated and quoted
	 *         elements of the template's SubnetIds array.
	 */
	public static String getDatabaseSubnets(CloudFormationClientWrapper cloudFormationClientWrapper,
			RdsClientWrapper rdsClientWrapper, String stack, String color, List<String> instanceClasses,
			List<String> excludedZones) {
		List<String> vpcSubnets = getPrivateSubnets(cloudFormationClientWrapper, stack, color);
		return rdsClientWrapper
				.getAvailableSubnetsForDBInstanceClasses(Constants.RDS_ENGINE, Constants.RDS_ENGINE_VERSION,
						instanceClasses, vpcSubnets, excludedZones, Constants.RDS_MINIMUM_SUBNET_COUNT)
				.stream().map(subnetId -> "\"" + subnetId + "\"").collect(Collectors.joining(","));
	}

	/**
	 * The private subnet ids exported by the VPC subnets stack of a color.
	 */
	public static List<String> getPrivateSubnets(CloudFormationClientWrapper cloudFormationClientWrapper, String stack,
			String color) {
		String privateSubnets = cloudFormationClientWrapper.getOutput(
				Constants.createVpcPrivateSubnetsStackName(stack, color),
				Constants.VPC_PRIVATE_SUBNETS_STACK_PRIVATE_SUBNETS_OUPUT_KEY);
		return Arrays.stream(privateSubnets.split(",")).map(String::trim).collect(Collectors.toList());
	}

}
