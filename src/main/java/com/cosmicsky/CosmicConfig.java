package com.cosmicsky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** All settings. Colors are plain 0xRRGGBB ints. Saved to config/cosmicsky.json */
public class CosmicConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean starsEnabled = true;
    public int starsColor = 0xFFFFFF;

    public boolean shootingEnabled = true;
    public int shootingColor = 0xBFE6FF;
    public int shootingPerMinute = 6;

    public boolean planetsEnabled = true;
    public int saturnColor = 0xF2D9A5;
    public int jupiterColor = 0xE0B48A;
    public int marsColor = 0xE06A4A;

    public boolean sunEnabled = true;
    public int sunColor = 0xFFFFFF;
    public boolean moonEnabled = true;
    public int moonColor = 0xFFFFFF;

    public boolean cloudsEnabled = true;
    public int cloudColor = 0xFFFFFF;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("cosmicsky.json");
    }

    public static CosmicConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    CosmicConfig c = GSON.fromJson(r, CosmicConfig.class);
                    if (c != null) return c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CosmicConfig c = new CosmicConfig();
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
