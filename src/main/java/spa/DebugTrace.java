/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package spa;

import spa.util.Convert;
import spa.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DebugTrace {

    static final String QUOTE = Spa.getStringProperty("spa.debugTraceQuote", "\"");
    static final String SEPARATOR = Spa.getStringProperty("spa.debugTraceSeparator", "\t");
    static final boolean LOG_UNCONFIRMED = Spa.getBooleanProperty("spa.debugLogUnconfirmed");

    static void init() {
        List<String> accountIdStrings = Spa.getStringListProperty("spa.debugTraceAccounts");
        String logName = Spa.getStringProperty("spa.debugTraceLog");
        if (accountIdStrings.isEmpty() || logName == null) {
            return;
        }
        Set<Long> accountIds = new HashSet<>();
        for (String accountId : accountIdStrings) {
            if ("*".equals(accountId)) {
                accountIds.clear();
                break;
            }
            accountIds.add(Convert.parseAccountId(accountId));
        }
        final DebugTrace debugTrace = addDebugTrace(accountIds, logName);
        Spa.getBlockchainProcessor().addListener(block -> debugTrace.resetLog(), BlockchainProcessor.Event.RESCAN_BEGIN);
        Logger.logDebugMessage("Debug tracing of " + (accountIdStrings.contains("*") ? "ALL"
                : String.valueOf(accountIds.size())) + " accounts enabled");
    }

    public static DebugTrace addDebugTrace(Set<Long> accountIds, String logName) {
        final DebugTrace debugTrace = new DebugTrace(accountIds, logName);
        Account.addListener(account -> debugTrace.trace(account, false), Account.Event.BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addListener(account -> debugTrace.trace(account, true), Account.Event.UNCONFIRMED_BALANCE);
        }
        Spa.getBlockchainProcessor().addListener(debugTrace::traceBeforeAccept, BlockchainProcessor.Event.BEFORE_BLOCK_ACCEPT);
        Spa.getBlockchainProcessor().addListener(debugTrace::trace, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
        return debugTrace;
    }

    //NOTE: first and last columns should not have a blank entry in any row, otherwise VerifyTrace fails to parse the line
    private static final String[] columns = {"height", "event", "account", "asset", "currency", "balance", "unconfirmed balance",
            "asset balance", "unconfirmed asset balance", "currency balance", "unconfirmed currency balance",
            "transaction amount", "transaction fee", "generation fee", "effective balance", "dividend",
            "order", "order price", "order quantity", "order cost",
            "offer", "buy rate", "sell rate", "buy units", "sell units", "buy cost", "sell cost",
            "trade price", "trade quantity", "trade cost",
            "exchange rate", "exchange quantity", "exchange cost", "currency cost",
            "crowdfunding", "claim", "mint",
            "asset quantity", "currency units", "transaction", "lessee", "lessor guaranteed balance",
            "purchase", "purchase price", "purchase quantity", "purchase cost", "discount", "refund",
            "shuffling",
            "sender", "recipient", "block", "timestamp"};

    private static final Map<String,String> headers = new HashMap<>();
    static {
        for (String entry : columns) {
            headers.put(entry, entry);
        }
    }

    private final Set<Long> accountIds;
    private final String logName;
    private PrintWriter log;

    private DebugTrace(Set<Long> accountIds, String logName) {
        this.accountIds = accountIds;
        this.logName = logName;
        resetLog();
    }

    void resetLog() {
        if (log != null) {
            log.close();
        }
        try {
            log = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logName)))), true);
        } catch (IOException e) {
            Logger.logDebugMessage("Debug tracing to " + logName + " not possible", e);
            throw new RuntimeException(e);
        }
        this.log(headers);
    }

    private boolean include(long accountId) {
        return accountId != 0 && (accountIds.isEmpty() || accountIds.contains(accountId));
    }

    private void trace(Account account, boolean unconfirmed) {
        if (include(account.getId())) {
            log(getValues(account.getId(), unconfirmed));
        }
    }

    private void traceBeforeAccept(Block block) {
        long generatorId = block.getGeneratorId();
        if (include(generatorId)) {
            log(getValues(generatorId, block));
        }
    }

    private void trace(Block block) {
        for (Transaction transaction : block.getTransactions()) {
            long senderId = transaction.getSenderId();
            if (((TransactionImpl)transaction).attachmentIsPhased()) {
                if (include(senderId)) {
                    log(getValues(senderId, transaction, false, true, false));
                }
                continue;
            }
            if (include(senderId)) {
                log(getValues(senderId, transaction, false, true, true));
                log(getValues(senderId, transaction, transaction.getAttachment(), false));
            }
            long recipientId = transaction.getRecipientId();
            if (transaction.getAmountAPL() > 0 && recipientId == 0) {
                recipientId = Genesis.CREATOR_ID;
            }
            if (include(recipientId)) {
                log(getValues(recipientId, transaction, true, true, true));
                log(getValues(recipientId, transaction, transaction.getAttachment(), true));
            }
        }
    }

    private Map<String,String> getValues(long accountId, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Long.toUnsignedString(accountId));
        Account account = Account.getAccount(accountId);
        map.put("balance", String.valueOf(account != null ? account.getBalanceAPL() : 0));
        map.put("unconfirmed balance", String.valueOf(account != null ? account.getUnconfirmedBalanceAPL() : 0));
        map.put("timestamp", String.valueOf(Spa.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Spa.getBlockchain().getHeight()));
        map.put("event", unconfirmed ? "unconfirmed balance" : "balance");
        return map;
    }

    private Map<String,String> getValues(long accountId, Transaction transaction, boolean isRecipient, boolean logFee, boolean logAmount) {
        long amount = transaction.getAmountAPL();
        long fee = transaction.getFeeAPL();
        if (isRecipient) {
            fee = 0; // fee doesn't affect recipient account
        } else {
            // for sender the amounts are subtracted
            amount = - amount;
            fee = - fee;
        }
        if (fee == 0 && amount == 0) {
            return Collections.emptyMap();
        }
        Map<String,String> map = getValues(accountId, false);
        if (logAmount) {
            map.put("transaction amount", String.valueOf(amount));
        }
        if (logFee) {
            map.put("transaction fee", String.valueOf(fee));
        }
        map.put("transaction", transaction.getStringId());
        if (isRecipient) {
            map.put("sender", Long.toUnsignedString(transaction.getSenderId()));
        } else {
            map.put("recipient", Long.toUnsignedString(transaction.getRecipientId()));
        }
        map.put("event", "transaction");
        return map;
    }

    private Map<String,String> getValues(long accountId, Block block) {
        long fee = block.getTotalFeeAPL();
        if (fee == 0) {
            return Collections.emptyMap();
        }
        long totalBackFees = 0;
        Map<String,String> map = getValues(accountId, false);
        map.put("effective balance", String.valueOf(Account.getAccount(accountId).getEffectiveBalanceSPA()));
        map.put("generation fee", String.valueOf(fee - totalBackFees));
        map.put("block", block.getStringId());
        map.put("event", "block");
        map.put("timestamp", String.valueOf(block.getTimestamp()));
        map.put("height", String.valueOf(block.getHeight()));
        return map;
    }

    private Map<String,String> getValues(long accountId, Transaction transaction, Attachment attachment, boolean isRecipient) {
        return Collections.emptyMap();
    }

    private void log(Map<String,String> map) {
        if (map.isEmpty()) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        for (String column : columns) {
            if (!LOG_UNCONFIRMED && column.startsWith("unconfirmed")) {
                continue;
            }
            String value = map.get(column);
            if (value != null) {
                buf.append(QUOTE).append(value).append(QUOTE);
            }
            buf.append(SEPARATOR);
        }
        log.println(buf.toString());
    }

}
