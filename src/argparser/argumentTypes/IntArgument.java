package argparser.argumentTypes;

import argparser.ArgumentType;
import argparser.ErrorLevel;
import argparser.utils.displayFormatter.TextFormatter;

public class IntArgument extends ArgumentType<Integer> {
	@Override
	public Integer parseValues(String[] args) {
		try {
			return Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			this.addError("Invalid integer value: '" + args[0] + "'.");
			return null;
		}
	}

	@Override
	public TextFormatter getRepresentation() {
		return new TextFormatter("int");
	}
}