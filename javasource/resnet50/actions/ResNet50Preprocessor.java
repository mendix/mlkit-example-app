// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package resnet50.actions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.core.Core;
import com.mendix.mlkit.MLKit;

public class ResNet50Preprocessor extends CustomJavaAction<IMendixObject>
{
	private IMendixObject __RawImage;
	private resnet50.proxies.ResNet RawImage;

	public ResNet50Preprocessor(IContext context, IMendixObject RawImage)
	{
		super(context);
		this.__RawImage = RawImage;
	}

	@java.lang.Override
	public IMendixObject executeAction() throws Exception
	{
		this.RawImage = this.__RawImage == null ? null : resnet50.proxies.ResNet.initialize(getContext(), __RawImage);

		// BEGIN USER CODE

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		this.RawImage.getContents(getContext(), bos);
		byte[] binaryImage = bos.toByteArray();
		
 		Core.getLogger("ResNet50").info("binaryImage size in byte(s): " + binaryImage.length);
		
		Mat img = Imgcodecs.imdecode(new MatOfByte(binaryImage), Imgcodecs.IMREAD_COLOR);

		// resize image
		Mat rim = new Mat();
		Size sz = new Size(224, 224); // this is ResNet50 Specific, change as per your current needs.
		Imgproc.resize(img, rim, sz);

		// normalize image. Again, specific for this ResNet50 model.
		float[] mean = new float[] {0.485f, 0.456f, 0.406f};
		float[] std = new float[] {0.229f, 0.224f, 0.225f};
		float[][][][] inputArray = new float[1][3][224][224];
		for(int i = 0; i < 224; i++) {
			for(int j = 0; j < 224; j++) {
				for(int k = 0; k <= 2; k++) {
					double[] rawValue = rim.get(i, j);
					float normalizedValue = (((float) (rawValue[Math.abs(k - 2)] / 255) - mean[k]) / std[k]);
					inputArray[0][k][i][j] = normalizedValue;
				}
			}
		}    
	
		// convert array to base64
		final InputStream is = MLKit.toInputStream(inputArray);
		final String base64 = MLKit.toBase64(is);
		// create output entity object

		final IMendixObject outputObject = Core.instantiate(getContext(), "ResNet50.ML_Input_Entity_ResNet50ModelMapping");
		outputObject.setValue(getContext(), "Data", base64);
		
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
		return "ResNet50Preprocessor";
	}

	// BEGIN EXTRA CODE
	static {
		nu.pattern.OpenCV.loadShared(); //OpenCV initialization
	}
	// END EXTRA CODE
}
