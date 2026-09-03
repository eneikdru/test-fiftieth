import json

with open(".eneik/records/task-plan-0495732a-15fd-4643-b5b7-f8ad753883f2.json") as f:
    data = json.load(f)

for epic in data.get('epics', []):
    for slice in epic.get('slices', []):
        if 'analysis' in slice.get('title', '').lower():
            print(slice)
