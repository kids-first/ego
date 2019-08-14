package org.overture.ego.provider.orcid;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.overture.ego.token.IDToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.xpath.AbstractXPathTemplate;
import org.springframework.xml.xpath.Jaxp13XPathTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ORCIDTokenService {

    @Value("${orcid.client.id}")
    private String clientId;
    @Value("${orcid.client.secret}")
    private String clientSecret;
    @Value("${orcid.client.accessTokenUri}")
    private String accessTokenUri;


    @Value("${orcid.client.timeout.connect}")
    private int connectTimeout;
    @Value("${orcid.client.timeout.read}")
    private int readTimeout;
    @Value("${orcid.client.userInfoUri}")
    private String userInfoUri;
    @Value("${orcid.client.redirectUri}")
    private String redirectUri;

    private RestTemplate orcidConnector;
    private AbstractXPathTemplate xpathTemplate;

    private final static List<MediaType> ORCID_XML_MEDIATYPE = Collections.singletonList(MediaType.valueOf("application/vnd.orcid+xml"));

    @PostConstruct
    protected void init() {
        orcidConnector = new RestTemplate(httpRequestFactory());

        xpathTemplate = new Jaxp13XPathTemplate();
        val namespaces = new HashMap<String, String>();
        namespaces.put("person", "http://www.orcid.org/ns/person");
        namespaces.put("personal-details", "http://www.orcid.org/ns/personal-details");
        namespaces.put("email", "http://www.orcid.org/ns/email");
        xpathTemplate.setNamespaces(namespaces);
    }

    public ORCIDResult getAuthInfo(String code) {
        JsonNode authInfo = exchangeCodeForToken(code);

        String accessToken = authInfo.get("access_token").asText();
        String orcId = authInfo.get("orcid").asText();

        ResponseEntity<String> response = getUserInfo(accessToken, orcId);
        return getResult(response.getBody());

    }

    @NotNull
    private ResponseEntity<String> getUserInfo(String accessToken, String orcId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(ORCID_XML_MEDIATYPE);
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        String uri = userInfoUri.replace("_ID_", orcId);
        return orcidConnector.exchange(uri, HttpMethod.GET, entity, String.class);
    }

    ORCIDResult getResult(String body) {

        val givenName = xpathTemplate.evaluateAsString("/person:person/person:name/personal-details:given-names[1]", new StringSource(body));
        val familyName = xpathTemplate.evaluateAsString("/person:person/person:name/personal-details:family-name[1]", new StringSource(body));
        val email = xpathTemplate.evaluateAsString("/person:person/email:emails/email:email[@primary='true']/email:email[1]", new StringSource(body));

        if (email.isEmpty()) {
            return new ORCIDResult("email.empty");
        } else {
            val emailVerified = xpathTemplate.evaluateAsString("/person:person/email:emails/email:email[@primary='true'][@verified='true']/email:email[1]", new StringSource(body));
            if (emailVerified.isEmpty()) {
                return new ORCIDResult("email.not.verified");
            }
            return new ORCIDResult(new IDToken(emailVerified, givenName, familyName));
        }

    }

    private JsonNode exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        return Objects.requireNonNull(orcidConnector.exchange(accessTokenUri, HttpMethod.POST, entity, JsonNode.class)
                .getBody());
    }

    private HttpComponentsClientHttpRequestFactory httpRequestFactory() {
        try {
            val factory = new HttpComponentsClientHttpRequestFactory();
            //Enforce TLS v1.2 : https://stackoverflow.com/questions/52836065/i-o-error-on-post-request-for-java-net-socketexception-connection-reset/55333280#55333280
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(context)
                    .evictIdleConnections(30, TimeUnit.SECONDS)
                    .build();
            factory.setConnectTimeout(connectTimeout);
            factory.setReadTimeout(readTimeout);
            factory.setHttpClient(httpClient);
            return factory;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

}
