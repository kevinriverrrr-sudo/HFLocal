#!/bin/bash
set -e

TAG="v1.0.0"
TOKEN="ghp_BQD1AE8EHeZFJpvklnqj1jSqmcy2lW4J4J4Dlx"
REPO="kevinriverrrr-sudo/HFLocal"
DOWNLOAD_DIR="/home/z/my-project/HFLocal/download"

echo "=== Creating GitHub Release $TAG ==="

# Check if repo exists, create if not
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: token $TOKEN" https://api.github.com/repos/$REPO)
if [ "$HTTP_STATUS" = "404" ]; then
    echo "Creating repository..."
    curl -s -X POST -H "Authorization: token $TOKEN" https://api.github.com/user/repos \
        -d "{\"name\":\"HFLocal\",\"description\":\"HF Local - Run AI models locally (KMP)\",\"private\":false,\"auto_init\":false}"
fi

# Create release
echo "Creating release..."
curl -s -X POST \
    -H "Authorization: token $TOKEN" \
    -H "Content-Type: application/json" \
    "https://api.github.com/repos/$REPO/releases" \
    -d "{
        \"tag_name\": \"$TAG\",
        \"name\": \"HF Local $TAG\",
        \"body\": \"## HF Local $TAG\\n\\n### Features:\\n- Browse HuggingFace model catalog\\n- Download GGUF models\\n- Local AI chat interface\\n- Device hardware detection\\n- Multi-platform: Android + Linux + Windows\\n\\n### Installation:\\n- **Android**: Download APK and install\\n- **Linux**: Download AppImage or .deb package\\n- **Windows**: Download .zip with JAR (requires Java 17+)\\n\\n### Supported Platforms:\\n- Android 9.0+ (API 28+)\\n- Linux x86_64\\n- Windows x86_64 (Java 17+)\",
        \"draft\": false,
        \"prerelease\": false
    }" > /tmp/release_response.json

RELEASE_ID=$(python3 -c "import json; print(json.load(open('/tmp/release_response.json')).get('id',''))" 2>/dev/null)
echo "Release ID: $RELEASE_ID"

# Upload assets
upload_asset() {
    local file=$1
    local name=$(basename "$file")
    if [ -f "$file" ]; then
        echo "Uploading: $name ($(du -h "$file" | cut -f1))"
        curl -s -X POST \
            -H "Authorization: token $TOKEN" \
            -H "Content-Type: application/octet-stream" \
            "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$name" \
            --data-binary "@$file"
        echo "  Done: $name"
    else
        echo "  Skip: $file not found"
    fi
}

mkdir -p "$DOWNLOAD_DIR"

# Find and upload APK
APK=$(find /home/z/my-project/HFLocal/androidApp/build/outputs/apk/release -name "*.apk" 2>/dev/null | head -1)
if [ -n "$APK" ]; then
    cp "$APK" "$DOWNLOAD_DIR/"
    upload_asset "$APK"
fi

# Find and upload JAR
JAR=$(find /home/z/my-project/HFLocal/desktopApp/build/libs -name "*.jar" 2>/dev/null | head -1)
if [ -n "$JAR" ]; then
    cp "$JAR" "$DOWNLOAD_DIR/"
    upload_asset "$JAR"
fi

# Find and upload Linux packages
for ext in "deb" "AppImage"; do
    FILE=$(find /home/z/my-project/HFLocal/desktopApp/build -name "*.$ext" 2>/dev/null | head -1)
    if [ -n "$FILE" ]; then
        cp "$FILE" "$DOWNLOAD_DIR/"
        upload_asset "$FILE"
    fi
done

echo ""
echo "=== Release Complete ==="
echo "URL: https://github.com/$REPO/releases/tag/$TAG"
