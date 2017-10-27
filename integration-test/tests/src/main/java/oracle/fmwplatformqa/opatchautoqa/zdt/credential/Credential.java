package oracle.fmwplatformqa.opatchautoqa.zdt.credential;

import java.io.Serializable;

public class Credential implements Serializable {
    private String _username;
    private transient char[] _password;

    public Credential() {
    }

    public Credential(String username, char[] password) {
        this._username = username;
        this._password = password;
    }

    public String getUsername() {
        return this._username;
    }

    public void setUsername(String username) {
        this._username = username;
    }

    public char[] getPassword() {
        return this._password;
    }

    public void setPassword(char[] password) {
        this._password = password;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Credential)) {
            return false;
        }
        Credential that = (Credential) o;
        if (this._username != null ? !this._username.equals(that._username) : that._username != null) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return this._username != null ? this._username.hashCode() : 0;
    }
}
