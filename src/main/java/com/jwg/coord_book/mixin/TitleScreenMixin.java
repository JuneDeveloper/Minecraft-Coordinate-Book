package com.jwg.coord_book.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	int l = this.height / 4 + 48;
	private static final Identifier ACCESSIBILITY_ICON_TEXTURE = new Identifier("textures/gui/accessibility.png");
	protected TitleScreenMixin(Text title) {
		super(title);
	}
	@Inject(at = @At("RETURN"), method="initWidgetsNormal")
	private void addCustomButton(int y, int spacingY, CallbackInfo ci) {
		this.addDrawableChild(new TexturedButtonWidget(this.width / 2 + 104, l + 72 + 12, 20, 20, 0, 0, 20, ACCESSIBILITY_ICON_TEXTURE, 32, 64, (button) -> {
			//Code is run when the button is clicked
			//this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options));
		}, Text.translatable("jwg.button.bookmenu")));
	}
}
