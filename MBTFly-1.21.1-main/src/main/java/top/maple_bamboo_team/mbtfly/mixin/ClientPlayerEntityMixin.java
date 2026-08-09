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
    @Unique
    private boolean elytraLowDurabilityWarning = false;
    
    // 新增：落地检测相关变量
    @Unique
    private boolean isOnGround = false;
    @Unique
    private Instant landTime = null;
    @Unique
    private boolean wasInAir = true;
    @Unique
    private int autoExitCountdownAfterLanding = 0;
    @Unique
    private static final long FIVE_MINUTES_IN_TICKS = 20L * 60L * 5L; // 5分钟 = 6000 ticks

    @Unique
    private boolean isElytraLowDurability(ClientPlayerEntity player) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestplate.getItem() == Items.ELYTRA) {
            int durability = chestplate.getDamage();
            int maxDurability = chestplate.getMaxDamage();
            return durability >= maxDurability * 0.93; // 耐久不足7%
        }
        return false;
    }

    @Unique
    private Vec3d findNearestLandingSpot(ClientPlayerEntity player) {
        Vec3d playerPos = player.getPos();
        World world = player.getEntityWorld();
        
        // 搜索半径
        int searchRadius = 100;
        int bestY = -1;
        Vec3d bestPos = null;
        double minDistance = Double.MAX_VALUE;
        
        // 在XZ平面上搜索
        for (int x = (int)(playerPos.x - searchRadius); x <= (int)(playerPos.x + searchRadius); x += 10) {
            for (int z = (int)(playerPos.z - searchRadius); z <= (int)(playerPos.z + searchRadius); z += 10) {
                // 从当前高度向下搜索地面
                for (int y = (int)playerPos.y; y >= 0; y--) {
                    if (!world.isAir(new net.minecraft.util.math.BlockPos(x, y, z))) {
                        // 找到地面，检查是否是安全的着陆点
                        if (y > 0 && world.isAir(new net.minecraft.util.math.BlockPos(x, y + 1, z))) {
                            Vec3d landingPos = new Vec3d(x, y + 1, z);
                            double distance = playerPos.distanceTo(landingPos);
                            
                            // 选择最近的陆地
                            if (distance < minDistance) {
                                minDistance = distance;
                                bestPos = landingPos;
                                bestY = y;
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

        // 检查是否在末地
        boolean isInEnd = player.getEntityWorld().getRegistryKey().getValue().equals(net.minecraft.util.Identifier.of("minecraft", "the_end"));
        
        // 如果在末地且正在飞行，检查鞘翅耐久
        if (isInEnd && FlightControl.enabled && isElytraLowDurability(player)) {
            if (!elytraLowDurabilityWarning) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §c警告: 鞘翅耐久不足7%，正在寻找最近的着陆点..."), false);
                elytraLowDurabilityWarning = true;
            }
            
            // 寻找最近的着陆点
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

        // 新增：落地检测和自动退出逻辑
        boolean currentlyOnGround = player.isOnGround();
        
        // 检测落地状态变化
        if (currentlyOnGround && !wasInAir) {
            // 刚落地
            if (!isOnGround) {
                isOnGround = true;
                landTime = Instant.now();
                autoExitCountdownAfterLanding = (int) FIVE_MINUTES_IN_TICKS; // 5分钟倒计时
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §a已落地，5分钟后将自动退出游戏"), false);
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §7请在5分钟内完成操作，否则将自动退出"), false);
            }
        } else if (!currentlyOnGround) {
            // 在空中
            isOnGround = false;
            landTime = null;
            autoExitCountdownAfterLanding = 0;
        }
        
        wasInAir = !currentlyOnGround;
        
        // 处理落地后自动退出倒计时
        if (isOnGround && autoExitCountdownAfterLanding > 0) {
            autoExitCountdownAfterLanding--;
            
            if (autoExitCountdownAfterLanding % 3000 == 0) { // 每2.5分钟（3000 ticks）提示一次
                long remainingSeconds = autoExitCountdownAfterLanding / 20;
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §6将在 §f" + (remainingSeconds / 60) + "分" + (remainingSeconds % 60) + "秒 §6后自动退出游戏"), false);
            }
            
            if (autoExitCountdownAfterLanding <= 0) {
                player.sendMessage(Text.literal("[Maple Client] [MBTFly] §2已落地5分钟，正在自动退出游戏..."), false);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    client.execute(() -> {
                        if (client.world != null) {
                            client.world.disconnect();
                            client.setScreen(new TitleScreen());
                        }
                    });
                }).start();
                autoExitCountdownAfterLanding = -1; // 防止重复执行
            }
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
                            client.world.disconnect();
                            client.setScreen(new TitleScreen());
                        }
                    });
                }).start();
                autoExitCountdown = -1;
            }
            return;
        }

        if (FlightControl.enabled && MBTFlyClient.destination != null) {
            Vec3d playerPos = player.getPos();

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