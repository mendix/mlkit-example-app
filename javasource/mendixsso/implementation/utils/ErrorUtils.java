package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import mendixsso.implementation.ConfigurationManager;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static mendixsso.proxies.constants.Constants.*;

public class ErrorUtils {

    private static final ILogNode LOG = Core.getLogger("ErrorUtils");
    private static final String RESOURCE_NOT_FOUND_USER_MESSAGE =
            "The resource you are looking for does not exist.";
    private static final String RESOURCE_NOT_FOUND_LOG_MESSAGE_TEMPLATE =
            "The requested path: %s does not exist.";

    public static void serveError(
            IMxRuntimeRequest request,
            IMxRuntimeResponse response,
            ResponseType responseType,
            String userFriendlyMessage,
            String message,
            boolean messageIncludesHtml,
            Throwable e) {
        serveError(
                response,
                createTemplateVariables(
                        request, responseType, userFriendlyMessage, messageIncludesHtml));
        logError(responseType, message, e);
    }

    public static void serveNotFoundError(
            IMxRuntimeRequest request, IMxRuntimeResponse response, String path) {
        LOG.trace(String.format(RESOURCE_NOT_FOUND_LOG_MESSAGE_TEMPLATE, path));
        serveError(
                response,
                createTemplateVariables(
                        request,
                        ResponseType.RESOURCE_NOT_FOUND,
                        RESOURCE_NOT_FOUND_USER_MESSAGE,
                        false));
    }

    public static void serveError(
            IMxRuntimeRequest request,
            IMxRuntimeResponse response,
            ResponseType responseType,
            String message,
            boolean messageIncludesHtml,
            Throwable e) {
        serveError(
                response,
                createTemplateVariables(request, responseType, message, messageIncludesHtml));
        logError(responseType, message, e);
    }

    private static void serveError(IMxRuntimeResponse response, TemplateVariables templateVars) {
        try {
            response.setContentType("text/html");
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            response.setStatus(HttpURLConnection.HTTP_OK);

            final String templateFilePath =
                    Core.getConfiguration().getResourcesPath()
                            + File.separator
                            + "mendixsso"
                            + File.separator
                            + "templates"
                            + File.separator
                            + "sso_error.html";

            final String renderedTemplate = TemplateRenderer.render(templateFilePath, templateVars);

            try (OutputStream out = response.getOutputStream()) {
                // Ignore Veracode issues: CWE-209 and CWE-201; false positive. The templates do not
                // contain any sensitive data.
                out.write(renderedTemplate.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Error while serving error page", e);
        }
    }

    private static TemplateVariables createTemplateVariables(
            IMxRuntimeRequest request,
            ResponseType responseType,
            String message,
            boolean messageIncludesHtml) {

        TemplateVariables templateVariables = new TemplateVariables();
        templateVariables.putString(
                "{{TITLE}}",
                StringUtils.isBlank(responseType.title) ? "Oops!" : responseType.title);

        if (messageIncludesHtml) {
            templateVariables.putHtml("{{MESSAGE}}", message);
        } else {
            templateVariables.putString("{{MESSAGE}}", message);
        }

        templateVariables.putString("{{SHOW_TRY_AGAIN}}", "");
        templateVariables.putString("{{TRY_AGAIN_URL}}", OpenIDUtils.getApplicationUrl(request));
        templateVariables.putString("{{TRY_AGAIN_TEXT}}", getTryAgainText());

        templateVariables.putString("{{SHOW_CONTACT_SUPPORT}}", "");
        templateVariables.putString("{{SUPPORT_EMAIL}}", getSupportEmail());
        templateVariables.putString("{{SUPPORT_EMAIL_SUBJECT}}", getSupportEmailSubject());
        templateVariables.putString(
                "{{HOMEPAGE}}", ConfigurationManager.getInstance().getIndexPage());

        return templateVariables;
    }

    private static void logError(ResponseType responseType, String message, Throwable e) {
        if (e != null) {
            LOG.error(
                    "Error occured: "
                            + responseType.title
                            + ":\n"
                            + message
                            + ": "
                            + e.getMessage(),
                    e);
        } else {
            LOG.error("Error occured: " + responseType.title + ":\n" + message);
        }
    }

    public enum ResponseType {
        INTERNAL_SERVER_ERROR("Internal Server Error"),
        OOPS("Oops!"),
        UNAUTHORIZED("No Application Access"),
        INCOMPATIBLE_USER_TYPE_ERROR("Incompatible User Type Error"),
        RESOURCE_NOT_FOUND("Oops, 404!");

        public final String title;

        ResponseType(String title) {
            this.title = title;
        }
    }
}
