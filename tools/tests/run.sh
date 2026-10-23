#!/usr/bin/env sh
# Requires Java 21 and Python 3. No Minecraft/Gradle dependencies for these contracts.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" "$ROOT/src/main/java/com/kostya/cataclysm/ImpactDynamics.java" "$ROOT/tools/tests/ImpactDynamicsTest.java"
java -cp "$TMP" ImpactDynamicsTest
python3 "$ROOT/tools/tests/test_visual_contract.py"
