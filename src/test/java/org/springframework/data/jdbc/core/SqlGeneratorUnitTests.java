/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.core;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.PersistentPropertyPathTestUtils;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Unit tests for the {@link SqlGenerator}.
 *
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Bastian Wilhelm
 */
public class SqlGeneratorUnitTests {

	private SqlGenerator sqlGenerator;
	private RelationalMappingContext context = new RelationalMappingContext();

	@Before
	public void setUp() {

		this.sqlGenerator = createSqlGenerator(DummyEntity.class);
	}

	SqlGenerator createSqlGenerator(Class<?> type) {

		NamingStrategy namingStrategy = new PrefixingNamingStrategy();
		RelationalMappingContext context = new RelationalMappingContext(namingStrategy);
		RelationalPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(type);

		return new SqlGenerator(context, persistentEntity, new SqlGeneratorSource(context));
	}

	@Test // DATAJDBC-112
	public void findOne() {

		String sql = sqlGenerator.getFindOne();

		SoftAssertions softAssertions = new SoftAssertions();
		softAssertions.assertThat(sql) //
				.startsWith("SELECT") //
				.contains("dummy_entity.id1 AS id1,") //
				.contains("dummy_entity.x_name AS x_name,") //
				.contains("ref.x_l1id AS ref_x_l1id") //
				.contains("ref.x_content AS ref_x_content").contains(" FROM dummy_entity") //
				.contains("ON ref.dummy_entity = dummy_entity.id1") //
				.contains("WHERE dummy_entity.id1 = :id") //
				// 1-N relationships do not get loaded via join
				.doesNotContain("Element AS elements");
		softAssertions.assertAll();
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteFirstLevel() {

		String sql = sqlGenerator.createDeleteByPath(getPath("ref"));

		assertThat(sql).isEqualTo("DELETE FROM referenced_entity WHERE dummy_entity = :rootId");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteAllSecondLevel() {

		String sql = sqlGenerator.createDeleteByPath(getPath("ref.further"));

		assertThat(sql).isEqualTo(
				"DELETE FROM second_level_referenced_entity WHERE referenced_entity IN (SELECT x_l1id FROM referenced_entity WHERE dummy_entity = :rootId)");
	}

	@Test // DATAJDBC-112
	public void deleteAll() {

		String sql = sqlGenerator.createDeleteAllSql(null);

		assertThat(sql).isEqualTo("DELETE FROM dummy_entity");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteAllFirstLevel() {

		String sql = sqlGenerator.createDeleteAllSql(getPath("ref"));

		assertThat(sql).isEqualTo("DELETE FROM referenced_entity WHERE dummy_entity IS NOT NULL");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteSecondLevel() {

		String sql = sqlGenerator.createDeleteAllSql(getPath("ref.further"));

		assertThat(sql).isEqualTo(
				"DELETE FROM second_level_referenced_entity WHERE referenced_entity IN (SELECT x_l1id FROM referenced_entity WHERE dummy_entity IS NOT NULL)");
	}

	@Test // DATAJDBC-227
	public void deleteAllMap() {

		String sql = sqlGenerator.createDeleteAllSql(getPath("mappedElements"));

		assertThat(sql).isEqualTo("DELETE FROM element WHERE dummy_entity IS NOT NULL");
	}

	@Test // DATAJDBC-227
	public void deleteMapByPath() {

		String sql = sqlGenerator.createDeleteByPath(getPath("mappedElements"));

		assertThat(sql).isEqualTo("DELETE FROM element WHERE dummy_entity = :rootId");
	}

	@Test // DATAJDBC-131
	public void findAllByProperty() {

		// this would get called when DummyEntity is the element type of a Set
		String sql = sqlGenerator.getFindAllByProperty("back-ref", null, false);

		assertThat(sql).isEqualTo("SELECT dummy_entity.id1 AS id1, dummy_entity.x_name AS x_name, "
				+ "ref.x_l1id AS ref_x_l1id, ref.x_content AS ref_x_content, ref.x_further AS ref_x_further "
				+ "FROM dummy_entity LEFT OUTER JOIN referenced_entity AS ref ON ref.dummy_entity = dummy_entity.id1 "
				+ "WHERE back-ref = :back-ref");
	}

	@Test // DATAJDBC-131
	public void findAllByPropertyWithKey() {

		// this would get called when DummyEntity is th element type of a Map
		String sql = sqlGenerator.getFindAllByProperty("back-ref", "key-column", false);

		assertThat(sql).isEqualTo("SELECT dummy_entity.id1 AS id1, dummy_entity.x_name AS x_name, " //
				+ "ref.x_l1id AS ref_x_l1id, ref.x_content AS ref_x_content, ref.x_further AS ref_x_further, " //
				+ "dummy_entity.key-column AS key-column " //
				+ "FROM dummy_entity LEFT OUTER JOIN referenced_entity AS ref ON ref.dummy_entity = dummy_entity.id1 " //
				+ "WHERE back-ref = :back-ref");
	}

	@Test(expected = IllegalArgumentException.class) // DATAJDBC-130
	public void findAllByPropertyOrderedWithoutKey() {
		sqlGenerator.getFindAllByProperty("back-ref", null, true);
	}

	@Test // DATAJDBC-131
	public void findAllByPropertyWithKeyOrdered() {

		// this would get called when DummyEntity is th element type of a Map
		String sql = sqlGenerator.getFindAllByProperty("back-ref", "key-column", true);

		assertThat(sql).isEqualTo("SELECT dummy_entity.id1 AS id1, dummy_entity.x_name AS x_name, "
				+ "ref.x_l1id AS ref_x_l1id, ref.x_content AS ref_x_content, ref.x_further AS ref_x_further, "
				+ "dummy_entity.key-column AS key-column "
				+ "FROM dummy_entity LEFT OUTER JOIN referenced_entity AS ref ON ref.dummy_entity = dummy_entity.id1 "
				+ "WHERE back-ref = :back-ref " + "ORDER BY key-column");
	}

	@Test // DATAJDBC-264
	public void getInsertForEmptyColumnList() {

		SqlGenerator sqlGenerator = createSqlGenerator(IdOnlyEntity.class);

		String insert = sqlGenerator.getInsert(emptySet());

		assertThat(insert).endsWith("()");
	}

	@Test // DATAJDBC-334
	public void getInsertForQuotedColumnName() {

		SqlGenerator sqlGenerator = createSqlGenerator(EntityWithQuotedColumnName.class);

		String insert = sqlGenerator.getInsert(emptySet());

		assertThat(insert)
				.isEqualTo("INSERT INTO entity_with_quoted_column_name " + "(\"test_@123\") " + "VALUES (:test_123)");
	}

	@Test // DATAJDBC-334
	public void getUpdateForQuotedColumnName() {

		SqlGenerator sqlGenerator = createSqlGenerator(EntityWithQuotedColumnName.class);

		String update = sqlGenerator.getUpdate();

		assertThat(update).startsWith("UPDATE entity_with_quoted_column_name " + "SET ")
				.endsWith("\"test_@123\" = :test_123 " + "WHERE \"test_@id\" = :test_id");
	}

	private PersistentPropertyPath<RelationalPersistentProperty> getPath(String path) {
		return PersistentPropertyPathTestUtils.getPath(context, path, DummyEntity.class);
	}

	@SuppressWarnings("unused")
	static class DummyEntity {

		@Column("id1") @Id Long id;
		String name;
		ReferencedEntity ref;
		Set<Element> elements;
		Map<Integer, Element> mappedElements;
	}

	@SuppressWarnings("unused")
	static class ReferencedEntity {

		@Id Long l1id;
		String content;
		SecondLevelReferencedEntity further;
	}

	@SuppressWarnings("unused")
	static class SecondLevelReferencedEntity {

		@Id Long l2id;
		String something;
	}

	static class Element {
		@Id Long id;
		String content;
	}

	private static class PrefixingNamingStrategy implements NamingStrategy {

		@Override
		public String getColumnName(RelationalPersistentProperty property) {
			return "x_" + NamingStrategy.super.getColumnName(property);
		}

	}

	@SuppressWarnings("unused")
	static class IdOnlyEntity {

		@Id Long id;
	}

	static class EntityWithQuotedColumnName {

		@Id @Column("\"test_@id\"") Long id;
		@Column("\"test_@123\"") String name;
	}
}