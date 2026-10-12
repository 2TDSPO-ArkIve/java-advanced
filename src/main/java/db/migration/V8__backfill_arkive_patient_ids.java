package db.migration;

import br.com.fiap.arkive.service.ArkivePatientIdGenerator;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class V8__backfill_arkive_patient_ids extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		ArkivePatientIdGenerator generator = new ArkivePatientIdGenerator();
		Set<String> allocated = new HashSet<>();
		try (Statement statement = context.getConnection().createStatement();
			 ResultSet rows = statement.executeQuery("SELECT DS_ARKIVE_PATIENT_ID FROM TB_ARKIVE_ANIMAL WHERE DS_ARKIVE_PATIENT_ID IS NOT NULL")) {
			while (rows.next()) {
				allocated.add(rows.getString(1));
			}
		}

		try (Statement statement = context.getConnection().createStatement();
			 ResultSet rows = statement.executeQuery("SELECT ID_ANIMAL FROM TB_ARKIVE_ANIMAL WHERE DS_ARKIVE_PATIENT_ID IS NULL");
			 PreparedStatement update = context.getConnection().prepareStatement(
					 "UPDATE TB_ARKIVE_ANIMAL SET DS_ARKIVE_PATIENT_ID = ? WHERE ID_ANIMAL = ? AND DS_ARKIVE_PATIENT_ID IS NULL")) {
			while (rows.next()) {
				String patientId;
				do {
					patientId = generator.generate();
				} while (!allocated.add(patientId));
				update.setString(1, patientId);
				update.setLong(2, rows.getLong(1));
				update.addBatch();
			}
			update.executeBatch();
		}
	}
}
