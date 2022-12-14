package argparser.utils.displayFormatter;

public enum FormatOption {
	RESET_ALL(0),
	BOLD(1),
	ITALIC(3),
	DIM(2),
	UNDERLINE(4),
	BLINK(5),
	REVERSE(7),
	HIDDEN(8),
	STRIKE_THROUGH(9);

	private final Byte value;

	FormatOption(int value) {
		this.value = (byte)value;
	}

	@Override
	public String toString() {
		return TextFormatter.ESC + String.format("[%dm", this.value);
	}

	public String toStringReset() {
		return TextFormatter.ESC + String.format("[%dm", this.value + 20 + (this == BOLD ? 1 : 0));
	}
}