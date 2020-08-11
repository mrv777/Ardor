/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.blockchain.chaincontrol;

import nxt.Constants;
import nxt.Nxt;
import nxt.blockchain.ChildChain;
import nxt.db.DbClause;
import nxt.db.DbClause.IntClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbKey.LongIntKeyFactory;
import nxt.db.VersionedEntityDbTable;
import nxt.dbschema.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static nxt.db.DbClause.LongClause;

class ChildChainPermissionStore {
    private static final PermissionKeyFactory permissionDbKeyFactory = new PermissionKeyFactory();

    static void init() {
    }

    private final VersionedEntityDbTable<ChildChainPermission> permissionTable;

    ChildChainPermissionStore(ChildChain childChain) {
        permissionTable = createVersionedEntityDbTable(childChain);
    }

    ChildChainPermission get(long accountId, PermissionType name) {
        DbClause clause = new LongClause("account_id", accountId)
                .and(new IntClause("permission_id", name.getId()));

        return permissionTable.getBy(clause);
    }

    DbIterator<ChildChainPermission> get(long accountId, int from, int to) {
        DbClause clause = new LongClause("account_id", accountId);

        return permissionTable.getManyBy(clause, from, to, " ORDER BY permission_id DESC ");
    }

    private DbIterator<ChildChainPermission> getAll(int from, int to) {
        return permissionTable.getAll(from, to, " ORDER BY height DESC, db_id DESC ");
    }

    DbIterator<ChildChainPermission> getPermissions(PermissionType permissionType, long granterId, int minHeight, int from, int to) {
        DbClause clause = null;
        if (permissionType != null) {
            clause = new IntClause("permission_id", permissionType.getId());
        }
        if (granterId != 0) {
            DbClause granterClause = new LongClause("granter_id", granterId);
            clause = clause == null ? granterClause : clause.and(granterClause);
        }
        if (minHeight >= 0) {
            DbClause heightClause = new IntClause("height", DbClause.Op.GTE, minHeight);
            clause = clause == null ? heightClause : clause.and(heightClause);
        }
        return clause == null ? getAll(from, to) : permissionTable.getManyBy(clause, from, to, " ORDER BY height DESC, db_id DESC ");
    }

    void remove(long accountId, PermissionType name) {
        permissionTable.delete(get(accountId, name));
    }

    // for testing
    void removeAll() {
        permissionTable.truncateAll();
    }

    void removeByGranter(PermissionType name, long granterId, int height) {
        if (name == null || granterId == 0) {
            throw new IllegalArgumentException("Permission name and granterId must be specified");
        }
        int deleted;
        do {
            deleted = 0;
            try (DbIterator<ChildChainPermission> permissions = getPermissions(name, granterId, height, 0, Constants.BATCH_COMMIT_SIZE)) {
                for (ChildChainPermission permission : permissions) {
                    permissionTable.delete(permission);
                    deleted += 1;
                }
            }
            if (deleted >= Constants.BATCH_COMMIT_SIZE) {
                Db.db.commitTransaction();
            }
        } while (deleted > 0);

    }

    ChildChainPermission save(long accountId, PermissionType name, long granterId) {
        DbKey dbKey = permissionDbKeyFactory.newKey(accountId, name);
        ChildChainPermission stored = permissionTable.get(dbKey);
        if (stored != null) {
            stored.setGranterId(granterId);
        } else {
            stored = new ChildChainPermission(accountId, name, granterId, Nxt.getBlockchain().getHeight());
        }
        permissionTable.insert(stored);
        return stored;
    }

    ChildChainPermission saveInitial(long accountId, PermissionType name, long granterId) {
        ChildChainPermission permission = new ChildChainPermission(accountId, name, granterId, -1);
        permissionTable.insertInitial(permission);
        return permission;
    }

    private static class PermissionKeyFactory extends LongIntKeyFactory<ChildChainPermission> {
        PermissionKeyFactory() {
            super("account_id", "permission_id");
        }

        @Override
        public DbKey newKey(ChildChainPermission permission) {
            return newKey(permission.getAccountId(), permission.getPermissionType().getId());
        }

        DbKey newKey(long accountId, PermissionType name) {
            return newKey(accountId, name.getId());
        }
    }


    private static VersionedEntityDbTable<ChildChainPermission> createVersionedEntityDbTable(ChildChain childChain) {
        return new VersionedEntityDbTable<ChildChainPermission>(childChain.getSchemaTable("account_permission"), permissionDbKeyFactory) {
            @Override
            protected ChildChainPermission load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
                return new ChildChainPermission(
                        rs.getLong("account_id"),
                        PermissionType.fromId(rs.getInt("permission_id")),
                        rs.getLong("granter_id"),
                        rs.getInt("height"));
            }

            @Override
            protected void save(Connection con, ChildChainPermission permission) throws SQLException {
                try (PreparedStatement statement = con.prepareStatement(
                        "MERGE INTO account_permission (account_id, permission_id, granter_id, height, latest) " +
                                "KEY (account_id, permission_id, height) " +
                                "VALUES (                            ?,             ?,          ?,      ?, TRUE) ")) {
                    int i = 0;
                    statement.setLong(++i, permission.getAccountId());
                    statement.setInt(++i, permission.getPermissionType().getId());
                    statement.setLong(++i, permission.getGranterId());
                    statement.setInt(++i, Nxt.getBlockchain().getHeight());
                    statement.executeUpdate();
                }
            }
        };
    }
}