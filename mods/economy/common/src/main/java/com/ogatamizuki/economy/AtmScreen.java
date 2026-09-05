package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AtmScreen extends Screen {
    private EditBox amountBox;

    // 長押しオートリピート用の管理フィールド
    private int currentAmount = 0;
    private int activeChangeValue = 0;
    private int holdTicks = 0;

    // ショートカットボタンの参照リスト
    private final List<ButtonRef> shortcutButtons = new ArrayList<>();

    private static class ButtonRef {
        public final Button button;
        public final int value;

        public ButtonRef(Button button, int value) {
            this.button = button;
            this.value = value;
        }
    }

    private int cachedBalance = -1;
    private int cachedBankBalance = -1;
    /** Half-width of the outer ATM panel (JA 135 / EN wider for $ labels). */
    private int panelHalf = 135;

    protected AtmScreen() {
        super(EconomyMasterI18n.tr("economy.ui.atm.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.shortcutButtons.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cents = EconomyMasterI18n.useCents();
        this.panelHalf = cents ? 148 : 135;
        int panelRight = centerX + this.panelHalf;

        // 1. ALL ボタン（左：所持金用, 右：銀行残高用）
        int allWidth = 35;
        int allHeight = 16;
        this.addRenderableWidget(Button.builder(Component.literal("ALL"), button -> {
            int maxBalance = EconomyCommon.getCurrentBalance();
            setAmountValue(maxBalance);
            stopHolding();
        }).bounds(centerX - 105, centerY - 32, allWidth, allHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("ALL"), button -> {
            int maxBankBalance = EconomyCommon.getCurrentBankBalance();
            setAmountValue(maxBankBalance);
            stopHolding();
        }).bounds(centerX - 40, centerY - 32, allWidth, allHeight).build());

        // 3. 右側の加減算ボタン — パネル右端から内側に配置（はみ出し防止）
        int rBtnWidth = cents ? 50 : 48;
        int rBtnGap = 3;
        int rBtnMargin = 10;
        int rBtnX_sub = panelRight - rBtnMargin - rBtnWidth;
        int rBtnX_add = rBtnX_sub - rBtnGap - rBtnWidth;
        int rBtnHeight = 16;
        int rSpacingY = 4;

        // 2. 入力ボックス（加減算ボタン左端までに収める）
        int amountBoxRight = rBtnX_add - 6;
        int amountBoxLeft = centerX - 120;
        int amountBoxWidth = Math.max(100, amountBoxRight - amountBoxLeft);
        this.amountBox = new EditBox(this.font, amountBoxLeft, centerY - 10, amountBoxWidth, 20,
                EconomyMasterI18n.tr("economy.ui.amount"));
        this.amountBox.setMaxLength(15);
        this.amountBox.setValue("");
        this.amountBox.setResponder(this::onAmountBoxChanged);
        this.addRenderableWidget(this.amountBox);

        // クリアボタン (C) — 減算列の下
        this.addRenderableWidget(Button.builder(Component.literal("C"), button -> {
            this.amountBox.setValue("");
            this.currentAmount = 0;
            stopHolding();
        }).bounds(rBtnX_sub + (rBtnWidth - 20) / 2, centerY + 34, 20, 20).build());

        Button btnAdd10k = Button.builder(EconomyMasterI18n.amountDeltaComponent(10000), button -> startHolding(10000))
                .bounds(rBtnX_add, centerY - 25, rBtnWidth, rBtnHeight).build();
        Button btnSub10k = Button.builder(EconomyMasterI18n.amountDeltaComponent(-10000), button -> startHolding(-10000))
                .bounds(rBtnX_sub, centerY - 25, rBtnWidth, rBtnHeight).build();

        Button btnAdd1k = Button.builder(EconomyMasterI18n.amountDeltaComponent(1000), button -> startHolding(1000))
                .bounds(rBtnX_add, centerY - 25 + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();
        Button btnSub1k = Button.builder(EconomyMasterI18n.amountDeltaComponent(-1000), button -> startHolding(-1000))
                .bounds(rBtnX_sub, centerY - 25 + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();

        Button btnAdd100 = Button.builder(EconomyMasterI18n.amountDeltaComponent(100), button -> startHolding(100))
                .bounds(rBtnX_add, centerY - 25 + 2 * (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();
        Button btnSub100 = Button.builder(EconomyMasterI18n.amountDeltaComponent(-100), button -> startHolding(-100))
                .bounds(rBtnX_sub, centerY - 25 + 2 * (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();

        this.addRenderableWidget(btnAdd10k);
        this.addRenderableWidget(btnSub10k);
        this.addRenderableWidget(btnAdd1k);
        this.addRenderableWidget(btnSub1k);
        this.addRenderableWidget(btnAdd100);
        this.addRenderableWidget(btnSub100);

        this.shortcutButtons.add(new ButtonRef(btnAdd10k, 10000));
        this.shortcutButtons.add(new ButtonRef(btnSub10k, -10000));
        this.shortcutButtons.add(new ButtonRef(btnAdd1k, 1000));
        this.shortcutButtons.add(new ButtonRef(btnSub1k, -1000));
        this.shortcutButtons.add(new ButtonRef(btnAdd100, 100));
        this.shortcutButtons.add(new ButtonRef(btnSub100, -100));

        // 4. アクションボタン（EN は Withdraw が長いので幅を広げる）
        int actBtnWidth = cents ? 70 : 55;
        int actBtnHeight = 20;
        int actGap = 8;
        int actLeft = amountBoxLeft;
        this.addRenderableWidget(
                Button.builder(EconomyMasterI18n.tr("economy.ui.atm.deposit"), button -> handleDeposit())
                        .bounds(actLeft, centerY + 25, actBtnWidth, actBtnHeight)
                        .build());

        this.addRenderableWidget(
                Button.builder(EconomyMasterI18n.tr("economy.ui.atm.withdraw"), button -> handleWithdraw())
                        .bounds(actLeft + actBtnWidth + actGap, centerY + 25, actBtnWidth, actBtnHeight)
                        .build());

        // 5. 閉じるボタン
        this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), button -> this.onClose())
                .bounds(centerX - 40, centerY + 55, 80, 20)
                .build());
    }

    private boolean isUpdating = false;

    private void onAmountBoxChanged(String text) {
        if (isUpdating)
            return;
        isUpdating = true;

        String clean = text.replaceAll(EconomyMasterI18n.useCents() ? "[^0-9.]" : "[^0-9]", "");
        if (EconomyMasterI18n.useCents()) {
            int firstDot = clean.indexOf('.');
            if (firstDot != -1) {
                String before = clean.substring(0, firstDot + 1);
                String after = clean.substring(firstDot + 1).replace(".", "");
                if (after.length() > 2) {
                    after = after.substring(0, 2);
                }
                clean = before + after;
            }
        }

        if (clean.isEmpty()) {
            this.currentAmount = 0;
            this.amountBox.setValue("");
        } else {
            try {
                long val = EconomyMasterI18n.parseInputToRawValue(clean);
                int limit = Math.max(EconomyCommon.getCurrentBalance(), EconomyCommon.getCurrentBankBalance());
                if (val > limit) {
                    val = limit;
                    clean = EconomyMasterI18n.formatRawValueForInput(val);
                }
                if (val > 99999999999L) { // Supports larger cents bounds
                    val = 99999999999L;
                    clean = EconomyMasterI18n.formatRawValueForInput(val);
                }
                this.currentAmount = (int) val;
                if (!text.equals(clean)) {
                    this.amountBox.setValue(clean);
                }
            } catch (Exception e) {
                this.currentAmount = 0;
                this.amountBox.setValue("");
            }
        }
        isUpdating = false;
    }

    private void setAmountValue(int amount) {
        int limit = Math.max(EconomyCommon.getCurrentBalance(), EconomyCommon.getCurrentBankBalance());
        if (amount > limit) {
            amount = limit;
        }
        if (amount <= 0) {
            this.currentAmount = 0;
            this.amountBox.setValue("");
        } else {
            this.currentAmount = amount;
            this.amountBox.setValue(EconomyMasterI18n.formatRawValueForInput(amount));
        }
    }

    private void drawMinecraftBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        int topLeftColor, bottomRightColor, bgColor;
        if (sunken) {
            topLeftColor = 0xFF1F1F1F;
            bottomRightColor = 0xFF4A4A4A;
            bgColor = 0xFF161616;
        } else {
            topLeftColor = 0xFF5F5F5F;
            bottomRightColor = 0xFF1F1F1F;
            bgColor = 0xFF2A2A2A;
        }
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, topLeftColor);
        gui.fill(x1, y1, x1 + 1, y2, topLeftColor);
        gui.fill(x1, y2 - 1, x2, y2, bottomRightColor);
        gui.fill(x2 - 1, y1, x2, y2, bottomRightColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int half = this.panelHalf;
        int inner = half - 5;

        drawMinecraftBevel(guiGraphics, centerX - half, centerY - 88, centerX + half, centerY + 85, false);
        guiGraphics.fill(centerX - half + 1, centerY - 87, centerX + half - 1, centerY - 86, 0xFFDFB323);

        drawMinecraftBevel(guiGraphics, centerX - inner, centerY - 83, centerX + inner, centerY - 65, true);
        guiGraphics.fill(centerX - inner, centerY - 83, centerX + inner, centerY - 82, 0xFFDFB323);
        guiGraphics.centeredText(this.font, "§e§lBANK ATM", centerX, centerY - 78, 0xFFFFFFFF);

        drawMinecraftBevel(guiGraphics, centerX - inner, centerY - 60, centerX + inner, centerY - 40, true);

        String balanceText = EconomyMasterI18n
                .tr("economy.ui.atm.balance", EconomyMasterI18n.formatCurrency(EconomyCommon.getCurrentBalance()))
                .getString();
        String bankText = EconomyMasterI18n
                .tr("economy.ui.atm.bank_balance", EconomyMasterI18n.formatCurrency(EconomyCommon.getCurrentBankBalance()))
                .getString();

        int balanceWidth = this.font.width(balanceText);
        int balanceX = (centerX - 10) - balanceWidth;
        int bankX = centerX + 10;

        guiGraphics.text(this.font, balanceText, balanceX, centerY - 54, 0xFF55FF55, true);
        guiGraphics.text(this.font, bankText, bankX, centerY - 54, 0xFF55FFFF, true);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();

        // 残高の同期監視と入力上限の自動追従
        int currentBal = EconomyCommon.getCurrentBalance();
        int currentBankBal = EconomyCommon.getCurrentBankBalance();
        if (currentBal != this.cachedBalance || currentBankBal != this.cachedBankBalance) {
            this.cachedBalance = currentBal;
            this.cachedBankBalance = currentBankBal;
            int limit = Math.max(currentBal, currentBankBal);
            if (this.currentAmount > limit) {
                setAmountValue(limit);
            }
        }

        // オートリピート処理の更新
        Minecraft mc = Minecraft.getInstance();

        // LWJGL を用いて現在アクティブなスレッドの GLFW ウィンドウコンテキストを取得
        long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();

        // マウス左クリックが押されているか判定
        boolean mouseLDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (mouseLDown) {
            // 現在のマウス座標を取得して GUI スケールに変換
            double rawX = mc.mouseHandler.xpos();
            double rawY = mc.mouseHandler.ypos();
            double mouseX = rawX * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
            double mouseY = rawY * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();

            // 現在マウスがホバーしているショートカットボタンを検索
            ButtonRef activeBtn = null;
            for (ButtonRef ref : this.shortcutButtons) {
                if (ref.button.isMouseOver(mouseX, mouseY)) {
                    activeBtn = ref;
                    break;
                }
            }

            if (activeBtn != null) {
                // ホバーしているボタンの値をアクティブ値に設定
                if (this.activeChangeValue == activeBtn.value) {
                    this.holdTicks++;
                    // 10 ticks (0.5秒) 以上押し続けられたら、2 ticks (0.1秒) ごとに連続増減
                    if (this.holdTicks > 10 && this.holdTicks % 2 == 0) {
                        changeAmount(this.activeChangeValue);
                    }
                } else {
                    // 別のボタンに移動した場合は長押し状態を切り替え
                    startHolding(activeBtn.value);
                }
            } else {
                stopHolding();
            }
        } else {
            stopHolding();
        }
    }

    public void startHolding(int value) {
        this.activeChangeValue = value;
        this.holdTicks = 0;
        changeAmount(value);
    }

    public void stopHolding() {
        this.activeChangeValue = 0;
        this.holdTicks = 0;
    }

    private void changeAmount(int delta) {
        long nextAmount = (long) this.currentAmount + delta;

        // 所持金と銀行残高の最大値（Limit）を取得
        int limit = Math.max(EconomyCommon.getCurrentBalance(), EconomyCommon.getCurrentBankBalance());
        if (nextAmount > limit) {
            nextAmount = limit;
        }

        if (nextAmount < 0) {
            nextAmount = 0;
        } else if (nextAmount > 999999999) {
            nextAmount = 999999999;
        }

        setAmountValue((int) nextAmount);
    }

    private void handleDeposit() {
        if (this.currentAmount <= 0)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String uuid = mc.player.getUUID().toString();
            EconomyService.depositBank(uuid, this.currentAmount).thenRun(() -> {
                Minecraft.getInstance().execute(() -> {
                    setAmountValue(0);
                });
            });
        }
    }

    private void handleWithdraw() {
        if (this.currentAmount <= 0)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String uuid = mc.player.getUUID().toString();
            EconomyService.withdrawBank(uuid, this.currentAmount).thenRun(() -> {
                Minecraft.getInstance().execute(() -> {
                    setAmountValue(0);
                });
            });
        }
    }
}
