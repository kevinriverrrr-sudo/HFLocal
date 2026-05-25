#!/bin/bash
set -e

TOKEN="ghp_BQD1AE8EHeZFJpvklnqj1jSqmcy2lW4J4J4Dlx"
REPO="kevinriverrrr-sudo/HFLocal"

cd /home/z/my-project/HFLocal

# Check if git is initialized
if [ ! -d ".git" ]; then
    echo "Initializing git..."
    git init
    git remote add origin "https://${TOKEN}@github.com/$REPO.git"
else
    git remote set-url origin "https://${TOKEN}@github.com/$REPO.git"
fi

# Configure git
git config user.email "dev@hflocal.app"
git config user.name "HF Local Dev"

# Add and commit
echo "Committing changes..."
git add -A
git commit -m "v1.0.0: Complete KMP implementation with Android, Linux, Windows support

- Full HuggingFace Hub integration
- GGUF model catalog with smart device filtering
- Local AI chat interface with streaming
- SQLDelight database persistence
- Koin dependency injection
- Compose Multiplatform UI
- Dark theme
- Device hardware detection and tier profiling
- Download management with progress tracking
- Settings with proxy support"

# Push
echo "Pushing to GitHub..."
git push -u origin main --force

echo "Done! Repository: https://github.com/$REPO"
