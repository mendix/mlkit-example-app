package bert.actions;

public class QaAnswer {
	public Pos pos;
	public String text;

	public QaAnswer(String text, Pos pos) {
		this.text = text;
		this.pos = pos;
	}

	public QaAnswer(String text, int start, int end, float logit) {
		this(text, new Pos(start, end, logit));
	}

	/**
	 * Position and related information from the model.
	 */
	public static class Pos implements Comparable<Pos> {
		public int start;
		public int end;
		public float logit;

		public Pos(int start, int end, float logit) {
			this.start = start;
			this.end = end;
			this.logit = logit;
		}

		@Override
		public int compareTo(Pos other) {
			return Float.compare(other.logit, this.logit);
		}
	}
}
