#!/usr/bin/env python3
import glob
import json
import os
import sys
import unittest

def validate_task_graph_json(filepath):
    """
    Validates that a task graph JSON file:
    1. Is structurally valid JSON.
    2. Has required top-level fields (taskId, epics).
    3. Every epic contains at least one BARCAN-TAG-06 slice.
    """
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)

    if not isinstance(data, dict):
        raise ValueError(f"Root JSON element in {filepath} is not an object.")

    if "epics" not in data or not isinstance(data["epics"], list):
        raise ValueError(f"Missing or invalid 'epics' array in {filepath}.")

    epics = data["epics"]
    if not epics:
        raise ValueError(f"'epics' array in {filepath} is empty.")

    for i, epic in enumerate(epics):
        if not isinstance(epic, dict):
            raise ValueError(f"Epic #{i} in {filepath} is not an object.")

        slices = epic.get("slices", [])
        if not isinstance(slices, list) or not slices:
            raise ValueError(f"Epic #{i} ('{epic.get('title', 'Untitled')}') in {filepath} has no slices.")

        has_qa_slice = any(
            isinstance(s, dict) and (s.get("roleTag") == "BARCAN-TAG-06" or s.get("role") == "BARCAN-TAG-06")
            for s in slices
        )
        if not has_qa_slice:
            raise ValueError(f"Epic #{i} ('{epic.get('title', 'Untitled')}') in {filepath} lacks a BARCAN-TAG-06 QA slice.")

    return True

class TestTaskGraphQASlices(unittest.TestCase):
    def test_patched_task_graphs_have_qa_slices(self):
        plan_files = sorted(glob.glob("docs/task-plan-*.json"))
        self.assertGreater(len(plan_files), 0, "No task plan files found matching docs/task-plan-*.json")

        for filepath in plan_files:
            with self.subTest(filepath=filepath):
                try:
                    valid = validate_task_graph_json(filepath)
                    self.assertTrue(valid)
                except Exception as e:
                    self.fail(f"Validation failed for {filepath}: {str(e)}")

if __name__ == "__main__":
    unittest.main()
