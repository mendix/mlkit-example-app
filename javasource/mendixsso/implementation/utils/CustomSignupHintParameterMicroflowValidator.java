package mendixsso.implementation.utils;

import java.util.ArrayList;
import java.util.List;

public final class CustomSignupHintParameterMicroflowValidator {

    private CustomSignupHintParameterMicroflowValidator(){}

    public static void validateMicroflows(String signupHintMicroflowName) {
        final List<MicroflowParameter> microflowParameters = buildMicroflowParameters();
        MicroflowValidator.validateMicroflow(signupHintMicroflowName, microflowParameters);

    }


    private static List<MicroflowParameter> buildMicroflowParameters() {
        final List<MicroflowParameter> microflowParameters = new ArrayList<>();
        microflowParameters.add(new MicroflowParameter("SignupHint", "String", false, false, false));
        microflowParameters.add(new MicroflowParameter("HttpRequest", "System.HttpRequest", true, true, false));
        return microflowParameters;
    }
}
