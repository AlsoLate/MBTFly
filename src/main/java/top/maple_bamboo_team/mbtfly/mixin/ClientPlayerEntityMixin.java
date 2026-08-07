package top.maple_bamboo_team.mbtfly.mixin;

import top.maple_bamboo_team.mbtfly.util.AimingUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.maple_bamboo_team.mbtfly.client.MBTFlyClient;
import top.maple_bamboo_team.mbtfly.client.flight.FlightControl;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.gui.screen.TitleScreen;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Unique
    private static final double Y_TOLERANCE = 0.5;
    @Unique
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE).withZone(ZoneId.systemDefault());
    @Unique
    private static final Text PREFIX = Text.literal("[Maple Client] [MBTFly] ");
    @Unique
    private int autoExitCountdown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (client == null || client.world == null || player == null) {
            return;
        }

        if (FlightControl.enabled) {
            client.options.forwardKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }

        if (autoExitCountdown > 0) {
            autoExitCountdown--;
            if (autoExitCountdown % 20 == 0) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6在 §f" + (autoExitCountdown / 20) + " §6秒后自动退出..."), false);
            }
            if (autoExitCountdown <= 0) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6正在退出"), false);
                new Thread(() -> {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    client.execute(() -> {
                        if (client.world != null) {
                            client.world.disconnect(Text.literal("MBTFly: 到达目的地,自动退出"));
                            client.disconnect(Text.literal("MBTFly: 到达目的地,自动退出"));
                            client.setScreen(new TitleScreen());
                        }
                    });
                }).start();
                autoExitCountdown = -1;
            }
            return;
        }

        if (FlightControl.enabled && MBTFlyClient.destination != null) {
            Vec3d playerPos = player.getEntityPos();

            Vec3d flatPlayerPos = new Vec3d(playerPos.x, MBTFlyClient.destination.y, playerPos.z);
            double flatDistance = flatPlayerPos.distanceTo(MBTFlyClient.destination);

            double totalDistance = playerPos.distanceTo(MBTFlyClient.destination);

            boolean ascentIssue = flatDistance <= MBTFlyClient.detectionRange && playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE;
            boolean reachedDestination = totalDistance <= MBTFlyClient.detectionRange;

            if (ascentIssue || reachedDestination) {
                FlightControl.enabled = false;

                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);

                Instant endTime = Instant.now();
                Duration flightDuration = Duration.between(MBTFlyClient.startTime, endTime).minus(MBTFlyClient.pausedDuration);
                double originalTotalDistance = MBTFlyClient.startPos.distanceTo(MBTFlyClient.destination);

                long totalSeconds = flightDuration.getSeconds();
                long days = totalSeconds / (24 * 3600);
                long hours = (totalSeconds % (24 * 3600)) / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                StringBuilder timeString = new StringBuilder();
                if (days > 0) timeString.append(days).append("天");
                if (hours > 0) timeString.append(hours).append("时");
                if (minutes > 0) timeString.append(minutes).append("分");
                timeString.append(seconds).append("秒");

                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a============================================="), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a            MBTFly 自动飞行数据统计             "), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a============================================="), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §b玩家名: §f" + MBTFlyClient.playerName), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §b开始时间: §f" + formatter.format(MBTFlyClient.startTime)), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §b结束时间: §f" + formatter.format(endTime)), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §b总耗时: §f" + timeString.toString()), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §b总路程: §f" + String.format("%.2f 格", originalTotalDistance)), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a============================================="), false);

                if (ascentIssue) {
                    player.sendMessage(Text.literal("[Maple Client] [MBTFly] \n§c没有足够的距离拉升到目标高度,请指定足够远的距离以拉升高度"), false);
                }

                if (MBTFlyClient.autoExitEnabled && !MBTFlyClient.autoExitTriggered) {
                    MBTFlyClient.autoExitTriggered = true;
                    autoExitCountdown = 200;
                    player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6已到达目的地, 10秒后自动退出游戏"), false);
                }
            } else if (flatDistance <= MBTFlyClient.detectionRange) {
                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.backKey.setPressed(false);

                if (playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(true);
                    client.options.sneakKey.setPressed(false);
                } else if (playerPos.y > MBTFlyClient.destination.y + Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(true);
                } else {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                }
            } else {
                double yaw = AimingUtils.getYaw(playerPos, MBTFlyClient.destination);
                double pitch = AimingUtils.getPitch(playerPos, MBTFlyClient.destination);
                double yawDiff = yaw - player.getYaw();
                double pitchDiff = pitch - player.getPitch();

                if (yawDiff > 180) {
                    yawDiff -= 360;
                } else if (yawDiff < -180) {
                    yawDiff += 360;
                }

                float newYaw = (float) (player.getYaw() + yawDiff);
                float newPitch = (float) (player.getPitch() + pitchDiff);

                player.setYaw(newYaw);
                player.setPitch(newPitch);

                client.options.forwardKey.setPressed(true);

                if (playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(true);
                    client.options.sneakKey.setPressed(false);
                } else if (playerPos.y > MBTFlyClient.destination.y + Y_TOLERANCE) {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(true);
                } else {
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                }
            }
        }
    }
}