package com.ogatamizuki.economy.backend.local;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;
import com.ogatamizuki.economy.master.EconomyMasterData;

/** 借金のローカル処理。 */
public final class EconomyLocalLoanService {
    private EconomyLocalLoanService() {
    }

    public static JsonObject fetchLimit(UUID playerUuid) {
        EconomyWorldSavedData data = EconomyLocalPlayerService.worldData();
        if (data == null) {
            return error("World data unavailable");
        }
        EconomyWorldSavedData.PlayerRecord record = data.getOrCreate(playerUuid, "");
        EconomyMasterData.LoanDebtLimitConfig config = EconomyMasterData.get().loanDebtLimit();
        int maxAllowedDebt = calculateMaxAllowedDebt(record.balance(), record.bankBalance(), config);
        int maxBorrowAmount = calculateMaxBorrowAmount(record.debt(), record.balance(), record.bankBalance(), config);

        JsonObject json = new JsonObject();
        json.addProperty("maxAllowedDebt", maxAllowedDebt);
        json.addProperty("maxBorrowAmount", maxBorrowAmount);
        json.addProperty("currentDebt", record.debt());
        json.addProperty("balance", record.balance());
        json.addProperty("bankBalance", record.bankBalance());
        return json;
    }

    public static JsonObject borrow(UUID playerUuid, int amount) {
        if (amount <= 0) {
            return error("Invalid borrow amount");
        }
        EconomyWorldSavedData data = EconomyLocalPlayerService.worldData();
        if (data == null) {
            return error("World data unavailable");
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        EconomyMasterData.LoanDebtLimitConfig config = EconomyMasterData.get().loanDebtLimit();
        int interestAdded = (int) Math.round(amount * 1.1);
        int maxAllowed = calculateMaxAllowedDebt(current.balance(), current.bankBalance(), config);
        if (current.debt() + interestAdded > maxAllowed) {
            return error("借入上限を超えています。");
        }

        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() + amount,
                current.bankBalance(),
                current.debt() + interestAdded,
                current.totalEarnings(),
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(playerUuid, updated);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("newBalance", updated.balance());
        result.addProperty("newDebt", updated.debt());
        result.addProperty("message", String.format("¥%,d 借り入れました。返済額は ¥%,d 増額されました。", amount, interestAdded));
        return result;
    }

    public static JsonObject repay(UUID playerUuid, int amount) {
        if (amount <= 0) {
            return error("Invalid repay amount");
        }
        EconomyWorldSavedData data = EconomyLocalPlayerService.worldData();
        if (data == null) {
            return error("World data unavailable");
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        int repayAmount = Math.min(amount, current.debt());
        if (repayAmount <= 0) {
            return error("返済する借金がありません。");
        }
        if (current.balance() < repayAmount) {
            return error("所持金が不足しています。");
        }

        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() - repayAmount,
                current.bankBalance(),
                current.debt() - repayAmount,
                current.totalEarnings(),
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(playerUuid, updated);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("newBalance", updated.balance());
        result.addProperty("newDebt", updated.debt());
        result.addProperty("message", String.format("¥%,d 返済しました。", repayAmount));
        return result;
    }

    static int calculateMaxAllowedDebt(int balance, int bankBalance, EconomyMasterData.LoanDebtLimitConfig config) {
        int totalAssets = balance + bankBalance;
        return Math.min(config.maxAmount(), (int) Math.floor(totalAssets * config.assetMultiplier()));
    }

    static int calculateMaxBorrowAmount(int currentDebt, int balance, int bankBalance, EconomyMasterData.LoanDebtLimitConfig config) {
        int maxAllowedDebt = calculateMaxAllowedDebt(balance, bankBalance, config);
        int remaining = maxAllowedDebt - currentDebt;
        if (remaining <= 0) {
            return 0;
        }
        int lo = 0;
        int hi = remaining;
        while (lo < hi) {
            int mid = (int) Math.ceil((lo + hi + 1) / 2.0);
            if (Math.round(mid * 1.1) <= remaining) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }
}
