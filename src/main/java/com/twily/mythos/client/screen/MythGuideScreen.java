package com.twily.mythos.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.client.FairyVisionKeyHandler;
import com.twily.mythos.client.FairyFlightModeKeyHandler;
import com.twily.mythos.client.KitsuneActionKeyHandler;
import com.twily.mythos.client.OniActionKeyHandler;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.MythGuideEntry;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class MythGuideScreen extends Screen {

    private static final int PANEL_FILL = 0xEC151821;
    private static final int PANEL_BORDER = 0xFF4A4F63;
    private static final int PANEL_HEADER = 0xF0242937;
    private static final int BODY_TEXT = 0xFFF3F4F8;
    private static final int MUTED_TEXT = 0xFFB4B8C7;
    private static final int POSITIVE_TEXT = 0xFF9DE07E;
    private static final int NEGATIVE_TEXT = 0xFFFF9F8A;
    private static final int FEATURE_TEXT = 0xFF9BC7FF;
    private static final int CRAFT_TEXT = 0xFFFFD37A;
    private static final int DOT_ACTIVE = 0xFF9146FF;
    private static final int DOT_INACTIVE = 0x803B4051;
    private static final Identifier SLOT_SPRITE = Identifier.fromNamespaceAndPath("minecraft", "container/slot");
    private static final int RECIPE_CARD_FILL = 0xCC1A1D26;
    private static final int RECIPE_CARD_BORDER = 0xFF4A4F63;
    private static final int RECIPE_CARD_HEADER = 0xFF252934;

    private final List<MythGuideEntry> myths;
    private final Identifier currentMyth;
    private int selectedIndex;
    private int contentScroll;
    private Button previousButton;
    private Button nextButton;
    private Button closeButton;

    public MythGuideScreen(List<MythGuideEntry> myths, Identifier currentMyth) {
        super(Component.translatable("gui.mythos.guide.title"));
        this.myths = List.copyOf(myths);
        this.currentMyth = currentMyth;
        this.selectedIndex = indexFor(currentMyth);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(390, this.width - 34);
        int panelX = (this.width - panelWidth) / 2;
        int buttonY = Math.min(this.height - 40, (this.height + 276) / 2 + 16);

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
        this.closeButton = this.addRenderableWidget(
            Button.builder(Component.translatable("gui.mythos.close"), button -> this.onClose())
                .bounds(panelX + panelWidth - 86, buttonY, 86, 20)
                .build()
        );

        this.updateButtons();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelWidth = Math.min(390, this.width - 34);
        int panelHeight = Math.min(276, this.height - 54);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int panelWidth = Math.min(390, this.width - 34);
        int panelHeight = Math.min(276, this.height - 54);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_FILL);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 46, PANEL_HEADER);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
        graphics.horizontalLine(panelX + 1, panelX + panelWidth - 2, panelY + 46, PANEL_BORDER);

        if (this.myths.isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.translatable("gui.mythos.no_myths"), panelX + 16, panelY + 58, panelWidth - 32, MUTED_TEXT, false);
            return;
        }

        MythGuideEntry selected = this.selectedMyth();
        Identifier mythLogo = mythLogoTexture(selected.id());
        if (hasTexture(mythLogo)) {
            graphics.blit(mythLogo, panelX + 12, panelY + 7, panelX + 46, panelY + 41, 0.0F, 1.0F, 0.0F, 1.0F);
        }

        graphics.text(this.font, Component.translatable("gui.mythos.guide.book_name"), panelX + 58, panelY + 8, MUTED_TEXT, false);
        graphics.text(this.font, MythState.displayName(selected.id()), panelX + 58, panelY + 20, BODY_TEXT, false);
        Component pageText = Component.translatable("gui.mythos.page", this.selectedIndex + 1, this.myths.size());
        graphics.text(this.font, pageText, panelX + panelWidth - 12 - this.font.width(pageText), panelY + 8, MUTED_TEXT, false);

        int dotsX = panelX + panelWidth - 34;
        int dotsY = panelY + 25;
        Component complexityText = Component.translatable("gui.mythos.complexity");
        graphics.text(this.font, complexityText, dotsX - 8 - this.font.width(complexityText), panelY + 21, MUTED_TEXT, false);
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
        y = drawSection(graphics, Component.translatable("gui.mythos.disadvantages"), selected.disadvantages(), textX, y, contentWidth, NEGATIVE_TEXT);
        y = drawSection(graphics, Component.translatable("gui.mythos.guide.features"), selected.features(), textX, y, contentWidth, FEATURE_TEXT);
        drawCraftingSection(graphics, Component.translatable("gui.mythos.guide.crafting"), selected.crafting(), textX, y, contentWidth);
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
        y = drawBullets(graphics, lines, x, y, width, color, "• ");
        return y + 4;
    }

    private int drawCraftingSection(GuiGraphicsExtractor graphics, Component title, List<String> lines, int x, int y, int width) {
        if (lines.isEmpty()) {
            return y;
        }

        graphics.text(this.font, title, x, y, CRAFT_TEXT, false);
        y += 11;
        for (String line : lines) {
            if (isRecipeEntry(line)) {
                y = drawRecipeCard(graphics, line, x, y, width);
            } else {
                y = drawWrappedText(graphics, Component.literal("• ").append(resolveGuideLine(line)), x, y, width, CRAFT_TEXT, 2);
            }
        }
        return y + 4;
    }

    private int drawSingleLineSection(GuiGraphicsExtractor graphics, Component title, Component line, int x, int y, int width, int color) {
        graphics.text(this.font, title, x, y, color, false);
        y += 11;
        return drawWrappedText(graphics, line, x, y, width, color, 4);
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
    }

    private MythGuideEntry selectedMyth() {
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

    private int drawRecipeCard(GuiGraphicsExtractor graphics, String entry, int x, int y, int width) {
        Optional<RecipePreview> recipe = recipeFor(entry);
        if (recipe.isEmpty()) {
            return drawWrappedText(graphics, Component.literal("• ").append(Component.literal(entry.substring("recipe:".length()))), x, y, width, CRAFT_TEXT, 2);
        }

        RecipePreview shaped = recipe.get();
        ItemStack result = shaped.result();
        int cardWidth = Math.min(width, 188);
        int cardHeight = 92;

        graphics.fill(x, y, x + cardWidth, y + cardHeight, RECIPE_CARD_FILL);
        graphics.outline(x, y, cardWidth, cardHeight, RECIPE_CARD_BORDER);
        graphics.fill(x, y, x + cardWidth, y + 22, RECIPE_CARD_HEADER);
        graphics.text(this.font, result.getHoverName(), x + 6, y + 3, CRAFT_TEXT, false);
        Component method = Component.translatable(shaped.layout() == PreviewLayout.SMITHING
            ? "gui.mythos.recipe_method.smithing_table"
            : "gui.mythos.recipe_method.crafting_table");
        graphics.text(this.font, method, x + 6, y + 12, MUTED_TEXT, false);

        if (shaped.layout() == PreviewLayout.SMITHING) {
            int leftX = x + 10;
            int slotY = y + 36;
            drawSlotWithItem(graphics, shaped.ingredients().get(0), leftX, slotY);
            drawSlotWithItem(graphics, shaped.ingredients().get(1), leftX + 22, slotY);
            Component arrow = Component.literal("->");
            graphics.text(this.font, arrow, x + 85, y + 43, BODY_TEXT, false);
            int resultSlotX = x + 112;
            drawSlotWithItem(graphics, result, resultSlotX, slotY);
        } else {
            int gridX = x + 6;
            int gridY = y + 28;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int slotX = gridX + column * 18;
                    int slotY = gridY + row * 18;
                    drawSlotWithItem(graphics, ingredientAt(shaped.ingredients(), row * 3 + column), slotX, slotY);
                }
            }

            Component arrow = Component.literal("->");
            graphics.text(this.font, arrow, x + 85, y + 53, BODY_TEXT, false);
            int resultSlotX = x + 112;
            int resultSlotY = y + 46;
            drawSlotWithItem(graphics, result, resultSlotX, resultSlotY);
        }

        return y + cardHeight + 4;
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

        return direct;
    }

    private boolean hasTexture(Identifier texture) {
        return this.minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private int drawBullets(GuiGraphicsExtractor graphics, List<String> lines, int x, int y, int width, int color, String prefix) {
        for (String lineKey : lines) {
            y = drawWrappedText(graphics, Component.literal(prefix).append(resolveGuideLine(lineKey)), x, y, width, color, 2);
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
        int panelHeight = Math.min(276, this.height - 54);
        int visibleHeight = panelHeight - 90;
        return Math.max(0, totalContentHeight() - visibleHeight);
    }

    private int totalContentHeight() {
        MythGuideEntry selected = this.selectedMyth();
        if (selected == null) {
            return 0;
        }

        int panelWidth = Math.min(390, this.width - 34);
        int contentWidth = panelWidth - 32;
        int height = measureWrappedText(Component.translatable(selected.description()), contentWidth, 8);
        height += measureSingleLineSection(Component.translatable("gui.mythos.growth"), Component.translatable(selected.growth()), contentWidth);
        height += measureSection(Component.translatable("gui.mythos.advantages"), selected.advantages(), contentWidth);
        height += measureSection(Component.translatable("gui.mythos.disadvantages"), selected.disadvantages(), contentWidth);
        height += measureSection(Component.translatable("gui.mythos.guide.features"), selected.features(), contentWidth);
        height += measureCraftingSection(Component.translatable("gui.mythos.guide.crafting"), selected.crafting(), contentWidth);
        return height;
    }

    private int measureSection(Component title, List<String> lines, int width) {
        if (lines.isEmpty()) {
            return 0;
        }

        int height = 11;
        for (String lineKey : lines) {
            height += measureWrappedText(Component.literal("• ").append(resolveGuideLine(lineKey)), width, 2);
        }
        return height + 4;
    }

    private int measureCraftingSection(Component title, List<String> lines, int width) {
        if (lines.isEmpty()) {
            return 0;
        }

        int height = 11;
        for (String line : lines) {
            height += isRecipeEntry(line)
                ? 96
                : measureWrappedText(Component.literal("• ").append(resolveGuideLine(line)), width, 2);
        }
        return height + 4;
    }

    private int measureSingleLineSection(Component title, Component line, int width) {
        return 11 + measureWrappedText(line, width, 4);
    }

    private int measureWrappedText(Component text, int width, int bottomSpacing) {
        return this.font.split(text, width).size() * 10 + bottomSpacing;
    }

    private Component resolveGuideLine(String lineKey) {
        return switch (lineKey) {
            case "myth.mythos.fairy.guide.feature.low_flight" ->
                Component.translatable(lineKey, FairyFlightModeKeyHandler.keyName());
            case "myth.mythos.fairy.guide.feature.shared_vision" ->
                Component.translatable(lineKey, FairyVisionKeyHandler.keyName());
            case "myth.mythos.kitsune.guide.feature.night_mask" ->
                Component.translatable(lineKey, KitsuneActionKeyHandler.maskKeyName());
            case "myth.mythos.kitsune.guide.feature.foxfire" ->
                Component.translatable(lineKey, KitsuneActionKeyHandler.foxfireKeyName());
            case "myth.mythos.kitsune.guide.feature.fox_dash" ->
                Component.translatable(lineKey, KitsuneActionKeyHandler.dashKeyName());
            case "myth.mythos.oni.guide.feature.battle_form" ->
                Component.translatable(lineKey, OniActionKeyHandler.battleFormKeyName());
            default -> Component.translatable(lineKey);
        };
    }

    private boolean isRecipeEntry(String entry) {
        return entry.startsWith("recipe:");
    }

    private Optional<RecipePreview> recipeFor(String entry) {
        if (!isRecipeEntry(entry)) {
            return Optional.empty();
        }

        Identifier recipeId = Identifier.tryParse(entry.substring("recipe:".length()));
        if (recipeId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(switch (recipeId.toString()) {
            case "mythos:mythos_guide" -> craftingPreview(
                new ItemStack(MythosItems.MYTHOS_GUIDE.get()),
                new ItemStack(Items.BOOK),
                new ItemStack(Items.LAPIS_LAZULI)
            );
            case "mythos:dwarven_ale" -> craftingPreview(
                new ItemStack(MythosItems.DWARVEN_ALE.get()),
                new ItemStack(Items.HONEY_BOTTLE),
                new ItemStack(Items.WHEAT),
                new ItemStack(Items.WHEAT)
            );
            case "mythos:dwarven_pickaxe_tempering" -> smithingPreview(
                dwarvenPickaxePreview(),
                enchanted(new ItemStack(Items.DIAMOND_PICKAXE)),
                new ItemStack(Items.NETHERITE_INGOT)
            );
            case "mythos:looting_bow_upgrade" -> smithingPreview(
                elvenBowPreview(),
                new ItemStack(Items.BOW),
                new ItemStack(Items.NETHERITE_INGOT)
            );
            case "mythos:fairy_boots" -> smithingPreview(
                fairyBootsPreview(Items.LEATHER_BOOTS),
                new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.WIND_CHARGE)
            );
            case "mythos:fairy_minecart" -> smithingPreview(
                new ItemStack(MythosItems.FAIRY_MINECART.get()),
                new ItemStack(Items.MINECART),
                new ItemStack(Items.WIND_CHARGE)
            );
            case "mythos:fox_lantern" -> smithingPreview(
                new ItemStack(MythosItems.FOX_LANTERN.get()),
                new ItemStack(Items.LANTERN),
                new ItemStack(Items.GLOW_INK_SAC)
            );
            case "mythos:siren_elixir" -> smithingPreview(
                sirenElixirPreview(),
                PotionContents.createItemStack(Items.POTION, Potions.WATER),
                new ItemStack(Items.NAUTILUS_SHELL)
            );
            case "mythos:rage_talisman" -> smithingPreview(
                new ItemStack(MythosItems.RAGE_TALISMAN.get()),
                new ItemStack(Items.PAPER),
                new ItemStack(Items.NETHER_WART)
            );
            case "mythos:clinging_gel" -> craftingPreview(
                new ItemStack(MythosItems.CLINGING_GEL.get()),
                PotionContents.createItemStack(Items.POTION, Potions.OOZING),
                new ItemStack(Items.HONEY_BLOCK)
            );
            case "mythos:shulker_box" -> gridPreview(
                new ItemStack(Items.SHULKER_BOX),
                List.of(
                    new ItemStack(Items.AMETHYST_SHARD),
                    new ItemStack(MythosItems.RESONANCE_SHARD.get()),
                    new ItemStack(Items.AMETHYST_SHARD),
                    new ItemStack(Items.SLIME_BALL),
                    new ItemStack(Items.CHEST),
                    new ItemStack(Items.SLIME_BALL),
                    new ItemStack(Items.AMETHYST_SHARD),
                    new ItemStack(Items.SLIME_BALL),
                    new ItemStack(Items.AMETHYST_SHARD)
                )
            );
            case "mythos:reinforced_shulker_box" -> gridPreview(
                new ItemStack(MythosItems.REINFORCED_SHULKER_BOX.get()),
                List.of(
                    ItemStack.EMPTY,
                    new ItemStack(MythosItems.RESONANCE_SHARD.get()),
                    ItemStack.EMPTY,
                    new ItemStack(Items.SHULKER_BOX),
                    new ItemStack(Items.NETHERITE_INGOT),
                    new ItemStack(Items.SHULKER_BOX),
                    ItemStack.EMPTY,
                    new ItemStack(MythosItems.RESONANCE_SHARD.get()),
                    ItemStack.EMPTY
                )
            );
            default -> null;
        });
    }

    private void drawSlotWithItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 1, y + 1);
            graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private ItemStack ingredientAt(List<ItemStack> ingredients, int index) {
        return index < ingredients.size() ? ingredients.get(index) : ItemStack.EMPTY;
    }

    private RecipePreview craftingPreview(ItemStack result, ItemStack... ingredients) {
        return gridPreview(result, List.of(ingredients));
    }

    private RecipePreview gridPreview(ItemStack result, List<ItemStack> ingredients) {
        return new RecipePreview(result, ingredients, PreviewLayout.GRID);
    }

    private RecipePreview smithingPreview(ItemStack result, ItemStack base, ItemStack addition) {
        return new RecipePreview(result, List.of(base, addition), PreviewLayout.SMITHING);
    }

    private ItemStack count(ItemStack stack, int count) {
        ItemStack result = stack.copy();
        result.setCount(count);
        return result;
    }

    private ItemStack enchanted(ItemStack stack) {
        ItemStack result = stack.copy();
        if (this.minecraft.level != null) {
            var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            result.enchant(registry.getOrThrow(Enchantments.EFFICIENCY), 4);
        }
        return result;
    }

    private ItemStack named(ItemStack stack, String key, ChatFormatting color) {
        ItemStack result = stack.copy();
        result.set(DataComponents.CUSTOM_NAME, Component.translatable(key).withStyle(color));
        return result;
    }

    private ItemStack fairyBootsPreview(net.minecraft.world.item.Item item) {
        return named(new ItemStack(item), "item.mythos.fairy_boots", ChatFormatting.LIGHT_PURPLE);
    }

    private ItemStack dwarvenPickaxePreview() {
        return named(enchanted(new ItemStack(Items.DIAMOND_PICKAXE)), "item.mythos.dwarven_pickaxe", ChatFormatting.BLUE);
    }

    private ItemStack elvenBowPreview() {
        ItemStack bow = named(new ItemStack(Items.BOW), "item.mythos.elven_bow", ChatFormatting.GREEN);
        if (this.minecraft.level != null) {
            var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            bow.enchant(registry.getOrThrow(Enchantments.LOOTING), 3);
        }
        return bow;
    }

    private ItemStack sirenElixirPreview() {
        return named(new ItemStack(MythosItems.SIREN_ELIXIR.get()), "item.mythos.siren_elixir", ChatFormatting.AQUA);
    }

    private enum PreviewLayout {
        GRID,
        SMITHING
    }

    private record RecipePreview(ItemStack result, List<ItemStack> ingredients, PreviewLayout layout) {
    }
}
