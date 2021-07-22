import sys
import re
import json
import boto3

def get_ec2_instance_ids(environment, stack_instance):
    tag_name = environment + "-prod-" + stack_instance
    instances = get_ec2_instances_by_name(tag_name)
    return [inst.id for inst in instances]

def get_ec2_instances_by_name(name):
    ec2 = boto3.resource("ec2")
    filters = [{"Name":"tag:Name", "Values":[name]}]
    instances = ec2.instances.filter(Filters=filters)
    return instances

def get_instance_from_stack_instance(stack_instance):
    idx = stack_instance.find("-")
    if idx == -1:
        raise ValueError("stack_instance expected format is 'xxx-y'")
    instance = stack_instance[:idx]
    return instance

def get_rds_instance_ids(stack_instance):
    instance = get_instance_from_stack_instance(stack_instance)
    db_name = "prod" + instance
    rds_client = boto3.client("rds")
    res = rds_client.describe_db_instances()
    db_instances = res['DBInstances']
    instance_ids = [inst['DBInstanceIdentifier'] for inst in db_instances if inst['DBName'] == db_name ]
    return instance_ids

def get_rds_idgen_id():
    db_name = "prodidgen"
    rds_client = boto3.client("rds")
    res = rds_client.describe_db_instances()
    db_instances = res['DBInstances']
    instance_ids = [inst['DBInstanceIdentifier'] for inst in db_instances if inst['DBName'] == db_name ]
    return instance_ids[0]

def get_rds_idgen_ids():
    db_name = "prodidgen"
    rds_client = boto3.client("rds")
    res = rds_client.describe_db_instances()
    db_instances = res['DBInstances']
    instance_ids = [inst['DBInstanceIdentifier'] for inst in db_instances if inst['DBName'] == db_name ]
    return instance_ids

def get_memory_namespace(stack_instance, instance_type):
    namespace_prefix = ""
    if instance_type == "R":
        namespace_prefix = "Repository"
    elif instance_type == "W":
        namespace_prefix = "Workers"
    else:
        raise ValueError("instance_type can be 'R' or 'W'")
    instance = get_instance_from_stack_instance(stack_instance)
    namespace = "-".join([namespace_prefix, "Memory", str(instance)])
    return namespace

def get_worker_stats_namespace(stack_instance):
    instance = get_instance_from_stack_instance(stack_instance)
    namespace = "Worker-Statistics-" + instance
    return namespace

def get_async_workers_namespace(stack_instance):
    instance = get_instance_from_stack_instance(stack_instance)
    namespace = "Asynchronous Workers - " + instance
    return namespace

def get_async_job_stats_namespace(stack_instance):
    instance = get_instance_from_stack_instance(stack_instance)
    namespace = "-".join(["Asynchronous-Jobs", instance])
    return namespace

def get_cloudwatch_memory_instances(stack_instance, instance_type):
    namespace = get_memory_namespace(stack_instance, instance_type)
    cw_client = boto3.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName='used')
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

def get_cloudwatch_worker_stats_instances(stack_instance, metric_name):
    namespace = get_worker_stats_namespace(stack_instance)
    cw_client = boto3.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

def get_cloudwatch_async_job_stats_instances(stack_instance, metric_name):
    namespace = get_async_job_stats_namespace(stack_instance)
    cw_client = boto3.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

# Worker stats
def get_cloudwatch_worker_stats_completed_job_count_instances(stack_instance):
    return get_cloudwatch_worker_stats_instances(stack_instance, "Completed Job Count")

def get_cloudwatch_worker_stats_time_running_instances(stack_instance):
    return get_cloudwatch_worker_stats_instances(stack_instance, "% Time Running")

def get_cloudwatch_worker_stats_cumulative_time_instances(stack_instance):
    return get_cloudwatch_worker_stats_instances(stack_instance, "Cumulative runtime")

# Async job stats


# Widget generation - base
def generate_text_widget(txt, x=0, y=0, width=24, height=1):
    widget = {
        "type": "text",
        "x": x,
        "y": y,
        "width": width,
        "height": height,
        "properties": {
            "markdown": txt
        }
    }
    return widget

def generate_metric_widget(namespace, metric_name, dimension_name, values, stat="Average", title="Title", x=0, y=0, width=8, height=6):
    metrics = [
        [namespace, metric_name, dimension_name, values[0]]
    ]
    for v in values[1:]:
        metrics.append(['...', v])
    widget = {
        "type": "metric",
        "x": x,
        "y": y,
        "width": width,
        "height": height,
        "properties": {
            "metrics": metrics,
            "period": 300,
            "stat": stat,
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False
        }
    }
    return widget

# Widget generation
def generate_ec2_cpu_utilization_widget(x, y, title, ec2_instance_ids):
    return generate_metric_widget("AWS/EC2", "CPUUtilization", "InstanceId", ec2_instance_ids, "Average", title, x, y, 8, 6)

