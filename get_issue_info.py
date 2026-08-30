import json

with open("blocker.json", "w") as f:
    json.dump({"issue": "Missing consent platform script endpoint or script block"}, f)
