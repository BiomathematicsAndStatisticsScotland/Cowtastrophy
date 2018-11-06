

import broadwick.BroadwickException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;

/**
 * Construct and read command line arguments. This class contains methods for extracting some of
 * the main options such as configuration file etc.
 */
@Slf4j
public class CommandLineOptions {

    /**
     * Construct and provide GNU-compatible Options. Read the command line extracting the arguments,
     * this additionally displays the help message if the command line is empty.
     *
     * @param args the command line arguments.
     */
    public CommandLineOptions(final String[] args) {
        buildCommandLineArguments();

        final Parser parser = new Parser();
        parser.setGroup(options);
        final HelpFormatter hf = new HelpFormatter(SPACE, SPACE, SPACE, LINEWIDTH);
        parser.setHelpFormatter(hf);
        parser.setHelpTrigger("--help");
        cmdLine = parser.parseAndHelp(args);

        log.debug("options = {}", cmdLine);

        if (cmdLine == null) {
            hf.printHeader();
            throw new BroadwickException("Invalid command line options; see help message.");
        }
    }

    /**
     * Construct and provide GNU-compatible Options.
     */
    private void buildCommandLineArguments() {

        final DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
        final ArgumentBuilder abuilder = new ArgumentBuilder();
        final GroupBuilder gbuilder = new GroupBuilder();

        sessionIdOpt = obuilder.withShortName("sessionId")
                .withShortName("s")
                .withDescription("recover the simulation with the given session id")
                .withArgument(
                        abuilder
                                .withName("sessionId")
                                .withMinimum(1)
                                .withMaximum(1)
                                .create())
                .create();
        controlStgyOpt = obuilder.withShortName("controlStgy")
                .withShortName("c")
                .withDescription("Use the specified control Strategy")
                .withArgument(
                        abuilder
                                .withName("controlStrategy")
                                .withMinimum(1)
                                .withMaximum(1)
                                .create())
                .create();
        fForwardOpt = obuilder.withShortName("fastForward")
                .withShortName("f")
                .withDescription("fast forward the simulation until the end")
                .create();

        options = gbuilder.withName("options")
                .withOption(sessionIdOpt)
                .withOption(controlStgyOpt)
                .withOption(fForwardOpt)
                .create();
    }

    /**
     * Get the sessionId specified on the command line.
     *
     * @return the name of the options file specified by the -s option.
     */
    public final String getsessionId() {
        return getOpt(sessionIdOpt);
    }

    /**
     * Get the control strategy specified on the command line. The command line is expected to be
     * in the form ' -c "Strategy:{json formatted parameters}"'; where Strategy can be one of
     *   "Cull on confirmation"
     *   "Cull on suspicion"
     *   "Cull on confirmation with Ring"
     *   "Vaccinate at random"
     *   "Ring vaccination"
     *   "Ring vaccination with culling"
     * @return a string with the name of the control strategy and the  specified by the -c option.
     */
    public final String getcontrolStrategy() {
        return getOpt(controlStgyOpt);
    }

    /**
     * Obtain the simulation mode from the command line.
     * @return Mode.FAST_FORWARD if "-f" was found on the command line, Mode.SINGLE_DAY otherwise.
     */
    public final Mode getMode() {
        if (cmdLine.hasOption(fForwardOpt)) {
            return Mode.FAST_FORWARD;
        }
        return Mode.SINGLE_DAY;
    }

    /**
     * Get the string that accompanies this option or "" if there is none. For example, if the
     * command line is "-s 1331845.ser" and this method is called with the "-s" option it will
     * return "1331845.ser".
     * @param option the option whose string is required
     * @return the string that is specified by this option.
     */
    private String getOpt(final Option option) {
        if (cmdLine.hasOption(option)) {
            return (String) cmdLine.getValue(option);
        } else {
            return "";
        }
    }

    private final CommandLine cmdLine;
    private Group options;
    private static final int LINEWIDTH = 120;
    private static final String SPACE = " ";
    private Option sessionIdOpt;
    private Option controlStgyOpt;
    private Option fForwardOpt;
}
