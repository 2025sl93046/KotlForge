#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
REQUIRED_JAVA_HOME="/Users/I578425/Library/Java/JavaVirtualMachines/sapmachine-17.0.16/Contents/Home"
ZSHRC="$HOME/.zshrc"

echo "=== KotlForge Setup ==="
echo ""

# ── 1. Verify JDK 17 exists ──────────────────────────────────────────────────
if [ ! -d "$REQUIRED_JAVA_HOME" ]; then
  echo "ERROR: JDK 17 not found at $REQUIRED_JAVA_HOME"
  echo "Install SAP Machine 17: https://sap.github.io/SapMachine/"
  exit 1
fi
echo "[1/4] JDK 17 found: $REQUIRED_JAVA_HOME"

# ── 2. Set JAVA_HOME for this session ────────────────────────────────────────
export JAVA_HOME="$REQUIRED_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[2/4] JAVA_HOME set for this session: $JAVA_HOME"

# ── 3. Persist JAVA_HOME in ~/.zshrc (only if not already set) ───────────────
EXPORT_LINE="export JAVA_HOME=\"$REQUIRED_JAVA_HOME\""
if grep -qF "$REQUIRED_JAVA_HOME" "$ZSHRC" 2>/dev/null; then
  echo "[3/4] JAVA_HOME already in $ZSHRC — skipping"
else
  echo "" >> "$ZSHRC"
  echo "# KotlForge: pin JDK 17 (SAP Machine) for Gradle compatibility" >> "$ZSHRC"
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
