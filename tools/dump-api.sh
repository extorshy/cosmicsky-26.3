#!/usr/bin/env bash
# Dumps the REAL 26.3 class signatures so the sky/cloud renderer can be ported against facts, not guesses.
OUT=api-dump; mkdir -p "$OUT"
JAR=""
for j in $(find "$HOME/.gradle" . -name "*.jar" -size +5M 2>/dev/null); do
  if unzip -l "$j" 2>/dev/null | grep -q "net/minecraft/client/Minecraft.class"; then JAR="$j"; break; fi
done
echo "minecraft jar: $JAR" | tee "$OUT/_info.txt"
[ -z "$JAR" ] && exit 0
unzip -l "$JAR" | awk '{print $4}' | grep -E "^net/minecraft/client/(renderer|gui)/.*\.class$" | grep -iE "sky|cloud|celestial|weather|environment|GuiGraphicsExtractor|Renderable|LevelRenderer|LevelExtractor" > "$OUT/class-list.txt"
mkdir -p /tmp/mc && cd /tmp/mc && unzip -q -o "$JAR" 'net/*' 'com/mojang/*' 2>/dev/null; cd - >/dev/null
while read -r f; do
  c=$(echo "${f%.class}" | tr / .)
  javap -p -cp /tmp/mc "$c" > "$OUT/$(echo "$c" | tr '$' '_').txt" 2>&1
done < "$OUT/class-list.txt"
for c in net.minecraft.client.KeyMapping 'net.minecraft.client.KeyMapping$Category' com.mojang.blaze3d.platform.InputConstants \
         net.minecraft.client.gui.components.Button net.minecraft.client.gui.components.AbstractSliderButton \
         net.minecraft.client.gui.screens.Screen net.minecraft.client.Minecraft net.minecraft.client.gui.Gui; do
  javap -p -cp /tmp/mc "$c" > "$OUT/$(echo "$c" | tr '$' '_').txt" 2>&1
done
find "$HOME/.gradle" -name "fabric-key*.jar" -o -name "fabric-rendering*.jar" 2>/dev/null | while read -r j; do
  echo "== $j"; unzip -l "$j" | awk '{print $4}' | grep "\.class$"
done > "$OUT/fabric-api-classes.txt"
