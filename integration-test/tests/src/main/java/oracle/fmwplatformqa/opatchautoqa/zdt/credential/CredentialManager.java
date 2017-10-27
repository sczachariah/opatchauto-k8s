package oracle.fmwplatformqa.opatchautoqa.zdt.credential;


import oracle.security.pki.OracleSecretStore;
import oracle.security.pki.OracleWallet;
import oracle.security.pki.exception.OracleSecretNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CredentialManager {
    public static final String DEFAULT_PROTOCOL = "ssh";
    private static final String KEY_STORE_MAP_NAME = "oracle";
    private File _walletLocation;
    private char[] _walletPassword;
    private OracleWallet _wallet;
    private Map<String, Credential> _cache = new HashMap();

    public void setWallet(File walletLocation, char[] walletPassword) throws Exception {
        this._walletLocation = walletLocation;
        this._walletPassword = walletPassword;
        if (this._walletLocation == null) {
            throw new NullPointerException("The wallet location provided was null.");
        }
        if (!this._walletLocation.exists()) {
            throw new Exception("The wallet location provided did not exist " + this._walletLocation);
        }
        try {
            this._wallet = new OracleWallet();
            this._wallet.open(this._walletLocation.getAbsolutePath(), this._walletPassword);
        } catch (IOException e) {
            throw new Exception("Failure occurred while attempting to open wallet at location " + this._walletLocation, e);
        }
    }

    public Credential getCredential(String host) throws Exception {
        return getCredential(host, null);
    }

    public Credential getCredential(String host, String protocol) throws Exception {
        Credential credential = null;
        if (!(host.isEmpty() || host == null)) {
            if (protocol.isEmpty() || protocol == null) {
                protocol = DEFAULT_PROTOCOL;
            }
            String key = getCredentialKey(host, protocol);
            credential = (Credential) this._cache.get(key);
            if (credential == null) {
                OracleWallet wallet = getWallet();
                if (wallet != null) {
                    try {
                        OracleSecretStore oracleSecretStore = wallet.getSecretStore();

                        credential = new Credential(oracleSecretStore.getUsernameCredential("oracle", key), oracleSecretStore.getPasswordCredential("oracle", key));
                        this._cache.put(key, credential);
                    } catch (OracleSecretNotFoundException e) {
                        System.out.println("Failed to locate credential for " + key);
                    } catch (Exception e) {
                        throw new Exception("A failure occurred while trying to get credentials from wallet [" + this._walletLocation + "]. Please ensure the correct wallet password was provided.", e);
                    }
                }
            }
        }
        return credential;
    }

    public void addCredential(String alias, String username, char[] password) {
        addCredential(alias, DEFAULT_PROTOCOL, username, password);
    }

    public void addCredential(String alias, String protocol, String username, char[] password) {
        Credential credential = new Credential(username, password);
        if(protocol.equals("ssh") || protocol.equals("wls"))
        this._cache.put(getCredentialKey(alias, protocol), credential);
        else
            this._cache.put(alias, credential);
    }

//    public void addCredential(String alias, String String username, char[] password) {
//        Credential credential = new Credential(username, password);
//        this._cache.put(alias, credential);
//    }

    public String getCredentialKey(String host, String protocol) {
        return host + ":" + protocol;
    }

    public File getWalletLocation() {
        return this._walletLocation;
    }

    public char[] getWalletPassword() {
        return this._walletPassword;
    }

    public OracleWallet getWallet() {
        return this._wallet;
    }
}

