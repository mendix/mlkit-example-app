package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IDataType;
import java.util.ArrayList;
import java.util.List;

public final class UserMapperMicroflowsValidator {

    private UserMapperMicroflowsValidator() {
    }

    public static String validateMicroflows(String createMicroflowName, String updateMicroflowName) {
        final IDataType userEntityType = Core.getReturnType(createMicroflowName);
        final List<MicroflowParameter> microflowParameters = buildMicroflowParameters(userEntityType.toString());

        MicroflowValidator.validateMicroflow(createMicroflowName, microflowParameters);

        microflowParameters.add(new MicroflowParameter("User", userEntityType.toString(), true, true, true));
        MicroflowValidator.validateMicroflow(updateMicroflowName, microflowParameters);

        return userEntityType.toString();
    }

    private static List<MicroflowParameter> buildMicroflowParameters(String userEntityType) {
        final List<MicroflowParameter> microflowParameters = new ArrayList<>();
        microflowParameters.add(new MicroflowParameter("UUID", "String", false, true, false));
        microflowParameters.add(new MicroflowParameter("EmailAddress", "String", false, true, false));
        microflowParameters.add(new MicroflowParameter("UserProfile", "MendixSSO.UserProfile", true, true, false));
        microflowParameters.add(new MicroflowParameter("User", userEntityType, true, false, true));
        return microflowParameters;
    }

}
