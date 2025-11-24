package com.mmorpg.mining;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 新しい採掘システムのリスナー
 * 鉱石(ArmorStand)への右クリックを検知してManagerに採掘セッションの開始を依頼する
 */
public class MiningListener implements Listener {

    private final MiningManager miningManager;

    public MiningListener(MiningManager miningManager) {
        this.miningManager = miningManager;
    }

    @EventHandler
    public void onOreInteract(PlayerInteractAtEntityEvent event) {
        // メインハンドのインタラクションでなければ無視（二重実行防止）
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // 対象が鉱石(ArmorStand)でなければ無視
        if (!miningManager.isOre(event.getRightClicked())) {
            return;
        }

        // デフォルトの動作（アーマースタンドのインベントリを開くなど）をキャンセル
        event.setCancelled(true);

        Player player = event.getPlayer();
        ArmorStand ore = (ArmorStand) event.getRightClicked();

        // Managerに採掘セッションの開始を依頼
        miningManager.startMiningSession(player, ore);
    }
}
