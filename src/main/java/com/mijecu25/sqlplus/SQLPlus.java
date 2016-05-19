package com.mijecu25.sqlplus;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import jline.console.UserInterruptException;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mijecu25.messages.Messages;
import com.mijecu25.sqlplus.compiler.core.statement.Statement;
import com.mijecu25.sqlplus.connection.SQLPlusConnection;
import com.mijecu25.sqlplus.connection.SQLPlusMySQLConnection;
import com.mijecu25.sqlplus.parser.SQLPlusLex;
import com.mijecu25.sqlplus.parser.SQLPlusParser;

import jline.TerminalFactory;
import jline.console.ConsoleReader;

/**
 * SQLPlus add alerts to your sql queries.
 * 
 * @author Miguel Velez - miguelvelezmj25
 * @version 0.1.0.15
 */
public class SQLPlus {

    private static final String PROGRAM_NAME = "SQLPlus";
    private static final String EXIT = "exit";
    private static final String QUIT = "quit";
    private static final String PROMPT = "sqlplus> ";   
    private static final char END_OF_COMMAND = ';'; 
    private static final String WAIT_FOR_END_OF_COMMAND = "      -> "; 
    private static final String LICENSE_FILE = "LICENSE";
    private static final String APPLICATION_PROPERTIES_FILE = "application.properties";
    private static final String APPLICATION_PROPERTIES_FILE_VERSION = "application.version";
    
    private static SQLPlusConnection sqlPlusConnection;
    private static ConsoleReader console;
    
    private static final Logger logger = LogManager.getLogger(SQLPlus.class);
    
