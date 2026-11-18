#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ZSHRC="$HOME/.zshrc"

resolve_java_home() {
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    /usr/libexec/java_home -v 17 2>/dev/null \
      || /usr/libexec/java_home -v 21 2>/dev/null \
      || true
  fi
}
# because this code base works only for java version < 25

JAVA_HOME_CANDIDATE="$(resolve_java_home)"

echo "=== KotlForge Setup ==="
echo ""

# ── 1. Verify JDK 17 exists ──────────────────────────────────────────────────
if [ -z "$JAVA_HOME_CANDIDATE" ] || [ ! -d "$JAVA_HOME_CANDIDATE" ]; then
  echo "ERROR: No compatible JDK found (requires JDK 17 or higher)."
  echo "Install SAP Machine: https://sap.github.io/SapMachine/"
  exit 1
fi
echo "[1/4] Compatible JDK found: $JAVA_HOME_CANDIDATE"

# ── 2. Set JAVA_HOME for this session ────────────────────────────────────────
export JAVA_HOME="$JAVA_HOME_CANDIDATE"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[2/4] JAVA_HOME set for this session: $JAVA_HOME"

# ── 3. Persist JAVA_HOME in ~/.zshrc (only if not already set) ───────────────
EXPORT_LINE="export JAVA_HOME=\"$JAVA_HOME\""
if grep -qF "$JAVA_HOME" "$ZSHRC" 2>/dev/null; then
  echo "[3/4] JAVA_HOME already in $ZSHRC — skipping"
else
  echo "" >> "$ZSHRC"
  echo "# KotlForge: pin JDK 17+ for Gradle compatibility" >> "$ZSHRC"
  echo "$EXPORT_LINE" >> "$ZSHRC"
  echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> "$ZSHRC"
  echo "[3/4] JAVA_HOME persisted to $ZSHRC"
fi

# ── 4. Make Gradle wrapper executable and validate ───────────────────────────
chmod +x "$PROJECT_DIR/gradlew"
GRADLE_VERSION=$("$PROJECT_DIR/gradlew" --version 2>/dev/null | grep "^Gradle" | awk '{print $2}')
echo "[4/4] Gradle wrapper ready: Gradle $GRADLE_VERSION"

echo ""
echo "Setup complete."
echo "  Java:   $(java -version 2>&1 | head -1)"
echo "  Gradle: $GRADLE_VERSION"
echo ""
echo "Run './build.sh' to build the project."
