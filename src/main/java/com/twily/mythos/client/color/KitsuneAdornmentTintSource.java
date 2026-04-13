package com.twily.mythos.client.color;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.twily.mythos.client.config.KitsuneAdornmentColorMode;
import com.twily.mythos.client.config.MythosClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class KitsuneAdornmentTintSource implements net.minecraft.client.color.item.ItemTintSource {
    public static final MapCodec<KitsuneAdornmentTintSource> MAP_CODEC = MapCodec.unit(new KitsuneAdornmentTintSource());
    private static final Map<Identifier, Integer> AUTO_COLOR_CACHE = new ConcurrentHashMap<>();
    private static final int SAMPLE_X = 12;
    private static final int SAMPLE_Y = 3;

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        KitsuneAdornmentColorMode mode = MythosClientConfig.KITSUNE_COLOR_MODE.get();
        return switch (mode) {
            case DEFAULT -> MythosClientConfig.defaultKitsuneColor();
            case MANUAL -> MythosClientConfig.manualKitsuneColor();
            case AUTO -> resolveAutoColor(owner);
        };
    }

    @Override
    public MapCodec<KitsuneAdornmentTintSource> type() {
        return MAP_CODEC;
    }

    private static int resolveAutoColor(@Nullable LivingEntity owner) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer clientPlayer = owner instanceof AbstractClientPlayer abstractClientPlayer
            ? abstractClientPlayer
            : minecraft.player;
        if (clientPlayer == null) {
            return MythosClientConfig.defaultKitsuneColor();
        }

        PlayerSkin skin = clientPlayer.getSkin();
        Identifier texturePath = skin.body().texturePath();
        return AUTO_COLOR_CACHE.computeIfAbsent(texturePath, id -> sampleSkinColor(minecraft, id));
    }

    private static int sampleSkinColor(Minecraft minecraft, Identifier texturePath) {
        int sampledColor = sampleLoadedTexture(minecraft, texturePath);
        if (sampledColor == 0) {
            sampledColor = sampleResourceTexture(minecraft, texturePath);
        }
        if (sampledColor == 0) {
            sampledColor = sampleResourceTexture(minecraft, DefaultPlayerSkin.getDefaultTexture());
        }

        return sampledColor == 0 ? MythosClientConfig.defaultKitsuneColor() : ARGB.opaque(sampledColor);
    }

    private static int sampleLoadedTexture(Minecraft minecraft, Identifier texturePath) {
        try {
            AbstractTexture texture = minecraft.getTextureManager().getTexture(texturePath);
            if (texture instanceof DynamicTexture dynamicTexture) {
                NativeImage image = dynamicTexture.getPixels();
                if (image != null && !image.isClosed() && SAMPLE_X < image.getWidth() && SAMPLE_Y < image.getHeight()) {
                    return sanitizeSample(image.getPixel(SAMPLE_X, SAMPLE_Y));
                }
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private static int sampleResourceTexture(Minecraft minecraft, Identifier texturePath) {
        try (InputStream inputStream = minecraft.getResourceManager().open(texturePath); NativeImage image = NativeImage.read(inputStream)) {
            if (SAMPLE_X < image.getWidth() && SAMPLE_Y < image.getHeight()) {
                return sanitizeSample(image.getPixel(SAMPLE_X, SAMPLE_Y));
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private static int sanitizeSample(int color) {
        if (ARGB.alpha(color) < 16) {
            return 0;
        }

        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int brightest = Math.max(red, Math.max(green, blue));
        int darkest = Math.min(red, Math.min(green, blue));
        if (brightest < 18) {
            return MythosClientConfig.defaultKitsuneColor();
        }
        if (brightest - darkest < 8) {
            int boosted = Math.min(255, brightest + 24);
            return ARGB.color(boosted, boosted, boosted);
        }
        return ARGB.color(red, green, blue);
    }
}
