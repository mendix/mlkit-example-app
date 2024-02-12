package mendixsso.implementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import java.util.Optional;
import mendixsso.implementation.utils.CustomSignupHintParameterMicroflowValidator;
import system.proxies.HttpRequest;

public final class CustomSignupHintParameterMapper {


    private String signupHintMicroflowName;

    private CustomSignupHintParameterMapper() {
    }

    private static class SingletonInstanceHolder {

        private static final CustomSignupHintParameterMapper INSTANCE = new CustomSignupHintParameterMapper();

    }

    public static CustomSignupHintParameterMapper getInstance() {
        return SingletonInstanceHolder.INSTANCE;
    }

    public void initialize(final String signupHintMicroflowName) {
        this.signupHintMicroflowName = signupHintMicroflowName;
        CustomSignupHintParameterMicroflowValidator.validateMicroflows(signupHintMicroflowName);
    }

    public String getSignupHint(final IContext context, final HttpRequest httpRequest) {
        if (Optional.ofNullable(signupHintMicroflowName).isEmpty()) {
            return ConfigurationManager.getInstance().getDefaultSignupHint();
        }

        return Core.microflowCall(this.signupHintMicroflowName)
            .withParam("HttpRequest", httpRequest.getMendixObject())
            .execute(context);
    }

}
