#!/usr/bin/env bash
set -euo pipefail

entities=(
  "org"
)

for entity in "${entities[@]}"; do
  echo "Creating entity: $entity"
  python3 main.py --action create --name "$entity"
done
