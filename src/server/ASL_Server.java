package server;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author Marcel Lüdi
 *
 * Message passing middle ware for the Advanced Systems Lab Project
 */
public class ASL_Server implements Runnable {

	public final int portNumber;

	private ASL_ConnectionPool pool;

	private ASL_Worker[] workers;
	private Thread[] workerThreads;
	private ASL_Listener listener;

	private Thread listenerThread;
	private boolean running;
	
	private final Logger logger;

	/**
	 * Creates a new ASL_Server object connected to a database
	 * 
	 * @param portNumber
	 *            the port the server will listen on. If there are multiple
	 *            listeners they will choose the next higher ports
	 * @param url
	 *            the url of the database
	 * @param user
	 *            the user account to be used
	 * @param password
	 *            the password for the user
	 * @param driverClassName
	 *            the name of the JDBC driver
	 * @param poolSize
	 *            the size of the connection pool to the database
	 * @param nWorkers
	 *            the number of worker threads created
	 * @throws ClassNotFoundException
	 *             if the JDBC driver could not be found
	 * @throws IOException
	 *             if the port is not available
	 */
	public ASL_Server(int portNumber, String url, String user, String password,
			String driverClassName, int poolSize, int nWorkers)
			throws ClassNotFoundException, IOException {

		this.portNumber = portNumber;

		this.pool = new ASL_ConnectionPool(poolSize, url, user, password,
				driverClassName);

		this.listener = new ASL_Listener(portNumber);
		this.listenerThread = new Thread(this.listener, "ASL listener");

		this.workers = new ASL_Worker[nWorkers];
		this.workerThreads = new Thread[nWorkers];
		for (int i = 0; i < this.workerThreads.length; i++) {
			this.workers[i] = new ASL_Worker(this.listener.getQueue(),
					this.pool, i);
			this.workerThreads[i] = new Thread(this.workers[i], "ASL worker "
					+ i);
			this.workerThreads[i].setDaemon(true);
		}
		
		this.logger = LogManager.getLogger("ASL Server");
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		//listenerThread = Thread.currentThread();
		//System.out.println("starting server...");
		logger.info("starting server");
		
		// fill the connection pool
		try {
			this.pool.initPool();
		} catch (SQLException e1) {
			System.out.println("could not connect to database");
			return;
		}
		
		// start workers
		for (int i = 0; i < workerThreads.length; i++) {
			workerThreads[i].start();
		}
		
		// start listener
		listenerThread.start();
		//System.out.println("server is running");
		running = true;
		logger.info("server is running");
		
		//run listener on main thread
		//listener.run();
	}

	
	/**
	 * stops all processes of the server
	 */
	public void shutdown() {
		if (!running) {
			return;
		} else {
			//System.out.println("server shutting down...");
			logger.info("server shutting down");
			// stop workers
			for (int i = 0; i < workers.length; i++) {
				workers[i].stop();
			}
			for (int i = 0; i < workers.length; i++) {
				// wait for workers to terminate
				try {
					workerThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// stop listener
			listener.stop();
			try {
				// wait for listener to settle down
				listenerThread.join(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			running = false;
			//System.out.println("server stopped");
			logger.info("server stopped");
		}
	}

	public static void main(String[] args) {
		if (args.length < 6) {
			System.err
					.println("Usage: ASL_Server <port> <db-ip:db-port> <db-user> <db-password> <# of db-connections> <# of workers>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		String url = "jdbc:postgresql://" + args[1] + "/ASL";
		String user = args[2];//"postgres";
		String password = args[3];//"qwer1";
		String driverName = "org.postgresql.Driver";
		int nConns = Integer.parseInt(args[4]);
		int nWorkers = Integer.parseInt(args[5]);

		try {
			final ASL_Server server = new ASL_Server(port, url, user, password,
					driverName, nConns, nWorkers);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					server.shutdown();
				}
			});
			server.run();
		} catch (ClassNotFoundException ex) {
			System.err.println("JDBC Driver not found");
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}
}
