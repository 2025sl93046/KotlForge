#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

resolve_java_home() {
	if command -v /usr/libexec/java_home >/dev/null 2>&1; then
		/usr/libexec/java_home -v 17 2>/dev/null \
			|| /usr/libexec/java_home -v 21 2>/dev/null \
			|| true
	fi
}
# because this code base works only for java version < 25 

JAVA_HOME_CANDIDATE="$(resolve_java_home)"

if [ -z "$JAVA_HOME_CANDIDATE" ] || [ ! -d "$JAVA_HOME_CANDIDATE" ]; then
	echo "ERROR: No compatible JDK found (requires JDK 17 or higher)."
	echo "Install SAP Machine: https://sap.github.io/SapMachine/"
	exit 1
fi

# ── Ensure correct JDK is active ─────────────────────────────────────────────
export JAVA_HOME="$JAVA_HOME_CANDIDATE"
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
