import re

def _gen_stack_instance(stack_version, stack_number):
  return f"{stack_version}-{stack_number}"

def _parse_stack_instance(stack_instance):
  p = re.compile("(\d+)-(\d+)")
  m = p.match(stack_instance)
  if m.group():
    stack_version = m.group(1)
    stack_number = m.group(2)
  else:
    raise ValueError("Could not parse key")
  return stack_version, stack_number

def _get_stack_version(stack_instance):
  stack_version, stack_number = _parse_stack_instance(stack_instance)
  return stack_version

def _get_stack_number(stack_instance):
  stack_version, stack_number = _parse_stack_instance(stack_instance)
  return stack_number

