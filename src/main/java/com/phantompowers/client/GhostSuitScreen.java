package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Pick how your ghost suit looks. A live preview of you spins on the left. */
public class GhostSuitScreen extends Screen {
	/** While open, your own model wears the suit being previewed. */
	public static boolean previewing;
	public static int preview;

	private static final int[][] SWATCH = {
			{0xFFF4F6FA, 0xFF16161E}, {0xFF22222C, 0xFFEEF0F4}, {0xFF18201C, 0xFF5AFF96},
			{0xFF961A24, 0xFF16141A}, {0xFFCDE8FC, 0xFF46AAFF}, {0xFF48287C, 0xFFE8C04A}};
	private static final String[] BLURB = {
			"Bright white with black stripes.", "Pitch black with white stripes.", "Dark suit with glowing green seams.",
			"Deep red with black stripes.", "Icy white-blue with glowing blue seams.", "Royal purple with gold trim."};

	private final Screen parent;
	private int left, top;
	private static final int W = 300, H = 200;

	public GhostSuitScreen(Screen parent) {
		super(Text.literal("Ghost Suit"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		previewing = true;
		preview = ClientGhost.suitVariant;
		left = (width - W) / 2;
		top = (height - H) / 2;
		for (int i = 0; i < GhostPowers.SUIT_VARIANTS.length; i++) {
			final int v = i;
			addDrawableChild(ButtonWidget.builder(Text.literal(GhostPowers.SUIT_VARIANTS[i]), b -> preview = v)
					.dimensions(left + 130 + (i % 2) * 84, top + 34 + (i / 2) * 32, 80, 20).build());
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Wear it"), b -> {
			PhantomClient.send(GhostPowers.A_SUIT_VARIANT + preview);
			close();
		}).dimensions(left + 130, top + H - 30, 80, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
				.dimensions(left + 214, top + H - 30, 80, 20).build());
	}

	@Override
	public void close() {
		previewing = false;
		if (client != null) client.setScreen(parent);
	}

	@Override
	public void removed() {
		previewing = false;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		ctx.fill(left, top, left + W, top + H, 0xE0101814);
		ctx.fill(left, top, left + W, top + 2, 0xFF33FF77);
		ctx.fill(left + 8, top + 24, left + 120, top + H - 8, 0x80000000);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, "Customize Ghost Suit", width / 2, top + 9, 0xFF66FF99);
		if (client != null && client.player != null) {
			InventoryScreen.drawEntity(ctx, left + 10, top + 28, left + 118, top + H - 10, 52, 0.0625f, mouseX, mouseY, client.player);
		}
		// colour swatches next to each button
		for (int i = 0; i < SWATCH.length; i++) {
			int x = left + 130 + (i % 2) * 84, y = top + 56 + (i / 2) * 32;
			ctx.fill(x, y, x + 80, y + 4, SWATCH[i][0]);
			ctx.fill(x + 30, y, x + 50, y + 4, SWATCH[i][1]);
			if (i == preview) ctx.fill(x - 2, y - 24, x, y + 4, 0xFF33FF77);
		}
		ctx.drawTextWithShadow(textRenderer, GhostPowers.SUIT_VARIANTS[preview], left + 130, top + H - 58, 0xFFFFEE55);
		ctx.drawTextWithShadow(textRenderer, BLURB[preview], left + 130, top + H - 46, 0xFFCCCCCC);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
