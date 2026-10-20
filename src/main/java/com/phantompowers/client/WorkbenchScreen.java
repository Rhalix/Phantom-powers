package com.phantompowers.client;

import com.phantompowers.HunterPaint;
import com.phantompowers.network.WorkbenchPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * The Hunter Workbench: hold your gear (Ghost Blaster, Ecto Launcher, Thermos, Hunter Suit pack or Hover Glider),
 * pick any colour and paint it, or spend materials to upgrade it. The preview shows the paint job before you commit.
 */
public class WorkbenchScreen extends Screen {
	private static final int[] SWATCHES = {0x16161E, 0xF4F6FA, 0x8A9098, 0xB01830, 0xFF6A1A, 0xF0C840, 0x55FF88, 0x1E8A4A,
			0x46AAFF, 0x2A3CB0, 0x8A3CE0, 0xFF66CC};
	private final BlockPos pos;
	private int left, top, panelW, panelH;
	private float hue = 0.55f, sat = 0.7f, val = 0.8f;
	private int dragging;
	private int sqX, sqY, sqW, sqH, hueY;
	private ButtonWidget paintBtn, stripBtn, upgradeBtn;

	public WorkbenchScreen(BlockPos pos) {
		super(Text.literal("Hunter Workbench"));
		this.pos = pos;
	}

	private ItemStack held() {
		return client != null && client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
	}

	private int color() {
		return MathHelper.hsvToRgb(Math.min(hue, 0.9999f), sat, val) & 0xFFFFFF;
	}

	@Override
	protected void init() {
		panelW = Math.min(340, width - 12);
		panelH = Math.min(220, height - 12);
		left = (width - panelW) / 2;
		top = (height - panelH) / 2;
		int p = HunterPaint.paint(held());
		if (p >= 0) {
			float[] hsv = GhostSuitScreen.toHsv(p);
			hue = hsv[0];
			sat = hsv[1];
			val = hsv[2];
		}
		sqX = left + 120;
		sqY = top + 30;
		sqW = panelW - 130;
		sqH = 60;
		hueY = sqY + sqH + 5;
		int by = top + panelH - 50;
		paintBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Paint it"), b -> send(0, color())).dimensions(sqX, by - 2, 74, 20).build());
		stripBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Factory finish"), b -> send(0, -1)).dimensions(sqX + 78, by - 2, Math.min(100, sqW - 78), 20).build());
		upgradeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Upgrade"), b -> send(1, 0)).dimensions(left + 10, top + panelH - 28, 100, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(left + panelW - 70, top + panelH - 28, 60, 20).build());
	}

	private void send(int action, int value) {
		ClientPlayNetworking.send(new WorkbenchPayload(pos, action, value));
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		for (int i = 4; i >= 1; i--) ctx.fill(left - i, top - i, left + panelW + i, top + panelH + i, (0x10 * (5 - i)) << 24 | 0x66E0FF);
		ctx.fillGradient(left, top, left + panelW, top + panelH, 0xF0101820, 0xF0080C10);
		ctx.fill(left, top, left + panelW, top + 2, 0xFF66E0FF);
		// hazard stripe along the bottom
		for (int x = left; x < left + panelW; x += 6) ctx.fill(x, top + panelH - 3, Math.min(x + 3, left + panelW), top + panelH, 0xFFF0C828);
		ctx.fill(left + 8, top + 26, left + 112, top + panelH - 34, 0x60000000);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ItemStack s = held();
		boolean paintable = HunterPaint.paintable(s), upgradable = HunterPaint.upgradable(s);
		paintBtn.active = paintable;
		stripBtn.active = paintable && HunterPaint.paint(s) >= 0;
		int tier = upgradable ? HunterPaint.tier(s) : 0;
		upgradeBtn.active = upgradable && tier < HunterPaint.MAX_TIER;
		upgradeBtn.setMessage(Text.literal(!upgradable ? "Upgrade" : tier >= HunterPaint.MAX_TIER ? "Maxed out" : "Upgrade to Lv " + (tier + 1)));
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, "HUNTER WORKBENCH", left + panelW / 2, top + 9, 0xFF66E0FF);

