package org.overture.ego.provider.orcid;

import lombok.Data;
import org.overture.ego.token.IDToken;

@Data
public class ORCIDResult {

    IDToken token;
    boolean isValid;
    String error;

    ORCIDResult(IDToken token) {
        this.token = token;
        this.isValid = true;
    }

    ORCIDResult(String error) {
        this.error = error;
        this.isValid = false;
    }

    public boolean isValid() {
        return isValid;
    }
}
