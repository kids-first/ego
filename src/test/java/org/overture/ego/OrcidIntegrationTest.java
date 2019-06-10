package org.overture.ego;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AuthorizationServiceMain.class)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
public class OrcidIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void normalProcess() {

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(equalTo("client_id=APP-1234567890&client_secret=5f63d1c5-3f00-4fa5-b096-fd985ffd0df7&grant_type=authorization_code&code=123456&redirect_uri=http%3A%2F%2Flocalhost%3A8888%2Forcid"))
                .willReturn(okJson(
                            "{" +
                                    "   \"access_token\":\"f5af9f51-07e6-4332-8f1a-c0c11c1e3728\",\"token_type\":\"bearer\",\n" +
                                    "   \"refresh_token\":\"f725f747-3a65-49f6-a231-3e8944ce464d\",\"expires_in\":631138518,\n" +
                                    "   \"scope\":\"/authorize\",\"name\":\"Sofia Garcia\",\"orcid\":\"0000-0002-9227-8514\"" +
                                    "}"
                        )));

        stubFor(get(urlEqualTo("/v2.1/0000-0002-9227-8514/person"))
                .withHeader("Accept", equalTo("application/vnd.orcid+xml"))
                .withHeader("Authorization", equalTo("Bearer f5af9f51-07e6-4332-8f1a-c0c11c1e3728"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/vnd.orcid+xml")
                        .withBodyFile("ok.xml")));



        HttpHeaders headers = new HttpHeaders();
        headers.add("code", "123456");
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = this.restTemplate.exchange("/oauth/orcid/token", HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("eyJhbGciOiJSUzI1NiJ9.");
    }

    @Test
    public void noEmail() {

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(equalTo("client_id=APP-1234567890&client_secret=5f63d1c5-3f00-4fa5-b096-fd985ffd0df7&grant_type=authorization_code&code=123456&redirect_uri=http%3A%2F%2Flocalhost%3A8888%2Forcid"))
                .willReturn(okJson(
                        "{" +
                                "   \"access_token\":\"f5af9f51-07e6-4332-8f1a-c0c11c1e3728\",\"token_type\":\"bearer\",\n" +
                                "   \"refresh_token\":\"f725f747-3a65-49f6-a231-3e8944ce464d\",\"expires_in\":631138518,\n" +
                                "   \"scope\":\"/authorize\",\"name\":\"Sofia Garcia\",\"orcid\":\"0000-0002-9227-8514\"" +
                                "}"
                )));

        stubFor(get(urlEqualTo("/v2.1/0000-0002-9227-8514/person"))
                .withHeader("Accept", equalTo("application/vnd.orcid+xml"))
                .withHeader("Authorization", equalTo("Bearer f5af9f51-07e6-4332-8f1a-c0c11c1e3728"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/vnd.orcid+xml")
                        .withBodyFile("no_email.xml")));



        HttpHeaders headers = new HttpHeaders();
        headers.add("code", "123456");
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = this.restTemplate.exchange("/oauth/orcid/token", HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("email.empty");
    }
    @Test
    public void errorFromOrcid() {

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(equalTo("client_id=APP-1234567890&client_secret=5f63d1c5-3f00-4fa5-b096-fd985ffd0df7&grant_type=authorization_code&code=123456&redirect_uri=http%3A%2F%2Flocalhost%3A8888%2Forcid"))
                .willReturn(okJson(
                        "{" +
                                "   \"access_token\":\"f5af9f51-07e6-4332-8f1a-c0c11c1e3728\",\"token_type\":\"bearer\",\n" +
                                "   \"refresh_token\":\"f725f747-3a65-49f6-a231-3e8944ce464d\",\"expires_in\":631138518,\n" +
                                "   \"scope\":\"/authorize\",\"name\":\"Sofia Garcia\",\"orcid\":\"0000-0002-9227-8514\"" +
                                "}"
                )));

        stubFor(get(urlEqualTo("/v2.1/0000-0002-9227-8514/person"))
                .withHeader("Accept", equalTo("application/vnd.orcid+xml"))
                .withHeader("Authorization", equalTo("Bearer f5af9f51-07e6-4332-8f1a-c0c11c1e3728"))
                .willReturn(aResponse()
                        .withStatus(500)));



        HttpHeaders headers = new HttpHeaders();
        headers.add("code", "123456");
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = this.restTemplate.exchange("/oauth/orcid/token", HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
