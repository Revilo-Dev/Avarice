package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.DungeonDeck;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardState;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class DungeonDeckRenderer {

    private static final ResourceLocation NEGATIVE_CARD_BACKGROUND = texture("gui/dungeon/damage-card.png");
    private static final ResourceLocation POSITIVE_CARD_BACKGROUND = texture("gui/dungeon/stat-card.png");
    private static final ResourceLocation ENEMY_HEALTH_ICON = texture("gui/icon/cards/more-health.png");
    private static final ResourceLocation ENEMY_DAMAGE_ICON = texture("gui/icon/cards/more-damage.png");
    private static final ResourceLocation ENEMY_QUANTITY_ICON = texture("gui/icon/cards/more-mobs.png");
    private static final ResourceLocation ENEMY_SPEED_ICON = texture("gui/icon/cards/more-speed.png");
    private static final ResourceLocation ENEMY_LEECH_ICON = texture("gui/icon/cards/more-leech.png");
    private static final ResourceLocation COINS_ICON = texture("item/gold_coin.png");
    private static final ResourceLocation LOOT_QUANTITY_ICON = texture("gui/dungeon/icons/multi_roll.png");
    private static final ResourceLocation LOOT_QUALITY_ICON = texture("gui/dungeon/icons/fortune.png");
    private static final ResourceLocation XP_ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png");
    private static final CardType[] CARD_TYPES = CardType.values();
    private static final int CARD_WIDTH = 76;
    private static final int CARD_HEIGHT = 103;
    private static final int FULL_CARD_GAP = 4;
    private static final int COMPACT_CARD_GAP = 2;
    private static final float COMPACT_SCALE = 0.225F;
    private static final float MAX_FULL_SCALE = 0.86F;

    private DungeonDeckRenderer() {
    }

    public static void renderCompact(GuiGraphics guiGraphics, Font font, List<CardState> deck, int right, int bottom) {
        List<CardState> normalized = DungeonDeck.normalize(deck);
        List<Integer> visible = visibleIndexes(normalized, DungeonDeck.NEGATIVE_CARD_COUNT);
        if (visible.isEmpty()) return;
        int cardWidth = Math.round(CARD_WIDTH * COMPACT_SCALE);
        int cardHeight = Math.round(CARD_HEIGHT * COMPACT_SCALE);
        int totalWidth = visible.size() * cardWidth + (visible.size() - 1) * COMPACT_CARD_GAP;
        int startX = right - totalWidth;
        int cardY = bottom - cardHeight;

        guiGraphics.drawString(font, "DECK", startX + (totalWidth - font.width("DECK")) / 2, cardY - 10, 0xFFE36B, true);
        for (int position = 0; position < visible.size(); position++) {
            int index = visible.get(position);
            int cardX = startX + position * (cardWidth + COMPACT_CARD_GAP);
            renderCardArt(guiGraphics, CARD_TYPES[index], cardX, cardY, COMPACT_SCALE);
            String count = "x" + normalized.get(index).count();
            guiGraphics.drawString(
                    font,
                    count,
                    cardX + cardWidth - font.width(count) - 2,
                    cardY + cardHeight - 10,
                    0xFFFFFFFF,
                    true);
        }
    }

    public static void renderFullDeck(GuiGraphics guiGraphics, Font font, List<CardState> deck, int centerX, int bottom, int maxWidth) {
        renderFullDeck(guiGraphics, font, deck, centerX, bottom, maxWidth, false);
    }

    public static void renderFullDeck(GuiGraphics guiGraphics, Font font, List<CardState> deck, int centerX, int bottom,
            int maxWidth, boolean largeCounts) {
        List<CardState> normalized = DungeonDeck.normalize(deck);
        List<Integer> visible = visibleIndexes(normalized, CARD_TYPES.length);
        if (visible.isEmpty()) return;
        float scale = fullDeckScale(maxWidth, visible.size());
        int cardWidth = Math.round(CARD_WIDTH * scale);
        int cardHeight = Math.round(CARD_HEIGHT * scale);
        int totalWidth = visible.size() * cardWidth + (visible.size() - 1) * FULL_CARD_GAP;
        int startX = centerX - totalWidth / 2;
        int cardY = bottom - cardHeight;

        guiGraphics.drawCenteredString(font, "DECK", centerX, cardY - 11, 0xFFE36B);
        for (int position = 0; position < visible.size(); position++) {
            int index = visible.get(position);
            int cardX = startX + position * (cardWidth + FULL_CARD_GAP);
            renderCard(guiGraphics, font, CARD_TYPES[index], normalized.get(index), cardX, cardY, scale, largeCounts);
        }
    }

    public static int fullDeckHeight(int maxWidth) {
        return Math.round(CARD_HEIGHT * fullDeckScale(maxWidth)) + 11;
    }

    public static int cardWidth() {
        return CARD_WIDTH;
    }

    public static int cardHeight() {
        return CARD_HEIGHT;
    }

    public static void renderCard(GuiGraphics guiGraphics, Font font, CardType type, CardState state, int x, int y, float scale) {
        renderCard(guiGraphics, font, type, state, x, y, scale, false);
    }

    private static void renderCard(GuiGraphics guiGraphics, Font font, CardType type, CardState state, int x, int y,
            float scale, boolean largeCount) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        ResourceLocation background = type.negative() ? NEGATIVE_CARD_BACKGROUND : POSITIVE_CARD_BACKGROUND;
        guiGraphics.blit(background, 0, 0, 0, 0, CARD_WIDTH, CARD_HEIGHT, CARD_WIDTH, CARD_HEIGHT);

        float titleScale = Math.min(0.64F, 68.0F / Math.max(1, font.width(type.title())));
        drawScaledCentered(guiGraphics, font, type.title(), CARD_WIDTH / 2.0F, 7, titleScale, 0xFFF3D78A);

        Icon icon = icon(type);
        int iconY = icon.height() == 32 ? 25 : 33;
        guiGraphics.blit(icon.texture(), (CARD_WIDTH - icon.width()) / 2, iconY, 0, 0,
                icon.width(), icon.height(), icon.width(), icon.height());

        int lineY = 62;
        for (String line : wrap(type.description(), 20)) {
            drawScaledCentered(guiGraphics, font, line, CARD_WIDTH / 2.0F, lineY, 0.43F, 0xFFE7E7E7);
            lineY += 6;
            if (lineY > 80) {
                break;
            }
        }

        drawScaledCentered(guiGraphics, font, "x" + state.count(), CARD_WIDTH / 2.0F, 83,
                largeCount ? 0.68F : 0.48F, 0xFFD1D1D1);
        int effectColor = type.negative() ? 0xFFFFB0B0 : 0xFFB8F5BE;
        drawScaledCentered(guiGraphics, font, "+" + formatPercent(state.appliedPercent()) + "% applied",
                CARD_WIDTH / 2.0F, 91, 0.50F, effectColor);
        guiGraphics.pose().popPose();
    }

    private static void renderCardArt(GuiGraphics guiGraphics, CardType type, int x, int y, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        ResourceLocation background = type.negative() ? NEGATIVE_CARD_BACKGROUND : POSITIVE_CARD_BACKGROUND;
        guiGraphics.blit(background, 0, 0, 0, 0, CARD_WIDTH, CARD_HEIGHT, CARD_WIDTH, CARD_HEIGHT);
        Icon icon = icon(type);
        guiGraphics.blit(icon.texture(), (CARD_WIDTH - icon.width()) / 2, (CARD_HEIGHT - icon.height()) / 2, 0, 0,
                icon.width(), icon.height(), icon.width(), icon.height());
        guiGraphics.pose().popPose();
    }

    private static float fullDeckScale(int maxWidth) {
        return fullDeckScale(maxWidth, CARD_TYPES.length);
    }

    private static float fullDeckScale(int maxWidth, int count) {
        int safeCount = Math.max(1, count);
        int gaps = (safeCount - 1) * FULL_CARD_GAP;
        float availableForCards = Math.max(safeCount, maxWidth - gaps);
        return Math.min(MAX_FULL_SCALE, availableForCards / (safeCount * CARD_WIDTH));
    }

    private static List<Integer> visibleIndexes(List<CardState> deck, int limit) {
        ArrayList<Integer> visible = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, deck.size()); index++) {
            if (deck.get(index).count() > 0) visible.add(index);
        }
        return visible;
    }

    private static void drawScaledCentered(GuiGraphics guiGraphics, Font font, String text, float centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX - font.width(text) * scale / 2.0F, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private static List<String> wrap(String text, int maxCharacters) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + word.length() + 1 <= maxCharacters) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static String formatPercent(double percent) {
        if (Math.abs(percent - Math.rint(percent)) < 0.05D) {
            return Long.toString(Math.round(percent));
        }
        return String.format(Locale.ROOT, "%.1f", percent);
    }

    private static Icon icon(CardType type) {
        return switch (type) {
            case ENEMY_HEALTH -> new Icon(ENEMY_HEALTH_ICON, 16, 32);
            case ENEMY_DAMAGE -> new Icon(ENEMY_DAMAGE_ICON, 16, 32);
            case ENEMY_QUANTITY -> new Icon(ENEMY_QUANTITY_ICON, 16, 32);
            case ENEMY_SPEED -> new Icon(ENEMY_SPEED_ICON, 16, 32);
            case ENEMY_LEECH -> new Icon(ENEMY_LEECH_ICON, 16, 32);
            case COINS -> new Icon(COINS_ICON, 16, 16);
            case LOOT_QUANTITY -> new Icon(LOOT_QUANTITY_ICON, 16, 16);
            case LOOT_QUALITY -> new Icon(LOOT_QUALITY_ICON, 16, 16);
            case XP -> new Icon(XP_ICON, 16, 16);
        };
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/" + path);
    }

    private record Icon(ResourceLocation texture, int width, int height) {
    }
}
