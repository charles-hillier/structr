package org.structr.core.property;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.TestEnum;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.OneFourOneToOne;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;

/**
 *
 * @author Christian Morgner
 */
public class EnumPropertyTest extends StructrTest {

	public void testSimpleProperty() {
		
		try {
			final PropertyMap properties    = new PropertyMap();
			
			properties.put(TestFour.enumProperty, TestEnum.Status1);
			
			final TestFour testEntity        = createTestNode(TestFour.class, properties);
			
			assertNotNull(testEntity);

			// check value from database
			assertEquals(TestEnum.Status1, testEntity.getProperty(TestFour.enumProperty));
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
	
	public void testSimpleSearchOnNode() {
		
		try {
			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<TestEnum> key = TestFour.enumProperty;
			
			properties.put(key, TestEnum.Status1);
			
			final TestFour testEntity     = createTestNode(TestFour.class, properties);
			
			assertNotNull(testEntity);

			// check value from database
			assertEquals(TestEnum.Status1, testEntity.getProperty(key));
			
			Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, TestEnum.Status1).getResult();
			
			assertEquals(1, result.size());
			assertEquals(testEntity, result.get(0));
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
		
	}
	
	public void testSimpleSearchOnRelationship() {
		
		try {
			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<TestEnum> key = OneFourOneToOne.enumProperty;
			
			assertNotNull(testOne);
			assertNotNull(testFour);
			
			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);
			
			assertNotNull(testEntity);

			try {
				app.beginTx();
				testEntity.setProperty(key, TestEnum.Status1);
				app.commitTx();

			} finally {

				app.finishTx();
			}
			
			// check value from database
			assertEquals(TestEnum.Status1, testEntity.getProperty(key));
			
			Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, TestEnum.Status1).getResult();
			
			assertEquals(1, result.size());
			assertEquals(testEntity, result.get(0));
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
}


