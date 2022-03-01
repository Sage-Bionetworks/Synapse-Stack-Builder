import os
import sys
import boto3
import re
from cloudwatchmetrics_instances_history import CloudwatchMetricsInstancesHistory
from utils import _gen_stack_instance, _get_stack_version, _get_stack_number

class CloudwatchMetricsInstancesProvider:

  def __init__(self, session, hpath):
    self.boto3_session = session
    self.cloudwatch_history = CloudwatchMetricsInstancesHistory(hpath)
    return

  def _get_history(self):
    return self.cloudwatch_history

### EC@ instance
  # Save instance ids from Cloudwatch for EC2 instances in (stack_version, enviromment) to history
  def _refresh_ec2_instance_ids(self, stack, stack_instance, environment):
    stack_version = _get_stack_version(stack_instance)
    instance_ids = self._get_ec2_instance_ids(stack, stack_instance, environment)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, "EC2_CPU", environment, instance_ids)
    return

  def _get_ec2_instance_ids(self, stack, stack_instance, environment):
    env = environment.lower()
    tag_name = f"{env}-{stack}-{stack_instance}"
    instances = self._cw_get_ec2_instances_by_name(tag_name)
    instance_ids = [inst.id for inst in instances]
    return instance_ids

  # Get instance ids from Cloudwatch
  def _cw_get_ec2_instances_by_name(self, name):
    ec2 = self.boto3_session.resource("ec2")
    filters = [{"Name": "tag:Name", "Values": [name]}]
    instances = ec2.instances.filter(Filters=filters)
    return instances

  # Return instance ids for (stack_version, environment) from history
  def get_ec2_instances_ids(self, stack_version, environment):
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, "EC2_CPU", environment)
    return ids

### RDS
  def _refresh_rds_instance_ids(self, stack, stack_version):
    instance_ids = self._cw_get_rds_instance_ids(stack, stack_version)
    id_gen_id = self._cw_get_rds_idgen_ids(stack)
    instance_ids.extend(id_gen_id)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, "RDS_CPU", None, instance_ids)
    return

  # Get instance ids from Cloudwatch
  def _cw_get_rds_instance_ids(self, stack, stack_version):
    db_name = f"{stack}{stack_version}"
    rds_client = self.boto3_session.client("rds")
    res = rds_client.describe_db_instances()
    db_instances = res['DBInstances']
    instance_ids = [inst['DBInstanceIdentifier'] for inst in db_instances if inst['DBName'] == db_name]
    return instance_ids

  def _cw_get_rds_idgen_ids(self, stack):
    db_name = f"devidgen"
    rds_client = self.boto3_session.client("rds")
    res = rds_client.describe_db_instances()
    db_instances = res['DBInstances']
    instance_ids = [inst['DBInstanceIdentifier'] for inst in db_instances if inst['DBName'] == db_name]
    return instance_ids

  # Return instance ids for (stack, stack_version) from history
  def get_rds_instances_ids(self, stack_version):
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, "RDS_CPU", None)
    return ids

### JVM Memory instance ids
  def _refresh_memory_instance_ids(self, stack, stack_version, environment):
    instance_ids = self._cw_get_cloudwatch_memory_instances(stack_version, environment)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, "JVM_MEMORY", environment, instance_ids)
    return

  def cw_get_memory_namespace(self, stack_version, environment):
    namespace_prefix = ""
    if environment == "REPO":
      namespace_prefix = "Repository"
    elif environment == "WORKERS":
      namespace_prefix = "Workers"
    else:
      raise ValueError("instance_type can be 'REPO' or 'WORKERS'")
    namespace = f"{namespace_prefix}-Memory-{stack_version}"
    return namespace

  def _cw_get_cloudwatch_memory_instances(self, stack_version, environment):
    namespace = self.cw_get_memory_namespace(stack_version, environment)
    cw_client = self.boto3_session.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName='used')
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

  def get_cloudwatch_memory_instances(self, stack_version, environment):
    namespace  = self.cw_get_memory_namespace(stack_version, environment)
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, "JVM_MEMORY", environment)
    return namespace, ids

