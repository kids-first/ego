package org.overture.ego.provider.orcid;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.overture.ego.token.IDToken;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class ORCIDTokenServiceTest {

    @Test
    public void testGetResult() throws IOException {

        String body = getResourceFileAsString("/__files/ok.xml");

        ORCIDTokenService service = new ORCIDTokenService();
        service.init();
        assertThat(service.getResult(body)).isEqualTo(new ORCIDResult(new IDToken("s.garcia@orcid.org", "Sofia", "Garcia")));

    }

    @Test
    public void testGetResult_noEmail() throws IOException {

        String body = getResourceFileAsString("/__files/no_email.xml");

        ORCIDTokenService service = new ORCIDTokenService();
        service.init();
        assertThat(service.getResult(body)).isEqualTo(new ORCIDResult("email.empty"));

    }

    @Test
    public void testGetResult_noVerifiedEmail() throws IOException {

        String body = getResourceFileAsString("/__files/no_verified_email.xml");

        ORCIDTokenService service = new ORCIDTokenService();
        service.init();
        assertThat(service.getResult(body)).isEqualTo(new ORCIDResult("email.not.verified"));

    }

    private String getResourceFileAsString(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResource(fileName), "UTF-8");
    }
}
