package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class LoanScreen extends Screen {
    private EditBox amountBox;
    private int currentAmount = 0;
    private boolean isUpdating = false;
    private boolean isProcessing = false;

    private int maxBorrowAmount = 0;
    private int maxAllowedDebt = 0;
    private boolean isLimitLoaded = false;
    private boolean isLimitLoading = true;

    private Button borrowButton;
    private Button repayButton;

    // ATM より一回り大きいパネル（情報2行分の余白確保）
    private static final int PANEL_HALF_W = 155;
    private static final int PANEL_INNER_HALF_W = 150;
    private static final int PANEL_TOP = -105;
    private static final int PANEL_BOTTOM = 115;
    private static final int TITLE_TOP = -100;
    private static final int TITLE_BOTTOM = -80;
    private static final int INFO_TOP = -78;
    private static final int INFO_BOTTOM = -48;
    private static final int INFO_LINE1_Y = -73;
    private static final int INFO_LINE2_Y = -58;
    private static final int ALL_Y = -36;
    private static final int INPUT_Y = -14;
    private static final int PRESET_TOP = -29;
    private static final int CLEAR_Y = 30;
    private static final int ACT_Y = 42;
    private static final int FOOTER_TOP = 68;
    private static final int FOOTER_BOTTOM = 84;
    private static final int FOOTER_TEXT_Y = 72;
    private static final int CLOSE_Y = 88;

    protected LoanScreen() {
        super(EconomyMasterI18n.tr("economy.ui.loan.title"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int allWidth = 35;
        int allHeight = 16;
        this.addRenderableWidget(Button.builder(Component.literal("ALL"), button -> {
            if (isLimitLoaded && maxBorrowAmount > 0) {
                setAmountValue(maxBorrowAmount);
            }
        }).bounds(centerX - 105, centerY + ALL_Y, allWidth, allHeight).build());

        this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.loan.repay_all"), button -> {
            int debt = EconomyMod.getCurrentDebt();
            int balance = EconomyMod.getCurrentBalance();
            setAmountValue(Math.min(debt, balance));
        }).bounds(centerX - 40, centerY + ALL_Y, allWidth, allHeight).build());

        this.amountBox = new EditBox(this.font, centerX - 120, centerY + INPUT_Y, 130, 20,
                EconomyMasterI18n.tr("economy.ui.amount"));
        this.amountBox.setMaxLength(15);
        this.amountBox.setValue("");
        this.amountBox.setResponder(this::onAmountBoxChanged);
        this.addRenderableWidget(this.amountBox);

        this.addRenderableWidget(Button.builder(Component.literal("C"), button -> {
            this.amountBox.setValue("");
            this.currentAmount = 0;
        }).bounds(centerX + 60, centerY + CLEAR_Y, 20, 20).build());

        int rBtnX_add = centerX + 20;
        int rBtnX_sub = centerX + 72;
        int rBtnWidth = 48;
        int rBtnHeight = 16;
        int rSpacingY = 4;

        this.addRenderableWidget(Button.builder(Component.literal("+10,000"), button -> changeAmount(10000))
                .bounds(rBtnX_add, centerY + PRESET_TOP, rBtnWidth, rBtnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("-10,000"), button -> changeAmount(-10000))
                .bounds(rBtnX_sub, centerY + PRESET_TOP, rBtnWidth, rBtnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1,000"), button -> changeAmount(1000))
                .bounds(rBtnX_add, centerY + PRESET_TOP + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("-1,000"), button -> changeAmount(-1000))
                .bounds(rBtnX_sub, centerY + PRESET_TOP + (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("+100"), button -> changeAmount(100))
                .bounds(rBtnX_add, centerY + PRESET_TOP + 2 * (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("-100"), button -> changeAmount(-100))
                .bounds(rBtnX_sub, centerY + PRESET_TOP + 2 * (rBtnHeight + rSpacingY), rBtnWidth, rBtnHeight).build());

        int actBtnWidth = 55;
        int actBtnHeight = 20;
        this.borrowButton = Button.builder(EconomyMasterI18n.tr("economy.ui.loan.borrow"), button -> handleBorrow())
                .bounds(centerX - 115, centerY + ACT_Y, actBtnWidth, actBtnHeight)
                .build();
        this.repayButton = Button.builder(EconomyMasterI18n.tr("economy.ui.loan.repay"), button -> handleRepay())
                .bounds(centerX - 50, centerY + ACT_Y, actBtnWidth, actBtnHeight)
                .build();

        this.addRenderableWidget(this.borrowButton);
        this.addRenderableWidget(this.repayButton);

        this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), button -> this.onClose())
                .bounds(centerX - 40, centerY + CLOSE_Y, 80, 20)
                .build());

        refreshLoanLimit();
        updateButtonStates();
    }

    private void refreshLoanLimit() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        isLimitLoading = true;
        isLimitLoaded = false;
        EconomyService.fetchLoanLimit(mc.player.getUUID().toString()).thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                isLimitLoading = false;
                if (json != null && !json.has("error")) {
                    maxAllowedDebt = json.has("maxAllowedDebt") ? json.get("maxAllowedDebt").getAsInt() : 0;
                    maxBorrowAmount = json.has("maxBorrowAmount") ? json.get("maxBorrowAmount").getAsInt() : 0;
                    isLimitLoaded = true;
                    // 非同期読み込み完了時、ユーザーが未入力(0)であれば上限額をデフォルトセットする
                    if (this.currentAmount == 0 && maxBorrowAmount > 0) {
                        setAmountValue(maxBorrowAmount);
                    }
                } else {
                    maxAllowedDebt = 0;
                    maxBorrowAmount = 0;
                }
                updateButtonStates();
            });
        });
    }

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
                if (val > 99999999999L) {
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
        updateButtonStates();
    }

    private void setAmountValue(int amount) {
        if (amount <= 0) {
            this.currentAmount = 0;
            this.amountBox.setValue("");
        } else {
            if (amount > 999999999) {
                amount = 999999999;
            }
            this.currentAmount = amount;
            this.amountBox.setValue(EconomyMasterI18n.formatRawValueForInput(amount));
        }
        updateButtonStates();
    }

    private void changeAmount(int delta) {
        long nextAmount = (long) this.currentAmount + delta;
        if (nextAmount < 0) {
            nextAmount = 0;
        } else if (nextAmount > 999999999) {
            nextAmount = 999999999;
        }
        setAmountValue((int) nextAmount);
    }

    private void updateButtonStates() {
        if (isProcessing) {
            this.borrowButton.active = false;
            this.repayButton.active = false;
            return;
        }

        boolean borrowWithinLimit = !isLimitLoaded || (this.currentAmount > 0 && this.currentAmount <= maxBorrowAmount);
        this.borrowButton.active = this.currentAmount > 0 && borrowWithinLimit && !isLimitLoading;

        int debt = EconomyMod.getCurrentDebt();
        int balance = EconomyMod.getCurrentBalance();
        this.repayButton.active = this.currentAmount > 0
                && this.currentAmount <= balance
                && this.currentAmount <= debt;
    }

    private void handleBorrow() {
        if (this.currentAmount <= 0 || isProcessing)
            return;
        if (isLimitLoaded && this.currentAmount > maxBorrowAmount)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.isProcessing = true;
            updateButtonStates();
            mc.getConnection().send(new LoanRequestPayload("BORROW", this.currentAmount));
        }
    }

    private void handleRepay() {
        if (this.currentAmount <= 0 || isProcessing)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.isProcessing = true;
            updateButtonStates();
            mc.getConnection().send(new LoanRequestPayload("REPAY", this.currentAmount));
        }
    }

    public void onTransactionResult(boolean success) {
        this.isProcessing = false;
        if (success) {
            setAmountValue(0);
            refreshLoanLimit();
        } else {
            updateButtonStates();
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

        drawMinecraftBevel(guiGraphics,
                centerX - PANEL_HALF_W, centerY + PANEL_TOP,
                centerX + PANEL_HALF_W, centerY + PANEL_BOTTOM, false);
        guiGraphics.fill(centerX - PANEL_HALF_W + 1, centerY + PANEL_TOP + 1,
                centerX + PANEL_HALF_W - 1, centerY + PANEL_TOP + 2, 0xFFFF4444);

        drawMinecraftBevel(guiGraphics,
                centerX - PANEL_INNER_HALF_W, centerY + TITLE_TOP,
                centerX + PANEL_INNER_HALF_W, centerY + TITLE_BOTTOM, true);
        guiGraphics.fill(centerX - PANEL_INNER_HALF_W, centerY + TITLE_TOP,
                centerX + PANEL_INNER_HALF_W, centerY + TITLE_TOP + 1, 0xFFFF4444);
        guiGraphics.centeredText(this.font, "§c§l闇金融 (LOAN SHARK)", centerX, centerY + TITLE_TOP + 5, 0xFFFFFFFF);

        drawMinecraftBevel(guiGraphics,
                centerX - PANEL_INNER_HALF_W, centerY + INFO_TOP,
                centerX + PANEL_INNER_HALF_W, centerY + INFO_BOTTOM, true);

        String balanceText = EconomyMasterI18n
                .tr("economy.ui.loan.balance", EconomyMasterI18n.formatCurrency(EconomyMod.getCurrentBalance()))
                .getString();
        String debtText = EconomyMasterI18n
                .tr("economy.ui.loan.debt", EconomyMasterI18n.formatCurrency(EconomyMod.getCurrentDebt())).getString();
        int balanceWidth = this.font.width(balanceText);
        guiGraphics.text(this.font, balanceText, (centerX - 10) - balanceWidth, centerY + INFO_LINE1_Y, 0xFF55FF55,
                true);
        guiGraphics.text(this.font, debtText, centerX + 10, centerY + INFO_LINE1_Y, 0xFFFF5555, true);

        if (isLimitLoading) {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.loan.loading"), centerX,
                    centerY + INFO_LINE2_Y, 0xFFAAAAAA);
        } else if (isLimitLoaded) {
            String limitText = EconomyMasterI18n.tr("economy.ui.loan.limit",
                    EconomyMasterI18n.formatCurrency(maxAllowedDebt), EconomyMasterI18n.formatCurrency(maxBorrowAmount))
                    .getString();
            guiGraphics.centeredText(this.font, limitText, centerX, centerY + INFO_LINE2_Y, 0xFFCCCCCC);
        } else {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.loan.load_fail"), centerX,
                    centerY + INFO_LINE2_Y, 0xFFFF5555);
        }

        drawMinecraftBevel(guiGraphics,
                centerX - PANEL_INNER_HALF_W, centerY + FOOTER_TOP,
                centerX + PANEL_INNER_HALF_W, centerY + FOOTER_BOTTOM, true);
        if (this.currentAmount > 0) {
            long repayPreview = Math.round(this.currentAmount * 1.1);
            String previewText = EconomyMasterI18n
                    .tr("economy.ui.loan.preview", EconomyMasterI18n.formatCurrency(repayPreview)).getString();
            int previewColor = isLimitLoaded
                    && Math.round(this.currentAmount * 1.1) + EconomyMod.getCurrentDebt() > maxAllowedDebt
                            ? 0xFFFF5555
                            : 0xFFFFBB33;
            guiGraphics.centeredText(this.font, previewText, centerX, centerY + FOOTER_TEXT_Y, previewColor);
        } else {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.loan.disclaimer"), centerX,
                    centerY + FOOTER_TEXT_Y, 0xFF777777);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
