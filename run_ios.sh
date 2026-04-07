#!/bin/zsh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/bruno/Documents/Athenna/KomaAi/Leiria_Eats

echo "=== Localizando .app ==="
APP_PATH=$(find "$HOME/Library/Developer/Xcode/DerivedData" -name "LeiriaEats.app" -path "*Debug-iphonesimulator*" 2>/dev/null | grep -v ".dSYM" | head -1)

if [ -z "$APP_PATH" ]; then
  echo "App nao encontrado. Tentando nome generico..."
  APP_PATH=$(find "$HOME/Library/Developer/Xcode/DerivedData/iosApp-bnbszjchygqqrphdmducnxichvne/Build/Products/Debug-iphonesimulator" -name "*.app" 2>/dev/null | grep -v ".dSYM" | head -1)
fi

echo "App: $APP_PATH"

echo "=== Abrindo Simulator ==="
open -a Simulator

sleep 2

echo "=== Instalando no simulador ==="
xcrun simctl install "0D83FD5F-3492-4F5C-8580-9D3857F5FF5E" "$APP_PATH"

echo "=== Lançando app ==="
xcrun simctl launch "0D83FD5F-3492-4F5C-8580-9D3857F5FF5E" org.leria.eats.project.LeiriaEats

echo "=== Concluido ==="

