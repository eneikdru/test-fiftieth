import json
import os

data = {
  "epics": [
    {
      "existingEpicId": "b9c3cb6d-c0a1-4178-b202-1093b5058008",
      "sourceIndex": 0,
      "requirements": [
        "R1: The intended code change for Merge Readiness task 012e2239 is documented for recovery"
      ],
      "coverageComplete": True,
      "slices": [
        {
          "title": "Spike missing deliverable for task 012e2239",
          "roleTag": "BARCAN-TAG-01",
          "jtbd": "When implementing recovery for this epic, I want to spike and document the missing implementation details for 012e2239, so that the delivered code can be planned for the main branch.",
          "acceptanceCriteria": "Given a repository missing the commit for 012e2239\nWhen the spike is completed\nThen an architecture decision record is written detailing the missing code changes\nGiven the decision record\nWhen reviewed\nThen the path forward is clear and deterministic",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "delivery-pipeline-integrity",
          "sixSigmaMetric": "reduce the count of phantom deliverables from 1 to 0 for this specific task",
          "hasUi": False,
          "requirementRefs": ["R1"]
        },
        {
          "title": "Verify QA layer for task 012e2239 recovery spike",
          "roleTag": "BARCAN-TAG-06",
          "jtbd": "When testing this epic, I want to verify that the missing deliverable spike is comprehensive, so that stakeholders can trust the delivery pipeline.",
          "acceptanceCriteria": "Given the architecture decision record for 012e2239\nWhen the QA review is performed\nThen all edge cases and integration points are considered\nGiven the QA review\nWhen it is approved\nThen the spike is considered complete",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "qa-delivery-trust",
          "sixSigmaMetric": "reduce the defect escape rate from 1 to 0 for this missing deliverable",
          "hasUi": False,
          "requirementRefs": ["R1"]
        }
      ]
    },
    {
      "existingEpicId": "b9c3cb6d-c0a1-4178-b202-1093b5058008",
      "sourceIndex": 1,
      "requirements": [
        "R1: The intended code change for Merge Readiness task F4703f33 is documented for recovery"
      ],
      "coverageComplete": True,
      "slices": [
        {
          "title": "Spike missing deliverable for task F4703f33",
          "roleTag": "BARCAN-TAG-01",
          "jtbd": "When implementing recovery for this epic, I want to spike and document the missing implementation details for F4703f33, so that the delivered code can be planned for the main branch.",
          "acceptanceCriteria": "Given a repository missing the commit for F4703f33\nWhen the spike is completed\nThen an architecture decision record is written detailing the missing code changes\nGiven the decision record\nWhen reviewed\nThen the path forward is clear and deterministic",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "delivery-pipeline-integrity",
          "sixSigmaMetric": "reduce the count of phantom deliverables from 1 to 0 for this specific task",
          "hasUi": False,
          "requirementRefs": ["R1"]
        },
        {
          "title": "Verify QA layer for task F4703f33 recovery spike",
          "roleTag": "BARCAN-TAG-06",
          "jtbd": "When testing this epic, I want to verify that the missing deliverable spike is comprehensive, so that stakeholders can trust the delivery pipeline.",
          "acceptanceCriteria": "Given the architecture decision record for F4703f33\nWhen the QA review is performed\nThen all edge cases and integration points are considered\nGiven the QA review\nWhen it is approved\nThen the spike is considered complete",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "qa-delivery-trust",
          "sixSigmaMetric": "reduce the defect escape rate from 1 to 0 for this missing deliverable",
          "hasUi": False,
          "requirementRefs": ["R1"]
        }
      ]
    },
    {
      "existingEpicId": "b9c3cb6d-c0a1-4178-b202-1093b5058008",
      "sourceIndex": 2,
      "requirements": [
        "R1: The intended code change for Merge Readiness task Da083b0a is documented for recovery"
      ],
      "coverageComplete": True,
      "slices": [
        {
          "title": "Spike missing deliverable for task Da083b0a",
          "roleTag": "BARCAN-TAG-01",
          "jtbd": "When implementing recovery for this epic, I want to spike and document the missing implementation details for Da083b0a, so that the delivered code can be planned for the main branch.",
          "acceptanceCriteria": "Given a repository missing the commit for Da083b0a\nWhen the spike is completed\nThen an architecture decision record is written detailing the missing code changes\nGiven the decision record\nWhen reviewed\nThen the path forward is clear and deterministic",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "delivery-pipeline-integrity",
          "sixSigmaMetric": "reduce the count of phantom deliverables from 1 to 0 for this specific task",
          "hasUi": False,
          "requirementRefs": ["R1"]
        },
        {
          "title": "Verify QA layer for task Da083b0a recovery spike",
          "roleTag": "BARCAN-TAG-06",
          "jtbd": "When testing this epic, I want to verify that the missing deliverable spike is comprehensive, so that stakeholders can trust the delivery pipeline.",
          "acceptanceCriteria": "Given the architecture decision record for Da083b0a\nWhen the QA review is performed\nThen all edge cases and integration points are considered\nGiven the QA review\nWhen it is approved\nThen the spike is considered complete",
          "leanValue": "essential",
          "cynefinDomain": "complicated",
          "tocConstraintRef": "qa-delivery-trust",
          "sixSigmaMetric": "reduce the defect escape rate from 1 to 0 for this missing deliverable",
          "hasUi": False,
          "requirementRefs": ["R1"]
        }
      ]
    },
    {
      "existingEpicId": "ad6a8f66-e34b-44b9-a85a-644f8cc6013d",
      "sourceIndex": 0,
      "requirements": [
        "R1: The system continuously measures and reports the ratio of closed tasks that possess verified merge evidence on the main branch."
      ],
      "coverageComplete": True,
      "slices": [
        {
          "title": "Implement Code Delivery Integrity Metrics",
          "roleTag": "BARCAN-TAG-02",
          "jtbd": "When implementing the measurement for this epic, I want to collect merge evidence metrics for closed tasks, so that we can trust the delivery pipeline.",
          "acceptanceCriteria": "Given a set of closed tasks in the system\nWhen the metric job runs\nThen it calculates the ratio of tasks with actual merge commits on main\nGiven the calculated metric\nWhen the reporting endpoint is queried\nThen the metric is exposed for monitoring",
          "leanValue": "essential",
          "cynefinDomain": "clear",
          "tocConstraintRef": "delivery-metrics-visibility",
          "sixSigmaMetric": "increase metric coverage of task delivery integrity from 0% to 100%",
          "hasUi": False,
          "requirementRefs": ["R1"]
        },
        {
          "title": "Verify Code Delivery Integrity Metrics",
          "roleTag": "BARCAN-TAG-06",
          "jtbd": "When testing this epic, I want to verify the metric accuracy, so that stakeholders can trust the delivery pipeline.",
          "acceptanceCriteria": "Given a mock set of closed tasks and git histories\nWhen the metric calculation is tested\nThen the output matches the expected ratio exactly\nGiven the reporting endpoint\nWhen queried via integration test\nThen the response format is correct",
          "leanValue": "essential",
          "cynefinDomain": "clear",
          "tocConstraintRef": "qa-delivery-trust",
          "sixSigmaMetric": "reduce metric reporting errors from 1 to 0",
          "hasUi": False,
          "requirementRefs": ["R1"]
        }
      ]
    }
  ]
}

os.makedirs(".eneik/records", exist_ok=True)
with open(".eneik/records/task-plan-0495732a-15fd-4643-b5b7-f8ad753883f2.json", "w") as f:
    json.dump(data, f, indent=2)