def generate_ec2_networkout_widget(x, y, title, ec2_instance_ids):
    return generate_metric_widget("AWS/EC2", "NetworkOut", "InstanceId", ec2_instance_ids, "Average", title, x, y, 8, 6)

def generate_rds_cpu_utilization_widget(x, y, title, rds_instance_ids):
    return generate_metric_widget("AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", rds_instance_ids, "Average", title, x, y, 8, 6)

def generate_rds_free_storage_space_widget(x, y, title, rds_instance_ids):
    return generate_metric_widget("AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", rds_instance_ids, "Average", title, x, y, 8, 6)

def generate_memory_widget(x, y, title, namespace, metrics_instance_ids):
    return generate_metric_widget(namespace, "used", "instance", metrics_instance_ids, "Average", title, x, y, 8, 6)

def generate_worker_stats_completed_job_count_widget(x, y, title, namespace, metric_name, metric_instance_ids):
    return generate_metric_widget(namespace, metric_name, "Worker Name", metric_instance_ids, "Sum", title, x, y, 8, 6)

def generate_worker_stats_pc_time_running_widget(x, y, title, namespace, metric_name, metric_instance_ids):
    return generate_metric_widget(namespace, metric_name, "Worker Name", metric_instance_ids, "Average", title, x, y, 8, 6)

def generate_worker_stats_cumulative_time_widget(x, y, title, namespace, metric_name, metric_instance_ids):
    return generate_metric_widget(namespace, metric_name, "Worker Name", metric_instance_ids, "Average", title, x, y, 8, 6)

def generate_banner_widget(txt, x, y):
    return generate_text_widget(txt, x, y)

# Widget generation - grouped
def generate_stack_widgets(stack_name, stack_id_backend, stack_id_worker, stack_id_portal, x=0, y=0):
    widgets = []

    widget1 = generate_banner_widget(stack_name, x, y)
    widgets.append(widget1)

    # repo
    instance_ids = get_ec2_instance_ids("repo", stack_id_backend)
    repo_widget = generate_ec2_cpu_utilization_widget(x, y+1, "Repo - CPU utilization (%)", instance_ids)
    widgets.append(repo_widget)

    # portal
    instance_ids = get_ec2_instance_ids("portal", stack_id_portal)
    portal_widget = generate_ec2_cpu_utilization_widget(x+8, y+1, "Portal - CPU utilization (%)", instance_ids)
    widgets.append(portal_widget)

    # workers
    instance_ids = get_ec2_instance_ids("workers", stack_id_worker)
    workers_widget = generate_ec2_cpu_utilization_widget(x+16, y+1, "Workers - CPU utilization (%)", instance_ids)
    widgets.append(workers_widget)

    # portal - networkOut
    instance_ids = get_ec2_instance_ids("portal", stack_id_portal)
    portaln_widget = generate_ec2_networkout_widget(x, y+7, "Portal - Network Out (B)", instance_ids)
    widgets.append(portaln_widget)

    # rds_cpu
    instance_ids = get_rds_instance_ids(stack_id_backend)
    instance_ids.extend(get_rds_idgen_ids())
    rds_widget_cpu = generate_rds_cpu_utilization_widget(x+8, y+7, "RDS - CPU Utilization {%)", instance_ids)
    widgets.append(rds_widget_cpu)

    # rds_freespace
    instance_ids = get_rds_instance_ids(stack_id_backend)
    instance_ids.append(get_rds_idgen_id())
    rds_widget_free_storage_space = generate_rds_free_storage_space_widget(x+16, y+7, "RDS - Free Storage Space (GB)", instance_ids)
    widgets.append(rds_widget_free_storage_space)

    # memory repo
    memory_instance_ids = get_cloudwatch_memory_instances(stack_id_backend, "R")
    memory_widget = generate_memory_widget(x, y+14, "Repo - Memory used (MB)", get_memory_namespace(stack_id_backend, "R"), memory_instance_ids)
    widgets.append(memory_widget)

    # memory workers
    w_memory_instance_ids = get_cloudwatch_memory_instances(stack_id_worker, "W")
    if len(w_memory_instance_ids) > 0:
        memory_namespace = get_memory_namespace(stack_id_worker, "W")
        w_memory_widget = generate_memory_widget(x + 8, y + 14, "Worker - Memory used (MB)", memory_namespace, w_memory_instance_ids)
        widgets.append(w_memory_widget)

    return widgets

def generate_docker_widgets(instance_ids, x=0, y=0):
    widgets = []
    widget1 = generate_banner_widget("# Docker", x, y)
    widgets.append(widget1)
    cpu_widget = generate_ec2_cpu_utilization_widget(x, y + 1, "CPU Utilization (%)", instance_ids)
    widgets.append(cpu_widget)
    network_widget = generate_ec2_networkout_widget(x + 8, y + 1, "Network Out (B)", instance_ids)
    widgets.append(network_widget)
    return widgets

