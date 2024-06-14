package com.smushytaco.legacy_display
import com.smushytaco.legacy_display.configuration_support.ModConfiguration
import com.smushytaco.legacy_display.mixin_logic.MixinSyntacticSugar.chunkUpdaters
import com.smushytaco.legacy_display.mixins.CurrentFPSMixin
import kotlinx.coroutines.*
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.SharedConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import java.util.*
object LegacyDisplay : ClientModInitializer {
    private fun startRepeatingJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                chunkUpdateCount = MinecraftClient.getInstance().world?.chunkUpdaters?.size ?: 0
                delay(125L)
            }
        }
    }
    private var chunkUpdateCount = 0
    const val MOD_ID = "legacy_display"
    val MENU_BACKGROUND_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/menu_background.png")
    val INWORLD_MENU_BACKGROUND_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/inworld_menu_background.png")
    val HEADER_SEPARATOR_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/header_separator.png")
    val FOOTER_SEPARATOR_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/footer_separator.png")
    val MENU_LIST_BACKGROUND_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/menu_list_background.png")
    val INWORLD_MENU_LIST_BACKGROUND_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/inworld_menu_list_background.png")
    val TAB_HEADER_BACKGROUND_TEXTURE: Identifier = Identifier.of(MOD_ID, "textures/gui/tab_header_background.png")
    lateinit var config: ModConfiguration
        private set
    private lateinit var coroutine: Job
    private val minecraftVersion = SharedConstants.getGameVersion().name
    const val TEXT_COLOR = 16777215
    override fun onInitializeClient() {
        AutoConfig.register(ModConfiguration::class.java) { definition: Config, configClass: Class<ModConfiguration> ->
            GsonConfigSerializer(definition, configClass)
        }
        config = AutoConfig.getConfigHolder(ModConfiguration::class.java).config
        HudRenderCallback.EVENT.register(HudRenderCallback { context, _ ->
            if (MinecraftClient.getInstance().debugHud.shouldShowDebugHud()) return@HudRenderCallback
            if (config.enableMinecraftKeywordDisplay || config.enableVersionDisplay) {
                context.drawTextWithShadow(MinecraftClient.getInstance().inGameHud.textRenderer,
                    "${if (config.enableMinecraftKeywordDisplay) "Minecraft" else ""}${if (config.enableVersionDisplay && config.enableMinecraftKeywordDisplay) " " else ""}${if (config.enableVersionDisplay) minecraftVersion else ""}",
                    2, 2, TEXT_COLOR)
            }
            if (config.enableFPSDisplay || config.enableChunkUpdateDisplay) {
                if (config.enableChunkUpdateDisplay) {
                    if (LegacyDisplay::coroutine.isInitialized) {
                        if (!coroutine.isActive) coroutine = startRepeatingJob()
                    } else {
                        coroutine = startRepeatingJob()
                    }
                } else {
                    if (LegacyDisplay::coroutine.isInitialized && coroutine.isActive) coroutine.cancel()
                }
                context.drawTextWithShadow(MinecraftClient.getInstance().inGameHud.textRenderer,
                    "${if (config.enableFPSDisplay) "${CurrentFPSMixin.getCurrentFPS()} fps" else ""}${if (config.enableFPSDisplay && config.enableChunkUpdateDisplay) ", " else ""}${if (config.enableChunkUpdateDisplay) "$chunkUpdateCount chunk update${if (chunkUpdateCount != 1) "s" else ""}" else ""}",
                    2, if (config.enableMinecraftKeywordDisplay || config.enableVersionDisplay) 14 else 2, TEXT_COLOR
                )
            } else {
                if (LegacyDisplay::coroutine.isInitialized && coroutine.isActive) coroutine.cancel()
            }
            if (config.enableCoordinateDisplay) {
                val unformattedCoordinates = String.format(Locale.ROOT, "%.3f %.5f %.3f", MinecraftClient.getInstance().cameraEntity?.x, MinecraftClient.getInstance().cameraEntity?.y, MinecraftClient.getInstance().cameraEntity?.z)
                val coordinateList = unformattedCoordinates.split(".", " ")
                val formattedCoordinates = StringBuilder()
                for (i in coordinateList.indices) {
                    if (i % 2 == 0) {
                        formattedCoordinates.append(coordinateList[i])
                        if (i != coordinateList.indices.last - 1) formattedCoordinates.append(", ")
                    }
                }
                context.drawTextWithShadow(MinecraftClient.getInstance().inGameHud.textRenderer, "${if (config.enablePositionKeywordInCoordinateDisplay) "Position: " else ""}$formattedCoordinates",
                    2, if ((config.enableMinecraftKeywordDisplay || config.enableVersionDisplay) && (config.enableFPSDisplay || config.enableChunkUpdateDisplay)) 26 else if ((config.enableMinecraftKeywordDisplay || config.enableVersionDisplay) xor (config.enableFPSDisplay || config.enableChunkUpdateDisplay)) 14 else 2, TEXT_COLOR)
            }
        })
    }
}