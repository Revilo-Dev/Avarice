package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.client.KnowledgeLibraryClientState;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A compact paged catalogue of the discoveries a player still needs to find. */
public final class KnowledgeLibraryScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/knowledge-library/book-panel.png");
    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/icon/unknown-knowledge.png");
    private static final int PANEL_WIDTH = 147;
    private static final int PANEL_HEIGHT = 166;
    private static final int ENTRIES_PER_PAGE = 10;
    private int page;

    public KnowledgeLibraryScreen() { super(Component.literal("Personal Library")); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.blit(PANEL, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(this.font, "Personal Library", left + PANEL_WIDTH / 2, top + 10, 0xFFEBD8A8);

        List<KnowledgeManager.KnowledgeEntry> entries = KnowledgeManager.entries();
        int start = page * ENTRIES_PER_PAGE;
        for (int index = 0; index < ENTRIES_PER_PAGE && start + index < entries.size(); index++) {
            KnowledgeManager.KnowledgeEntry entry = entries.get(start + index);
            int column = index % 2;
            int row = index / 2;
            int x = left + 10 + column * 68;
            int y = top + 29 + row * 23;
            boolean unlocked = KnowledgeLibraryClientState.isUnlocked(entry.id());
            graphics.blit(UNKNOWN, x, y, 0, 0, 16, 16, 16, 16);
            String label = this.font.plainSubstrByWidth(entry.title(), 48);
            graphics.drawString(this.font, label, x + 18, y + 1, unlocked ? entry.rarity().color().getColor() : 0xFF8A8172, false);
            graphics.drawString(this.font, unlocked ? "Learned" : entry.rarity().name().toLowerCase(), x + 18, y + 9,
                    unlocked ? 0xFF8BD47D : 0xFF625B53, false);
            if (mouseX >= x && mouseX < x + 64 && mouseY >= y && mouseY < y + 18) {
                graphics.renderTooltip(this.font, List.of(
                        Component.literal(entry.title()).withStyle(unlocked ? entry.rarity().color() : ChatFormatting.GRAY),
                        Component.literal(unlocked ? entry.description() : "Knowledge not yet discovered.").withStyle(ChatFormatting.DARK_GRAY)
                ), mouseX, mouseY);
            }
        }
        int pages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        graphics.drawCenteredString(this.font, (page + 1) + " / " + pages, left + PANEL_WIDTH / 2, top + 145, 0xFFB7A383);
        graphics.drawString(this.font, "<", left + 12, top + 144, page > 0 ? 0xFFEBD8A8 : 0xFF665C4E, false);
        graphics.drawString(this.font, ">", left + PANEL_WIDTH - 18, top + 144, page + 1 < pages ? 0xFFEBD8A8 : 0xFF665C4E, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int pages = Math.max(1, (KnowledgeManager.entries().size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (button == 0 && mouseY >= top + 138 && mouseY <= top + 162) {
            if (mouseX >= left + 4 && mouseX <= left + 30 && page > 0) page--;
            if (mouseX >= left + PANEL_WIDTH - 30 && mouseX <= left + PANEL_WIDTH - 4 && page + 1 < pages) page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
