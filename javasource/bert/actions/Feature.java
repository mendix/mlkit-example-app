package bert.actions;

import com.google.common.primitives.Ints;
import java.util.List;
import java.util.Map;

public class Feature {

	public final int[] inputIds;
	public final int[] inputMask;
	public final int[] segmentIds;
	public final List<String> origTokens;
	public final Map<Integer, Integer> tokenToOrigMap;

	public Feature(List<Integer> inputIds, List<Integer> inputMask, List<Integer> segmentIds, List<String> origTokens,
			Map<Integer, Integer> tokenToOrigMap) {
		this.inputIds = Ints.toArray(inputIds);
		this.inputMask = Ints.toArray(inputMask);
		this.segmentIds = Ints.toArray(segmentIds);
		this.origTokens = origTokens;
		this.tokenToOrigMap = tokenToOrigMap;
	}

}
