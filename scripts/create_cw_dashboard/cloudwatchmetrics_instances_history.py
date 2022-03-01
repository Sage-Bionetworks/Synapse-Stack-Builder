import os
import re
import logging
from utils import _gen_stack_instance


class CloudwatchMetricsInstancesHistory:

  def __init__(self, history_path):
    self.history_path = history_path
    self.history = dict()
    self._init()

  def _init(self):
    if not os.path.exists(self.history_path):
      logging.info(f"Creating directory for Cloudwatch Metrics Instance ID history at {self.history_path}")
      os.makedirs(self.history_path)
    filenames = [f for f in os.listdir(self.history_path) if os.path.isfile(os.path.join(self.history_path, f)) and f.find('.txt') != -1]
    for fname in filenames:
      with open(os.path.join(self.history_path, fname), 'r') as f:
        instances = [line.rstrip('\n') for line in f]
        self.history[os.path.splitext(fname)[0]] = instances
    return

  def _dump(self):
    print(self.history)

  def write_cloudwatchmetrics_instances_ids(self, stack_version, metric_type, environment, instance_ids):
    if environment != None:
      fname = f"{stack_version}-{metric_type}-{environment}.txt"
    else:
      fname = f"{stack_version}-{metric_type}.txt"
    self._write_cloudwatchmetrics_instances_ids(fname, instance_ids)
    return

  def _write_cloudwatchmetrics_instances_ids(self, fname, instance_ids):
    fpath = os.path.join(self.history_path, fname)
    with open(fpath, "w") as f:
      for id in instance_ids:
        f.write(id)
        f.write("\n")
    self._init()
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
