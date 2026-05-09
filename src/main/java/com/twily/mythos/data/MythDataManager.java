package com.twily.mythos.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class MythDataManager {

    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "myth_data");
    private static final MythReloadListener RELOAD_LISTENER = new MythReloadListener();
    private static volatile MythRepository repository = MythRepository.empty();

    private MythDataManager() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(RELOAD_LISTENER_ID, RELOAD_LISTENER);
    }

    public static boolean hasMyth(Identifier mythId) {
        return MythState.NONE.equals(mythId) || repository.myths.containsKey(mythId);
    }

    public static Set<String> mythCommandIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Identifier mythId : repository.myths.keySet()) {
            ids.add(mythId.getNamespace().equals(Mythos.MOD_ID) ? mythId.getPath() : mythId.toString());
        }
        return ids;
    }

    public static Collection<Identifier> mythIds() {
        return repository.myths.keySet();
    }

    public static List<MythDefinition> mythsInOrder() {
        return orderedMyths(repository.myths.values());
    }

    public static List<MythDefinition> visibleMythsInOrder() {
        return orderedMyths(repository.myths.values().stream()
            .filter(definition -> !definition.hidden())
            .toList());
    }

    public static boolean selectableInMenu(Identifier mythId) {
        MythDefinition definition = repository.myths.get(mythId);
        return definition != null && !definition.hidden();
    }

    private static List<MythDefinition> orderedMyths(Collection<MythDefinition> definitions) {
        return definitions.stream()
            .sorted((left, right) -> {
                int orderCompare = Integer.compare(left.order(), right.order());
                return orderCompare != 0 ? orderCompare : left.id().toString().compareTo(right.id().toString());
            })
            .toList();
    }

    public static Optional<MythDefinition> getMyth(Identifier mythId) {
        return Optional.ofNullable(repository.myths.get(mythId));
    }

    public static boolean matches(Identifier currentMythId, Identifier targetMythId) {
        if (currentMythId.equals(targetMythId)) {
            return true;
        }

        Set<Identifier> visited = new LinkedHashSet<>();
        Identifier cursor = currentMythId;
        while (visited.add(cursor)) {
            MythDefinition definition = repository.myths.get(cursor);
            if (definition == null || definition.inherits().isEmpty()) {
                return false;
            }

            cursor = definition.inherits().get();
            if (cursor.equals(targetMythId)) {
                return true;
            }
        }

        return false;
    }

    public static int biomeSpeedAmplifier(Player player, Holder<Biome> biome) {
        int maxAmplifier = -1;

        for (MythPowerDefinition power : powersFor(MythState.get(player))) {
            if (power.type() != MythPowerType.BIOME_SPEED) {
                continue;
            }

            TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, power.biomeTag().orElseThrow());
            if (biome.is(tagKey)) {
                maxAmplifier = Math.max(maxAmplifier, power.amplifier());
            }
        }

        return maxAmplifier;
    }

    public static double rangedDamageMultiplier(Player player) {
        double multiplier = 1.0D;

        for (MythPowerDefinition power : powersFor(MythState.get(player))) {
            if (power.type() == MythPowerType.RANGED_DAMAGE_MULTIPLIER) {
                multiplier *= power.multiplier();
            }
        }

        return multiplier;
    }

    public static double itemDamageMultiplier(Player player, ItemStack stack) {
        double multiplier = 1.0D;

        for (MythPowerDefinition power : powersFor(MythState.get(player))) {
            if (power.type() != MythPowerType.ITEM_DAMAGE_MULTIPLIER) {
                continue;
            }

            if (stack.is(TagKey.create(Registries.ITEM, power.itemTag().orElseThrow()))) {
                multiplier *= power.multiplier();
            }
        }

        return multiplier;
    }

    public static Optional<Component> deniedSmithingMessage(Player player, ItemStack result) {
        if (result.isEmpty()) {
            return Optional.empty();
        }

        List<MythPowerDefinition> matchingRestrictions = repository.powers.values().stream()
            .filter(power -> power.type() == MythPowerType.SMITHING_RECIPE_UNLOCK)
            .filter(power -> power.resultMarker().isPresent() && MythItemMarkerHelper.hasMarker(result, power.resultMarker().get()))
            .toList();

        if (matchingRestrictions.isEmpty()) {
            return Optional.empty();
        }

        boolean allowed = powersFor(MythState.get(player)).stream()
            .filter(power -> power.type() == MythPowerType.SMITHING_RECIPE_UNLOCK)
            .map(MythPowerDefinition::resultMarker)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(marker -> matchingRestrictions.stream().anyMatch(match -> match.resultMarker().orElse("").equals(marker)));

        if (allowed) {
            return Optional.empty();
        }

        String key = matchingRestrictions.getFirst().denyMessage().orElse("message.mythos.smithing_locked");
        return Optional.of(Component.translatable(key));
    }

    private static List<MythPowerDefinition> powersFor(Identifier mythId) {
        LinkedHashSet<Identifier> powerIds = new LinkedHashSet<>();
        collectInheritedPowers(mythId, powerIds, new LinkedHashSet<>());
        return powerIds.stream()
            .map(repository.powers::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private static void collectInheritedPowers(Identifier mythId, Set<Identifier> powerIds, Set<Identifier> visitedMyths) {
        if (!visitedMyths.add(mythId)) {
            return;
        }

        MythDefinition definition = repository.myths.get(mythId);
        if (definition == null) {
            return;
        }

        definition.inherits().ifPresent(parent -> collectInheritedPowers(parent, powerIds, visitedMyths));
        powerIds.addAll(definition.powers());
    }

    private static final class MythReloadListener extends SimplePreparableReloadListener<MythRepository> {
        private static final FileToIdConverter MYTHS = FileToIdConverter.json("myths");
        private static final FileToIdConverter POWERS = FileToIdConverter.json("powers");

        @Override
        protected MythRepository prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<Identifier, MythDefinition.MythFile> mythFiles = new LinkedHashMap<>();
            SimpleJsonResourceReloadListener.scanDirectory(resourceManager, MYTHS, JsonOps.INSTANCE, MythDefinition.FILE_CODEC, mythFiles);

            Map<Identifier, MythPowerDefinition> powers = new LinkedHashMap<>();
            for (Map.Entry<Identifier, Resource> entry : POWERS.listMatchingResources(resourceManager).entrySet()) {
                Identifier id = POWERS.fileToId(entry.getKey());
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);
                    powers.put(id, MythPowerDefinition.fromJson(id, json));
                } catch (Exception exception) {
                    throw new IllegalStateException("Failed to load myth power " + id, exception);
                }
            }

            Map<Identifier, MythDefinition> myths = new LinkedHashMap<>();
            for (Map.Entry<Identifier, MythDefinition.MythFile> entry : mythFiles.entrySet()) {
                myths.put(entry.getKey(), MythDefinition.fromFile(entry.getKey(), entry.getValue()));
            }

            return new MythRepository(Map.copyOf(myths), Map.copyOf(powers));
        }

        @Override
        protected void apply(MythRepository prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
            repository = prepared;
        }
    }

    private record MythRepository(Map<Identifier, MythDefinition> myths, Map<Identifier, MythPowerDefinition> powers) {
        private static MythRepository empty() {
            return new MythRepository(Map.of(), Map.of());
        }
    }
}
