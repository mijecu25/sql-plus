package com.mijecu25.sqlplus.compiler.core.statement.dml;

import com.mijecu25.messages.Messages;
import com.mijecu25.sqlplus.compiler.core.statement.Statement;
import com.mijecu25.sqlutils.SQLUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * This class represents the "select...." SQL statement. It prints the columns that match the query.
 *
 * @author Miguel Velez - miguelvelezmj25
 * @version 0.1.0.1
 */
public class StatementSelectExpression extends StatementDML {

    private static final Logger logger = LogManager.getLogger(StatementSelectExpression.class);

    public StatementSelectExpression(List<String> columns, List<String> tables) {
        super(StatementDML.SELECT + StatementDML.unrollList(columns)
                + StatementDML.FROM + StatementDML.unrollList(tables),
                columns, tables);
        StatementSelectExpression.logger.info("Parsed and created a StatementSelectExpression");
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        StatementSelectExpression.logger.info("Will execute the code to query the database using a select statement");

        // If the connection is null
        if(connection == null) {
            IllegalArgumentException iae = new IllegalArgumentException();
            StatementSelectExpression.logger.fatal(Messages.FATAL + "The connection passed to execute the statement "
                    + "cannot be null");
            System.out.println(Messages.FATAL_EXCEPTION_ACTION(iae.getClass().getSimpleName()) + Messages.SPACE
                    + Messages.CHECK_LOG_FILES);
            StatementSelectExpression.logger.warn(Messages.WARNING + "Throwing a " + iae.getClass().getSimpleName()
                    + " to the calling class");
            throw iae;
        }

        // Set the connection
        this.connection = connection;

        try {
            // Execute the query
            java.sql.Statement statement = this.connection.createStatement();
            this.resultSet = statement.executeQuery(this.statement);

            // The result from the query is null
            if(this.resultSet == null) {
                // Throw an exception because this will be very weird. Also,
                // if there is no response, we do not want to continue executing
                throw new SQLException();
            }

            this.printResult();

            // Close the result set and statement
            this.resultSet.close();
            statement.close();
        }
        catch(SQLException sqle) {
            StatementSelectExpression.logger.warn(Messages.WARNING + "Error when executing " + this, sqle);
            System.out.println(Messages.WARNING + "(" + sqle.getErrorCode() + ") (" + sqle.getSQLState() + ") "
                    + sqle.getMessage());

            StatementSelectExpression.logger.warn(Messages.WARNING + "Throwing a " + sqle.getClass().getSimpleName()
                    + " to the calling class");
            throw sqle;
        }
        
    }

    @Override
    protected void printResult() {
        StatementSelectExpression.logger.info("Printing the result");
        try{
            // Get the medatadata from the result set
            ResultSetMetaData resultSetMetaData;
            resultSetMetaData = this.resultSet.getMetaData();

            // Get the maximum database name length
            int maxDatabaseNameLength = SQLUtils.maxDatabaseNameLength(this.connection);
            // The total length is added by 4 for the 2 borders and the 2 spaces on either side
            int lineTotalLength = maxDatabaseNameLength + 4;
            // Build the horizontal border based on the length
            StringBuilder line = new StringBuilder();
            line.append(Statement.buildHorizontalBorder(lineTotalLength) + "\n");

            // Print the label of the column
            String label = resultSetMetaData.getColumnLabel(1);

            line.append(StatementSelectExpression.VERTICAL_BORDERL + " ");
            line.append(label);
            line.append(StringUtils.repeat(" ", lineTotalLength - 3 - label.length()));
            line.append(StatementSelectExpression.VERTICAL_BORDERL + "\n");

            // Build a border after the name of the column
            line.append(Statement.buildHorizontalBorder(lineTotalLength) + "\n");

            // While the are more rows to process
            while (resultSet.next()) {
                // Get and print the current row value
                String row = this.resultSet.getString(1);

                line.append(StatementSelectExpression.VERTICAL_BORDERL + " ");
                line.append(row);
                line.append(StringUtils.repeat(" ", lineTotalLength - 3 - row.length()));
                line.append(StatementSelectExpression.VERTICAL_BORDERL + "\n");
            }

            // Build a border after the all of the rows
            line.append(Statement.buildHorizontalBorder(lineTotalLength) + "\n");

            System.out.println(line);
        }
        catch (SQLException sqle) {
            StatementSelectExpression.logger.warn(Messages.WARNING + "Error when printing the result of " + this, sqle);
            System.out.println(Messages.WARNING + "(" + sqle.getErrorCode() + ") (" + sqle.getSQLState() + ") "
                    + "Could not print the result." + Messages.SPACE + Messages.WARNING + Messages.CHECK_LOG_FILES);
        }
        
    }

    @Override
    public String toString() {
        return "StatementSelectExpression [statement=" + this.statement + "]";
    }
    
}