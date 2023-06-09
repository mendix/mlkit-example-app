// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package bert.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.base.Joiner;
import com.mendix.core.Core;
import com.mendix.mlkit.MLKit;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import com.mendix.systemwideinterfaces.core.IMendixObject;

public class BERT_PostProcessor extends CustomJavaAction<IMendixObject>
{
	private IMendixObject __OutputEntity;
	private bert.proxies.ML_Output_Entity_BERT_mapping OutputEntity;
	private IMendixObject __PredictRequest;
	private bert.proxies.PredictRequest PredictRequest;

	public BERT_PostProcessor(IContext context, IMendixObject OutputEntity, IMendixObject PredictRequest)
	{
		super(context);
		this.__OutputEntity = OutputEntity;
		this.__PredictRequest = PredictRequest;
	}

	@java.lang.Override
	public IMendixObject executeAction() throws Exception
	{
		this.OutputEntity = this.__OutputEntity == null ? null : bert.proxies.ML_Output_Entity_BERT_mapping.initialize(getContext(), __OutputEntity);

		this.PredictRequest = this.__PredictRequest == null ? null : bert.proxies.PredictRequest.initialize(getContext(), __PredictRequest);

		// BEGIN USER CODE
		float[][] Unstack_1 = new float[1][256];
		MLKit.toArray(MLKit.fromBase64(OutputEntity.getUnstack_1()), Unstack_1);
		
		float[][] Unstack_0 = new float[1][256];
		MLKit.toArray(MLKit.fromBase64(OutputEntity.getUnstack_0()), Unstack_0);
		
		Feature feature = BERT_PreProcessor.featureConverter.convert(PredictRequest.getQuestion(), PredictRequest.getQuestionContext());
		List<QaAnswer> answers = getBestAnswers(Unstack_1[0], Unstack_0[0], feature);
		
		final IMendixObject outputObject = Core.instantiate(getContext(), "BERT.PredictResponse");
		outputObject.setValue(getContext(), "Answer", answers.get(0).text);
		
		return outputObject;

		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 * @return a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "BERT_PostProcessor";
	}

	// BEGIN EXTRA CODE
	private static final int MAX_ANS_LEN = 30;
	private static final int PREDICT_ANS_NUM = 5;
	private static final int OUTPUT_OFFSET = 1;
	private static final int MAX_SEQ_LEN = 256;
	private static final Joiner SPACE_JOINER = Joiner.on(" ");
	
	private List<QaAnswer> getBestAnswers(float[] startLogits, float[] endLogits, Feature feature) {
		int[] startIndexes = getBestIndex(startLogits, feature.tokenToOrigMap);
		int[] endIndexes = getBestIndex(endLogits, feature.tokenToOrigMap);

		List<QaAnswer.Pos> origResults = new ArrayList<>();
		for (int start : startIndexes) {
			for (int end : endIndexes) {
				if (end < start) {
					continue;
				}
				int length = end - start + 1;
				if (length > MAX_ANS_LEN) {
					continue;
				}
				origResults.add(new QaAnswer.Pos(start, end, startLogits[start] + endLogits[end]));
			}
		}

		Collections.sort(origResults);

		List<QaAnswer> answers = new ArrayList<>();
		for (int i = 0; i < origResults.size(); i++) {
			if (i >= PREDICT_ANS_NUM) {
				break;
			}

			String convertedText;
			if (origResults.get(i).start > 0) {
				convertedText = convertBack(feature, origResults.get(i).start, origResults.get(i).end);
			} else {
				convertedText = "";
			}
			QaAnswer ans = new QaAnswer(convertedText, origResults.get(i));
			answers.add(ans);
		}
		return answers;
	}
	
	private int[] getBestIndex(float[] logits, Map<Integer, Integer> tokenToOrigMap) {
		List<QaAnswer.Pos> tmpList = new ArrayList<>();
		for (int i = 0; i < MAX_SEQ_LEN; i++) {
			if (tokenToOrigMap.containsKey(i + OUTPUT_OFFSET)) {
				tmpList.add(new QaAnswer.Pos(i, i, logits[i]));
			}
		}

		Collections.sort(tmpList);

		int[] indexes = new int[PREDICT_ANS_NUM];
		for (int i = 0; i < PREDICT_ANS_NUM; i++) {
			indexes[i] = tmpList.get(i).start;
		}

		return indexes;
	}
	
	private static String convertBack(Feature feature, int start, int end) {
		// Shifted index is: index of logits + offset.
		int shiftedStart = start + OUTPUT_OFFSET;
		int shiftedEnd = end + OUTPUT_OFFSET;
		int startIndex = feature.tokenToOrigMap.get(shiftedStart);
		int endIndex = feature.tokenToOrigMap.get(shiftedEnd);
		// end + 1 for the closed interval.
		String ans = SPACE_JOINER.join(feature.origTokens.subList(startIndex, endIndex + 1));
		return ans;
	}
	// END EXTRA CODE
}
