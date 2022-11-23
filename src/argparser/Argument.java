package argparser;

import argparser.utils.*;

import java.util.List;
import java.util.function.Consumer;

public class Argument<Type extends ArgumentType<TInner>, TInner>
	implements IMinimumErrorLevelConfig<CustomError>, IErrorCallbacks<TInner, Argument<Type, TInner>>
{
	public static final char[] INVALID_CHARACTERS = {'=', ' '};
	final Type argType;
	private char prefix = '-';
	private Character name;
	private String alias;
	private short usageCount = 0;
	private boolean obligatory = false, positional = false;
	private TInner defaultValue;
	private Command parentCmd;
	private Consumer<Argument<Type, TInner>> onErrorCallback;
	private Consumer<TInner> onCorrectCallback;

	public Argument(Character name, String alias, Type argType) {
		if (name == null && alias == null) {
			throw new IllegalArgumentException("A name or an alias must be specified");
		}
		this.setAlias(alias);
		this.setName(name);
		this.argType = argType;
	}

	public Argument(Character name, Type argType) {
		this(name, null, argType);
	}

	public Argument(String alias, Type argType) {
		this(null, alias, argType);
	}

	@SuppressWarnings("unchecked cast") // we know for sure type returned by BOOLEAN is compatible
	public Argument(Character name) {this(name, null, (Type)ArgumentType.BOOLEAN());}

	public String getAlias() {
		if (this.alias == null) return this.name.toString();
		return alias;
	}

	public void setAlias(String alias) {
		if (alias == null) return;
		if (!Argument.isValidAlias(alias)) {
			throw new IllegalArgumentException("invalid alias '" + alias + "'");
		}
		this.alias = alias.replaceAll(String.format("^%s+", this.prefix), "");
	}

	public void setName(Character name) {
		if (name == null) return;
		if (!Argument.isValidName(name)) {
			throw new IllegalArgumentException("invalid name '" + name + "'");
		}
		this.name = name;
	}

	public char getPrefix() {
		return prefix;
	}

	/**
	 * Marks the argument as obligatory. This means that this argument should <b>always</b> be used
	 * by the user.
	 */
	public Argument<Type, TInner> obligatory() {
		this.obligatory = true;
		return this;
	}

	/**
	 * Marks the argument as positional. This means that the value of this argument may be specified directly
	 * without indicating the name/alias of this argument. The positional place where it should be placed is
	 * defined by the order of creation of the argument definitions.
	 * <li>Note that an argument marked as positional can still be used by specifying its name/alias.
	 */
	public Argument<Type, TInner> positional() {
		if (this.getNumberOfValues().max == 0) {
			throw new IllegalArgumentException("An argument that does not accept values cannot be positional");
		}
		this.positional = true;
		return this;
	}

	/**
	 * Specify the prefix of this argument. By default, this is <code>'-'</code>. If this argument is used in an
	 * argument name list (-abcd), the prefix that will be valid is any against all the arguments specified
	 * in that name list.
	 */
	public Argument<Type, TInner> prefix(char prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * The value that should be used if the user does not specify a value for this argument. If the argument
	 * does not accept values, this value will be ignored.
	 */
	public Argument<Type, TInner> defaultValue(TInner value) {
		this.defaultValue = value;
		return this;
	}

	public Argument<Type, TInner> onOk(Consumer<TInner> callback) {
		this.setOnCorrectCallback(callback);
		return this;
	}

	public Argument<Type, TInner> onErr(Consumer<Argument<Type, TInner>> callback) {
		this.setOnErrorCallback(callback);
		return this;
	}

	TInner finishParsing(Command.ParsingState parseState) {
		if (this.usageCount == 0) {
			if (this.obligatory) {
				parseState.addError(ParseError.ParseErrorType.OBLIGATORY_ARGUMENT_NOT_USED, this, 0);
				return null;
			}
			return this.defaultValue;
		}

		this.argType.getErrorsUnderDisplayLevel().forEach(parseState::addError);
		return this.argType.getFinalValue();
	}

	public void parseValues(String[] value, short tokenIndex) {
		this.argType.setTokenIndex(tokenIndex);
		this.argType.parseArgumentValues(value);
		this.usageCount++;
	}

	public void parseValues() {
		this.parseValues(new String[0], (short)0);
	}

	public ArgValueCount getNumberOfValues() {
		return this.argType.getNumberOfArgValues();
	}

	public boolean checkMatch(String alias) {
		if (this.alias == null) return false;
		return alias.equals(Character.toString(this.prefix).repeat(2) + this.alias);
	}

	public boolean checkMatch(char name) {
		// getAlias because it has a fallback to return the name if there's no alias.
		// we want to match single-char aliases too
		if (this.name == null) {
			String alias = this.getAlias();
			return alias.length() == 1 && alias.charAt(0) == name;
		}
		return this.name == name;
	}

	public boolean isObligatory() {
		return obligatory;
	}

	public boolean isPositional() {
		return positional;
	}

	Command getParentCmd() {
		return parentCmd;
	}

	void setParentCmd(Command parentCmd) {
		this.parentCmd = parentCmd;
	}

	public boolean equals(Argument<?, ?> obj) {
		// we just want to check if there's a difference between identifiers and both are part of the same command
		return (this.getAlias().equals(obj.getAlias()) || this.prefix == obj.prefix) && this.parentCmd == obj.parentCmd;
	}

	// --------------------------------- just act as a proxy to the type error handling ---------------------------------
	@Override
	public List<CustomError> getErrorsUnderExitLevel() {
		return this.argType.getErrorsUnderExitLevel();
	}

	@Override
	public List<CustomError> getErrorsUnderDisplayLevel() {
		return this.argType.getErrorsUnderDisplayLevel();
	}

	@Override
	public boolean hasExitErrors() {
		return this.argType.hasExitErrors() || !this.getErrorsUnderExitLevel().isEmpty();
	}

	@Override
	public boolean hasDisplayErrors() {
		return this.argType.hasDisplayErrors() || !this.getErrorsUnderDisplayLevel().isEmpty();
	}

	@Override
	public void setOnErrorCallback(Consumer<Argument<Type, TInner>> callback) {
		this.onErrorCallback = callback;
	}



	/**
	 * Specify a function that will be called with the value introduced by the user. This function is only
	 * called if the user used the argument, so it will never be called with a default value, for example.
	 */
	@Override
	public void setOnCorrectCallback(Consumer<TInner> callback) {
		this.onCorrectCallback = callback;
	}

	@Override
	public void invokeCallbacks() {
		if (this.onErrorCallback == null || this.getErrorsUnderDisplayLevel().isEmpty()) return;

		this.onErrorCallback.accept(this);
	}

	@SuppressWarnings("unchecked")
	void invokeCallbacks(Object okValue) {
		this.invokeCallbacks();
		this.onCorrectCallback.accept((TInner)okValue);
	}

	@Override
	public void setMinimumDisplayErrorLevel(ErrorLevel level) {
		this.argType.setMinimumDisplayErrorLevel(level);
	}

	@Override
	public ModifyRecord<ErrorLevel> getMinimumDisplayErrorLevel() {
		return this.argType.getMinimumDisplayErrorLevel();
	}

	@Override
	public void setMinimumExitErrorLevel(ErrorLevel level) {
		this.argType.setMinimumExitErrorLevel(level);
	}

	@Override
	public ModifyRecord<ErrorLevel> getMinimumExitErrorLevel() {
		return this.argType.getMinimumExitErrorLevel();
	}

	/**
	 * Checks if the specified alias is invalid or not
	 *
	 * @return <code>true</code> if the alias is valid
	 */
	private static boolean isValidAlias(String alias) {
		return UtlString.matchCharacters(alias, c -> {
			for (char chr : INVALID_CHARACTERS) {
				if (c == chr) {
					return false;
				}
			}
			return true;
		});
	}

	private static boolean isValidName(char name) {
		for (char invalidChar : Argument.INVALID_CHARACTERS) {
			if (invalidChar == name) return false;
		}
		return true;
	}
}