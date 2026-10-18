package com.phantompowers.client;

import com.phantompowers.network.ClonePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** Cloning Pod: pick a female or male clone of yourself. */
public class CloneChoiceScreen extends Screen {
	private final BlockPos pos;
	private int left, top;
	private static final int W = 240, H = 130;
	private int ticks;

	public CloneChoiceScreen(BlockPos pos) {
		super(Text.literal("Cloning Pod"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		addDrawableChild(ButtonWidget.builder(Text.literal("Female clone"), b -> pick(true)).dimensions(left + 14, top + 62, 100, 24).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Male clone"), b -> pick(false)).dimensions(left + W - 114, top + 62, 100, 24).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(left + W / 2 - 40, top + H - 30, 80, 20).build());
	}

	private void pick(boolean female) {
		ClientPlayNetworking.send(new ClonePayload(pos, female));
		close();
	}

	@Override
	public void tick() {
		ticks++;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		for (int i = 4; i >= 1; i--) ctx.fill(left - i, top - i, left + W + i, top + H + i, (0x10 * (5 - i)) << 24 | 0x33FF77);
		ctx.fillGradient(left, top, left + W, top + H, 0xF00A1A12, 0xF0050A08);
		ctx.fill(left, top, left + W, top + 2, 0xFF33FF77);
		// bubbling fluid bar along the bottom
		float t = ticks + delta;
		for (int x = left + 4; x < left + W - 4; x += 3) {
			int h = 3 + (int) (2 * MathHelper.sin(t * 0.2f + x * 0.3f));
			ctx.fill(x, top + H - 4 - h, x + 2, top + H - 4, 0x6033FF77);
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, "CLONING POD", left + W / 2, top + 10, 0xFF66FF99);
		ctx.drawCenteredTextWithShadow(textRenderer, "Grow a clone from this DNA.", left + W / 2, top + 28, 0xFFDDEEE4);
		ctx.drawCenteredTextWithShadow(textRenderer, "Which version do you want?", left + W / 2, top + 40, 0xFFB8C8BE);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
