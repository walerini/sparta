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

import spa.db.BasicDb;
import spa.db.TransactionalDb;

public final class Db {

    public static final TransactionalDb db = new TransactionalDb(
            new BasicDb.DbProperties().maxCacheSize(Spa.getIntProperty("spa.dbCacheKB"))
                                      .dbUrl(Spa.getStringProperty("spa.dbUrl"))
                                      .dbType(Spa.getStringProperty("spa.dbType"))
                                      .dbDir(Spa.getStringProperty("spa.dbDir"))
                                      .dbParams(Spa.getStringProperty("spa.dbParams"))
                                      .dbUsername(Spa.getStringProperty("spa.dbUsername"))
                                      .dbPassword(Spa.getStringProperty("spa.dbPassword", null, true))
                                      .maxConnections(Spa.getIntProperty("spa.maxDbConnections"))
                                      .loginTimeout(Spa.getIntProperty("spa.dbLoginTimeout"))
                                      .defaultLockTimeout(Spa.getIntProperty("spa.dbDefaultLockTimeout") * 1000)
                                      .maxMemoryRows(Spa.getIntProperty("spa.dbMaxMemoryRows"))
    );

    static void init() {
        db.init(new SpaDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {}

}
