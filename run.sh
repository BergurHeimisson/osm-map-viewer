#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home
exec "$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar "$DIR/target/osm-map-1.0-SNAPSHOT.jar"
