package com.twily.mythos.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.ChooseMythPayload;
import com.twily.mythos.network.MythSelectionEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
    private int contentScroll;

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
        this.contentScroll = 0;

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelWidth = Math.min(360, this.width - 40);
        int panelHeight = Math.min(246, this.height - 56);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int contentTop = panelY + 58;
        int contentBottom = panelY + panelHeight - 32;

        if (mouseX >= panelX + 8 && mouseX <= panelX + panelWidth - 8 && mouseY >= contentTop && mouseY <= contentBottom) {
            this.scrollContent((int)Math.round(-scrollY * 14.0D));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return switch (event.key()) {
            case InputConstants.KEY_DOWN -> {
                this.scrollContent(14);
                yield true;
            }
            case InputConstants.KEY_UP -> {
                this.scrollContent(-14);
                yield true;
            }
            case InputConstants.KEY_HOME -> {
                this.contentScroll = 0;
                yield true;
            }
            case InputConstants.KEY_END -> {
                this.contentScroll = this.maxScroll();
                yield true;
            }
            default -> super.keyPressed(event);
        };
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
        int contentTop = panelY + 58;
        int contentBottom = panelY + panelHeight - 32;
        int contentHeight = contentBottom - contentTop;
        this.contentScroll = Math.max(0, Math.min(this.contentScroll, this.maxScroll()));

        graphics.enableScissor(panelX + 8, contentTop, panelX + panelWidth - 8, contentBottom);
        int y = contentTop - this.contentScroll;
        y = drawWrappedText(graphics, Component.translatable(selected.description()), textX, y, contentWidth, BODY_TEXT, 8);
        y = drawSingleLineSection(graphics, Component.translatable("gui.mythos.growth"), Component.translatable(selected.growth()), textX, y, contentWidth, BODY_TEXT);
        y = drawSection(graphics, Component.translatable("gui.mythos.advantages"), selected.advantages(), textX, y, contentWidth, POSITIVE_TEXT);
        drawSection(graphics, Component.translatable("gui.mythos.disadvantages"), selected.disadvantages(), textX, y, contentWidth, NEGATIVE_TEXT);
        graphics.disableScissor();

        if (this.maxScroll() > 0) {
            int trackX = panelX + panelWidth - 6;
            int trackHeight = contentHeight;
            graphics.fill(trackX, contentTop, trackX + 2, contentBottom, 0x554A4F63);

            int knobHeight = Math.max(18, (int)Math.floor((contentHeight * (double)contentHeight) / totalContentHeight()));
            int knobTravel = trackHeight - knobHeight;
            int knobY = contentTop + (knobTravel <= 0 ? 0 : (int)Math.round((this.contentScroll / (double)this.maxScroll()) * knobTravel));
            graphics.fill(trackX - 1, knobY, trackX + 3, knobY + knobHeight, 0xCC9146FF);
            graphics.outline(trackX - 1, knobY, 4, knobHeight, 0xFF252934);
        }
    }

    private int drawSection(GuiGraphicsExtractor graphics, Component title, List<String> lines, int x, int y, int width, int color) {
        if (lines.isEmpty()) {
            return y;
        }

        graphics.text(this.font, title, x, y, color, false);
        y += 11;
        y = drawBullets(graphics, lines, x, y, width, color, "* ");
        return y + 4;
    }

    private int drawSingleLineSection(GuiGraphicsExtractor graphics, Component title, Component line, int x, int y, int width, int color) {
        graphics.text(this.font, title, x, y, color, false);
        y += 11;
        return drawWrappedText(graphics, line, x, y, width, color, 4);
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
        this.contentScroll = 0;
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

    private void scrollContent(int delta) {
        this.contentScroll = Math.max(0, Math.min(this.contentScroll + delta, this.maxScroll()));
    }

    private int maxScroll() {
        int panelHeight = Math.min(246, this.height - 56);
        int visibleHeight = panelHeight - 90;
        return Math.max(0, totalContentHeight() - visibleHeight);
    }

    private int totalContentHeight() {
        MythSelectionEntry selected = this.selectedMyth();
        if (selected == null) {
            return 0;
        }

        int panelWidth = Math.min(360, this.width - 40);
        int contentWidth = panelWidth - 32;
        int height = measureWrappedText(Component.translatable(selected.description()), contentWidth, 8);
        height += measureSingleLineSection(Component.translatable("gui.mythos.growth"), Component.translatable(selected.growth()), contentWidth);
        height += measureSection(Component.translatable("gui.mythos.advantages"), selected.advantages(), contentWidth);
        height += measureSection(Component.translatable("gui.mythos.disadvantages"), selected.disadvantages(), contentWidth);
        return height;
    }

    private int measureSection(Component title, List<String> lines, int width) {
        if (lines.isEmpty()) {
            return 0;
        }

        int height = 11;
        for (String lineKey : lines) {
            height += measureWrappedText(Component.literal("* " + Component.translatable(lineKey).getString()), width, 2);
        }
        return height + 4;
    }

    private int measureSingleLineSection(Component title, Component line, int width) {
        return 11 + measureWrappedText(line, width, 4);
    }

    private int measureWrappedText(Component text, int width, int bottomSpacing) {
        return this.font.split(text, width).size() * 10 + bottomSpacing;
    }
}