		// big preview of the held item, wearing the colour you're about to paint
		if (!s.isEmpty()) {
			ItemStack preview = s.copy();
			if (paintable) {
				NbtCompound n = preview.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
				n.putInt("Paint", color());
				preview.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
			}
			ctx.getMatrices().push();
			ctx.getMatrices().translate(left + 60 - 32, top + 34, 0);
			ctx.getMatrices().scale(4f, 4f, 1f);
			ctx.drawItem(preview, 0, 0);
			ctx.getMatrices().pop();
			drawFit(ctx, s.getName().getString(), left + 60, top + 104, 100, 0xFFFFFFFF);
		} else {
			drawFit(ctx, "Hold your gear", left + 60, top + 60, 100, 0xFFB8C8D0);
		}
		if (!paintable && !s.isEmpty()) drawFit(ctx, "Can't be painted", left + 60, top + 116, 100, 0xFFFF8866);
		if (upgradable) {
			drawFit(ctx, "Level " + tier + " / " + HunterPaint.MAX_TIER, left + 60, top + 116, 100, 0xFFFFE066);
			String what = s.isOf(com.phantompowers.ModItems.HUNTER_SUIT) ? "+armor, health, damage" : s.isOf(com.phantompowers.ModItems.HOVER_GLIDER) ? "+speed, laser power" : "+25% damage per level";
			drawFit(ctx, what, left + 60, top + 128, 100, 0xFF9AB4C0);
			if (tier < HunterPaint.MAX_TIER) {
				ctx.getMatrices().push();
				ctx.getMatrices().translate(left + 10, top + panelH - 44, 0);
				ctx.getMatrices().scale(0.7f, 0.7f, 1f);
				ctx.drawTextWithShadow(textRenderer, "Cost: " + HunterPaint.costText(tier + 1), 0, 0, 0xFFD8E4EA);
				ctx.getMatrices().pop();
			}
		}

		// colour picker
		ctx.fill(sqX - 1, sqY - 1, sqX + sqW + 1, sqY + sqH + 1, 0xFF000000);
		for (int x = 0; x < sqW; x++) {
			int bright = 0xFF000000 | (MathHelper.hsvToRgb(Math.min(hue, 0.9999f), x / (float) (sqW - 1), 1f) & 0xFFFFFF);
			ctx.fillGradient(sqX + x, sqY, sqX + x + 1, sqY + sqH, bright, 0xFF000000);
		}
		int mx = sqX + (int) (sat * (sqW - 1)), my = sqY + (int) ((1 - val) * (sqH - 1));
		ctx.fill(mx - 2, my - 2, mx + 3, my + 3, 0xFFFFFFFF);
		ctx.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000 | color());
		ctx.fill(sqX - 1, hueY - 1, sqX + sqW + 1, hueY + 7, 0xFF000000);
		for (int x = 0; x < sqW; x++) ctx.fill(sqX + x, hueY, sqX + x + 1, hueY + 6, 0xFF000000 | (MathHelper.hsvToRgb(x / (float) (sqW - 1), 1f, 1f) & 0xFFFFFF));
		int hx = sqX + (int) (hue * (sqW - 1));
		ctx.fill(hx - 1, hueY - 2, hx + 2, hueY + 8, 0xFFFFFFFF);
		int swY = hueY + 12, sw = Math.max(8, sqW / SWATCHES.length);
		for (int i = 0; i < SWATCHES.length; i++) {
			int x = sqX + i * sw;
			ctx.fill(x, swY, x + sw - 2, swY + 12, 0xFF000000);
			ctx.fill(x + 1, swY + 1, x + sw - 3, swY + 11, 0xFF000000 | SWATCHES[i]);
		}
		ctx.drawTextWithShadow(textRenderer, String.format("#%06X", color()), sqX, sqY - 11, 0xFFD8E4EA);
	}

	private void drawFit(DrawContext ctx, String str, int cx, int y, int maxW, int color) {
		int w = textRenderer.getWidth(str);
		float k = w > maxW ? maxW / (float) w : 1f;
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, y, 0);
		ctx.getMatrices().scale(k, k, 1f);
		ctx.drawTextWithShadow(textRenderer, str, -w / 2, 0, color);
		ctx.getMatrices().pop();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) return true;
		if (button != 0) return false;
		if (mx >= sqX && mx < sqX + sqW && my >= sqY && my < sqY + sqH) {
			dragging = 1;
			pick(mx, my);
			return true;
		}
		if (mx >= sqX && mx < sqX + sqW && my >= hueY - 2 && my < hueY + 8) {
			dragging = 2;
			pick(mx, my);
			return true;
		}
		int swY = hueY + 12, sw = Math.max(8, sqW / SWATCHES.length);
		if (my >= swY && my < swY + 12 && mx >= sqX && mx < sqX + sw * SWATCHES.length) {
			float[] hsv = GhostSuitScreen.toHsv(SWATCHES[(int) ((mx - sqX) / sw)]);
			hue = hsv[0];
			sat = hsv[1];
			val = hsv[2];
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragging != 0) {
			pick(mx, my);
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		dragging = 0;
		return super.mouseReleased(mx, my, button);
	}

	private void pick(double mx, double my) {
		if (dragging == 1) {
			sat = MathHelper.clamp((float) ((mx - sqX) / (sqW - 1)), 0f, 1f);
			val = MathHelper.clamp(1f - (float) ((my - sqY) / (sqH - 1)), 0f, 1f);
		} else if (dragging == 2) {
			hue = MathHelper.clamp((float) ((mx - sqX) / (sqW - 1)), 0f, 1f);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
