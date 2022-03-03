import sys
import re
import json
import boto3
from cloudwatchmetrics_instances_provider import CloudwatchMetricsInstancesProvider
from utils import _parse_stack_instance, _get_stack_version, _get_stack_number, _gen_stack_instance

# List of stack_versions to include, given current
def included_versions(stack_version, num_versions):
    csvs = []
    csv = int(stack_version)
    for sv in range(csv, csv-num_versions, -1):
      csvs.append(str(sv))
    return csvs

def get_ec2_instance_ids(environment, stack_version):
    stack_versions = included_versions(stack_version, 3)
    instance_ids = []
    for sv in stack_versions:
      ids = cloudwatchmetrics_instances_provider.get_ec2_instances_ids(sv, environment)
      instance_ids.extend(ids)
    return instance_ids

# TODO: Simplify, RDS instance IDs can be generated from stack_version, no need to save/load
def get_rds_instance_ids(stack_version):
    stack_versions = included_versions(stack_version, 3)
    instance_ids = []
    for sv in stack_versions:
      ids = cloudwatchmetrics_instances_provider.get_rds_instances_ids(sv)
      instance_ids.extend(ids)
    return instance_ids

# Memory used: save/load vm_ids
def get_cloudwatch_memory_instances(stack_version, environment):
    stack_versions = included_versions(stack_version, 3)
    instance_ids = {}
    for sv in stack_versions:
        namespace, ids = cloudwatchmetrics_instances_provider.get_cloudwatch_memory_instances(sv, environment)
        instance_ids[namespace] = ids
    return instance_ids

# TODO: Simplify, namespaces can be generated
def get_cloudwatch_worker_stats_instances(stack_version, metric_name):
    instances = cloudwatchmetrics_instances_provider.get_cloudwatch_worker_stats_instance_ids(stack_version, metric_name)
    return instances

def get_async_job_stats_namespace(stack_instance):
    version = _get_stack_version(stack_instance)
    namespace = "-".join(["Asynchronous-Jobs", version])
    return namespace

def get_cloudwatch_async_job_stats_instances(stack_version, metric_name):
    instance_ids = cloudwatchmetrics_instances_provider.get_cloudwatch_async_job_stats_instances(stack_version)
    namespace = get_async_job_stats_namespace(stack_version)
    cw_client = boto3.client("cloudwatch")
    res = cw_client.list_metrics(Namespace=namespace, MetricName=metric_name)
    instances = [metric["Dimensions"][0]["Value"] for metric in res["Metrics"]]
    return instances

# Worker stats
def get_cloudwatch_worker_stats_completed_job_count_instances(stack_version):
    return get_cloudwatch_worker_stats_instances(stack_version, "COMPLETED_JOB_COUNT")

def get_cloudwatch_worker_stats_time_running_instances(stack_instance):
    return get_cloudwatch_worker_stats_instances(stack_instance, "% Time Running")

def get_cloudwatch_worker_stats_cumulative_time_instances(stack_instance):
    return get_cloudwatch_worker_stats_instances(stack_instance, "Cumulative runtime")

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

def generate_metric_widget2(metric_name, dimension_name, nsvalues, stat="Average", title="Title", x=0, y=0, width=8, height=6):
    metrics = []
    for namespace in nsvalues:
        metrics.append([namespace, metric_name, dimension_name, nsvalues[namespace][0]])
        for v in nsvalues[namespace][1:]:
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

# In this case, metrics_instance_ids is a dictionary (namespace, ids)
def generate_memory_widget(x, y, title, metrics_instance_ids):
    return generate_metric_widget2("used", "instance", metrics_instance_ids, "Average", title, x, y, 8, 6)

def generate_worker_stats_completed_job_count_widget(x, y, title, metric_name, metric_instance_ids):
    return generate_metric_widget2(metric_name, "Worker Name", metric_instance_ids, "Sum", title, x, y, 8, 6)

def generate_worker_stats_pc_time_running_widget(x, y, title, metric_name, metric_instance_ids):
    return generate_metric_widget2(metric_name, "Worker Name", metric_instance_ids, "Average", title, x, y, 8, 6)

def generate_worker_stats_cumulative_time_widget(x, y, title, metric_name, metric_instance_ids):
    return generate_metric_widget2(metric_name, "Worker Name", metric_instance_ids, "Average", title, x, y, 8, 6)

def generate_banner_widget(txt, x, y):
    return generate_text_widget(txt, x, y)

