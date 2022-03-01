import os

import pytest

from cloudwatchmetrics_instances_history import CloudwatchMetricsInstancesHistory

class TestCloudwatchMetricsInstancesHistory:

  def test_init_no_dir(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert os.path.exists(history_path)
    assert len(history._get_raw_history()) == 0

  def test_init_dir_no_files(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    os.makedirs(history_path)
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert len(history._get_raw_history()) == 0

  def test_init_dir_with_files(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    os.makedirs(history_path)
    self.gen_history(history_path, "repo", 400, 5, 3, "ec2")
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert len(history._get_raw_history()) == 15

  def test_write_history(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    os.makedirs(history_path)
    self.gen_history(history_path, "repo", 400, 5, 3, "ec2")
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert len(history._get_raw_history()) == 15
    cw_metrics_instance_ids = ["worker-01", "worker-02"]
    history.write_cloudwatch_metrics_instance_ids("400-0", "ec2", "workers", cw_metrics_instance_ids)
    assert len(history._get_raw_history()) == 16

  def test_get_cloudwatch_metrics_instance_ids(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    os.makedirs(history_path)
    self.gen_history(history_path, "repo", 400, 5, 3, "ec2")
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert len(history._get_raw_history()) == 15
    cw_metrics_instances = history.get_cloudwatch_metrics_instance_ids("401-1", "ec2", "repo")
    assert len(cw_metrics_instances) == 4
    assert "repo-401-1-0" in cw_metrics_instances
    assert "repo-401-1-3" in cw_metrics_instances

  def test_get_cloudwatch_metrics_instance_ids_for_stack_version(self, tmpdir):
    history_path = os.path.join(tmpdir, "histdir")
    os.makedirs(history_path)
    self.gen_history(history_path, "repo", 400, 2, 2, "ec2")
    history = CloudwatchMetricsInstancesHistory(history_path)
    assert len(history._get_raw_history()) == 4
    cw_metrics_instances = history.get_cloudwatch_metrics_instance_ids_for_stack_version("repo", "401", "ec2")
    assert len(cw_metrics_instances) == 8
    assert "repo-401-0-0" in cw_metrics_instances
    assert "repo-401-1-3" in cw_metrics_instances

  def test_parse_history_key(self):
    k = "390-EC2_CPU-REPO"
    sv, m, e = CloudwatchMetricsInstancesHistory._parse_history_key(k)
    assert [sv, m, e] == ["390", "EC2_CPU", "REPO"]
    k = "391-RDS_CPU"
    sv, m, e = CloudwatchMetricsInstancesHistory._parse_history_key(k)
    assert e == None
    assert [sv, m] == ["391", "RDS_CPU"]


  def test_parse_history_key_invalid(self):
    with pytest.raises(ValueError):
      k = "REPO-392-RDS_CPU"
      sv, m, e = CloudwatchMetricsInstancesHistory._parse_history_key(k)


  @staticmethod
  def gen_history(path, environment, start_stack_version, num_versions, num_per_version, metric_type):
    for version in range(start_stack_version, start_stack_version+num_versions):
      for sn in range(num_per_version):
        filename = f"{environment}-{version}-{sn}-{metric_type}"
        filepath = os.path.join(path, filename)
        with open(filepath, "w") as f:
          for l in range(4):
            metrics_instance_id = environment.lower() + "-" + f"{version}-{sn}-{l}\n"
            f.write(metrics_instance_id)




