package org.sagebionetworks.template.repo.athena;

import java.util.Objects;

import org.sagebionetworks.template.Constants;

/**
 * DTO that describes an Athena query that is run on a cron schedule, a separate step function that
 * executes the query will be created for each query
 */
public class RecurrentAthenaQuery {

	private String database;
	private String queryName;
	private String queryPath;
	private String queryString;
	private String cronExpression;
	private String destinationQueue;
	
	public String getDatabase() {
		return database;
	}
	
	public void setDatabase(String database) {
		this.database = database;
	}

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public String getQueryPath() {
		return queryPath;
	}

	public void setQueryPath(String queryPath) {
		this.queryPath = queryPath;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getDestinationQueue() {
		return destinationQueue;
	}

	public void setDestinationQueue(String destinationQueue) {
		this.destinationQueue = destinationQueue;
	}
	
	public String getDestinationQueueReferenceName() {
		return Constants.createCamelCaseName(destinationQueue, "_");
	}

	@Override
	public int hashCode() {
		return Objects.hash(cronExpression, database, destinationQueue, queryName, queryPath, queryString);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RecurrentAthenaQuery other = (RecurrentAthenaQuery) obj;
		return Objects.equals(cronExpression, other.cronExpression) && Objects.equals(database, other.database)
				&& Objects.equals(destinationQueue, other.destinationQueue) && Objects.equals(queryName, other.queryName)
				&& Objects.equals(queryPath, other.queryPath) && Objects.equals(queryString, other.queryString);
	}

	@Override
	public String toString() {
		return "RecurrentAthenaQuery [dataBase=" + database + ", queryName=" + queryName + ", queryPath=" + queryPath + ", queryString="
				+ queryString + ", cronExpression=" + cronExpression + ", destinationQueue=" + destinationQueue + "]";
	}

}
