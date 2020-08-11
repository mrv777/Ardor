/*
 * Copyright © 2020 Jelurida IP B.V.
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

package nxt.addons;

import nxt.util.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Load a contract and all its classes from data retrieved from the blockchain.
 * The protection domain represents the permissions granted to the contract by aggregating permissions from the policy file in the following order
 * 1. All default permissions provided by Java
 * 2. Permissions from the "file://untrustedContractCode" codebase
 * 3. Permissions from the "file://untrustedContractCode" codebase where signedBy is set to the public key of the account which uploaded the
 *    contract to the blockchain
 * 4. Permissions from the "file://untrustedContractCode" codebase where principal is set to {@link nxt.util.security.TransactionPrincipal} followed
 *    by the transaction hash (full hash or tagged data hash or hash of the contract data)
 */
public class CloudDataClassLoader extends SecureClassLoader {
    // Create fake codebase to map  contract permissions to the policy file
    private static final String UNTRUSTED_CONTRACT_CODE = "file://untrustedContractCode";
    private static final URL CODE_SOURCE_URL;
    static {
        try {
            CODE_SOURCE_URL = new URL(UNTRUSTED_CONTRACT_CODE);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private final ProtectionDomain protectionDomain;
    private final Map<String, byte[]> classFileData;

    public CloudDataClassLoader(Map<String, byte[]> classFileData, CodeSigner[] codeSigners, Principal[] principals) {
        this.classFileData = new HashMap<>(Objects.requireNonNull(classFileData));
        this.protectionDomain = new ProtectionDomain(new CodeSource(CODE_SOURCE_URL, codeSigners), null, this, principals);

        // Log the actual permissions assigned to this protection domain i.e. contract
        PermissionCollection permissions = Policy.getPolicy().getPermissions(protectionDomain);
        Enumeration<Permission> permissionEnumeration = permissions.elements();
        String impliedPermissions = Collections.list(permissionEnumeration).stream().map(Permission::toString).collect(Collectors.joining (","));
        Logger.logInfoMessage("CloudDataClassLoader implied permissions %s", impliedPermissions);
    }

    @Override
    protected Class<?> findClass(String name) {
        // Prevent usage of internal packages by this class loader
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                sm.checkPackageDefinition(name.substring(0, i));
            }
        }

        // Check if the class is already loaded
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        } else {
            // Define the class based on blockchain data
            byte[] bytes = classFileData.get(name);
            return defineClass(name, bytes, 0, bytes.length, protectionDomain);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // For a contract with many classes, we have no control over the loading order of the classes. For example if contract inner class X uses
        // contract inner class Y and X is loaded before Y. The class loader will implicitly try to load Y without calling findClass() on Y.
        // The code below makes sure that in this case Y is also loaded by this class loader.
        if (classFileData != null && classFileData.containsKey(name)) {
            Logger.logInfoMessage("delegate the loading of contract inner class %s back to the cloud data class loader, resolve %b", name, resolve);
            findClass(name);
        }

        // Load the class using the normal Java class loader hierarchy
        return super.loadClass(name, resolve);
    }
}
