package net.carbonmc.graphene.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * 管理「末影珍珠强制加载区块」的单例工具类。
 * 同时作为 Forge SavedData 保存到 level.dat，支持重启恢复。
 */
@Mod.EventBusSubscriber
public final class PearlsChunkMap extends SavedData {

    private static final String DATA_NAME = "graphene_pearls";
    private static final Logger LOGGER = LogManager.getLogger();

    // 维度 -> (玩家UUID -> 区块集合)
    private final SetMultimap<UUID, ChunkPos> map = HashMultimap.create();

    /* ---------- 单例 ---------- */

    public static PearlsChunkMap INSTANCE;

    /** 获取当前维度实例（懒加载） */
    public static PearlsChunkMap get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(PearlsChunkMap::load, PearlsChunkMap::new, DATA_NAME);
    }

    /* ---------- 对外 API ---------- */

    /** 珍珠落地：把区块标为强制加载 */
    public void add(UUID owner, ChunkPos pos, ServerLevel level) {
        if (!CoolConfig.FIX_PEARL_LEAK.get()) return;
        get(level).map.put(owner, pos);
        level.setChunkForced(pos.x, pos.z, true);
        get(level).setDirty();
        LOGGER.debug("Pearl forced chunk {} in {}", pos, level.dimension().location());
    }

    /** 珍珠销毁/玩家死亡：卸载对应区块 */
    public void remove(UUID owner, ServerLevel level) {
        PearlsChunkMap data = get(level);
        Set<ChunkPos> set = data.map.removeAll(owner);
        if (set != null) {
            set.forEach(p -> level.setChunkForced(p.x, p.z, false));
            data.setDirty();
            LOGGER.debug("Unforced {} chunks for {}", set.size(), owner);
        }
    }

    /* ---------- 事件监听 ---------- */

    /** 玩家死亡后自动清理 */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone evt) {
        if (evt.isWasDeath() && evt.getEntity().level() instanceof ServerLevel sl) {
            UUID uuid = evt.getOriginal().getUUID();
            get(sl).remove(uuid, sl);
        }
    }

    /** 服务器 Tick 结束：定期保存（可选） */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END && evt.level instanceof ServerLevel sl) {
            if (sl.getServer().getTickCount() % 6000 == 0) {   // 5 分钟
                get(sl).setDirty();
            }
        }
    }

    /* ---------- SavedData 序列化 ---------- */

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        map.asMap().forEach((uuid, poses) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", uuid);
            ListTag chunks = new ListTag();
            poses.forEach(p -> chunks.add(StringTag.valueOf(p.toString())));
            entry.put("Chunks", chunks);
            list.add(entry);
        });
        tag.put("Entries", list);
        return tag;
    }

    /** 反序列化 */
    public static PearlsChunkMap load(CompoundTag tag) {
        PearlsChunkMap data = new PearlsChunkMap();
        ListTag list = tag.getList("Entries", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID owner = entry.getUUID("Owner");
            ListTag chunks = entry.getList("Chunks", 8);
            for (int j = 0; j < chunks.size(); j++) {
                String s = chunks.getString(j);
                ChunkPos pos = new ChunkPos(Long.parseLong(s));
                data.map.put(owner, pos);
            }
        }
        return data;
    }

    /* ---------- 重启恢复 ---------- */

    /** 服务器启动时，为每个维度恢复强制加载 */
    public static void onServerStarted(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PearlsChunkMap data = get(level);
            data.map.asMap().forEach((uuid, poses) ->
                    poses.forEach(p -> level.setChunkForced(p.x, p.z, true))
            );
            LOGGER.info("Graphene: restored {} forced chunks in {}",
                    data.map.size(), level.dimension().location());
        }
    }
}