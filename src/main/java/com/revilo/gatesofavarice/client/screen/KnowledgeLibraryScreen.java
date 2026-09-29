package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.client.KnowledgeLibraryClientState;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import com.revilo.gatesofavarice.network.RedeemKnowledgeBookPayload;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// a paged catalogue of knowledge books obtained and undiscovered
public final class KnowledgeLibraryScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/knowledge-library/book-panel.png");
    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/icon/unknown-knowledge.png");
    private static final int PANEL_WIDTH = 147;
    private static final int PANEL_HEIGHT = 166;
    private static final int ENTRIES_PER_PAGE = 10;
    private static final int PANEL_GAP = 8;
    private int page;

    public KnowledgeLibraryScreen() { super(Component.literal("Personal Library")); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        int left = (this.width - (PANEL_WIDTH * 2 + PANEL_GAP)) / 2;
        int learnLeft = left + PANEL_WIDTH + PANEL_GAP;
        int top = (this.height - PANEL_HEIGHT) / 2;
        graphics.blit(PANEL, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.blit(PANEL, learnLeft, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
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
                ), Optional.empty(), mouseX, mouseY);
            }
        }
        int pages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        graphics.drawCenteredString(this.font, (page + 1) + " / " + pages, left + PANEL_WIDTH / 2, top + 145, 0xFFB7A383);
        graphics.drawString(this.font, "<", left + 12, top + 144, page > 0 ? 0xFFEBD8A8 : 0xFF665C4E, false);
        graphics.drawString(this.font, ">", left + PANEL_WIDTH - 18, top + 144, page + 1 < pages ? 0xFFEBD8A8 : 0xFF665C4E, false);

        graphics.drawCenteredString(this.font, "Learn Knowledge", learnLeft + PANEL_WIDTH / 2, top + 10, 0xFFEBD8A8);
        graphics.drawCenteredString(this.font, "Select a book", learnLeft + PANEL_WIDTH / 2, top + 21, 0xFFB7A383);
        List<Integer> bookSlots = this.knowledgeBookSlots();
        if (bookSlots.isEmpty()) {
            graphics.drawCenteredString(this.font, "No knowledge books", learnLeft + PANEL_WIDTH / 2, top + 66, 0xFF8A8172);
        }
        for (int index = 0; index < bookSlots.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            int x = learnLeft + 10 + column * 68;
            int y = top + 34 + row * 24;
            ItemStack book = Minecraft.getInstance().player.getInventory().getItem(bookSlots.get(index));
            graphics.fill(x - 2, y - 2, x + 64, y + 19, 0x66413031);
            graphics.renderItem(book, x, y);
            String title = KnowledgeManager.getBookEntry(book).map(KnowledgeManager.KnowledgeEntry::title).orElse("Unknown book");
            graphics.drawString(this.font, this.font.plainSubstrByWidth(title, 44), x + 18, y + 4, 0xFFDFC6FF, false);
            if (mouseX >= x - 2 && mouseX < x + 64 && mouseY >= y - 2 && mouseY < y + 19) {
                graphics.renderTooltip(this.font, Component.literal("Learn " + title).withStyle(ChatFormatting.LIGHT_PURPLE), mouseX, mouseY);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - (PANEL_WIDTH * 2 + PANEL_GAP)) / 2;
        int learnLeft = left + PANEL_WIDTH + PANEL_GAP;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int pages = Math.max(1, (KnowledgeManager.entries().size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (button == 0 && mouseY >= top + 138 && mouseY <= top + 162) {
            if (mouseX >= left + 4 && mouseX <= left + 30 && page > 0) page--;
            if (mouseX >= left + PANEL_WIDTH - 30 && mouseX <= left + PANEL_WIDTH - 4 && page + 1 < pages) page++;
            return true;
        }
        if (button == 0) {
            List<Integer> bookSlots = this.knowledgeBookSlots();
            for (int index = 0; index < bookSlots.size(); index++) {
                int x = learnLeft + 10 + (index % 2) * 68;
                int y = top + 34 + (index / 2) * 24;
                if (mouseX >= x - 2 && mouseX < x + 64 && mouseY >= y - 2 && mouseY < y + 19) {
                    PacketDistributor.sendToServer(new RedeemKnowledgeBookPayload(bookSlots.get(index)));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<Integer> knowledgeBookSlots() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return List.of();
        java.util.ArrayList<Integer> slots = new java.util.ArrayList<>();
        for (int slot = 0; slot < minecraft.player.getInventory().items.size() && slots.size() < 10; slot++) {
            if (KnowledgeManager.getBookEntry(minecraft.player.getInventory().getItem(slot)).isPresent()) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    @Override public boolean isPauseScreen() { return false; }
}
