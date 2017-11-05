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

import java.util.Map;

public final class AccountRestrictions {

    static void init() {
    }

    static void checkTransaction(Transaction transaction, boolean validatingAtFinish) throws SpaException.NotCurrentlyValidException {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        if (senderAccount == null) {
            throw new SpaException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " does not exist yet");
        }
    }

    static boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
//        Account senderAccount = Account.getAccount(transaction.getSenderId());
//        if (!senderAccount.getControls().contains(Account.ControlType.PHASING_ONLY)) {
//            return false;
//        }
//        return transaction.getType() != TransactionType.AccountControl.SET_PHASING_ONLY &&
//                TransactionType.isDuplicate(TransactionType.AccountControl.SET_PHASING_ONLY, Long.toUnsignedString(senderAccount.getId()),
//                        duplicates, true);
    }

}
