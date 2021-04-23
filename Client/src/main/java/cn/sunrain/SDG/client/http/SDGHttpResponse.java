package cn.sunrain.SDG.client.http;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SDGHttpResponse<T> {



    private final int statusCode;
    private final T entity;
    private final Map<String, String> headers;
    private final URI location;

    protected SDGHttpResponse(int statusCode, T entity) {
        this.statusCode = statusCode;
        this.entity = entity;
        this.headers = null;
        this.location = null;
    }

    private SDGHttpResponse(SDGHttpResponseBuilder<T> builder) {
        this.statusCode = builder.statusCode;
        this.entity = builder.entity;
        this.headers = builder.headers;

        if (headers != null) {
            String locationValue = headers.get(HttpHeaders.LOCATION);
            try {
                this.location = locationValue == null ? null : new URI(locationValue);
            } catch (URISyntaxException e) {
                throw new SDGHttpException("Invalid Location header value in response; cannot complete the request (location="
                        + locationValue + ')', e);
            }
        } else {
            this.location = null;
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public URI getLocation() {
        return location;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? Collections.<String, String>emptyMap() : headers;
    }

    public T getEntity() {
        return entity;
    }

    public static SDGHttpResponse<Void> status(int status) {
        return new SDGHttpResponse<>(status, null);
    }

    public static SDGHttpResponseBuilder<Void> anSDGHttpResponse(int statusCode) {
        return new SDGHttpResponseBuilder<>(statusCode);
    }

    public static <T> SDGHttpResponseBuilder<T> anSDGHttpResponse(int statusCode, Class<T> entityType) {
        return new SDGHttpResponseBuilder<T>(statusCode);
    }

    public static <T> SDGHttpResponseBuilder<T> anSDGHttpResponse(int statusCode, T entity) {
        return new SDGHttpResponseBuilder<T>(statusCode).entity(entity);
    }

    public static class SDGHttpResponseBuilder<T> {

        private final int statusCode;
        private T entity;
        private Map<String, String> headers;

        private SDGHttpResponseBuilder(int statusCode) {
            this.statusCode = statusCode;
        }

        public SDGHttpResponseBuilder<T> entity(T entity) {
            this.entity = entity;
            return this;
        }

        public SDGHttpResponseBuilder<T> entity(T entity, MediaType contentType) {
            return entity(entity).type(contentType);
        }

        public SDGHttpResponseBuilder<T> type(MediaType contentType) {
            headers(HttpHeaders.CONTENT_TYPE, contentType.toString());
            return this;
        }

        public SDGHttpResponseBuilder<T> headers(String name, Object value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(name, value.toString());
            return this;
        }

        public SDGHttpResponseBuilder<T> headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public SDGHttpResponse<T> build() {
            return new SDGHttpResponse<T>(this);
        }
    }
    
}
