package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IDataType;
import java.util.List;
import java.util.Map;

public final class MicroflowValidator {

    private MicroflowValidator() {
    }

    public static void validateMicroflow(String microflowName, List<MicroflowParameter> microflowParameters) {
        final Map<String, IDataType> inputParameters = Core.getInputParameters(microflowName);
        final IDataType returnType = Core.getReturnType(microflowName);
        for (MicroflowParameter microflowParameter : microflowParameters) {
            microflowParameter.validate(microflowName, inputParameters, returnType);
        }
    }

}