def generate_worker_stats_widgets(title, stack_instance, x=0, y=0):
    widgets = []
    widget1 = generate_banner_widget(title, x, y)
    widgets.append(widget1)

    worker_stats_instances = get_cloudwatch_worker_stats_completed_job_count_instances(stack_instance)
    ws_job_completed_count_widget = generate_worker_stats_completed_job_count_widget(x, y+1, "Worker Stats - Completed Job Count", get_worker_stats_namespace(stack_instance), "Completed Job Count", worker_stats_instances)
    widgets.append(ws_job_completed_count_widget)
    ws_pc_time_running_widget = generate_worker_stats_pc_time_running_widget(x+8, y+1, "Worker Stats - % Time Running", get_worker_stats_namespace(stack_instance), "% Time Running", worker_stats_instances)
    widgets.append(ws_pc_time_running_widget)
    ws_cumulative_time_widget = generate_worker_stats_cumulative_time_widget(x+16, y+1, "Worker Stats - Cumulative Time", get_worker_stats_namespace(stack_instance), "Cumulative runtime", worker_stats_instances)
    widgets.append(ws_cumulative_time_widget)

    return widgets

def generate_async_job_stats_widgets(title, stack_instance, x=0, y=0):
    widgets = []
    widget1 = generate_banner_widget(title, x, y)
    widgets.append(widget1)

    async_job_stats_instances = get_cloudwatch_async_job_stats_instances(stack_instance, "Job elapse time")
    w = generate_metric_widget(get_async_job_stats_namespace(stack_instance), "Job elapse time", "JobType", async_job_stats_instances, "Average", "Average elapse time", x, y+1)
    widgets.append(w)
    return widgets

def generate_ses_stats_widgets(title, x=0, y=0):
    widgets = []
    # widget1 = generate_banner_widget(title, x, y)
    # widgets.append(widget1)

    metrics = [["AWS/SES", "Reputation.BounceRate"]]
    w = {
        "type": "metric",
        "x": x,
        "y": y+1,
        "width": 24,
        "height": 6,
        "properties": {
            "metrics": metrics,
            "period": 300,
            "stat": "Average",
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False
        }
    }
    widgets.append(w)
    return widgets

def generate_sqs_stats_widgets(stack_instance, title, x=0, y=0):
    widgets = []
    # widget1 = generate_banner_widget(title, x, y)
    # widgets.append(widget1)

    queue_name = "prod-{}-QUERY".format(stack_instance)
    metrics = [[ "AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", queue_name ]]
    w = {
        "type": "metric",
        "x": x,
        "y": y + 1,
        "width": 24,
        "height": 6,
        "properties": {
            "metrics": metrics,
            "period": 300,
            "stat": "Average",
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False
        }
    }
    widgets.append(w)
    return widgets

def generate_repo_alb_target_responsetime_widgets(stack_instance, title, x=0, y=0):
    widgets = []

    alb_name = get_repo_alb_name(stack_instance)
    metrics = [
        ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", alb_name],
        ["...", {"stat": "p95"}]
    ]
    w = {
        "type": "metric",
        "x": x,
        "y": y,
        "width": 24,
        "height": 6,
        "properties": {
            "metrics": metrics,
            "period": 300,
            "stat": "Average",
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False,
            # "annotations": {
            #     "horizontal": [
            #         {
            #             "label": "TargetResponseTime >= 1 for 1 datapoints within 5 minutes",
            #             "value": 1
            #         }
            #     ]
            # },
            "setPeriodToTimeRange": True
        }
    }
    widgets.append(w)
    return widgets

def get_repo_alb_name(stack_instance):
    env_name = f'repo-prod-{stack_instance}'
    rgtapi_client = boto3.client('resourcegroupstaggingapi')
    tag_filters = [{'Key':'elasticbeanstalk:environment-name', 'Values': [env_name]}]
    resp = rgtapi_client.get_resources(
        TagFilters=tag_filters,
        ResourceTypeFilters = ['elasticloadbalancing:loadbalancer'],
        IncludeComplianceDetails=False,
        ExcludeCompliantResources=False
    )
    arn = resp["ResourceTagMappingList"][0]["ResourceARN"]
    p = re.compile('arn:aws:elasticloadbalancing:us-east-1:\d+:loadbalancer/(.+)')
    m = p.match(arn)
    alb_name = m.groups()[0]
    return alb_name

