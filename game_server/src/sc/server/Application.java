package sc.server;

import java.io.File;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.UnknownOptionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Application
{
	private static final Logger	logger	= LoggerFactory
												.getLogger(Application.class);

	public static void main(String[] params) throws InterruptedException,
			IllegalOptionValueException, UnknownOptionException
	{
		parseArguments(params);

		logger.info("Server is starting up...");

		addShutdownHook();
		long start = System.currentTimeMillis();

		final Lobby server = new Lobby();
		server.start();

		long end = System.currentTimeMillis();
		logger.info("Server has been initialized in {} ms.", end - start);

		while (!Thread.interrupted() && !ServiceManager.isEmpty())
		{
			Thread.sleep(50);
		}
	}

	public static void parseArguments(String[] params)
			throws IllegalOptionValueException, UnknownOptionException
	{
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option debug = parser.addBooleanOption('d', "debug");
		CmdLineParser.Option pluginDirectory = parser
				.addStringOption("plugins");
		parser.parse(params);

		String path = (String) parser.getOptionValue(pluginDirectory, null);
		if (path != null)
		{
			File f = new File(path);

			if (f.exists() && f.isDirectory())
			{
				Configuration.set(Configuration.PLUGIN_PATH_KEY, path);
				logger.info("Loading plugins from {}", f.getAbsoluteFile());
			}
			else
			{
				logger.warn("Could not find {} to load plugins from", f.getAbsoluteFile());
			}
		}
	}

	public static void addShutdownHook()
	{
		logger.info("Registering ShutdownHook (Ctrl+C)...");

		try
		{
			Thread shutdown = new Thread(new Runnable() {
				@Override
				public void run()
				{
					logger.info("Shutting down...");
					ServiceManager.killAll();
					logger.info("Exiting");
				}
			});

			shutdown.setName("ShutdownHook");
			Runtime.getRuntime().addShutdownHook(shutdown);
		}
		catch (Exception e)
		{
			logger.warn("Could not install ShutdownHook", e);
		}
	}
}
