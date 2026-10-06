package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V8__backfill_arkive_patient_idsTest {

	@Test
	void backfillsOnlyMissingIdsWithUniqueReadableValues() throws Exception {
		try (var connection = DriverManager.getConnection("jdbc:h2:mem:patient-id-backfill")) {
			try (var statement = connection.createStatement()) {
				statement.execute("create table TB_ARKIVE_ANIMAL (ID_ANIMAL bigint primary key, DS_ARKIVE_PATIENT_ID varchar(40))");
				statement.execute("insert into TB_ARKIVE_ANIMAL values (1, null)");
				statement.execute("insert into TB_ARKIVE_ANIMAL values (2, null)");
				statement.execute("insert into TB_ARKIVE_ANIMAL values (3, 'ARK-PET-22222-33333-44444-55555-666666')");
			}
			Context context = mock(Context.class);
			when(context.getConnection()).thenReturn(connection);

			new V8__backfill_arkive_patient_ids().migrate(context);

			Set<String> ids = new HashSet<>();
			try (var statement = connection.createStatement();
					var rows = statement.executeQuery("select ID_ANIMAL, DS_ARKIVE_PATIENT_ID from TB_ARKIVE_ANIMAL order by ID_ANIMAL")) {
				while (rows.next()) {
					String id = rows.getString(2);
					assertTrue(id.matches("ARK-PET-[2-9A-HJ-NP-Z]{5}(?:-[2-9A-HJ-NP-Z]{5}){3}-[2-9A-HJ-NP-Z]{6}"));
					assertTrue(ids.add(id));
					if (rows.getLong(1) == 3L) {
						assertEquals("ARK-PET-22222-33333-44444-55555-666666", id);
					}
				}
			}
			assertEquals(3, ids.size());
		}
	}
}
