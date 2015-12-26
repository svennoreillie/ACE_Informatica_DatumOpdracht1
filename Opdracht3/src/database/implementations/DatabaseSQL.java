package database.implementations;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import common.DBException;
import common.DBMissingException;
import database.DataService;
import database.helpers.ReflectionPropertyHelper;
import database.internalInterface.DataReadWriteService;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import model.ModelBase;

public class DatabaseSQL<T extends ModelBase> extends ReflectionDatabase<T>implements DataReadWriteService<T> {

	public DatabaseSQL(Class<T> classType) {
		super(classType);

		// Create the table in the database
		this.createTable();
	}

	@Override
	public List<T> readDB() throws DBMissingException, DBException {
		try {
			Connection conn = this.createConnection();
			Statement statement = conn.createStatement();
			ResultSet results = statement.executeQuery("SELECT FROM " + this.classType.getName());

			// load structure of T
			List<ReflectionPropertyHelper> genericFieldArray = getFields();
			List<T> returnList = new ArrayList<T>();

			while (results.next()) {
				T instance = this.classType.newInstance();
				for (ReflectionPropertyHelper property : genericFieldArray) {
					Object value = null;
					Method set = property.getSetter();
					String colName = property.getName();
					switch (property.getPropertyType().getName()) {
					case "java.lang.String":
						value = results.getString(colName);
						break;
					case "int":
						value = results.getInt(colName);
						break;
					case "boolean":
						value = results.getBoolean(colName);
						break;
					case "java.math.BigDecimal":
						value = results.getBigDecimal(colName);
						break;
					case "java.lang.Double":
						value = results.getDouble(colName);
						break;
					default:
						//ModelBase case
						if (ModelBase.class.isAssignableFrom(property.getPropertyType())) {
							// get from dedicated DataService
							DataService<? extends ModelBase> strategy = GetDedicatedDataService(property.getPropertyType().getName());
							int id = results.getInt(colName);
							ModelBase reference = strategy.get(id);
							set.invoke(instance, property.getPropertyType().cast(reference));
							value = null;
						}
						break;
					}
					if (value != null) set.invoke(instance, value);
				}
				returnList.add(instance);
			}
			
			results.close();
			statement.close();
			conn.close();
			
			return returnList;
		} catch (SQLException e) {
			throw new DBException("Error reading from SQL database " + this.classType.getName(), e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//TODO :: exceptions gooien
		return null;
	}

	@Override
	public void writeDB(List<T> list) throws DBMissingException, DBException {
		try {
			// clear entire table
			// TODO:: improve this, worst idea ever
			try {
				Connection conn = this.createConnection();
				Statement clearStatement = conn.createStatement();
				clearStatement.executeQuery("DELETE FROM " + this.classType.getName());
				clearStatement.close();
				conn.close();
			} catch (SQLException e) {
				System.out.println("Failure clearing db before write => " + e.toString());
			}

			// load structure of T
			List<ReflectionPropertyHelper> genericFieldArray = getFields();

			String insertQuery = "INSER INTO " + this.classType.getName() + " VALUES ";
			for (T item : list) {
				String rowQuery = "";
				for (ReflectionPropertyHelper property : genericFieldArray) {
					Object value;
					try {
						if (ModelBase.class.isAssignableFrom(property.getPropertyType())) {
							// found property is a ModelBase => lookup id to
							// write in this table
							// leave storing of the found ModelBase to an
							// instance of DatabaseSQL with type of found
							// ModelBase
							String classTypeString = property.getPropertyType().getName();
							// ugly helper method to create a DataService, could
							// not find a way to do it generic
							DataService<? extends ModelBase> strategy = GetDedicatedDataService(classTypeString);

							// get the value of this property for the current
							// item in the list & save to other DataService
							ModelBase model = (ModelBase) property.getGetter().invoke(item);
							strategy.add(model);
							// save id of this item in our own excel
							value = model.getId();
						} else {
							// normal property, get the object for this item in
							// the list
							value = property.getGetter().invoke(item);
						}

						if (value != null) {
							if (rowQuery.length() > 0)
								rowQuery += ", ";

							switch (value.getClass().getName()) {
							case "java.lang.String":
								rowQuery += ("\"" + value.toString() + "\"");
								break;
							case "boolean":
								if ((boolean) value) {
									rowQuery += "TRUE";
								} else {
									rowQuery += "FALSE";
								}
								break;
							default:
								rowQuery += value.toString();
								break;
							}
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// TODO :: logging
					} catch (InvocationTargetException e) {
						// error invoking the getter => just continue
						// TODO:: logging
					}
				}

				insertQuery += "(" + rowQuery + ")";
			}

			Connection conn = this.createConnection();
			Statement clearStatement = conn.createStatement();
			clearStatement.executeQuery(insertQuery);
			clearStatement.close();
			conn.close();
		} catch (Exception e) {
			throw new DBException("Error writing to SQL database " + this.classType.getName(), e);
		}
	}

	private Connection createConnection() throws SQLException {
		String dbURL = "jdbc:derby:resources/database;create=true";
		Connection conn = DriverManager.getConnection(dbURL);
		return conn;
	}

	private void createTable() {
		try {
			List<String> columnsList = new ArrayList<String>();
			columnsList.add("Id INTEGER PRIMARY KEY");

			// Get collection of fields to fill column list
			List<ReflectionPropertyHelper> fields = this.getFields();
			for (ReflectionPropertyHelper property : fields) {
				String column = "";
				if (property.getName() == "Id") {
					// continue, automatically added to fields
				} else if (ModelBase.class.isAssignableFrom(property.getPropertyType())) {
					column = property.getName() + " INTEGER";
				} else {
					// normal property
					column = property.getName() + " ";
					switch (property.getPropertyType().getName()) {
					case "java.lang.String":
						column += "VARCHAR(250)";
						break;
					case "int":
						column += "INTEGER";
						break;
					case "boolean":
						column += "BOOLEAN";
						break;
					case "java.math.BigDecimal":
						column += "DECIMAL";
						break;
					case "java.lang.Double":
						column += "FLOAT";
						break;
					default:
						break;
					}
				}

				columnsList.add(column);
			}

			// Create table query
			String createTableQry = "CREATE TABLE ";
			createTableQry += this.classType.getName();
			createTableQry += " (";
			for (String string : columnsList) {
				createTableQry += string;
			}
			createTableQry += ")";

			// Get a connection & statement
			Connection conn = createConnection();
			try {
				Statement statement = conn.createStatement();
				statement.execute(createTableQry);
				statement.close();
			} catch (SQLException e) {
				if (e.getSQLState() == "X0Y32") {
					// table already exists => all is well
				} else {
					throw e;
				}
			}
			conn.close();

			// verder kijken op :
			// http://www.tutorialspoint.com/jdbc/jdbc-sample-code.htm

		} catch (SQLException e) {
			System.out.println(e.toString());
		}
	}

}
