#!/bin/bash

REPO_DIR="/home/bot/Build/VotingMachine"
RUN_DIR="/home/bot/Run"

cd "$REPO_DIR"

BEFORE=$(git rev-parse HEAD)

if git pull origin master; then
    AFTER=$(git rev-parse HEAD)

    if [ "$BEFORE" != "$AFTER" ]; then
        echo "Changes detected, rebuilding..."
        mvn package -q
        cp "$REPO_DIR/target/pollster.jar" "$RUN_DIR/pollster.jar"
        echo "Rebuild complete."
    else
        echo "No changes detected, skipping rebuild."
    fi
else
    echo "Warning: git pull failed, starting with existing build."
fi
