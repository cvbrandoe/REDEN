package fr.ign.georeden.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OptionManager {
	private final Options options;
	private CommandLine cmd;
	
	/**
	 * Instantiates a new option manager.
	 *
	 * @param args the args
	 * @throws ParseException the parse exception
	 */
	public OptionManager() {
		options = new Options();
		Option teiSource   = Option.builder("teiSource")
				.required(true)
				.hasArg()
				.desc("the TEI annotated file name")
				.build();
		options.addOption(teiSource);
		options.addOption("h", "help", false, "print this message");
	}
	
	/**
	 * Parses the arguments.
	 *
	 * @param args the args
	 * @throws ParseException the parse exception
	 */
	public void parseArguments(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		cmd = parser.parse( options, args);
	}
	
	public void help() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "ant", options );
	}
	
	/**
	 * Checks for option.
	 *
	 * @param opt the opt
	 * @return the boolean
	 */
	public Boolean hasOption(String opt) {
		return cmd.hasOption(opt);
	}
	
	/**
	 * Gets the option value.
	 *
	 * @param opt the opt
	 * @return the option value
	 */
	public String getOptionValue(String opt) {
		return cmd.getOptionValue(opt);
	}
}