    public static void main(String[] args) throws IOException {
        // Create and load the properties from the application properties file
        Properties properties = new Properties();
        properties.load(SQLPlus.class.getClassLoader().getResourceAsStream(SQLPlus.APPLICATION_PROPERTIES_FILE));


        SQLPlus.logger.info("Initializing " + SQLPlus.PROGRAM_NAME + " version " + properties.getProperty(SQLPlus.APPLICATION_PROPERTIES_FILE_VERSION));

        // Check if the user is using a valid console (i.e. not from Eclipse)
        if (System.console() == null) {
            // The Console object for the JVM could not be found. Alert the user 
            SQLPlus.logger.fatal(Messages.FATAL + "A JVM Console object was not found. Try running " + SQLPlus.PROGRAM_NAME
                    + "from the command line");
            System.out.println(Messages.FATAL + SQLPlus.PROGRAM_NAME + " was not able to find your JVM's Console object. "
                    + "Try running " + SQLPlus.PROGRAM_NAME + " from the command line.");

            SQLPlus.exitSQLPlus();

            SQLPlus.logger.fatal(Messages.FATAL + Messages.QUIT_PROGRAM_ERROR(PROGRAM_NAME));
            return;
        }

        // UI intro
        System.out.println("Welcome to " + SQLPlus.PROGRAM_NAME + "! This program has a DSL to add alerts to various SQL DML events.");
        System.out.println("Be sure to use " + SQLPlus.PROGRAM_NAME + " from the command line.");
        System.out.println();

        // Get the version
        System.out.println("Version: " + properties.getProperty(SQLPlus.APPLICATION_PROPERTIES_FILE_VERSION));
        System.out.println();

        // Read the license file
        BufferedReader bufferedReader = null;
        bufferedReader = new BufferedReader(new FileReader(SQLPlus.LICENSE_FILE));

        // Read a line
        String line = bufferedReader.readLine();

        // While the line is not null
        while (line != null) {
            System.out.println(line);

            // Read a new lines
            line = bufferedReader.readLine();
        }

        // Close the buffer
        bufferedReader.close();
        System.out.println();

        // Create the jline console that allows us to remember commands, use arrow keys, and catch interruptions
        // from the user
        SQLPlus.console = new ConsoleReader();
        SQLPlus.console.setHandleUserInterrupt(true);

        try {
            // Get credentials from the user
            SQLPlus.logger.info("Create SQLPlusConnection");
            SQLPlus.createSQLPlusConnection();
        } 
        catch (NullPointerException npe) {
            // This exception can occur if the user is running the program where the JVM Console
            // object cannot be found
            SQLPlus.logger.fatal(Messages.FATAL + Messages.FATAL_EXIT(SQLPlus.PROGRAM_NAME, npe.getClass().getName()));
            System.out.println(Messages.FATAL + Messages.FATAL_EXCEPTION_ACTION(npe.getClass().getSimpleName())
                    + Messages.SPACE + Messages.CHECK_LOG_FILES);
            SQLPlus.exitSQLPlus();

            SQLPlus.logger.fatal(Messages.FATAL + Messages.QUIT_PROGRAM_ERROR(SQLPlus.PROGRAM_NAME));
            return;
        } 
        catch (SQLException sqle) {
            // This exception can occur when trying to establish a connection
            SQLPlus.logger.fatal(Messages.FATAL + Messages.FATAL_EXIT(SQLPlus.PROGRAM_NAME, sqle.getClass().getName()));
            System.out.println(Messages.FATAL + Messages.FATAL_EXCEPTION_ACTION(sqle.getClass().getSimpleName())
                    + Messages.SPACE + Messages.CHECK_LOG_FILES);
            SQLPlus.exitSQLPlus();

            SQLPlus.logger.fatal(Messages.FATAL + Messages.QUIT_PROGRAM_ERROR(SQLPlus.PROGRAM_NAME));
            return;
        } 
        catch (IllegalArgumentException iae) {
            // This exception can occur when trying to establish a connection
            SQLPlus.logger.fatal(Messages.FATAL + Messages.FATAL_EXIT(SQLPlus.PROGRAM_NAME, iae.getClass().getName()));
            System.out.println(Messages.FATAL + Messages.FATAL_EXCEPTION_ACTION(iae.getClass().getSimpleName())
                    + Messages.SPACE + Messages.CHECK_LOG_FILES);
            SQLPlus.exitSQLPlus();

            SQLPlus.logger.fatal(Messages.FATAL + Messages.QUIT_PROGRAM_ERROR(SQLPlus.PROGRAM_NAME));
            return;
        }
        catch(UserInterruptException uie) {
            SQLPlus.logger.warn(Messages.WARNING + "The user typed an interrupt instruction.");
            SQLPlus.exitSQLPlus();

            return ;
        }

        System.out.println("Connection established! Commands end with " + SQLPlus.END_OF_COMMAND);
        System.out.println("Type " + SQLPlus.EXIT + " or " + SQLPlus.QUIT + " to exit the application ");

        try {
            // Execute the input scanner
            while (true) {
                // Get a line from the user until the hit enter (carriage return, line feed/ new line).
                System.out.print(SQLPlus.PROMPT);
                try {
                    line = SQLPlus.console.readLine().trim();
                }
                catch(NullPointerException npe) {
                    // TODO test this behavior
                    // If this exception is catch, it is very likely that the user entered the end of line command.
                    // This means that the program should quit.
                    SQLPlus.logger.warn(Messages.WARNING + "The input from the user is null. It is very likely that" +
                            "the user entered the end of line command and they want to quit.");
                    SQLPlus.exitSQLPlus();

                    return;
                }
                
                // If the user did not enter anything
                if (line.isEmpty()) {
                    // Continue to the next iteration
                    continue;
                }

                // Logic to quit
                if (line.equals(SQLPlus.QUIT) || line.equals(SQLPlus.EXIT)) {
                    SQLPlus.logger.info("The user wants to quit " + SQLPlus.PROGRAM_NAME);
                    SQLPlus.exitSQLPlus();
                    break;
                }

                // Use a StringBuilder since jline works weird when it has read a line. The issue we were having was with the
                // end of command logic. jline does not keep the input from the user in the variable that was stored in. Each
                // time jline reads a new line, the variable is empty
                StringBuilder query = new StringBuilder();
                query.append(line);

                // While the user does not finish the command with the SQLPlus.END_OF_COMMAND
                while (query.charAt(query.length() - 1) != SQLPlus.END_OF_COMMAND) {
                    // Print the wait for command prompt and get the next line for the user
                    System.out.print(SQLPlus.WAIT_FOR_END_OF_COMMAND);
                    query.append(" ");
                    line = StringUtils.stripEnd(SQLPlus.console.readLine(), null);
                    query.append(line);
                }

                SQLPlus.logger.info("Raw input from the user: " + query);

                // Execute the antlr code to parse the user input
                SQLPlus.logger.info("Will parse the user input to determine what to execute");
                ANTLRStringStream input = new ANTLRStringStream(query.toString());
                SQLPlusLex lexer = new SQLPlusLex(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                SQLPlusParser parser = new SQLPlusParser(tokens);

                // TODO handle this better so that I print my own message when the string is not matched
                Statement statement = parser.sqlplus();

                if (statement == null) {
                    System.out.println("Return value is null");
                } else {
                    SQLPlus.sqlPlusConnection.execute(statement);
                }
            }
        }
        catch(RecognitionException e) {
            // TODO test this
            SQLPlus.logger.warn("There was an error while parsing the user input");
            e.printStackTrace();
        }
        catch(IllegalArgumentException iae) {
            // This exception can occur when a command is executed but it had illegal arguments. Most likely
            // it is a programmer's error and should be addressed by the developer.
            SQLPlus.logger.fatal(Messages.FATAL + Messages.FATAL_EXIT(SQLPlus.PROGRAM_NAME, iae.getClass().getName()));
            SQLPlus.exitSQLPlus();

            SQLPlus.logger.fatal(Messages.FATAL + Messages.QUIT_PROGRAM_ERROR(SQLPlus.PROGRAM_NAME));
            return ;
        }
        catch(UserInterruptException uie) {
            SQLPlus.logger.warn(Messages.WARNING + "The user typed an interrupt instruction.");
            SQLPlus.exitSQLPlus();

            return ;
        }
    }
    
    /**
     * Create an SQLPlusConnection by taking the credentials from the user.
     *
     * @throws IOException if there is an I/O error while reading input from the user.
     * @throws SQLException if there is an error while establishing a connection.
     */
    private static void createSQLPlusConnection() throws IOException, SQLException {                
        if(false) {
        System.out.println("You will now enter the credentials to connect to your database");
        
        // Add credentials
        System.out.print(SQLPlus.PROMPT + "Host(default " + SQLPlusConnection.getDefaultHost() + "): ");
        String host = SQLPlus.console.readLine().trim();
        SQLPlus.logger.info("User entered host:" + host);
        // TODO validate host        
          
//        if(!host.isEmpty()) {
//            // The Console object for the JVM could not be found. Alert the user and throw a
//            // NullPointerException that the caller will handle
//            SQLPlus.logger.fatal(Messages.FATAL + "The user wants to use a host that is not supported");
//            System.out.println(Messages.ERROR + SQLPlus.PROGRAM_NAME + " does not support the host that you entered");
//            
//            SQLPlus.logger.info("Throwing a " + IllegalArgumentException.class.getSimpleName() + " to the "
//                    + "calling class");
//            throw new IllegalArgumentException();  
//        }
        
        System.out.print(SQLPlus.PROMPT + "Database(default " + SQLPlusConnection.getDefaultDatabase() + "): ");
        String database = SQLPlus.console.readLine().trim();
        SQLPlus.logger.info("User entered database:" + database);
        
        if(database.isEmpty()) {
            database = SQLPlusConnection.getDefaultDatabase();
            SQLPlus.logger.info("Using default database:" + database);
        }
      
        String port = "";
        
        // While the port is not numeric
        while(!StringUtils.isNumeric(port)) {
            System.out.print(SQLPlus.PROMPT + "Port (default " + SQLPlusConnection.getDefaultPort() + "): ");
            port = SQLPlus.console.readLine().trim();
            SQLPlus.logger.info("Port entered: " + port);
            SQLPlus.logger.info("Port string length: " + port.length());
            
            // If the port is empty
            if(port.isEmpty()) {
                // Assume that the user wants to use the default port. Continue to the next step
                break;
            }
            
            // If the port has more than 5 numbers or is not numberic 
            if(port.length() > 5 || !StringUtils.isNumeric(port)) {
                SQLPlus.logger.warn("The user provided an invalid port number: " + port);
                System.out.println(Messages.WARNING + "You need to provided a valid port number "
                        + "from 0 to 65535");
                
                // Set the port to the empty string to ask the user again
                port = "";
            }
        }
        SQLPlus.logger.info("User entered port:" + port);
        
        String username = "";
        
        // While the username is empty
        while(username.isEmpty()) {
            System.out.print(SQLPlus.PROMPT + "Username: ");
            username = SQLPlus.console.readLine().trim();
            
            // If the username is empty
            if(username.isEmpty()) {
                SQLPlus.logger.warn("The user did not provide a username");
                System.out.println(Messages.WARNING + "You cannot have an empty username");
            }
        }
        SQLPlus.logger.info("User entered username:" + username);

        // Reset the jline console since we are going to use the regular console to securely get the password
        SQLPlus.resetConsole();
        
        // Get the console for safe password entry
        Console javaConsole = System.console();
        
        // If the console is null
        if(javaConsole == null) {
            // The Console object for the JVM could not be found. Alert the user and throw a
            // NullPointerException that the caller will handle
            SQLPlus.logger.fatal("A JVM Console object to enter a password was not found");
            System.out.println(Messages.ERROR + SQLPlus.PROGRAM_NAME + " was not able to find your JVM's Console object. "
                    + "Try running " + SQLPlus.PROGRAM_NAME + " from the command line.");
            
            SQLPlus.logger.info("Throwing a " + NullPointerException.class.getSimpleName() + " to the "
                    + "calling class");
            throw new NullPointerException();            
        }
        
        // Read the password without echoing the result
        char[] password = javaConsole.readPassword("%s", SQLPlus.PROMPT + "Password:");
                
        // If the password is null
        if(password == null) {
            // The Console object for the JVM could not be found. Alert the user and throw a
            // NullPointerException that the caller will handle
            SQLPlus.logger.fatal("The password captured by the JVM Console object returned null");
            System.out.println(Messages.ERROR + SQLPlus.PROGRAM_NAME + " was not able to get the password you entered from"
                    + "your JVM's Console object. Try running " + SQLPlus.PROGRAM_NAME + " from the command line or a different"
                    + "terminal program");
            
            SQLPlus.logger.info("Throwing a " + NullPointerException.class.getSimpleName() + " to the "
                    + "calling class");
            throw new NullPointerException(); 
        }
        SQLPlus.logger.info("User entered some password"); 
        System.out.println();

        // Create a SQLPlusConnection
        SQLPlusConnection sqlPlusConnection = null;

        // Create a connection based on the database system
        switch(database) {
            case SQLPlusMySQLConnection.MYSQL:
                // If the default port and host are used
                if(port.isEmpty() && host.isEmpty()) {
                    SQLPlus.logger.info("Connection with username, password");
                    sqlPlusConnection = SQLPlusMySQLConnection.getConnection(username, password);
                }
                // If the default port is used
                else if(port.isEmpty()) {
                    SQLPlus.logger.info("Connection with username, password, and host");
                    sqlPlusConnection = SQLPlusMySQLConnection.getConnection(username, password, host);
                }
                // All the values were provided by the user
                else {
                    SQLPlus.logger.info("Connection with all credentials");
                    sqlPlusConnection = SQLPlusMySQLConnection.getConnection(username, password, host, port);
                }
                break;  
            default:
                // Database entered is not supported
                SQLPlus.logger.fatal(Messages.FATAL + "The database system " + database + " is not supported");
                System.out.println(Messages.ERROR + SQLPlus.PROGRAM_NAME + " does not support the database that you entered");
  
                SQLPlus.logger.info("Throwing a " + IllegalArgumentException.class.getSimpleName() + " to the "
                        + "calling class");
                throw new IllegalArgumentException();
        }
  
        // Delete any traces of password in memory by filling the password array with with random characters
        // to minimize the lifetime of sensitive data in memory. Then call the garbage collections
        java.util.Arrays.fill(password, Character.MIN_VALUE);
        System.gc();
        
        // Recreate the jline console
        SQLPlus.console = new ConsoleReader();
        SQLPlus.console.setHandleUserInterrupt(true);
        }
        
        // TODO remove this which is for testing
        SQLPlus.logger.info("Connection with username, password, and host");
        SQLPlusConnection sqlPlusConnection = SQLPlusMySQLConnection.getConnection("root", new char[0], SQLPlusConnection.getDefaultHost());

        // TODO this does have to be in the final code
        SQLPlus.logger.info("Created and returning a SQLPlusConnection " + sqlPlusConnection);     
        SQLPlus.sqlPlusConnection = sqlPlusConnection;
    }
    
    /**
     * Exit SQLPlus.
     */
    private static void exitSQLPlus() {
        // If there is a SQLPlusConnection
        if(SQLPlus.sqlPlusConnection != null) {
            // Disconnect from the database
            SQLPlus.logger.info("Attempting to disconnect the SQLPlusConnection");
            SQLPlus.sqlPlusConnection.disconnect();
            SQLPlus.logger.info("Disconnected the SQLPlusConnection");
        }
        
        // Reset the console from jline
        SQLPlus.logger.info("Reset the console from jline");
        SQLPlus.resetConsole();
        
        SQLPlus.logger.info("Quitting " + SQLPlus.PROGRAM_NAME);
        System.out.println("Bye");
    }
    
    /**
     * Reset the console from the changes that jline has done.
     */
    private static void resetConsole() {
        // Reset the console
        try {
            SQLPlus.logger.info("About to reset the console from jline");
            TerminalFactory.get().restore();
            SQLPlus.logger.info("Reset the console from jline");
        } 
        catch(Exception e) {
            // This exception might never occur, but it is good practice to handle it
            SQLPlus.logger.warn(Messages.WARNING + "Error when attempting to reset the console from the changes made by jline", e);
            System.out.println(Messages.WARNING + "There was a error when trying to reset your console to its normal state");
            System.out.println(e.getMessage());
            System.out.println(Messages.WARNING + "Close this console window and open a new one to avoid any issues");
        }
    }
        
}
