package server;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Marcel Lüdi
 *
 * Message passing middle ware for the Advanced Systems Lab Project
 */
public class ASL_Server implements Runnable {

	private int portNumber;

	public int getPort() {
		return portNumber;
	}

	private ASL_ConnectionPool pool;

	private ASL_Worker[] workers;
	private Thread[] workerThreads;
	private ASL_Listener listener;

	private Thread serverThread;
	private boolean running;

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

		this.workers = new ASL_Worker[nWorkers];
		this.workerThreads = new Thread[nWorkers];
		for (int i = 0; i < this.workerThreads.length; i++) {
			this.workers[i] = new ASL_Worker(this.listener.getQueue(),
					this.pool);
			this.workerThreads[i] = new Thread(this.workers[i], "ASL worker "
					+ i);
			this.workerThreads[i].setDaemon(true);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		serverThread = Thread.currentThread();
		System.out.println("starting server...");
		
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
		System.out.println("server is running");
		running = true;
		
		//run listener on main thread
		listener.run();
	}

	
	/**
	 * stops all processes of the server
	 */
	public void shutdown() {
		if (!running) {
			return;
		} else {
			System.out.println("server shutting down...");
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
				serverThread.join(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			running = false;
			System.out.println("server stopped");
		}
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err
					.println("Usage: ASL_Server <port> <db-ip:db-port> <# of db-connections> <# of workers>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		String url = "jdbc:postgresql://" + args[1] + "/ASL";
		String user = "postgres";
		String password = "qwer1";
		String driverName = "org.postgresql.Driver";
		int nConns = Integer.parseInt(args[2]);
		int nWorkers = Integer.parseInt(args[3]);

		try {
			ASL_Server server = new ASL_Server(port, url, user, password,
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
