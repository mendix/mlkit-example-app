package mendixsso.implementation;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import org.apache.commons.lang3.StringUtils;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

public final class ContinuationURLManager {
    private static final ILogNode LOG = Core.getLogger(ContinuationURLManager.class.getName());
    private static final String RUNTIME_SETTINGS_NAME = "MendixSSO_AllowedContinuationURLs";
    private final HashMap<String, String> allowedContinuationURLMap;

    private ContinuationURLManager() {

        //Retrieve allowed redirects configuration and strip whitespaces
        allowedContinuationURLMap =  new HashMap<>();
        String commaSeparatedURLs = StringUtils.deleteWhitespace(ConfigurationManager.getInstance().getAllowedContinuationURLs());
        String hostname;

        //validate redirect URLs
        for (String allowedContinuationURL :commaSeparatedURLs.split(",")) {
            if(!allowedContinuationURL.isEmpty()) {
                try {
                    hostname = new URL(allowedContinuationURL).toURI().getHost();
                    allowedContinuationURLMap.put(hostname, allowedContinuationURL);
                } catch (MalformedURLException | URISyntaxException e) {
                    LOG.warn("Something went wrong while parsing " + RUNTIME_SETTINGS_NAME + ". The specified url: '" + allowedContinuationURL + "' is invalid.");
                }
            }
        }

        //Add application root URL to the allowed redirects
        String applicationRootUrl = Core.getConfiguration().getApplicationRootUrl();
        if(applicationRootUrl!= null && !applicationRootUrl.isEmpty()) {
            try {
                String appRootUrlHostname = new URL(applicationRootUrl).toURI().getHost();
                allowedContinuationURLMap.put(appRootUrlHostname, applicationRootUrl);
            } catch (URISyntaxException | MalformedURLException e) {
                LOG.warn("Something went wrong while parsing ApplicationRootURL. The specified url: '" + applicationRootUrl + "' is invalid.");
            }
        }
    }

    private static class SingletonInstanceHolder {
        private static final ContinuationURLManager INSTANCE = new ContinuationURLManager();
    }

    public static ContinuationURLManager getInstance() {
        return ContinuationURLManager.SingletonInstanceHolder.INSTANCE;
    }

    public boolean isRedirectionAllowedForUrl(String requestedContinuationURL) {
        try {
            String requestedRedirectUrlHostname = new URL(requestedContinuationURL).toURI().getHost();
            Optional<String> allowedRedirectUrl = Optional.ofNullable(allowedContinuationURLMap.get(requestedRedirectUrlHostname));
            return allowedRedirectUrl.isPresent();
        } catch (URISyntaxException | MalformedURLException e) {
            LOG.warn("Something went wrong while redirecting. The specified url: '" + requestedContinuationURL + "' is invalid.");
            return false;
        }
    }
}