# Widget generation - grouped
def generate_stack_widgets(stack_name, stack_version, x=0, y=0):
    widgets = []

    widget1 = generate_banner_widget(stack_name, x, y)
    widgets.append(widget1)

    # repo
    instance_ids = get_ec2_instance_ids("REPO", stack_version)
    repo_widget = generate_ec2_cpu_utilization_widget(x, y+1, "Repo - CPU utilization (%)", instance_ids)
    widgets.append(repo_widget)

    # portal
    instance_ids = get_ec2_instance_ids("PORTAL", stack_version)
    portal_widget = generate_ec2_cpu_utilization_widget(x+8, y+1, "Portal - CPU utilization (%)", instance_ids)
    widgets.append(portal_widget)

    # workers
    instance_ids = get_ec2_instance_ids("WORKERS", stack_version)
    workers_widget = generate_ec2_cpu_utilization_widget(x+16, y+1, "Workers - CPU utilization (%)", instance_ids)
    widgets.append(workers_widget)

    # portal - networkOut
    instance_ids = get_ec2_instance_ids("PORTAL", stack_version)
    portaln_widget = generate_ec2_networkout_widget(x, y+7, "Portal - Network Out (B)", instance_ids)
    widgets.append(portaln_widget)

    # rds_cpu
    instance_ids = get_rds_instance_ids(stack_version)
    rds_widget_cpu = generate_rds_cpu_utilization_widget(x+8, y+7, "RDS - CPU Utilization {%)", instance_ids)
    widgets.append(rds_widget_cpu)

    # rds_freespace
    instance_ids = get_rds_instance_ids(stack_version)
    rds_widget_free_storage_space = generate_rds_free_storage_space_widget(x+16, y+7, "RDS - Free Storage Space (GB)", instance_ids)
    widgets.append(rds_widget_free_storage_space)

    # memory repo
    memory_instance_ids = get_cloudwatch_memory_instances(stack_version, "REPO")
    memory_widget = generate_memory_widget(x, y+14, "Repo - Memory used (MB)", memory_instance_ids)
    widgets.append(memory_widget)

    # memory workers
    w_memory_instance_ids = get_cloudwatch_memory_instances(stack_version, "WORKERS")
    w_memory_widget = generate_memory_widget(x + 8, y + 14, "Worker - Memory used (MB)", w_memory_instance_ids)
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

def generate_worker_stats_widgets(title, stack_version, x=0, y=0):
    widgets = []
    widget1 = generate_banner_widget(title, x, y)
    widgets.append(widget1)
    stack_versions = included_versions(stack_version, 3)
    metrics_instances = {}
    for sv in stack_versions:
      namespace = cloudwatchmetrics_instances_provider.cw_get_worker_stats_namespace(sv)
      worker_stats_instances = get_cloudwatch_worker_stats_completed_job_count_instances(sv)
      metrics_instances[namespace] = worker_stats_instances
    ws_job_completed_count_widget = generate_worker_stats_completed_job_count_widget(x, y+1, "Worker Stats - Completed Job Count", "Completed Job Count", metrics_instances)
    widgets.append(ws_job_completed_count_widget)
    ws_pc_time_running_widget = generate_worker_stats_pc_time_running_widget(x+8, y+1, "Worker Stats - % Time Running", "% Time Running", metrics_instances)
    widgets.append(ws_pc_time_running_widget)
    ws_cumulative_time_widget = generate_worker_stats_cumulative_time_widget(x+16, y+1, "Worker Stats - Cumulative Time", "Cumulative runtime", metrics_instances)
    widgets.append(ws_cumulative_time_widget)

    return widgets

def generate_async_job_stats_widgets(title, stack_instance, x=0, y=0):
    widgets = []
    widget1 = generate_banner_widget(title, x, y)
    widgets.append(widget1)

    async_job_stats_instances = get_cloudwatch_async_job_stats_instances(stack_instance, "Job elapse time")
    namespace = get_async_job_stats_namespace(stack_instance)
    w = generate_metric_widget(namespace, "Job elapse time", "JobType", async_job_stats_instances, "Average", "Average elapse time", x, y+1)
    widgets.append(w)
    return widgets

# No change
def generate_ses_stats_widgets(title, x=0, y=0):
    widgets = []
    metrics = [
        [ "AWS/SES", "Reputation.BounceRate", { "id": "m1", "label": "Bounce Rate", "visible": False, "stat": "Maximum" } ],
        [ ".", "Reputation.ComplaintRate", { "id": "m2", "label": "Complaint Rate", "color": "#d62728", "visible": False, "stat": "Maximum" } ],
        [ { "expression": "100 * m1", "label": "Bounce Rate", "id": "e1", "color": "#1f77b4", "period": 3600 } ],
        [ { "expression": "100 * m2", "label": "Complaint Rate", "id": "e2", "color": "#d62728", "period": 3600 } ],
        [ "AWS/SES", "Bounce", { "id": "m3", "yAxis": "right", "color": "#ff7f0e", "stat": "Sum", "label": "Bounced Count" } ],
        [ ".", "Send", { "id": "m4", "yAxis": "right", "color": "#2ca02c", "stat": "Sum", "label": "Sent Count" } ]
    ]

    w = {
        "type": "metric",
        "x": x,
        "y": y+1,
        "width": 24,
        "height": 6,
        "properties": {
            "metrics": metrics,
            "period": 3600,
            "stat": "Average",
            "title": title,
            "region": "us-east-1",
            "view": "timeSeries",
            "stacked": False,
            "yAxis": {
                "right": {
                    "min": 0,
                    "label": "Count",
                    "showUnits": False
                },
                "left": {
                    "min": 0,
                    "label": "Rate",
                    "showUnits": False
                }
            }
        }
    }
    widgets.append(w)
    return widgets