def generate_repo_files_scanner_widgets(stack_instances, title, x=0, y=0):
    widgets = []
    metrics = []

    for stack_instance in stack_instances:
        metrics_namespace = get_async_workers_namespace(stack_instance)
        metrics.extend([
            [ metrics_namespace, "JobCompletedCount", "workerClass", "FileHandleAssociationScanRangeWorker", { "label": f"Jobs Completed - {stack_instance}" } ],
            [ ".", "JobFailedCount", ".", ".", { "label": f"Jobs Failed - {stack_instance}", "color": "#d62728" } ],
            [ ".", "AllJobsCompletedCount", ".", ".", { "label": f"Scans Completed - {stack_instance}", "yAxis": "right" } ]
        ])

    w = {
        "type": "metric",
        "x": x,
        "y": y,
        "width": 24,
        "height": 6,
        "properties": {
            "metrics": metrics,
            "period": 300,
            "stat": "Sum",
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False,
            "setPeriodToTimeRange": True,
            "yAxis": {
                "right": {
                    "min": 0,
                    "max": 2,
                    "label": "",
                    "showUnits": True
                }
            }
        }
    }
    widgets.append(w)
    return widgets

def print_usage():
    print("Usage: python create_dashboard_app.py <profile> <prod-stack_backend> <prod-stack_portal> <staging-stack_backend> <staging-stack_portal>")
    print("\twhere\t<profile> is the awscli profile to use")
    print("\t\t\t<prod-stack_backend> is the production stack backend (xxx-y)")
    print("\t\t\t<prod-stack_portal> is the production stack portal (xxx-y)")
    print("\t\t\t<staging-stack_backend> is the staging stack backend (xxx-y)")
    print("\t\t\t<staging-stack_portal> is the staging stack portal (xxx-y)")

if __name__ == "__main__":
    if (len(sys.argv) != 8):
        print_usage()
        exit(1)

    awscli_profile = sys.argv[1]
    prod_stack_backend = sys.argv[2]
    prod_stack_worker = sys.argv[3]
    prod_stack_portal = sys.argv[4]
    staging_stack_backend = sys.argv[5]
    staging_stack_worker = sys.argv[6]
    staging_stack_portal = sys.argv[7]

    p = re.compile("(\d+)-\d+")
    m = p.match(prod_stack_backend)
    if m.group():
        prod_stack_instance = m.group(1)
    else:
        raise ValueError("Could not extract production stack instance from parameters!")

    # print(awscli_profile)
    # print(prod_stack_backend)
    # print(staging_stack_backend)

    boto3.setup_default_session(profile_name=awscli_profile, region_name="us-east-1")

    prod_widgets = generate_stack_widgets("# Production stack", prod_stack_backend, prod_stack_worker, prod_stack_portal, 0, 0)
    prod_worker_stats_widgets = generate_worker_stats_widgets("# Prod Worker Statistics", prod_stack_worker, 0, 21)
    # TODO: Discover docker registry instance ids
    docker_widgets = generate_docker_widgets(["i-02323e8fe74ca20d5", "i-0bc0fd5b374421666"], 0, 35)
    staging_widgets = generate_stack_widgets("# Staging stack", staging_stack_backend, staging_stack_worker, staging_stack_portal, 0, 42)
    staging_worker_stats_widgets = generate_worker_stats_widgets("# Staging Worker Statistics", staging_stack_worker, 0, 63)
    #prod_async_job_stats_widgets = generate_async_job_stats_widgets("# Prod Async Job Statistics", prod_stack_worker, 0, 70)
    #staging_async_job_stats_widgets = generate_async_job_stats_widgets("# Staging Async Job Statistics", staging_stack_worker, 8, 70)
    ses_widgets = generate_ses_stats_widgets("SES Bounce Rate", x=0, y=70)
    sqs_widgets = generate_sqs_stats_widgets(prod_stack_instance, "PROD-QUERY-PERFORMANCE", x=0, y=77)
    prod_repo_alb_rt_widgets = generate_repo_alb_target_responsetime_widgets(prod_stack_backend, 'PROD-ALB-TARGET-RESPONSETIME', x=0, y=84)
    prod_files_scanner_widgets = generate_repo_files_scanner_widgets([prod_stack_worker, staging_stack_worker], 'FILES-SCANNER-STATS', x=0, y=91)

    widgets = prod_widgets
    widgets.extend(prod_worker_stats_widgets)
    widgets.extend(docker_widgets)
    widgets.extend(staging_widgets)
    widgets.extend(staging_worker_stats_widgets)
    #widgets.extend(prod_async_job_stats_widgets)
    #widgets.extend(staging_async_job_stats_widgets)
    widgets.extend(ses_widgets)
    widgets.extend(sqs_widgets)
    widgets.extend(prod_repo_alb_rt_widgets)
    widgets.extend(prod_files_scanner_widgets)

    body = {"widgets":widgets}
    json_body = json.dumps(body, indent=4, sort_keys=True)
    print(json_body)

    cw_client = boto3.client("cloudwatch")
    cw_client.put_dashboard(DashboardName="Stack-status", DashboardBody=json_body)