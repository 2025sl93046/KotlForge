#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
REQUIRED_JAVA_HOME="/Users/I578425/Library/Java/JavaVirtualMachines/sapmachine-17.0.16/Contents/Home"

# ── Ensure correct JDK is active ─────────────────────────────────────────────
export JAVA_HOME="$REQUIRED_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=== KotlForge Build ==="
echo "  Java:    $(java -version 2>&1 | head -1)"
echo "  Project: $PROJECT_DIR"
echo ""

# ── Run the build ─────────────────────────────────────────────────────────────
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" clean build

echo ""
echo "Build successful."
echo ""
echo "Try the CLI:"
echo "  ./gradlew :cli:run --args=\"doctor\""
echo "  ./gradlew :cli:run --args=\"build --project /path/to/android-app\""
