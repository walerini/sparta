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

package spa.http;

import java.util.HashMap;
import java.util.Map;

public enum APITag {

    ACCOUNTS("Accounts"),
    BLOCKS("Blocks"),
    CREATE_TRANSACTION("Create Transaction"),
    FORGING("Forging"),
    NETWORK("Networking"),
    INFO("Server Info"),
    TOKENS("Tokens"),
    TRANSACTIONS("Transactions"),
    UTILS("Utils"),
    DEBUG("Debug");

    private static final Map<String, APITag> apiTags = new HashMap<>();
    static {
        for (APITag apiTag : values()) {
            if (apiTags.put(apiTag.getDisplayName(), apiTag) != null) {
                throw new RuntimeException("Duplicate APITag name: " + apiTag.getDisplayName());
            }
        }
    }

    public static APITag fromDisplayName(String displayName) {
        APITag apiTag = apiTags.get(displayName);
        if (apiTag == null) {
            throw new IllegalArgumentException("Invalid APITag name: " + displayName);
        }
        return apiTag;
    }

    private final String displayName;

    APITag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
