
package com.cosmicsky;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class CosmicSkyScreen extends Screen {
    private static final String[] TABS = {
        "Stars", "Shooting", "Planets", "Sun & Moon", "Clouds"
    };

    private static int selectedTab = 0;

    private final Screen parent;
    private int y;

    public CosmicSkyScreen(Screen parent) {
        super(Component.literal("Cosmic Sky"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CosmicConfig c = CosmicSky.CONFIG;

        int tabW = 80;
        int gap = 3;
        int x0 = (width - (TABS.length * (tabW + gap) - gap)) / 2;

        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;

            Button b = Button.builder(
                Component.literal(TABS[i]),
                btn -> {
                    selectedTab = idx;
                    rebuildWidgets();
                }
            ).bounds(x0 + i * (tabW + gap), 26, tabW, 20).build();

            b.active = i != selectedTab;
            addRenderableWidget(b);
        }

        y = 62;

        switch (selectedTab) {
            case 0 -> {
                toggle("Stars", () -> c.starsEnabled,
                    v -> c.starsEnabled = v);
                color("Star color", () -> c.starsColor,
                    v -> c.starsColor = v);
            }

            case 1 -> {
                toggle("Shooting stars", () -> c.shootingEnabled,
                    v -> c.shootingEnabled = v);
                color("Color", () -> c.shootingColor,
                    v -> c.shootingColor = v);
                valueSlider("Per minute", 1, 30,
                    () -> c.shootingPerMinute,
                    v -> c.shootingPerMinute = v);
            }

            case 2 -> {
                toggle("Planets", () -> c.planetsEnabled,
                    v -> c.planetsEnabled = v);
                color("Saturn", () -> c.saturnColor,
                    v -> c.saturnColor = v);
                color("Jupiter", () -> c.jupiterColor,
                    v -> c.jupiterColor = v);
                color("Mars", () -> c.marsColor,
                    v -> c.marsColor = v);
            }

            case 3 -> {
                toggle("Sun", () -> c.sunEnabled,
                    v -> c.sunEnabled = v);
                color("Sun color", () -> c.sunColor,
                    v -> c.sunColor = v);
                toggle("Moon", () -> c.moonEnabled,
                    v -> c.moonEnabled = v);
                color("Moon color", () -> c.moonColor,
                    v -> c.moonColor = v);
            }

            case 4 -> {
                toggle("Clouds", () -> c.cloudsEnabled,
                    v -> c.cloudsEnabled = v);
                color("Cloud color", () -> c.cloudColor,
                    v -> c.cloudColor = v);
            }
        }

        addRenderableWidget(
            Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build()
        );
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private void toggle(
        String label,
        BooleanSupplier get,
        Consumer<Boolean> set
    ) {
        addRenderableWidget(
            Button.builder(toggleText(label, get.getAsBoolean()), b -> {
                set.accept(!get.getAsBoolean());
                b.setMessage(toggleText(label, get.getAsBoolean()));
            }).bounds(width / 2 - 100, y, 200, 20).build()
        );

        y += 26;
    }

    private void valueSlider(
        String label,
        int min,
        int max,
        IntSupplier get,
        IntConsumer set
    ) {
        addRenderableWidget(
            new ValueSlider(width / 2 - 100, y, 200, 20,
                label, min, max, get, set)
        );

        y += 26;
    }

    private void color(
        String label,
        IntSupplier get,
        IntConsumer set
    ) {
        final int rowY = y;
        final int x = width / 2 - 135;

        addRenderableOnly((g, mx, my, d) ->
            g.text(font, label, x, rowY + 6, 0xFFFFFFFF)
        );

        addRenderableWidget(
            new ChannelSlider(x + 75, rowY, 55, 20,
                "R", 16, get, set)
        );

        addRenderableWidget(
            new ChannelSlider(x + 132, rowY, 55, 20,
                "G", 8, get, set)
        );

        addRenderableWidget(
            new ChannelSlider(x + 189, rowY, 55, 20,
                "B", 0, get, set)
        );

        addRenderableOnly((g, mx, my, d) ->
            g.fill(x + 248, rowY, x + 268, rowY + 20,
                0xFF000000 | get.getAsInt())
        );

        y += 26;
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor g,
        int mouseX,
        int mouseY,
        float delta
    ) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        CosmicSky.CONFIG.save();

        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    private static class ChannelSlider extends AbstractSliderButton {
        private final String name;
        private final int shift;
        private final IntSupplier get;
        private final IntConsumer set;

        ChannelSlider(
            int x, int y, int w, int h,
            String name, int shift,
            IntSupplier get, IntConsumer set
        ) {
            super(x, y, w, h, Component.empty(),
                ((get.getAsInt() >> shift) & 255) / 255.0);

            this.name = name;
            this.shift = shift;
            this.get = get;
            this.set = set;

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(
                name + " " + (int) Math.round(value * 255)
            ));
        }

        @Override
        protected void applyValue() {
            int ch = (int) Math.round(value * 255);

            set.accept(
                (get.getAsInt() & ~(255 << shift)) | (ch << shift)
            );
        }
    }

    private static class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer set;

        ValueSlider(
            int x, int y, int w, int h,
            String label, int min, int max,
            IntSupplier get, IntConsumer set
        ) {
            super(x, y, w, h, Component.empty(),
                (get.getAsInt() - min) / (double) (max - min));

            this.label = label;
            this.min = min;
            this.max = max;
            this.set = set;

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(
                label + ": " +
                (int) Math.round(min + value * (max - min))
            ));
        }

        @Override
        protected void applyValue() {
            set.accept(
                (int) Math.round(min + value * (max - min))
            );
        }
    }
}
