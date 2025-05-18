#!/bin/bash

# Create new directory structure
echo "Creating new directory structure..."
mkdir -p robotplatform-client/src/main/java/com/robotplatform/client/model
mkdir -p robotplatform-service/src/main/java/com/robotplatform/service/external/cosyvoice
mkdir -p robotplatform-service/src/main/java/com/robotplatform/service/external/funasr
mkdir -p robotplatform-service/src/main/java/com/robotplatform/service/external/lm
mkdir -p robotplatform-service/src/main/java/com/robotplatform/service/impl
mkdir -p robotplatform-service/src/main/resources
mkdir -p robotplatform-service/src/test/java/com/robotplatform/service/external/cosyvoice
mkdir -p robotplatform-web/src/main/java/com/robotplatform/web/config
mkdir -p robotplatform-web/src/main/java/com/robotplatform/web/controller
mkdir -p robotplatform-web/src/main/java/com/robotplatform/web/websocket
mkdir -p robotplatform-web/src/main/resources/static
mkdir -p robotplatform-start/src/main/java/com/robotplatform/start

# Copy and modify files
echo "Copying and modifying files..."

# Function to copy and replace content in files
copy_and_replace() {
    SOURCE=$1
    DEST=$2
    
    if [ -f "$SOURCE" ]; then
        # Create destination directory if it doesn't exist
        mkdir -p "$(dirname "$DEST")"
        
        # Copy file with content replacement
        sed 's/com\.robotai/com.robotplatform/g; s/robotai/robotplatform/g; s/RobotAI/RobotPlatform/g' "$SOURCE" > "$DEST"
        echo "Processed: $SOURCE -> $DEST"
    else
        echo "Warning: Source file not found: $SOURCE"
    fi
}

# Copy and modify Java files
find robotai-client -name "*.java" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-service -name "*.java" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-web -name "*.java" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-start -name "*.java" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

# Copy and modify XML files
find robotai-client -name "*.xml" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-service -name "*.xml" -o -name "*.yml" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-web -name "*.xml" -o -name "*.yml" -o -name "*.html" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

find robotai-start -name "*.xml" -o -name "*.yml" | while read file; do
    new_file=$(echo $file | sed 's/robotai/robotplatform/g')
    copy_and_replace "$file" "$new_file"
done

# Copy README.md
sed 's/robotai/robotplatform/g; s/RobotAI/RobotPlatform/g' README.md > README.md.new
mv README.md.new README.md

echo "Renaming complete. Please verify the changes."
