package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.client.KnowledgeLibraryClientState;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager.KnowledgeCategory;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class KnowledgeLibraryScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/knowledge-library/book-panel.png");
    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/icon/unknown-knowledge.png");
    private static final int PANEL_WIDTH = 147;
    private static final int PANEL_HEIGHT = 166;
    private static final int ENTRIES_PER_PAGE = 10;
    private KnowledgeCategory category = KnowledgeCategory.ENCHANTS;
    private int page;

    public KnowledgeLibraryScreen() {
        super(Component.literal("Knowledge Binder"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.blit(PANEL, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        renderTabs(graphics, left, top, mouseX, mouseY);
        graphics.drawCenteredString(this.font, categoryTitle(), left + PANEL_WIDTH / 2, top + 10, 0xFFEBD8A8);

        List<KnowledgeManager.KnowledgeEntry> entries = filteredEntries();
        int start = page * ENTRIES_PER_PAGE;
        for (int index = 0; index < ENTRIES_PER_PAGE && start + index < entries.size(); index++) {
            KnowledgeManager.KnowledgeEntry entry = entries.get(start + index);
            int x = left + 8 + (index % 2) * 68;
            int y = top + 29 + (index / 2) * 23;
            boolean unlocked = KnowledgeLibraryClientState.isUnlocked(entry.id());
            graphics.blit(UNKNOWN, x, y, 0, 0, 16, 16, 16, 16);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(unlocked ? entry.title() : "Unknown", 48), x + 18, y + 1,
                    unlocked ? entry.rarity().color().getColor() : 0xFF8A8172, false);
            graphics.drawString(this.font, unlocked ? "Collected" : "Undiscovered", x + 18, y + 10,
                    unlocked ? 0xFF8BD47D : 0xFF625B53, false);
            if (mouseX >= x && mouseX < x + 64 && mouseY >= y && mouseY < y + 18) {
                int day = KnowledgeLibraryClientState.collectedDay(entry.id());
                graphics.renderTooltip(this.font, List.of(
                        Component.literal(unlocked ? entry.title() : "Unknown Knowledge").withStyle(unlocked ? entry.rarity().color() : ChatFormatting.GRAY),
                        Component.literal(unlocked ? entry.description() : "Knowledge not yet discovered.").withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal(unlocked ? "Collected on day " + Math.max(1, day) : "Not collected").withStyle(ChatFormatting.GRAY)
                ), Optional.empty(), mouseX, mouseY);
            }
        }

        int pages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        graphics.drawCenteredString(this.font, (page + 1) + " / " + pages, left + PANEL_WIDTH / 2, top + 145, 0xFFB7A383);
        graphics.drawString(this.font, "<", left + 12, top + 144, page > 0 ? 0xFFEBD8A8 : 0xFF665C4E, false);
        graphics.drawString(this.font, ">", left + PANEL_WIDTH - 18, top + 144, page + 1 < pages ? 0xFFEBD8A8 : 0xFF665C4E, false);
    }

    private void renderTabs(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        KnowledgeCategory[] tabs = KnowledgeCategory.values();
        for (int index = 0; index < tabs.length; index++) {
            int x = left + index * 49;
            int y = top - 15;
            boolean selected = category == tabs[index];
            boolean hovered = mouseX >= x && mouseX < x + 48 && mouseY >= y && mouseY < y + 16;
            graphics.fill(x, y, x + 48, y + 16, selected ? 0xFF8A6845 : hovered ? 0xFF66513C : 0xFF3D332A);
            graphics.drawCenteredString(this.font, tabName(tabs[index]), x + 24, y + 4, selected ? 0xFFFFE6AA : 0xFFC0AD8B);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        if (button == 0 && mouseY >= top - 15 && mouseY < top + 1) {
            int index = (int) ((mouseX - left) / 49);
            if (mouseX >= left && index >= 0 && index < KnowledgeCategory.values().length) {
                category = KnowledgeCategory.values()[index];
                page = 0;
                return true;
            }
        }
        List<KnowledgeManager.KnowledgeEntry> entries = filteredEntries();
        int pages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (button == 0 && mouseY >= top + 138 && mouseY <= top + 162) {
            if (mouseX >= left + 4 && mouseX <= left + 30 && page > 0) page--;
            if (mouseX >= left + PANEL_WIDTH - 30 && mouseX <= left + PANEL_WIDTH - 4 && page + 1 < pages) page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<KnowledgeManager.KnowledgeEntry> filteredEntries() {
        return KnowledgeManager.entries().stream().filter(entry -> entry.category() == category).toList();
    }

    private String categoryTitle() {
        return switch (category) {
            case ENCHANTS -> "Enchant Knowledge";
            case TECHNOLOGY -> "Technology Knowledge";
            case LORE -> "Lore Knowledge";
        };
    }

    private static String tabName(KnowledgeCategory category) {
        return switch (category) {
            case ENCHANTS -> "Enchants";
            case TECHNOLOGY -> "Tech";
            case LORE -> "Lore";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
