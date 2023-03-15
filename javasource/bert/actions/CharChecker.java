package bert.actions;

final class CharChecker {

	/**
	 * To judge whether it's an empty or unknown character.
	 */
	public static boolean isInvalid(char ch) {
		return (ch == 0 || ch == 0xfffd);
	}

	/**
	 * To judge whether it's a control character(exclude whitespace).
	 */
	public static boolean isControl(char ch) {
		if (Character.isWhitespace(ch)) {
			return false;
		}
		int type = Character.getType(ch);
		return (type == Character.CONTROL || type == Character.FORMAT);
	}

	/**
	 * To judge whether it can be regarded as a whitespace.
	 */
	public static boolean isWhitespace(char ch) {
		if (Character.isWhitespace(ch)) {
			return true;
		}
		int type = Character.getType(ch);
		return (type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR
				|| type == Character.PARAGRAPH_SEPARATOR);
	}

	/**
	 * To judge whether it's a punctuation.
	 */
	public static boolean isPunctuation(char ch) {
		int type = Character.getType(ch);
		return (type == Character.CONNECTOR_PUNCTUATION || type == Character.DASH_PUNCTUATION
				|| type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION
				|| type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION
				|| type == Character.OTHER_PUNCTUATION);
	}

	private CharChecker() {
	}
}
