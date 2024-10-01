package com.minenash.seamless_loading_screen.neoforge;

import com.minenash.seamless_loading_screen.SeamlessLoadingScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;

import java.io.IOException;

@EventBusSubscriber(modid = SeamlessLoadingScreen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SeamlessLoadingScreenShaderInit {

    public static Logger LOGGER = LogUtils.getLogger();

//    public static void init(IEventBus bus){
//        bus.addListener(SeamlessLoadingScreenShaderInit::registerShaders);
//    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderProgram(event.getResourceProvider(), Identifier.of(SeamlessLoadingScreen.MODID, "blur"), VertexFormats.POSITION), (shaderProgram) -> {
                SeamlessLoadingScreen.BLUR_PROGRAM.load(shaderProgram);
            });
        } catch (IOException e) {
            LOGGER.error("[SeamlessLoadingScreenShaderInit] An issue with loading the needed shader files has failed, blur will not be working!", e);
        }
    }
}
