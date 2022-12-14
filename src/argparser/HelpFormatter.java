package argparser;

import argparser.utils.displayFormatter.Color;
import argparser.utils.displayFormatter.FormatOption;
import argparser.utils.displayFormatter.TextFormatter;
import argparser.utils.UtlString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class HelpFormatter {
	private Command parentCmd;
	private byte indentSize = 3;
	public static short lineWrapMax = 110;
	private ArrayList<LayoutItem> layout = new ArrayList<>();
	public static boolean debugLayout = false;

	HelpFormatter(Command parentCmd) {
		this.parentCmd = parentCmd;
		this.setLayout();
	}

	// the user can create a helpFormatter, though, the parentCmd should be applied later (otherwise stuff will fail)
	public HelpFormatter() {
		this.setLayout();
	}

	HelpFormatter(HelpFormatter other) {
		this.parentCmd = other.parentCmd;
		this.indentSize = other.indentSize;
		this.layout.addAll(other.layout);
	}

	void setParentCmd(Command parentCmd) {
		this.parentCmd = parentCmd;
	}

	protected List<LayoutItem> getLayout() {
		return layout;
	}

	public void setLayout() {
		this.changeLayout(
			new LayoutItem(LayoutGenerators::title).marginBottom(1),
			new LayoutItem(LayoutGenerators::synopsis).indent(1)
		);
	}

	public void moveLayoutItem(int from, int to) {
		if (from < 0 || from >= this.layout.size() || to < 0 || to >= this.layout.size()) {
			throw new IndexOutOfBoundsException("invalid indexes given");
		}

		// same index, nothing to do
		if (from == to)
			return;

		final var item = this.layout.remove(from);
		this.layout.add(to, item);
	}

	protected final void addToLayout(LayoutItem... layoutItems) {
		this.layout.addAll(Arrays.asList(layoutItems));
		Collections.addAll(this.layout, layoutItems);
	}

	protected final void addToLayout(int after, LayoutItem... layoutItems) {
		this.layout.addAll(after, Arrays.asList(layoutItems));
	}

	protected final void changeLayout(LayoutItem... layoutItems) {
		this.layout = new ArrayList<>(Arrays.asList(layoutItems));
	}


	protected static class LayoutGenerators {

		public static String title(Command cmd) {
			return cmd.name + (cmd.description == null ? "" : ": " + cmd.description);
		}

		public static String synopsis(Command cmd, boolean includeHelp) {
			var args = new ArrayList<>(cmd.getArguments()) {{
				// only arguments that are not in groups, since we handle those later
				removeIf(arg -> arg.getParentGroup() != null);

				// remove help argument if it's not needed
				if (!includeHelp)
					removeIf(arg -> arg.getLongestName().equals("help") && arg.allowsUnique());

				// make sure all positional and obligatory arguments are at the beginning
				sort(Argument::compareByPriority);
			}};

			if (args.isEmpty() && cmd.argumentGroups.isEmpty()) return "";
			var buffer = new StringBuilder();

			for (var arg : args) {
				var repr = arg.getRepresentation();
				if (repr == null) continue;
				buffer.append(repr).append(' ');
			}

			for (var group : cmd.argumentGroups) {
				group.getRepresentation(buffer);
			}

			return buffer.toString();
		}

		public static String synopsis(Command cmd) {
			return synopsis(cmd, false);
		}

		public static String heading(String content, char lineChar) {
			return UtlString.center(content, lineWrapMax, lineChar);
		}

		public static String heading(String content) {
			return UtlString.center(content, lineWrapMax);
		}
	}




	public static class LayoutItem {
		private int indent = 0;
		private int lineWrapMax = HelpFormatter.lineWrapMax;
		private int marginTop, marginBottom;
		private final Function<Command, String> layoutGenerator;

		public LayoutItem(Function<Command, String> layoutGenerator) {
			this.layoutGenerator = layoutGenerator;
		}

		public LayoutItem indent(int indent) {
			this.indent = indent;
			return this;
		}

		public LayoutItem lineWrapAt(int maxTextLineLength) {
			this.lineWrapMax = maxTextLineLength;
			return this;
		}

		public LayoutItem marginTop(int marginTop) {
			this.marginTop = marginTop;
			return this;
		}

		public LayoutItem marginBottom(int marginBottom) {
			this.marginBottom = marginBottom;
			return this;
		}

		public LayoutItem margin(int margin) {
			this.marginTop = margin;
			this.marginBottom = margin;
			return this;
		}

		public String generate(HelpFormatter helpFormatter) {
			return "\n".repeat(this.marginTop) + UtlString.indent(
				UtlString.wrap(
					this.layoutGenerator.apply(helpFormatter.parentCmd),
					this.lineWrapMax - this.indent
				),
				this.indent * helpFormatter.indentSize
			) + "\n".repeat(this.marginBottom);
		}
	}


	@Override
	public String toString() {
		var buffer = new StringBuilder();

		for (int i = 0; i < this.layout.size(); i++) {
			var generator = this.layout.get(i);
			if (HelpFormatter.debugLayout)
				buffer.append(new TextFormatter("LayoutItem " + i + ":\n")
					.addFormat(FormatOption.UNDERLINE)
					.setColor(Color.GREEN)
				);
			buffer.append(generator.generate(this)).append('\n');
		}

		return buffer.toString();
	}
}
