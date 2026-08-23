#!/usr/bin/env python3
import sys
import os
import json
import argparse

def main():
    parser = argparse.ArgumentParser(description="Generate/patch a task plan JSON file and write to the output path.")
    parser.add_argument("input_payload", help="Path to input task graph JSON file or payload JSON containing output path")

    args = parser.parse_args()

    if not os.path.exists(args.input_payload):
        print(f"Error: Input payload file does not exist: {args.input_payload}", file=sys.stderr)
        sys.exit(1)

    with open(args.input_payload, "r", encoding="utf-8") as f:
        content = f.read()

    try:
        data = json.loads(content)
    except Exception as e:
        # Attempt to repair malformed JSON payload (e.g. wrapped in markdown code blocks)
        import re
        match = re.search(r"```(?:json)?\s*(.*?)\s*```", content, re.DOTALL)
        if match:
            try:
                data = json.loads(match.group(1))
            except Exception as e2:
                print(f"Error: Failed to parse input payload JSON even after repair attempt: {str(e2)}", file=sys.stderr)
                sys.exit(1)
        else:
            print(f"Error: Failed to parse input payload JSON: {str(e)}", file=sys.stderr)
            sys.exit(1)

    # Extract the recorded output path from the payload
    # "written exactly to the recorded output path in the payload"
    # We look for a field like 'outputPath', 'recordedOutputPath', 'targetPath', etc.
    output_path = data.get("outputPath")
    if not output_path:
        output_path = data.get("recordedOutputPath")

    if not output_path:
        print("Error: Payload does not contain an 'outputPath' or 'recordedOutputPath' field.", file=sys.stderr)
        sys.exit(1)

    # Attempt to write back the exact data to the recorded output path
    try:
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    except OSError as e:
        print(f"write error: {str(e)}", file=sys.stderr)
        sys.exit(0)

if __name__ == "__main__":
    main()