### Worker stats
  def _refresh_worker_stats_instance_ids(self, stack_version, cw_metric_name):
    metric_name = CloudwatchMetricsInstancesProvider._metric_name(cw_metric_name)
    instance_ids = self._cw_get_cloudwatch_worker_stats_instances_ids(stack_version, cw_metric_name)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, f"WS-{metric_name}", None, instance_ids)
    return

  def cw_get_worker_stats_namespace(self, stack_version):
    namespace = f"Worker-Statistics-{stack_version}"
    return namespace

  def _cw_get_cloudwatch_worker_stats_instances_ids(self, stack_version, cw_metric_name):
    namespace = self.cw_get_worker_stats_namespace(stack_version)
    cw_client = self.boto3_session.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=cw_metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

  def get_cloudwatch_worker_stats_instance_ids(self, stack_version, metric_name):
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, f"WS-{metric_name}", None)
    return ids

###  Async Workers stats
  def _refresh_async_worker_stats_instance_ids(self, stack_instance):
    stack_version = _get_stack_version(stack_instance)
    instance_ids = self._cw_get_async_workers_namespace(stack_instance)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, f"AWS-{metric_name}", None,
                                                                  instance_ids)

  def _cw_get_async_workers_namespace(self, stack_version):
    namespace = f"Asynchronous Workers - {stack_version}"
    return namespace

  def _cw_get_cloudwatch_async_workers_instances_ids(self, stack_version, cw_metric_name):
    namespace = self._cw_get_async_workers_namespace(stack_version)
    cw_client = self.boto3_session.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=cw_metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

  def get_async_worker_stats_instance_ids(self, stack_version):
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, 'AWS', None)
    return ids

### Async Job stats
  def _refresh_async_jobs_stats_instance_ids(self, stack_version, cw_metric_name):
    metric_name = CloudwatchMetricsInstancesProvider._metric_name(cw_metric_name)
    instance_ids = self._cw_get_cloudwatch_async_job_stats_instances(stack_version, cw_metric_name)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, f"AJS-{metric_name}", None, instance_ids)
    return

  def get_cloudwatch_async_job_stats_instances(self, stack_version, cw_metric_name):
    metric_name = CloudwatchMetricsInstancesProvider._metric_name(cw_metric_name)
    ids = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, f"AJS-{metric_name}", None)
    return ids

  def _cw_get_async_job_stats_namespace(self, stack_version):
    namespace = f"Asynchronous-Jobs-{stack_version}"
    return namespace

  def _cw_get_cloudwatch_async_job_stats_instances(self, stack_version, cw_metric_name):
    namespace = self._cw_get_async_job_stats_namespace(stack_version)
    cw_client = self.boto3_session.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=cw_metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

  ### ALB response time
  def _refresh_alb_response_time_instance_ids(self, stack, stack_instance):
    stack_version = _get_stack_version(stack_instance)
    alb_name = self._get_repo_alb_name(stack, stack_instance)
    self.cloudwatch_history.write_cloudwatchmetrics_instances_ids(stack_version, f'ALBRT', "REPO", [alb_name])
    return

  def _get_repo_alb_name(self, stack, stack_instance):
    env_name = f'repo-{stack}-{stack_instance}'
    rgtapi_client = self.boto3_session.client('resourcegroupstaggingapi')
    tag_filters = [{'Key': 'elasticbeanstalk:environment-name', 'Values': [env_name]}]
    resp = rgtapi_client.get_resources(
      TagFilters=tag_filters,
      ResourceTypeFilters=['elasticloadbalancing:loadbalancer'],
      IncludeComplianceDetails=False,
      ExcludeCompliantResources=False
    )
    arn = resp["ResourceTagMappingList"][0]["ResourceARN"]
    p = re.compile('arn:aws:elasticloadbalancing:us-east-1:\d+:loadbalancer/(.+)')
    m = p.match(arn)
    alb_name = m.groups()[0]
    return alb_name

  def get_repo_alb_name(self, stack_version):
    alb_name = self.cloudwatch_history.get_cloudwatch_metrics_instance_ids(stack_version, f'ALBRT', "REPO")
    return alb_name

  @staticmethod
  def _metric_name(cloudwatch_metric_name):
    return cloudwatch_metric_name.upper().replace(' ', '_')







