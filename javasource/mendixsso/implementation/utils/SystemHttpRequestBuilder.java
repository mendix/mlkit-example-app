package mendixsso.implementation.utils;

import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.systemwideinterfaces.core.IContext;
import java.util.Enumeration;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import system.proxies.HttpHeader;
import system.proxies.HttpRequest;

public final class SystemHttpRequestBuilder {

    private SystemHttpRequestBuilder() {
    }


    public static HttpRequest build(final IContext context, final IMxRuntimeRequest request) {
        final HttpServletRequest servletRequest = request.getHttpServletRequest();

        HttpRequest httpRequest = new HttpRequest(context);
        if (Optional.ofNullable(servletRequest.getQueryString()).isEmpty()) {
            httpRequest.setUri(servletRequest.getRequestURL().toString());
        } else {
            httpRequest.setUri(servletRequest.getRequestURL()
                .append("?")
                .append(servletRequest.getQueryString())
                .toString());

        }
        final Enumeration<String> headerKeys = servletRequest.getHeaderNames();
        while (headerKeys.hasMoreElements()) {
            final String key = headerKeys.nextElement();
            HttpHeader httpHeader = new HttpHeader(context);
            httpHeader.setKey(key);
            httpHeader.setValue(servletRequest.getHeader(key));
            httpHeader.setHttpHeaders(httpRequest);
        }
        return httpRequest;
    }
}
