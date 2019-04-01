package bio.overture.ego.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static bio.overture.ego.controller.AbstractControllerTest.MAPPER;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.AMPERSAND;
import static bio.overture.ego.utils.Joiners.PATH;
import static bio.overture.ego.utils.QueryParam.createQueryParam;
import static bio.overture.ego.utils.Streams.stream;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RequiredArgsConstructor
public class WebResource<T> {

  private static final ObjectMapper REGULAR_MAPPER = new ObjectMapper();
  private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();

  static {
    PRETTY_MAPPER.enable(INDENT_OUTPUT);
  }

  @NonNull private final TestRestTemplate restTemplate;
  @NonNull private final String serverUrl;
  @NonNull private final Class<T> responseType;

  private String endpoint;
  private Set<QueryParam> queryParams = newHashSet();
  private Object body;
  private HttpHeaders headers;
  private boolean enableLogging = false;
  private boolean pretty = false;

  public WebResource<T> endpoint(String formattedEndpoint, Object... args) {
    this.endpoint = format(formattedEndpoint, args);
    return this;
  }

  public WebResource<T> body(Object body) {
    this.body = body;
    return this;
  }

  public WebResource<T> headers(HttpHeaders httpHeaders) {
    this.headers = httpHeaders;
    return this;
  }

  public WebResource<T> logging() {
    return configLogging(true, false);
  }

  public WebResource<T> prettyLogging() {
    return configLogging(true, true);
  }

  public WebResource<T> queryParam(String key, Object... values) {
    queryParams.add(createQueryParam(key, values));
    return this;
  }

  public ResponseEntity<T> get() {
    return doRequest(null, HttpMethod.GET);
  }

  public ResponseEntity<T> put() {
    return doRequest(this.body, HttpMethod.PUT);
  }

  public ResponseEntity<T> post() {
    return doRequest(this.body, HttpMethod.POST);
  }

  public ResponseEntity<T> delete() {
    return doRequest(null, HttpMethod.DELETE);
  }

  public ResponseOption<T> deleteAnd() {
    return new ResponseOption<>(delete());
  }

  public ResponseOption<T> getAnd() {
    return new ResponseOption<>(get());
  }

  public ResponseOption<T> putAnd() {
    return new ResponseOption<>(put());
  }

  public ResponseOption<T> postAnd() {
    return new ResponseOption<>(post());
  }

  private WebResource<T> configLogging(boolean enable, boolean pretty) {
    this.enableLogging = enable;
    this.pretty = pretty;
    return this;
  }

  private Optional<String> getQuery() {
    val queryStrings = queryParams.stream().map(QueryParam::toString).collect(toImmutableSet());
    return queryStrings.isEmpty() ? Optional.empty() : Optional.of(AMPERSAND.join(queryStrings));
  }

  private String getUrl() {
    return PATH.join(this.serverUrl, this.endpoint) + getQuery().map(x -> "?" + x).orElse("");
  }

  @SneakyThrows
  private ResponseEntity<T> doRequest(Object body, HttpMethod httpMethod) {
    logRequest(enableLogging, pretty, httpMethod, getUrl(), body);
    val response =
        restTemplate.exchange(
            getUrl(), httpMethod, new HttpEntity<>(body, this.headers), this.responseType);
    logResponse(enableLogging, pretty, response);
    return response;
  }

  public static <T> WebResource<T> createWebResource(
      TestRestTemplate restTemplate, String serverUrl, Class<T> responseType) {
    return new WebResource<>(restTemplate, serverUrl, responseType);
  }

  @SneakyThrows
  private static void logRequest(
      boolean enable, boolean pretty, HttpMethod httpMethod, String url, Object body) {
    if (enable) {
      if (isNull(body)) {
        log.info("[REQUEST] {} {}", httpMethod, url);
      } else {
        if (pretty) {
          log.info(
              "[REQUEST] {} {} < \n{}", httpMethod, url, PRETTY_MAPPER.writeValueAsString(body));
        } else {
          log.info(
              "[REQUEST] {} {} < {}", httpMethod, url, REGULAR_MAPPER.writeValueAsString(body));
        }
      }
    }
  }

  @SneakyThrows
  private static <T> void logResponse(boolean enable, boolean pretty, ResponseEntity<T> response) {
    if (enable) {
      val output =
          CleanResponse.builder()
              .body(response.hasBody() ? response.getBody() : null)
              .statusCodeName(response.getStatusCode().name())
              .statusCodeValue(response.getStatusCodeValue())
              .build();
      if (pretty) {
        log.info("[RESPONSE] > \n{}", PRETTY_MAPPER.writeValueAsString(output));
      } else {
        log.info("[RESPONSE] > {}", REGULAR_MAPPER.writeValueAsString(output));
      }
    }
  }


  @RequiredArgsConstructor
  public static class ResponseOption<T> {

    @Getter
    @NonNull private final ResponseEntity<T> response;

    public ResponseOption<T> assertStatusCode(HttpStatus code) {
      assertThat(response.getStatusCode()).isEqualTo(code);
      return this;
    }

    public ResponseOption<T> assertOk() {
      assertStatusCode(OK);
      return this;
    }

    public ResponseOption<T> assertNotFound() {
      assertStatusCode(NOT_FOUND);
      return this;
    }

    public ResponseOption<T> assertConflict() {
      assertStatusCode(CONFLICT);
      return this;
    }

    public ResponseOption<T> assertBadRequest() {
      assertStatusCode(BAD_REQUEST);
      return this;
    }

    public ResponseOption<T> assertHasBody() {
      assertThat(response.hasBody()).isTrue();
      return this;
    }

    public <R> R map(Function<ResponseEntity<T>, R> transformingFunction) {
      return transformingFunction.apply(getResponse());
    }


    @SneakyThrows
    private static <T> List<T> extractPageResultSetFromResponse(ResponseEntity<String> r, Class<T> tClass) {
      assertThat(r.getStatusCode()).isEqualTo(OK);
      assertThat(r.getBody()).isNotNull();
      val page = MAPPER.readTree(r.getBody());
      assertThat(page).isNotNull();
      return stream(page.path("resultSet").iterator())
          .map(x -> MAPPER.convertValue(x, tClass))
          .collect(toImmutableList());
    }

    @SneakyThrows
    private static <T> T extractOneEntityFromResponse(ResponseEntity<String> r, Class<T> tClass) {
      assertThat(r.getStatusCode()).isEqualTo(OK);
      assertThat(r.getBody()).isNotNull();
      return MAPPER.readValue(r.getBody(), tClass);
    }

    @SneakyThrows
    private static <T> Set<T> extractManyEntitiesFromResponse(ResponseEntity<String> r, Class<T> tClass) {
      assertThat(r.getStatusCode()).isEqualTo(OK);
      assertThat(r.getBody()).isNotNull();
      return stream(MAPPER.readTree(r.getBody()).iterator())
          .map(x -> MAPPER.convertValue(x, tClass))
          .collect(toImmutableSet());
    }
  }

  public static class StringResponseOption extends ResponseOption<String>{

  }
}
