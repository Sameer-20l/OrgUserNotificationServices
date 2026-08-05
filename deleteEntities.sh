#!/usr/bin/env bash
set -euo pipefail

entities=(
  "org"
  "user"
)

for entity in "${entities[@]}"; do
  echo "Creating entity: $entity"
  python3 main.py --action delete --name "$entity"
done
