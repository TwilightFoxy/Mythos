package com.twily.mythos.client.screen;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.ChooseMythPayload;
import com.twily.mythos.network.MythSelectionEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class MythSelectionScreen extends Screen {

    private static final int PANEL_FILL = 0xE4171922;
    private static final int PANEL_BORDER = 0xFF4A4F63;
    private static final int PANEL_HEADER = 0xE1202431;
    private static final int BODY_TEXT = 0xFFF3F4F8;
    private static final int MUTED_TEXT = 0xFFB4B8C7;
    private static final int POSITIVE_TEXT = 0xFF9DE07E;
    private static final int NEGATIVE_TEXT = 0xFFFF9F8A;
    private static final int DOT_ACTIVE = 0xFF9146FF;
    private static final int DOT_INACTIVE = 0x803B4051;

    private List<MythSelectionEntry> myths;
    private Identifier currentMyth;
    private boolean canClose;
    private int selectedIndex;
    private Button previousButton;
    private Button nextButton;
    private Button chooseButton;
    private Button cancelButton;

    public MythSelectionScreen(List<MythSelectionEntry> myths, Identifier currentMyth, boolean canClose) {
        super(Component.translatable("gui.mythos.selection.title"));
        this.myths = List.copyOf(myths);
        this.currentMyth = currentMyth;
        this.canClose = canClose;
        this.selectedIndex = indexFor(currentMyth);
    }

    public void refresh(List<MythSelectionEntry> myths, Identifier currentMyth, boolean canClose) {
        Identifier selectedMythId = this.selectedMyth() != null ? this.selectedMyth().id() : currentMyth;
        this.myths = List.copyOf(myths);
        this.currentMyth = currentMyth;
        this.canClose = canClose;
        this.selectedIndex = indexFor(selectedMythId);

        if (this.selectedIndex < 0 || this.selectedIndex >= this.myths.size()) {
            this.selectedIndex = indexFor(currentMyth);
        }

        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, this.width - 40);
        int panelX = (this.width - panelWidth) / 2;
        int buttonY = Math.min(this.height - 42, (this.height + 246) / 2 + 16);

        this.previousButton = this.addRenderableWidget(
            Button.builder(Component.translatable("gui.mythos.previous"), button -> this.stepSelection(-1))
                .bounds(panelX, buttonY, 72, 20)
                .build()
        );
        this.nextButton = this.addRenderableWidget(
            Button.builder(Component.translatable("gui.mythos.next"), button -> this.stepSelection(1))
                .bounds(panelX + 80, buttonY, 72, 20)
                .build()
        );
        this.chooseButton = this.addRenderableWidget(
            Button.builder(Component.translatable("gui.mythos.choose"), button -> this.chooseCurrentMyth())
                .bounds(panelX + panelWidth - 86, buttonY, 86, 20)
                .build()
        );
        if (this.canClose) {
            this.cancelButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.mythos.close"), button -> this.onClose())
                    .bounds(panelX + panelWidth - 180, buttonY, 86, 20)
                    .build()
            );
        }

        this.updateButtons();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.canClose;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int panelWidth = Math.min(360, this.width - 40);
        int panelHeight = Math.min(246, this.height - 56);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_FILL);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 44, PANEL_HEADER);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
        graphics.horizontalLine(panelX + 1, panelX + panelWidth - 2, panelY + 44, PANEL_BORDER);

        if (this.myths.isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.translatable("gui.mythos.no_myths"), panelX + 16, panelY + 56, panelWidth - 32, MUTED_TEXT, false);
            return;
        }

        MythSelectionEntry selected = this.selectedMyth();
        Identifier mythLogo = mythLogoTexture(selected.id());
        if (hasTexture(mythLogo)) {
            graphics.blit(mythLogo, panelX + 12, panelY + 6, panelX + 44, panelY + 38, 0.0F, 1.0F, 0.0F, 1.0F);
        }

        graphics.text(this.font, MythState.displayName(selected.id()), panelX + 58, panelY + 16, BODY_TEXT, false);
        graphics.text(
            this.font,
            Component.translatable("gui.mythos.page", this.selectedIndex + 1, this.myths.size()),
            panelX + panelWidth - 12 - this.font.width(Component.translatable("gui.mythos.page", this.selectedIndex + 1, this.myths.size())),
            panelY + 8,
            MUTED_TEXT,
            false
        );

        int dotsX = panelX + panelWidth - 34;
        int dotsY = panelY + 24;
        int complexityLabelWidth = this.font.width(Component.translatable("gui.mythos.complexity"));
        graphics.text(this.font, Component.translatable("gui.mythos.complexity"), dotsX - 8 - complexityLabelWidth, panelY + 20, MUTED_TEXT, false);
        drawImpactDots(graphics, dotsX, dotsY, selected.complexity());

        int textX = panelX + 16;
        int contentWidth = panelWidth - 32;
        int y = panelY + 58;

        y = drawWrappedText(graphics, Component.translatable(selected.description()), textX, y, contentWidth, BODY_TEXT, 8);
        y += 6;
        graphics.text(this.font, Component.translatable("gui.mythos.advantages"), textX, y, POSITIVE_TEXT, false);
        y += 11;
        y = drawBullets(graphics, selected.advantages(), textX, y, contentWidth, POSITIVE_TEXT, "* ");
        y += 4;
        graphics.text(this.font, Component.translatable("gui.mythos.disadvantages"), textX, y, NEGATIVE_TEXT, false);
        y += 11;
        drawBullets(graphics, selected.disadvantages(), textX, y, contentWidth, NEGATIVE_TEXT, "* ");
    }

    private void chooseCurrentMyth() {
        MythSelectionEntry selected = this.selectedMyth();
        if (selected == null) {
            return;
        }

        ClientPacketDistributor.sendToServer(new ChooseMythPayload(selected.id()));
        this.onClose();
    }

    private void stepSelection(int delta) {
        if (this.myths.isEmpty()) {
            return;
        }

        this.selectedIndex = Math.max(0, Math.min(this.selectedIndex + delta, this.myths.size() - 1));
        this.updateButtons();
    }

    private void updateButtons() {
        boolean hasMyths = !this.myths.isEmpty();
        if (this.previousButton != null) {
            this.previousButton.active = hasMyths && this.selectedIndex > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = hasMyths && this.selectedIndex < this.myths.size() - 1;
        }
        if (this.chooseButton != null) {
            boolean sameMyth = hasMyths && this.selectedMyth().id().equals(this.currentMyth);
            this.chooseButton.active = hasMyths && !sameMyth;
            this.chooseButton.setMessage(Component.translatable(sameMyth ? "gui.mythos.current" : "gui.mythos.choose"));
        }
    }

    private MythSelectionEntry selectedMyth() {
        if (this.myths.isEmpty()) {
            return null;
        }

        return this.myths.get(this.selectedIndex);
    }

    private int indexFor(Identifier mythId) {
        for (int index = 0; index < this.myths.size(); index++) {
            if (this.myths.get(index).id().equals(mythId)) {
                return index;
            }
        }

        return 0;
    }

    private void drawImpactDots(GuiGraphicsExtractor graphics, int x, int y, int complexity) {
        int clamped = Math.max(1, Math.min(complexity, 3));
        for (int index = 0; index < 3; index++) {
            int dotX = x + index * 10;
            int color = index < clamped ? DOT_ACTIVE : DOT_INACTIVE;
            graphics.fill(dotX, y, dotX + 6, y + 6, color);
            graphics.outline(dotX, y, 6, 6, 0xFF252934);
        }
    }

    private Identifier mythLogoTexture(Identifier mythId) {
        Identifier direct = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "textures/gui/" + mythId.getPath() + "_logo.png");
        if (hasTexture(direct)) {
            return direct;
        }

        if ("dwarf".equals(mythId.getPath())) {
            Identifier legacyTypo = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "textures/gui/dworf_logo.png");
            if (hasTexture(legacyTypo)) {
                return legacyTypo;
            }
        }

        return Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "textures/gui/myths/" + mythId.getPath() + ".png");
    }

    private boolean hasTexture(Identifier texture) {
        return this.minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private int drawBullets(GuiGraphicsExtractor graphics, List<String> lines, int x, int y, int width, int color, String prefix) {
        for (String lineKey : lines) {
            y = drawWrappedText(graphics, Component.literal(prefix + Component.translatable(lineKey).getString()), x, y, width, color, 2);
        }
        return y;
    }

    private int drawWrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color, int bottomSpacing) {
        for (var line : this.font.split(text, width)) {
            graphics.text(this.font, line, x, y, color, false);
            y += 10;
        }
        return y + bottomSpacing;
    }
}
