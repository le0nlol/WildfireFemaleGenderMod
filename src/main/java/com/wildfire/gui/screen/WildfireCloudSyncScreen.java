/*
	Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
	Copyright (C) 2023 WildfireRomeo

	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 3 of the License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.gui.screen;

import com.wildfire.gui.GuiUtils;
import com.wildfire.gui.WildfireButton;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireLocalization;
import com.wildfire.main.cloud.CloudSync;
import com.wildfire.main.cloud.SyncLog;
import com.wildfire.main.cloud.SyncingTooFrequentlyException;
import com.wildfire.main.config.GlobalConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
public class WildfireCloudSyncScreen extends BaseWildfireScreen {

	private static final Identifier BACKGROUND = Identifier.of(WildfireGender.MODID, "textures/gui/sync_bg_v2.png");

	private WildfireButton btnAutomaticSync = null;
	private WildfireButton btnSyncNow = null;
	private WildfireButton btnHelp = null;

	protected WildfireCloudSyncScreen(Screen parent, UUID uuid) {
		super(Text.translatable("wildfire_gender.cloud_settings"), parent, uuid);
	}

	@Override
	public void init() {
		int x = this.width / 2;
		int y = this.height / 2;
		int yPos = y - 47;
		int xPos = x - 156 / 2 - 1;


		this.addDrawableChild(new WildfireButton(xPos, yPos, 157, 20,
				Text.translatable("wildfire_gender.cloud.status", CloudSync.isEnabled() ? WildfireLocalization.ENABLED : WildfireLocalization.DISABLED),
				button -> {
					var config = GlobalConfig.INSTANCE;
					config.set(GlobalConfig.CLOUD_SYNC_ENABLED, !config.get(GlobalConfig.CLOUD_SYNC_ENABLED));
					button.setMessage(Text.translatable("wildfire_gender.cloud.status", CloudSync.isEnabled() ? WildfireLocalization.ENABLED : WildfireLocalization.DISABLED));
					btnAutomaticSync.setActive(CloudSync.isEnabled());
					btnAutomaticSync.setMessage(Text.translatable("wildfire_gender.cloud.automatic", CloudSync.isEnabled() ? (GlobalConfig.INSTANCE.get(GlobalConfig.AUTOMATIC_CLOUD_SYNC) ? WildfireLocalization.ENABLED : WildfireLocalization.DISABLED) : WildfireLocalization.OFF));
					btnSyncNow.visible = GlobalConfig.INSTANCE.get(GlobalConfig.CLOUD_SYNC_ENABLED);
				}));

		this.addDrawableChild(btnAutomaticSync = new WildfireButton(xPos, yPos + 20, 157, 20,
				Text.translatable("wildfire_gender.cloud.automatic", CloudSync.isEnabled() ? (GlobalConfig.INSTANCE.get(GlobalConfig.AUTOMATIC_CLOUD_SYNC) ? WildfireLocalization.ENABLED : WildfireLocalization.DISABLED) : WildfireLocalization.OFF),
				button -> {
					var config = GlobalConfig.INSTANCE;
					var newVal = !config.get(GlobalConfig.AUTOMATIC_CLOUD_SYNC);
					config.set(GlobalConfig.AUTOMATIC_CLOUD_SYNC, newVal);
					button.setMessage(Text.translatable("wildfire_gender.cloud.automatic", newVal ? WildfireLocalization.ENABLED : WildfireLocalization.DISABLED));
				}));
		btnAutomaticSync.setTooltip(Tooltip.of(Text.empty()
				.append(Text.translatable("wildfire_gender.cloud.automatic.tooltip.line1"))
				.append("\n\n")
				.append(Text.translatable("wildfire_gender.cloud.automatic.tooltip.line2"))));
		btnAutomaticSync.setActive(CloudSync.isEnabled());

		btnSyncNow = new WildfireButton(xPos + 98, yPos + 42, 60, 15, Text.translatable("wildfire_gender.cloud.sync"), this::sync);
		//btnSyncNow.setTooltip(Tooltip.of(Text.empty()
		//		.append(Text.literal("Sync Server data is cached for a minimum time of 30 minutes. If you do not see any changes please try to re-sync later."))));
		btnSyncNow.visible = GlobalConfig.INSTANCE.get(GlobalConfig.CLOUD_SYNC_ENABLED);
		this.addDrawableChild(btnSyncNow);

		this.addDrawableChild(new WildfireButton(this.width / 2 + 73, yPos - 11, 9, 9, Text.literal("X"),
				button -> close(), text -> GuiUtils.doneNarrationText()));

		this.addDrawableChild(btnHelp = new WildfireButton(this.width / 2 + 73 - 10, yPos - 11, 9, 9, Text.literal("?"),
				button -> {
					//client.setScreen(new WildfireCloudDetailsScreen(this, client.player.getUuid())); // Disabled for now. Not complete
					// BUTTON IS SUPPOSED TO DO NOTHING AT THE MOMENT
				}, text -> GuiUtils.doneNarrationText()));
		btnHelp.setTooltip(Tooltip.of(Text.translatable("wildfire_gender.cloud.disclaimer.line1")
				.append("\n\n")
				.append(Text.translatable("wildfire_gender.cloud.disclaimer.line2"))));

		super.init();
	}

	private void sync(ButtonWidget button) {
		button.active = false;
		button.setMessage(Text.translatable("wildfire_gender.cloud.syncing"));
		CompletableFuture.runAsync(() -> {
			try {
				CloudSync.sync(Objects.requireNonNull(getPlayer())).join();
				button.setMessage(Text.translatable("wildfire_gender.cloud.syncing.success"));
			} catch(Exception e) {
				var actualException = e instanceof CompletionException ce ? ce.getCause() : e;
				if(actualException instanceof SyncingTooFrequentlyException) {
					WildfireGender.LOGGER.warn("Failed to sync settings as we've already synced too recently");
					SyncLog.add(WildfireLocalization.SYNC_LOG_SYNC_TOO_FREQUENTLY);
				} else {
					WildfireGender.LOGGER.error("Failed to sync settings", actualException);
				}
				button.setMessage(Text.translatable("wildfire_gender.cloud.syncing.fail"));
			}
		});
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderInGameBackground(ctx);
		ctx.drawTexture(RenderLayer::getGuiTextured, BACKGROUND, (this.width - 172) / 2, (this.height - 124) / 2, 0, 0, 172, 144, 256, 256);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		if(client == null || client.world == null) return;
		super.render(ctx, mouseX, mouseY, delta);

		int x = this.width / 2;
		int y = this.height / 2;
		y -= 47;

		GuiUtils.drawScrollableTextWithoutShadow(GuiUtils.Justify.LEFT, ctx, textRenderer, getTitle(),
				x - 79, y - 12, x - 79 + 141, y - 11 + 10, 4473924);
		GuiUtils.drawScrollableTextWithoutShadow(GuiUtils.Justify.LEFT, ctx, textRenderer, Text.translatable("wildfire_gender.cloud.status_log"),
				x - 79, y + 47, x - 79 + 95, y + 48 + 10, 4473924);

		for(int i = SyncLog.SYNC_LOG.size() - 1; i >= 0; i--) {
			int reverseIndex = SyncLog.SYNC_LOG.size() - 1 - i;
			var entry = SyncLog.SYNC_LOG.get(i);

			if(reverseIndex < 6) {
				int ey = y + 110 - (reverseIndex * 10);
				GuiUtils.drawScrollableTextWithoutShadow(GuiUtils.Justify.LEFT, ctx, textRenderer, entry.text(),
						x - 78, ey, x - 78 + 156, ey + 10, entry.color());
			}
		}
	}

	@Override
	public void close() {
		GlobalConfig.INSTANCE.save();
		super.close();
	}
}