def generate_sqs_stats_widgets(stack, stack_versions, title, x=0, y=0):
    widgets = []
    first_stack_version = stack_versions[0]
    queue_name = f"{stack}-{first_stack_version}-QUERY"
    metrics = [[ "AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", queue_name ]]
    for stack_version in stack_versions[1:]:
        queue_name = f"{stack}-{stack_version}-QUERY"
        metrics.append(["...", queue_name])
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

def generate_repo_alb_target_responsetime_widgets(stack_versions, title, x=0, y=0):
    widgets = []
    metrics = []
    for stack_version in stack_versions:
        alb_name = cloudwatchmetrics_instances_provider.get_repo_alb_name(stack_version)
        stack_metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", alb_name[0]],
            ["...", {"stat": "p95"}]
        ]
        metrics.extend(stack_metrics)
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

def generate_repo_files_scanner_widgets(stack_instances, title, x=0, y=0):
    widgets = []
    metrics = []

    for stack_instance in stack_instances:
        stack_version = _get_stack_version(stack_instance)
        metrics_namespace = f"Asynchronous Workers - {stack_version}"
        metrics.extend([
            [ metrics_namespace, "JobCompletedCount", "workerClass", "FileHandleAssociationScanRangeWorker", { "label": f"Jobs Completed - {stack_instance}", "color": "#1f77b4" } ],
            [ ".", "JobFailedCount", ".", ".", { "label": f"Jobs Failed - {stack_instance}", "color": "#d62728" } ],
            [ ".", "AllJobsCompletedCount", ".", ".", { "label": f"Scans Completed - {stack_instance}", "yAxis": "right", "color": "#2ca02c" } ]
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
    print("Usage: python create_dashboard_app.py <profile> <stack> <stack_instance>")
    print("\twhere\t<profile> is the awscli profile to use")
    print("\t\t\t<stack> is <dev|prod>")
    print("\t\t\t<stack_instance> is <stack_version>-<stack-number> (e.g. 398-0)")

if __name__ == "__main__":

    if (len(sys.argv) != 3):
        print_usage()
        exit(1)

    aws_profile_name = sys.argv[1]
    stack = sys.argv[2]
    stack_instance = sys.argv[3]

    stack_version, stack_number = _parse_stack_instance(stack_instance)
    stack_versions = included_versions(stack_version, 3)
    stack_instances = [f"{sv}-0" for sv in stack_versions]

    session = boto3.Session(profile_name=aws_profile_name, region_name="us-east-1")
    cloudwatchmetrics_instances_provider = CloudwatchMetricsInstancesProvider(session, f"./history/{stack}")

    ## Update history with current stack
    cloudwatchmetrics_instances_provider.refresh_all(stack, stack_instance)

    widgets = []
    stack_widgets = generate_stack_widgets("# Stacks", stack_version, 0, 0)
    widgets.extend(stack_widgets)
    docker_widgets = generate_docker_widgets(["i-03caba8ba8027dcdb", "i-022acbcb10b0610fa"], 0, 15)
    widgets.extend(docker_widgets)
    prod_worker_stats_widgets = generate_worker_stats_widgets("# Worker Statistics", stack_version, 0, 21)
    widgets.extend(prod_worker_stats_widgets)
    ses_widgets = generate_ses_stats_widgets("SES Statistics", x=0, y=70)
    widgets.extend(ses_widgets)
    sqs_widgets = generate_sqs_stats_widgets(stack, stack_versions, "QUERY-PERFORMANCE", x=0, y=77)
    widgets.extend(sqs_widgets)
    repo_alb_rt_widgets = generate_repo_alb_target_responsetime_widgets(stack_versions, 'ALB-TARGET-RESPONSETIME', x=0, y=84)
    widgets.extend(repo_alb_rt_widgets)
    files_scanner_widgets = generate_repo_files_scanner_widgets(stack_instances, 'FILES-SCANNER-STATS', x=0, y=91)
    widgets.extend(files_scanner_widgets)

    body = {"widgets":widgets, "start": "-PT336H"}
    json_body = json.dumps(body, indent=4, sort_keys=True)
    print(json_body)

    cw_client = session.client("cloudwatch")
    cw_client.put_dashboard(DashboardName="Stack-status-2", DashboardBody=json_body)

    exit(1)

