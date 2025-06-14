package org.sagebionetworks.template.vpc;

import static org.sagebionetworks.template.Constants.AVAILABILITY_ZONES;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PRIVATE_SUBNET_IDX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_AVAILABILITY_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_ENDPOINTS_AZ;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_ENDPOINTS_COLOR;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.SUBNETS;
import static org.sagebionetworks.template.Constants.TEMPLATES_VPC_PRIVATE_SUBNET_JSON_VTP;
import static org.sagebionetworks.template.Constants.TEMPLATES_VPC_PUBLIC_SUBNETS_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_CIDR;
import static org.sagebionetworks.template.Constants.VPC_CIDR_SUFFIX;
import static org.sagebionetworks.template.Constants.VPC_COLOR_GROUP_NETWORK_MASK;
import static org.sagebionetworks.template.Constants.VPC_ENDPOINTS_AZ;
import static org.sagebionetworks.template.Constants.VPC_ENDPOINTS_COLOR;
import static org.sagebionetworks.template.Constants.VPC_PRIVATE_SUBNET_STACKNAME_FORMAT;
import static org.sagebionetworks.template.Constants.VPC_PUBLIC_SUBNETS_STACKNAME_FORMAT;
import static org.sagebionetworks.template.Constants.VPC_STACKNAME;
import static org.sagebionetworks.template.Constants.VPC_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_NETWORK_MASK;

import java.io.StringWriter;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;

import java.util.List;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

public class SubnetTemplateBuilderImpl implements SubnetTemplateBuilder {

    CloudFormationClientWrapper cloudFormationClientWrapper;
    VelocityEngine velocityEngine;
    Configuration config;
    Logger logger;
    StackTagsProvider stackTagsProvider;

    final List<String> VPC_ENDPOINT_SERVICES = List.of("bedrock", "bedrock-agent", "bedrock-runtime", "bedrock-agent-runtime");

    @Inject
    public SubnetTemplateBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper, VelocityEngine velocityEngine,
                                     Configuration configuration, LoggerFactory loggerFactory, StackTagsProvider stackTagsProvider) {
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.velocityEngine = velocityEngine;
        this.config = configuration;
        this.logger = loggerFactory.getLogger(VpcTemplateBuilderImpl.class);
        this.stackTagsProvider = stackTagsProvider;
    }

    @Override
    public void buildAndDeployPublicSubnets() throws InterruptedException {
        String stackName = createPublicSubnetsStackName();
        // Create the context from the input
        VelocityContext context = createContext();
        Template publicSubnetsTemplate = this.velocityEngine.getTemplate(TEMPLATES_VPC_PUBLIC_SUBNETS_JSON_VTP);
        StringWriter stringWriter = new StringWriter();
        publicSubnetsTemplate.merge(context, stringWriter);
        String resultJSON = stringWriter.toString();
        JSONObject templateJson = new JSONObject(resultJSON);
        resultJSON = templateJson.toString(JSON_INDENT);

        this.cloudFormationClientWrapper.createOrUpdateStack(
            new CreateOrUpdateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(resultJSON)
                .withTags(stackTagsProvider.getStackTags(config))
        );

        this.cloudFormationClientWrapper.waitForStackToComplete(stackName);
    }

    @Override
    public void buildAndDeployPrivateSubnets() throws InterruptedException {

        VelocityContext context = createContext();

        Subnets subnets = (Subnets)context.get(SUBNETS);
        
        for (int i=0; i<subnets.getPrivateSubnetGroups().length; i++) {
            SubnetGroup sg = subnets.getPrivateSubnetGroups()[i];
            String stackName = createPrivateSubnetStackName(sg.getColor().toString());

            context.put(VPC_SUBNET_COLOR, sg.getColor());
            context.put(PRIVATE_SUBNET_IDX, i);
            Template privateSubnetsTemplate = this.velocityEngine.getTemplate(TEMPLATES_VPC_PRIVATE_SUBNET_JSON_VTP);
            StringWriter stringWriter = new StringWriter();
            privateSubnetsTemplate.merge(context, stringWriter);
            String resultJSON = stringWriter.toString();
            JSONObject templateJson = new JSONObject(resultJSON);
            resultJSON = templateJson.toString(JSON_INDENT);

            this.cloudFormationClientWrapper.createOrUpdateStack(
                    new CreateOrUpdateStackRequest()
                            .withStackName(stackName)
                            .withTemplateBody(resultJSON)
                            .withTags(stackTagsProvider.getStackTags(config))
            );

            this.cloudFormationClientWrapper.waitForStackToComplete(stackName);
        }
        
    }

    VelocityContext createContext() {
        VelocityContext context = new VelocityContext();
        String vpcSubnetPrefix = config.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX);
        String vpcCidr = vpcSubnetPrefix + VPC_CIDR_SUFFIX;
        context.put(VPC_CIDR, vpcCidr);
        String availabilityZonesRaw = config.getProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES);
        context.put(AVAILABILITY_ZONES, availabilityZonesRaw);
        String[] availabilityZones = config.getCommaSeparatedProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES);

        SubnetBuilder builder = new SubnetBuilder();
        builder.withCidrPrefix(vpcSubnetPrefix);
        builder.withColors(getColorsFromProperty());
        builder.withSubnetMask(VPC_SUBNET_NETWORK_MASK);
        builder.withColorGroupNetMaskSubnetMask(VPC_COLOR_GROUP_NETWORK_MASK);
        builder.withAvailabilityZones(availabilityZones);
        builder.withVpcEndpointServices(VPC_ENDPOINT_SERVICES);
        Subnets subnets = builder.build();

        context.put(SUBNETS, subnets);
        context.put(STACK, config.getProperty(PROPERTY_KEY_STACK));
        context.put(VPC_STACKNAME, String.format(VPC_STACK_NAME_FORMAT, config.getProperty(PROPERTY_KEY_STACK))); // Change this!
        context.put("vpcEndpointServices", VPC_ENDPOINT_SERVICES);
        context.put(VPC_ENDPOINTS_COLOR, config.getProperty(PROPERTY_KEY_VPC_ENDPOINTS_COLOR));
        context.put(VPC_ENDPOINTS_AZ, config.getProperty(PROPERTY_KEY_VPC_ENDPOINTS_AZ));
                
        return context;
    }

    String createPublicSubnetsStackName() {
        return String.format(VPC_PUBLIC_SUBNETS_STACKNAME_FORMAT, config.getProperty(PROPERTY_KEY_STACK));
    }

    String createPrivateSubnetStackName(String color) {
        return String.format(VPC_PRIVATE_SUBNET_STACKNAME_FORMAT, config.getProperty(PROPERTY_KEY_STACK), color);
    }

    Parameter[] createParameters(String stackName) {
        return null;
    }

    /**
     * Get the colors from the property CSV.
     *
     * @return
     */
    Color[] getColorsFromProperty() {
        String[] colorString = config.getCommaSeparatedProperty(PROPERTY_KEY_VPC_COLORS);
        Color[] colors = new Color[colorString.length];
        for (int i = 0; i < colorString.length; i++) {
            colors[i] = Color.valueOf(colorString[i]);
        }
        return colors;
    }

}
