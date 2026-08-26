import json

def validate():
    with open(".eneik/records/task-plan-33e03569-65c1-4b11-beac-f44c7ec0e6cc.json") as f:
        data = json.load(f)

    assert "epics" in data

    for epic in data["epics"]:
        assert "sourceIndex" in epic
        assert epic["sourceIndex"] in [0, 1, 2]

        if epic["existingEpicId"] is not None:
            assert epic["title"] is None
            assert epic["jtbd"] is None
            assert epic["kanoClass"] is None
            assert epic["cynefinDomain"] is None
        else:
            assert epic["title"] is not None
            assert epic["jtbd"] is not None
            assert epic["kanoClass"] is not None
            assert epic["cynefinDomain"] is not None

        assert "requirements" in epic
        for r in epic["requirements"]:
            assert r.startswith("R")

        assert "slices" in epic
        for s in epic["slices"]:
            assert "roleTag" in s
            assert "jtbd" in s
            assert "When implementing" in s["jtbd"]
            assert "acceptanceCriteria" in s
            assert "Given" in s["acceptanceCriteria"]
            assert "When" in s["acceptanceCriteria"]
            assert "Then" in s["acceptanceCriteria"]
            assert "requirementRefs" in s

    print("Validation passed!")

if __name__ == "__main__":
    validate()
