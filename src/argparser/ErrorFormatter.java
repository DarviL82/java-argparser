package argparser;

import argparser.utils.displayFormatter.FormatOption;
import argparser.utils.displayFormatter.TextFormatter;
import argparser.utils.UtlString;

import java.util.ArrayList;

class ErrorFormatter {
	private String contents = "";
	private String tokensView = "";
	private ErrorLevel errorLevel;
	private final ErrorHandler mainHandler;

	public ErrorFormatter(ErrorHandler mainHandler, ErrorLevel level) {
		this.errorLevel = level;
		this.mainHandler = mainHandler;
	}

	public ErrorFormatter setContents(String contents) {
		this.contents = contents;
		return this;
	}

	public ErrorFormatter setErrorLevel(ErrorLevel errorLevel) {
		this.errorLevel = errorLevel;
		return this;
	}

	public void print() {
		// first figure out the length of the longest line
		var maxLength = UtlString.getLongestLine(this.contents).length();

		var formatter = new TextFormatter()
			.setColor(this.errorLevel.color)
			.addFormat(FormatOption.BOLD);

		System.err.println(
			formatter.setContents(String.format(" ┌─%s%s", this.errorLevel, !this.tokensView.isEmpty() ? "\n" : "")).toString()
				+ this.tokensView
				+ this.contents.replaceAll(
				"^|\\n",
				formatter.setContents("\n │ ").toString() // first insert a vertical bar at the start of each line
			)
				// then insert a horizontal bar at the end, with the length of the longest line
				// approximately
				+ formatter.setContents("\n └" + "─".repeat(Math.max(maxLength - 5, 0)) + " ───── ── ─")
				.toString()
				+ "\n"
		);
	}

	public ErrorFormatter displayTokens(int start, int offset, boolean placeArrow) {
		start += this.mainHandler.cmdAbsoluteTokenIndex;
		final var arrow = TextFormatter.ERROR("<-").setColor(this.errorLevel.color);
		var tokensFormatters = new ArrayList<>(this.mainHandler.tokens.stream().map(Token::getFormatter).toList());
		int tokensLength = this.mainHandler.tokens.size();

		if (start < 0) {
			tokensFormatters.add(0, arrow);
		} else if (start >= tokensLength) {
			tokensFormatters.add(arrow);
		}

		for (int i = 0; i < tokensLength; i++) {
			if (i < this.mainHandler.cmdAbsoluteTokenIndex) {
				tokensFormatters.get(i).addFormat(FormatOption.DIM);
			}

			if (i >= start && i < start + offset + 1) {
				if (placeArrow) {
					tokensFormatters.add(i, arrow);
				} else {
					tokensFormatters.get(i)
						.setColor(this.errorLevel.color)
						.addFormat(FormatOption.REVERSE, FormatOption.BOLD);
				}
			}
		}

		this.tokensView = String.join(" ", tokensFormatters.stream().map(TextFormatter::toString).toList());
		return this;
	}

	public ErrorFormatter displayTokens(int index) {
		return this.displayTokens(index, 0, true);
	}
}
