package bert.actions;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.mendix.core.Core;

public class LoadDatasetClient {

	public static Map<String, Integer> loadDictionary() {
		Map<String, Integer> dic = new HashMap<>();
		try {
			File basePath = new File(Core.getConfiguration().getBasePath(), "ml");
			File filePath = Paths.get("bert", "vocab.txt").toFile();
			File vocabFile = new File(basePath, filePath.getPath());
			Scanner reader = new Scanner(vocabFile);
			int index = 0;
			while (reader.hasNextLine()) {
				String word = reader.nextLine();
				dic.put(word, index++);
			}
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return dic;
	}

}
