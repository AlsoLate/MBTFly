package top.maple_bamboo_team.mbtfly.mixin;

import top.maple_bamboo_team.mbtfly.util.AimingUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.Identifier;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Unique
    private static final double Y_TOLERANCE = 0.5;
    @Unique
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE)
            .withZone(ZoneId.systemDefault());
    @Unique
    private static final long FIVE_MINUTES_IN_TICKS = 20L * 60L * 5L; // 6000 ticks

    @Unique
    private int autoExitCountdown = 0;               // 到达目的地后10秒退出倒计时
    @Unique
    private boolean elytraLowDurabilityWarning = false;

    // 落地倒计时相关（使用上一帧地面状态）
    @Unique
    private boolean prevOnGround = true;             // 初始假设在地面，防止飞行启动时误触发
    @Unique
    private int landingCountdown = 0;                // 落地5分钟倒计时（tick数）

    @Unique
    private boolean isElytraLowDurability(ClientPlayerEntity player) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestplate.getItem() == Items.ELYTRA) {
            int durability = chestplate.getDamage();
            int maxDurability = chestplate.getMaxDamage();
            return durability >= maxDurability * 0.95; // 耐久低于5%
        }
        return false;
    }

    @Unique
    private Vec3d findNearestLandingSpot(ClientPlayerEntity player) {
        Vec3d playerPos = player.getPos();
        World world = player.getEntityWorld();
        int searchRadius = 100;
        Vec3d bestPos = null;
        double minDistance = Double.MAX_VALUE;

        for (int x = (int)(playerPos.x - searchRadius); x <= (int)(playerPos.x + searchRadius); x += 10) {
            for (int z = (int)(playerPos.z - searchRadius); z <= (int)(playerPos.z + searchRadius); z += 10) {
                for (int y = (int)playerPos.y; y >= 0; y--) {
                    if (!world.isAir(new net.minecraft.util.math.BlockPos(x, y, z))) {
                        if (y > 0 && world.isAir(new net.minecraft.util.math.BlockPos(x, y + 1, z))) {
                            Vec3d landingPos = new Vec3d(x, y + 1, z);
                            double distance = playerPos.distanceTo(landingPos);
                            if (distance < minDistance) {
                                minDistance = distance;
                                bestPos = landingPos;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return bestPos;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (client == null || client.world == null || player == null) {
            return;
        }

        boolean isInEnd = player.getEntityWorld().getRegistryKey().getValue()
                .equals(Identifier.of("minecraft", "the_end"));

        // ========== 1. 鞘翅耐久低于5%强制降落 ==========
        if (isInEnd && FlightControl.enabled && isElytraLowDurability(player)) {
            if (!elytraLowDurabilityWarning) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §c警告: 鞘翅耐久不足5%，正在寻找最近的着陆点..."), false);
                elytraLowDurabilityWarning = true;
            }
            Vec3d landingSpot = findNearestLandingSpot(player);
            if (landingSpot != null) {
                MBTFlyClient.destination = landingSpot;
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a找到着陆点: §b" +
                        String.format("%.1f", landingSpot.x) + ", " +
                        String.format("%.1f", landingSpot.y) + ", " +
                        String.format("%.1f", landingSpot.z)), false);
            } else {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §c未找到合适的着陆点，请手动降落"), false);
            }
        } else {
            elytraLowDurabilityWarning = false;
        }

        // ========== 2. 落地5分钟退出倒计时（仅飞行中且未暂停时触发） ==========
        boolean currentlyOnGround = player.isOnGround();

        // 2.1 落地事件（从空中 → 地面）
        if (FlightControl.enabled && !MBTFlyClient.isPaused && currentlyOnGround && !prevOnGround) {
            // 进入倒计时，重置为5分钟
            landingCountdown = (int) FIVE_MINUTES_IN_TICKS;
            player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a已落地，5分钟后将自动退出游戏"), false);
            player.sendMessage(Text.literal("[Maple Client] [MBTFly] §7请在5分钟内完成操作，否则将自动退出"), false);
        }

        // 2.2 离地事件（地面 → 空中）：取消倒计时
        if (!currentlyOnGround && prevOnGround) {
            landingCountdown = 0;
        }

        // 更新上一帧状态
        prevOnGround = currentlyOnGround;

        // 2.3 倒计时递减 / 暂停 / 停止处理
        if (landingCountdown > 0) {
            if (FlightControl.enabled && !MBTFlyClient.isPaused) {
                // 飞行进行中且未暂停 → 递减
                landingCountdown--;
                if (landingCountdown % 3000 == 0) {
                    long remainingSeconds = landingCountdown / 20;
                    player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6将在 §f" +
                            (remainingSeconds / 60) + "分" + (remainingSeconds % 60) + "秒 §6后自动退出游戏"), false);
                }
                if (landingCountdown <= 0) {
                    player.sendMessage(Text.literal("[Maple Client] [MBTFly] §2已落地5分钟，正在自动退出游戏..."), false);
                    new Thread(() -> {
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                        client.execute(() -> {
                            if (client.world != null) {
                                client.world.disconnect();
                                client.setScreen(new TitleScreen());
                            }
                        });
                    }).start();
                    landingCountdown = -1; // 防重复触发
                }
            } else if (!FlightControl.enabled && !MBTFlyClient.isPaused) {
                // 飞行已完全停止 → 清零倒计时
                landingCountdown = 0;
            }
            // 暂停状态（enabled=true, isPaused=true）→ 不递减也不清零，保留剩余时间
        }

        // ========== 3. 飞行控制：释放所有按键 ==========
        if (FlightControl.enabled) {
            client.options.forwardKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }

        // ========== 4. 到达目的地10秒退出倒计时 ==========
        if (autoExitCountdown > 0) {
            autoExitCountdown--;
            if (autoExitCountdown % 20 == 0) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6在 §f" + (autoExitCountdown / 20) + " §6秒后自动退出..."), false);
            }
            if (autoExitCountdown <= 0) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6正在退出"), false);
                new Thread(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    client.execute(() -> {
                        if (client.world != null) {
                            client.world.disconnect();
                            client.setScreen(new TitleScreen());
                        }
                    });
                }).start();
                autoExitCountdown = -1;
            }
            return; // 退出倒计时期间不执行飞行逻辑
        }

        // ========== 5. 自动飞行主逻辑 ==========
        if (FlightControl.enabled && MBTFlyClient.destination != null) {
            Vec3d playerPos = player.getPos();
            Vec3d flatPlayerPos = new Vec3d(playerPos.x, MBTFlyClient.destination.y, playerPos.z);
            double flatDistance = flatPlayerPos.distanceTo(MBTFlyClient.destination);
            double totalDistance = playerPos.distanceTo(MBTFlyClient.destination);

            boolean ascentIssue = flatDistance <= MBTFlyClient.detectionRange &&
                    playerPos.y < MBTFlyClient.destination.y - Y_TOLERANCE;
            boolean reachedDestination = totalDistance <= MBTFlyClient.detectionRange;

            if (ascentIssue || reachedDestination) {
                FlightControl.enabled = false;
                // 飞行停止 → 清除落地倒计时（若存在）
                landingCountdown = 0;
                prevOnGround = player.isOnGround(); // 重置状态，避免后续误触

                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);

                Instant endTime = Instant.now();
                Duration flightDuration = Duration.between(MBTFlyClient.startTime, endTime)
                        .minus(MBTFlyClient.pausedDuration);
                double originalTotalDistance = MBTFlyClient.startPos.distanceTo(MBTFlyClient.destination);

                long totalSeconds = flightDuration.getSeconds();
                long days = totalSeconds / (24 * 3600);
                long hours = (totalSeconds % (24 * 3600)) / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60; // 修正：之前误写为 % 3600

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
                // 水平已到达，仅调整垂直
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
                // 水平未到达，转向并前进
                double yaw = AimingUtils.getYaw(playerPos, MBTFlyClient.destination);
                double pitch = AimingUtils.getPitch(playerPos, MBTFlyClient.destination);
                double yawDiff = yaw - player.getYaw();
                double pitchDiff = pitch - player.getPitch();

                if (yawDiff > 180) yawDiff -= 360;
                else if (yawDiff < -180) yawDiff += 360;

                player.setYaw((float)(player.getYaw() + yawDiff));
                player.setPitch((float)(player.getPitch() + pitchDiff));

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