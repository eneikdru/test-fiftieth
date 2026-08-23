#!/usr/bin/env python3
import sys
import os
import json
import argparse

def validate_task_plan(plan_filepath, payload_filepath=None):
    errors = []

    # 1. Directory Path Validation
    normalized_path = os.path.normpath(plan_filepath)
    path_parts = normalized_path.split(os.sep)

    # Must be saved under .eneik directory rather than docs directory
    if "docs" in path_parts:
        errors.append(f"Directory Violation: Task plan JSON must be written to '.eneik' directory, not 'docs' directory ({plan_filepath}).")

    if ".eneik" not in path_parts:
        errors.append(f"Directory Violation: Task plan JSON must be saved in '.eneik' directory ({plan_filepath}).")

    if not os.path.exists(plan_filepath):
        errors.append(f"File Error: Task plan file does not exist: {plan_filepath}")
        return False, errors

    try:
        with open(plan_filepath, "r", encoding="utf-8") as f:
            plan_data = json.load(f)
    except Exception as e:
        errors.append(f"JSON Parse Error: Failed to parse task plan JSON: {str(e)}")
        return False, errors

    # 2. Schema and sourceIndex Validation
    if not isinstance(plan_data, dict):
        errors.append("Schema Violation: Root task plan JSON must be an object.")
        return False, errors

    if "sourceIndex" not in plan_data:
        errors.append("Schema Violation: Missing required 'sourceIndex' field in task plan JSON.")
    else:
        source_index = plan_data["sourceIndex"]

        # Extract wishlist IDs from sourceIndex
        plan_wishlist_ids = []
        if isinstance(source_index, dict):
            wishlist_ids = source_index.get("wishlistIds", [])
            if isinstance(wishlist_ids, list):
                plan_wishlist_ids = [str(x) for x in wishlist_ids]
            elif wishlist_ids:
                plan_wishlist_ids = [str(wishlist_ids)]
        elif isinstance(source_index, list):
            plan_wishlist_ids = [str(x) for x in source_index]
        else:
            plan_wishlist_ids = [str(source_index)]

        if not plan_wishlist_ids:
            errors.append("Schema Violation: 'sourceIndex' must contain at least one wishlist ID reference.")

        # 3. Validation against payload
        if payload_filepath:
            if not os.path.exists(payload_filepath):
                errors.append(f"Payload Error: Source payload file does not exist: {payload_filepath}")
            else:
                try:
                    with open(payload_filepath, "r", encoding="utf-8") as pf:
                        payload_data = json.load(pf)

                    payload_wishlist_ids = set()
                    if isinstance(payload_data, dict):
                        if "wishlistIds" in payload_data and isinstance(payload_data["wishlistIds"], list):
                            payload_wishlist_ids.update(str(x) for x in payload_data["wishlistIds"])
                        if "wishlist" in payload_data and isinstance(payload_data["wishlist"], list):
                            for item in payload_data["wishlist"]:
                                if isinstance(item, dict) and "id" in item:
                                    payload_wishlist_ids.add(str(item["id"]))
                                else:
                                    payload_wishlist_ids.add(str(item))
                        if "id" in payload_data:
                            payload_wishlist_ids.add(str(payload_data["id"]))
                    elif isinstance(payload_data, list):
                        for item in payload_data:
                            if isinstance(item, dict) and "id" in item:
                                payload_wishlist_ids.add(str(item["id"]))
                            else:
                                payload_wishlist_ids.add(str(item))

                    # Strict check: Every wishlist ID in plan's sourceIndex must exist in payload
                    unlinked_ids = [wid for wid in plan_wishlist_ids if wid not in payload_wishlist_ids]
                    if unlinked_ids:
                        errors.append(f"Linkage Violation: sourceIndex wishlist IDs {unlinked_ids} do not match source payload wishlist IDs {list(payload_wishlist_ids)}.")

                except Exception as e:
                    errors.append(f"Payload Parse Error: Failed to parse source payload JSON: {str(e)}")

    is_valid = len(errors) == 0
    return is_valid, errors

def main():
    parser = argparse.ArgumentParser(description="Validate task plan JSON file and sourceIndex payload linkage.")
    parser.add_argument("plan_file", help="Path to task plan JSON file")
    parser.add_argument("--payload", help="Path to source payload JSON file", default=None)

    args = parser.parse_args()

    is_valid, errors = validate_task_plan(args.plan_file, args.payload)

    if is_valid:
        print(f"SUCCESS: Task plan '{args.plan_file}' is valid and correctly linked.")
        sys.exit(0)
    else:
        print(f"VALIDATION FAILED for '{args.plan_file}':", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
