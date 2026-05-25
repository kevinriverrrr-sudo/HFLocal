#!/bin/bash
set -e

echo "=== HF Local Build Script ==="
cd /home/z/my-project/HFLocal

# Build Android APK (signed release)
echo "[1/4] Building Android APK..."
./gradlew :androidApp:assembleRelease -x test --no-daemon

# Build Desktop JAR (for both Linux and Windows)
echo "[2/4] Building Desktop JAR..."
./gradlew :desktopApp:jar -x test --no-daemon

# Build Desktop native packages (Linux)
echo "[3/4] Building Linux packages..."
./gradlew :desktopApp:packageDmg -x test --no-daemon 2>/dev/null || \
./gradlew :desktopApp:packageDeb -x test --no-daemon 2>/dev/null || \
./gradlew :desktopApp:createDistributable --no-daemon 2>/dev/null || \
echo "  Linux packaging not available, JAR will be used"

# Build Desktop native packages (Windows)
echo "[4/4] Building Windows packages..."
./gradlew :desktopApp:packageMsi -x test --no-daemon 2>/dev/null || \
./gradlew :desktopApp:packageExe -x test --no-daemon 2>/dev/null || \
echo "  Windows packaging not available, JAR will be used"

echo "=== Build Complete ==="

# List output files
echo ""
echo "Output files:"
find . -name "*.apk" -o -name "*.jar" -o -name "*.deb" -o -name "*.msi" -o -name "*.exe" -o -name "app-image" -type d 2>/dev/null | grep -v ".gradle" | head -20
