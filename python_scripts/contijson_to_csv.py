import json
import os, sys


valid_header = [
    "trafficlength", "trafficheigth", "drivetime", "trafficx", "rpm", "yawrate", "driver", "recordingid", "longitude", "speed", "trafficy", "pedal", "latitude"
]



def clean_file_and_get_json(file_path):
    f = open(file_path, "r")
    try:
        return json.load(f)
    except json.JSONDecodeError:
        f = open(file_path, "r")
        json_list = []
        for l in f.readlines():
            json_list.append(json.loads(l))
        return json_list


def parse_to_csv(file_path, filename):
    js = clean_file_and_get_json(file_path)
    g = open(filename.split(".")[0] + ".csv", "w")
    keys = []
    for head in valid_header:
        keys.append(head)
    g.write(','.join(keys) + '\n')
    for d in js:
        row = []
        for head in valid_header:
            if head in d:
                row.append(str(d[head]))
            else:
                row.append("0.0")
        g.write(','.join(row) + '\n')

    g.close()

# traverse root directory, and list directories as dirs and files as files
for root, dirs, files in os.walk("."):
    path = root.split(os.sep)
    for file in files:
        if file.endswith(".json"):
            parse_to_csv(os.path.join(root, file), file)
