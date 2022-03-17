import os
import re
import logging
import boto3
import pprint

class CloudwatchMetricsInstancesHistory:

  def __init__(self, boto3_session, history_path, history_bucket_name):
    self.boto3_session = boto3_session
    self.history_bucket_name = history_bucket_name
    self.history_path = history_path
    self.history = dict()
    self._init()

  def _init(self):
    if not os.path.exists(self.history_path):
      logging.info(f"Creating directory for Cloudwatch Metrics Instance ID history at {self.history_path}")
      os.makedirs(self.history_path)
    self._init_file_cache_from_s3()
    filenames = [f for f in os.listdir(self.history_path) if os.path.isfile(os.path.join(self.history_path, f)) and f.find('.txt') != -1]
    for fname in filenames:
      self._initfile(fname, False)
    return

  def _initfile(self, fname, from_s3):
    fpath = os.path.join(self.history_path, fname)
    if from_s3:
      s3 = self.boto3_session.resource('s3')
      bucket = s3.Bucket(self.history_bucket_name)
      bucket.download_file(fname, fpath)
    with open(fpath, 'r') as f:
      instances = [line.rstrip('\n') for line in f]
      hist_key = os.path.splitext(fname)[0]
      self.history[hist_key] = instances

  def _init_file_cache_from_s3(self):
    s3 = self.boto3_session.resource('s3')
    bucket = s3.Bucket(self.history_bucket_name)
    for o in bucket.objects.all():
      path, fname = os.path.split(o.key)
      target_path = os.path.join(self.history_path, fname)
      bucket.download_file(o.key, target_path)

  def _save_to_s3(self, fname):
    s3 = self.boto3_session.resource('s3')
    bucket = s3.Bucket(self.history_bucket_name)
    source_path = os.path.join(self.history_path, fname)
    bucket.upload_file(source_path, fname)
    return

  def _dump(self):
    pp = pprint.PrettyPrinter(indent=2)
    pp.pprint(self.history)

  def write_cloudwatchmetrics_instances_ids(self, stack_version, metric_type, environment, instance_ids):
    if environment != None:
      fname = f"{stack_version}-{metric_type}-{environment}.txt"
    else:
      fname = f"{stack_version}-{metric_type}.txt"
    self._write_cloudwatchmetrics_instances_ids(fname, instance_ids)
    self._save_to_s3(fname)
    self._initfile(fname, True) # put in memory cache
    return

  def _write_cloudwatchmetrics_instances_ids(self, fname, instance_ids):
    fpath = os.path.join(self.history_path, fname)
    with open(fpath, "w") as f:
      for id in instance_ids:
        f.write(id)
        f.write("\n")
    return

  # Returns instances ids used by Cloudwatch for a given (stack_version, metric_type, environment, )
  def get_cloudwatch_metrics_instance_ids(self, stack_version, metric_type, environment):
    if environment == None:
      history_key = f"{stack_version}-{metric_type}"
    else:
      history_key = f"{stack_version}-{metric_type}-{environment}"
    return self.history[history_key]

  def _get_raw_history(self):
    return self.history

  @staticmethod
  def _parse_history_key(k):
    p = re.compile("(\d+)-(\w+)(-(\w+))*")
    m = p.match(k)
    if m and m.group():
      stack_version = m.group(1)
      metrics_type = m.group(2)
      environment = m.group(4)
    else:
      raise ValueError("Could not parse key")
    return stack_version, metrics_type, environment
