package ca.uhn.fhir.jpa.migrate;

import ca.uhn.fhir.jpa.migrate.taskdef.ColumnTypeEnum;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JdbcUtilsTest {

	private static final Logger ourLog = LoggerFactory.getLogger(JdbcUtilsTest.class);
	@Mock
	DataSource myDataSource;
	@Mock
	Connection myConnection;
	@Mock
	DatabaseMetaData myDatabaseMetaData;
	@Mock
	ResultSet myResultSet;
	@Mock
	DriverTypeEnum.ConnectionProperties myConnectionProperties;
	@Mock
	TransactionTemplate myTxTemplate;

	@Test
	public void testGetColumnType_verifyTypeMappings() throws SQLException {
		testGetColumnType_verifyTypeMapping(Types.BIT, ColumnTypeEnum.BOOLEAN);
		testGetColumnType_verifyTypeMapping(Types.BOOLEAN, ColumnTypeEnum.BOOLEAN);
		testGetColumnType_verifyTypeMapping(Types.VARCHAR, ColumnTypeEnum.STRING);
		testGetColumnType_verifyTypeMapping(Types.NUMERIC, ColumnTypeEnum.LONG);
		testGetColumnType_verifyTypeMapping(Types.BIGINT, ColumnTypeEnum.LONG);
		testGetColumnType_verifyTypeMapping(Types.DECIMAL, ColumnTypeEnum.LONG);
		testGetColumnType_verifyTypeMapping(Types.INTEGER, ColumnTypeEnum.INT);
		testGetColumnType_verifyTypeMapping(Types.TIMESTAMP, ColumnTypeEnum.DATE_TIMESTAMP);
		testGetColumnType_verifyTypeMapping(Types.TIMESTAMP_WITH_TIMEZONE, ColumnTypeEnum.DATE_TIMESTAMP);
		testGetColumnType_verifyTypeMapping(Types.BLOB, ColumnTypeEnum.BLOB);
		testGetColumnType_verifyTypeMapping(Types.CLOB, ColumnTypeEnum.CLOB);
		testGetColumnType_verifyTypeMapping(Types.DOUBLE, ColumnTypeEnum.DOUBLE);
		testGetColumnType_verifyTypeMapping(Types.FLOAT, ColumnTypeEnum.FLOAT);

	}

	private void testGetColumnType_verifyTypeMapping(int theExistingDataType, ColumnTypeEnum theExpectedColumnType) throws SQLException {
		when(myResultSet.next()).thenReturn(true).thenReturn(false);
		when(myResultSet.getString("TABLE_NAME")).thenReturn("TEST_TABLE");
		when(myResultSet.getString("COLUMN_NAME")).thenReturn("TEST_COLUMN");
		when(myResultSet.getInt("DATA_TYPE")).thenReturn(theExistingDataType);
		when(myResultSet.getLong("COLUMN_SIZE")).thenReturn(17L);

		when(myDatabaseMetaData.getColumns("Catalog", "Schema", "TEST_TABLE", null)).thenReturn(myResultSet);
		when(myConnection.getMetaData()).thenReturn(myDatabaseMetaData);
		when(myConnection.getCatalog()).thenReturn("Catalog");
		when(myConnection.getSchema()).thenReturn("Schema");
		when(myDataSource.getConnection()).thenReturn(myConnection);
		DriverTypeEnum.ConnectionProperties myConnectionProperties = DriverTypeEnum.H2_EMBEDDED.newConnectionProperties(myDataSource);
		JdbcUtils.ColumnType testColumnType = JdbcUtils.getColumnType(myConnectionProperties, "TEST_TABLE", "TEST_COLUMN");
		ourLog.info("Column type: {}", testColumnType);

		assertEquals(theExpectedColumnType, testColumnType.getColumnTypeEnum());
	}

	@Test
	public void testGetIndexNames_verifyNullHandling() throws SQLException {

		// setup
		ResultSet mockTableResultSet = mock(ResultSet.class);
		when(mockTableResultSet.next()).thenReturn(true, false);
		when(mockTableResultSet.getString("TABLE_NAME")).thenReturn("TEST_TABLE");
		when(mockTableResultSet.getString("TABLE_TYPE")).thenReturn("USER TABLE");

		ResultSetMetaData mockResultSetMetaData = mock(ResultSetMetaData.class);
		when(mockResultSetMetaData.getColumnCount()).thenReturn(0);

		ResultSet mockIndicesResultSet = mock(ResultSet.class);
		when(mockIndicesResultSet.next()).thenReturn(true, true, true, false);
		when(mockIndicesResultSet.getString("INDEX_NAME")).thenReturn("IDX_1", null, "idx_2");
		when(mockIndicesResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

		ResultSet mockUniqueIndicesResultSet = mock(ResultSet.class);
		when(mockUniqueIndicesResultSet.next()).thenReturn(true, true, false);
		when(mockUniqueIndicesResultSet.getString("INDEX_NAME")).thenReturn(null, "Idx_3");
		when(mockUniqueIndicesResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

		when(myDatabaseMetaData.getTables("Catalog", "Schema", null, null)).thenReturn(mockTableResultSet);
		when(myDatabaseMetaData.getIndexInfo("Catalog", "Schema", "TEST_TABLE", false, true)).thenReturn(mockIndicesResultSet);
		when(myDatabaseMetaData.getIndexInfo("Catalog", "Schema", "TEST_TABLE", true, true)).thenReturn(mockUniqueIndicesResultSet);
		when(myConnection.getMetaData()).thenReturn(myDatabaseMetaData);
		when(myConnection.getCatalog()).thenReturn("Catalog");
		when(myConnection.getSchema()).thenReturn("Schema");
		when(myDataSource.getConnection()).thenReturn(myConnection);
		DriverTypeEnum.ConnectionProperties myConnectionProperties = DriverTypeEnum.H2_EMBEDDED.newConnectionProperties(myDataSource);

		//execute
		Set<String> indexNames = JdbcUtils.getIndexNames(myConnectionProperties, "TEST_TABLE");

		// verify
		assertThat(indexNames).hasSize(3);
		assertThat(indexNames).contains("IDX_1");
		assertThat(indexNames).contains("IDX_2");
		assertThat(indexNames).contains("IDX_3");
	}

	@Test
	void testGetSequenceInformation() throws SQLException {
		// setup
		when(myDataSource.getConnection()).thenReturn(myConnection);
		when(myConnectionProperties.getDataSource()).thenReturn(myDataSource);
		when(myConnectionProperties.getTxTemplate()).thenReturn(myTxTemplate);

		SequenceInformation sequenceInformation = mock(SequenceInformation.class);
		List<SequenceInformation> sequenceInformationList = List.of(
			sequenceInformation
		);

		when(myTxTemplate.execute(any())).thenAnswer(invocation -> sequenceInformationList);

		// execute
		List<SequenceInformation> sequenceInformationResult = JdbcUtils.getSequenceInformation(myConnectionProperties);

		// verify
		assertThat(sequenceInformationResult).hasSize(1);
		assertEquals(sequenceInformation, sequenceInformationResult.get(0));
	}

	@Test
	void testGetSequenceName() throws SQLException {
		// setup
		String expectedSequenceName = "TEST_SEQ";
		when(myDataSource.getConnection()).thenReturn(myConnection);
		when(myConnectionProperties.getDataSource()).thenReturn(myDataSource);
		when(myConnectionProperties.getTxTemplate()).thenReturn(myTxTemplate);

		SequenceInformation sequenceInformation = mock(SequenceInformation.class);
		QualifiedSequenceName sequenceName = mock(QualifiedSequenceName.class);
		Identifier identifier = mock(Identifier.class);
		when(identifier.getText()).thenReturn(expectedSequenceName);
		when(sequenceName.getSequenceName()).thenReturn(identifier);
		when(sequenceInformation.getSequenceName()).thenReturn(sequenceName);

		List<SequenceInformation> sequenceInformationList = List.of(
			sequenceInformation
		);

		when(myTxTemplate.execute(any())).thenAnswer(invocation -> sequenceInformationList);

		// execute
		Set<String> sequenceNames= JdbcUtils.getSequenceNames(myConnectionProperties);

		// verify
		assertThat(sequenceNames).hasSize(1);
		assertEquals(expectedSequenceName, sequenceNames.stream().findFirst().get());
	}
}
