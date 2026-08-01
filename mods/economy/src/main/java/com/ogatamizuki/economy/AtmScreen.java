package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class AtmScreen extends Screen {
    private EditBox amountBox;
    private static final NumberFormat YEN_FORMAT = NumberFormat.getNumberInstance(Locale.JAPAN);

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

    protected AtmScreen() {
        super(Component.literal("ATM"));
    }

    @Override
    protected void init() {
        super.init();
        this.shortcutButtons.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. ALL ボタン（左：所持金用, 右：銀行残高用）
        int allWidth = 35;
        int allHeight = 16;
        this.addRenderableWidget(Button.builder(Component.literal("ALL"), button -> {
            int maxBalance = EconomyMod.getCurrentBalance();
            setAmountValue(maxBalance);
            stopHolding();
        }).bounds(centerX - 105, centerY - 32, allWidth, allHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("ALL"), button -> {
            int maxBankBalance = EconomyMod.getCurrentBankBalance();
            setAmountValue(maxBankBalance);
            stopHolding();
        }).bounds(centerX - 40, centerY - 32, allWidth, allHeight).build());

        // 2. 入力ボックス (中央)
        this.amountBox = new EditBox(this.font, centerX - 120, centerY - 10, 130, 20, Component.literal("金額"));
        this.amountBox.setMaxLength(15);
        this.amountBox.setValue("");
        this.amountBox.setResponder(this::onAmountBoxChanged);
        this.addRenderableWidget(this.amountBox);

        // クリアボタン (C)
        this.addRenderableWidget(Button.builder(Component.literal("C"), button -> {
            this.amountBox.setValue("");
            this.currentAmount = 0;
            stopHolding();
        }).bounds(centerX + 60, centerY + 34, 20, 20).build());

        // 3. 右側の加減算縦並びボタン (+-10,000, +-1,000, +-100)
        int rBtnX_add = centerX + 20;
        int rBtnX_sub = centerX + 72;
        int rBtnWidth = 48;
        int rBtnHeight = 16;
        int rSpacingY = 4;

        Button btnAdd10k = Button.builder(Component.literal("+10,000"), button -> startHolding(10000))
                .bounds(rBtnX_add, centerY - 25, rBtnWidth, rBtnHeight).build();
        Button btnSub10k = Button.builder(Component.literal("-10,000"), button -> startHolding(-10000))
                .bounds(rBtnX_sub, centerY - 25, rBtnWidth, rBtnHeight).build();

        Button btnAdd1k = Button.builder(Component.literal("+1,000"), button -> startHolding(1000))
                .bounds(rBtnX_add, centerY - 25 + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();
        Button btnSub1k = Button.builder(Component.literal("-1,000"), button -> startHolding(-1000))
                .bounds(rBtnX_sub, centerY - 25 + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();

        Button btnAdd100 = Button.builder(Component.literal("+100"), button -> startHolding(100))
                .bounds(rBtnX_add, centerY - 25 + 2 * (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build();
        Button btnSub100 = Button.builder(Component.literal("-100"), button -> startHolding(-100))
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

        // 4. アクションボタン（「預け入れ」「引き出し」）
        int actBtnWidth = 55;
        int actBtnHeight = 20;
        this.addRenderableWidget(Button.builder(Component.literal("預け入れ"), button -> handleDeposit())
                .bounds(centerX - 115, centerY + 25, actBtnWidth, actBtnHeight)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("引き出し"), button -> handleWithdraw())
                .bounds(centerX - 50, centerY + 25, actBtnWidth, actBtnHeight)
                .build());

        // 5. 閉じるボタン (画面中央に配置)
        this.addRenderableWidget(Button.builder(Component.literal("閉じる"), button -> this.onClose())
                .bounds(centerX - 40, centerY + 55, 80, 20)
                .build());
    }

    private boolean isUpdating = false;

    private void onAmountBoxChanged(String text) {
        if (isUpdating)
            return;
        isUpdating = true;

        // 数字以外の文字をすべて削除
        String clean = text.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) {
            this.currentAmount = 0;
            this.amountBox.setValue("");
        } else {
            try {
                long val = Long.parseLong(clean);
                int limit = Math.max(EconomyMod.getCurrentBalance(), EconomyMod.getCurrentBankBalance());
                if (val > limit) {
                    val = limit;
                }
                if (val > 999999999) {
                    val = 999999999;
                }
                this.currentAmount = (int) val;
                String formatted = YEN_FORMAT.format(val);
                this.amountBox.setValue(formatted);
            } catch (NumberFormatException e) {
                this.currentAmount = 0;
                this.amountBox.setValue("");
            }
        }
        isUpdating = false;
    }

    private void setAmountValue(int amount) {
        int limit = Math.max(EconomyMod.getCurrentBalance(), EconomyMod.getCurrentBankBalance());
        if (amount > limit) {
            amount = limit;
        }
        if (amount <= 0) {
            this.currentAmount = 0;
            this.amountBox.setValue("");
        } else {
            this.currentAmount = amount;
            this.amountBox.setValue(YEN_FORMAT.format(amount));
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
        // 背景を描画 (ダークオーバーレイ)
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // メインパネル外枠（幅 270px, 左右対称）
        drawMinecraftBevel(guiGraphics, centerX - 135, centerY - 88, centerX + 135, centerY + 85, false);
        // メインパネル上部のゴールドエッジライン
        guiGraphics.fill(centerX - 134, centerY - 87, centerX + 134, centerY - 86, 0xFFDFB323);

        // タイトル枠
        drawMinecraftBevel(guiGraphics, centerX - 130, centerY - 83, centerX + 130, centerY - 65, true);
        guiGraphics.fill(centerX - 130, centerY - 83, centerX + 130, centerY - 82, 0xFFDFB323);
        guiGraphics.centeredText(this.font, "§e§lBANK ATM", centerX, centerY - 78, 0xFFFFFFFF); // 中央補正なし（centerX）

        // 残高情報表示の背景枠 (1つの凹み枠に統合)
        drawMinecraftBevel(guiGraphics, centerX - 130, centerY - 60, centerX + 130, centerY - 40, true);
        
        String balanceText = "所持金: ¥" + YEN_FORMAT.format(EconomyMod.getCurrentBalance());
        String bankText = "銀行残高: ¥" + YEN_FORMAT.format(EconomyMod.getCurrentBankBalance());

        int balanceWidth = this.font.width(balanceText);
        // 残高表示位置の調整（枠内での位置調整, centerXから左右対称にマージン10px）
        int balanceX = (centerX - 10) - balanceWidth;
        int bankX = centerX + 10;

        guiGraphics.text(this.font, balanceText, balanceX, centerY - 54, 0xFF55FF55, true);
        guiGraphics.text(this.font, bankText, bankX, centerY - 54, 0xFF55FFFF, true);

        // ウィジェット等の描画（最前面に配置）
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();

        // 残高の同期監視と入力上限の自動追従
        int currentBal = EconomyMod.getCurrentBalance();
        int currentBankBal = EconomyMod.getCurrentBankBalance();
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
        int limit = Math.max(EconomyMod.getCurrentBalance(), EconomyMod.getCurrentBankBalance());
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
