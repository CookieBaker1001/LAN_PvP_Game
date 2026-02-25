package com.springer.knakobrak.lwjgl3;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class texturePackerRunner {
    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.combineSubdirectories = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.edgePadding = true;
        settings.duplicatePadding = true;

        TexturePacker.process(
            settings,
            "assets/characters",          // input directory
            "assets/skins",                    // output directory
            "character_skins"            // atlas name (no extension)
        );
    }
}
