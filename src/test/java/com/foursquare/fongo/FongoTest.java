package com.foursquare.fongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class FongoTest {

  private static final String BasicBSONList = null;

  @Test
  public void testGetDb() {
    Fongo fongo = newFongo();
    DB db = fongo.getDB("db");
    assertNotNull(db);
    assertSame("getDB should be idempotent", db, fongo.getDB("db"));
    assertEquals(Arrays.asList(db), fongo.getUsedDatabases());
    assertEquals(Arrays.asList("db"), fongo.getDatabaseNames());
  }
  
  @Test
  public void testGetCollection() {
    Fongo fongo = newFongo();
    DB db = fongo.getDB("db");
    DBCollection collection = db.getCollection("coll");
    assertNotNull(collection);
    assertSame("getCollection should be idempotent", collection, db.getCollection("coll"));
    assertSame("getCollection should be idempotent", collection, db.getCollectionFromString("coll"));
    assertEquals(new HashSet<String>(Arrays.asList("coll")), db.getCollectionNames());
  }
  
  @Test 
  public void testCountCommand() {
    DBCollection collection = newCollection();
    assertEquals(0, collection.count());
  }
  
  @Test 
  public void testCountWithQueryCommand() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("n", 1));
    collection.insert(new BasicDBObject("n", 2));
    collection.insert(new BasicDBObject("n", 2));
    assertEquals(2, collection.count(new BasicDBObject("n", 2)));
  }
  
  
  @Test
  public void testInsertIncrementsCount() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("name", "jon"));
    assertEquals(1, collection.count());
  }
  
  @Test
  public void testFindOne() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("name", "jon"));
    DBObject result = collection.findOne();
    assertNotNull(result);
    assertNotNull("should have an _id", result.get("_id"));
  }
  
  @Test
  public void testFindOneIn() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    DBObject result = collection.findOne(new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(1,2))));
    assertEquals(new BasicDBObject("_id", 1), result);
  }
  
  @Test
  public void testFindOneById() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    DBObject result = collection.findOne(new BasicDBObject("_id", 1));
    assertEquals(new BasicDBObject("_id", 1), result);
    
    assertEquals(null, collection.findOne(new BasicDBObject("_id", 2)));
  }
  
  @Test
  public void testFindWithQuery() {
    DBCollection collection = newCollection();
    
    collection.insert(new BasicDBObject("name", "jon"));
    collection.insert(new BasicDBObject("name", "leo"));
    collection.insert(new BasicDBObject("name", "neil"));
    collection.insert(new BasicDBObject("name", "neil"));
    DBCursor cursor = collection.find(new BasicDBObject("name", "neil"));
    assertEquals("should have two neils", 2, cursor.toArray().size());
  }
  
  @Test
  public void testFindWithLimit() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    collection.insert(new BasicDBObject("_id", 2));
    collection.insert(new BasicDBObject("_id", 3));
    collection.insert(new BasicDBObject("_id", 4));

    DBCursor cursor = collection.find().limit(2);
    assertEquals(Arrays.asList(
        new BasicDBObject("_id", 1),
        new BasicDBObject("_id", 2)
    ), cursor.toArray());
  }
  
  @Test
  public void testFindWithSkipLimit() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    collection.insert(new BasicDBObject("_id", 2));
    collection.insert(new BasicDBObject("_id", 3));
    collection.insert(new BasicDBObject("_id", 4));

    DBCursor cursor = collection.find().limit(2).skip(2);
    assertEquals(Arrays.asList(
        new BasicDBObject("_id", 3),
        new BasicDBObject("_id", 4)
    ), cursor.toArray());
  }
  
  @Test
  public void testSort() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("a", 1).append("_id", 1));
    collection.insert(new BasicDBObject("a", 2).append("_id", 2));
    collection.insert(new BasicDBObject("_id", 5));
    collection.insert(new BasicDBObject("a", 3).append("_id", 3));
    collection.insert(new BasicDBObject("a", 4).append("_id", 4));

    DBCursor cursor = collection.find().sort(new BasicDBObject("a", -1));
    assertEquals(Arrays.asList(
        new BasicDBObject("a", 4).append("_id", 4),
        new BasicDBObject("a", 3).append("_id", 3),
        new BasicDBObject("a", 2).append("_id", 2),
        new BasicDBObject("a", 1).append("_id", 1),
        new BasicDBObject("_id", 5)
    ), cursor.toArray());
  }
  
  @Test
  public void testBasicUpdate() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    collection.insert(new BasicDBObject("_id", 2).append("b", 5));
    collection.insert(new BasicDBObject("_id", 3));
    collection.insert(new BasicDBObject("_id", 4));
    
    collection.update(new BasicDBObject("_id", 2), new BasicDBObject("a", 5));
    
    assertEquals(new BasicDBObject("_id", 2).append("a", 5), 
        collection.findOne(new BasicDBObject("_id",2)));
  }
  
  @Test
  public void testNoUpdateId() {
    DBCollection collection = newCollection();
    
    try {
      collection.update(new BasicDBObject("_id", 1).append("b", 2), new BasicDBObject("_id", 1));
      fail("should throw exception");
    } catch (MongoException e) {
      
    }
  }
  
  @Test
  public void testUpsert() {
    DBCollection collection = newCollection();
    collection.update(new BasicDBObject("_id", 1).append("n", "jon"), 
        new BasicDBObject("$inc", new BasicDBObject("a", 1)), true, false);
    assertEquals(new BasicDBObject("_id", 1).append("n", "jon").append("a", 1),
        collection.findOne());
  }
  
  @Test
  public void testUpsertWithConditional() {
    DBCollection collection = newCollection();
    collection.update(new BasicDBObject("_id", 1).append("b", new BasicDBObject("$gt", 5)), 
        new BasicDBObject("$inc", new BasicDBObject("a", 1)), true, false);
    assertEquals(new BasicDBObject("_id", 1).append("a", 1),
        collection.findOne());
  }
  
  @Test
  public void testUpsertWithIdIn() {
    DBCollection collection = newCollection();
    DBObject query = new BasicDBObjectBuilder().push("_id").append("$in", Arrays.asList(1)).pop().get();
    DBObject update = new BasicDBObjectBuilder()
      .push("$push").push("n").append("_id", 2).append("u", 3).pop().pop()
      .push("$inc").append("c",4).pop().get();
    DBObject expected = new BasicDBObjectBuilder().append("_id", 1).append("n", Arrays.asList(new BasicDBObject("_id", 2).append("u", 3))).append("c", 4).get();
    collection.update(query, update, true, false);
    assertEquals(expected, collection.findOne());
  }
  
  @Test
  public void testUpdatetWithIdIn() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    DBObject query = new BasicDBObjectBuilder().push("_id").append("$in", Arrays.asList(1)).pop().get();
    DBObject update = new BasicDBObjectBuilder()
      .push("$push").push("n").append("_id", 2).append("u", 3).pop().pop()
      .push("$inc").append("c",4).pop().get();
    DBObject expected = new BasicDBObjectBuilder().append("_id", 1).append("n", Arrays.asList(new BasicDBObject("_id", 2).append("u", 3))).append("c", 4).get();
    collection.update(query, update, false, true);
    assertEquals(expected, collection.findOne());
  }
  
  @Test
  public void testUpdatetWithObjectId() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", new BasicDBObject("n", 1)));
    DBObject query = new BasicDBObject("_id", new BasicDBObject("n", 1));
    DBObject update = new BasicDBObject("$set", new BasicDBObject("a", 1));
    collection.update(query, update, false, false);
    assertEquals(new BasicDBObject("_id", new BasicDBObject("n", 1)).append("a", 1), collection.findOne());
  }
  
  @Test
  public void testUpdateWithIdInMulti() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1),new BasicDBObject("_id", 2));
    collection.update(new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(1,2))), 
        new BasicDBObject("$set", new BasicDBObject("n", 1)), false, true);
    List<DBObject> results = collection.find().toArray();
    assertEquals(Arrays.asList(
        new BasicDBObject("_id", 1).append("n", 1),
        new BasicDBObject("_id", 2).append("n", 1)
    ), results);
    
  }
  
  @Test
  public void testCompoundDateIdUpserts(){
    DBCollection collection = newCollection();
    DBObject query = new BasicDBObjectBuilder().push("_id")
      .push("$lt").add("n", "a").add("t", 10).pop()
      .push("$gte").add("n", "a").add("t", 1).pop()
      .pop().get();
    List<BasicDBObject> toUpsert = Arrays.asList(
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 1)),
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 2)),
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 3)),
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 11))
    );
    for (BasicDBObject dbo : toUpsert) {
      collection.update(dbo, ((BasicDBObject)dbo.copy()).append("foo", "bar"), true, false);
    }
    System.out.println(collection.find().toArray());
    List<DBObject> results = collection.find(query).toArray();
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 1)).append("foo", "bar"),
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 2)).append("foo", "bar"),
        new BasicDBObject("_id", new BasicDBObject("n","a").append("t", 3)).append("foo", "bar")
    ), results);
    
  }
  
  @Test
  public void testAnotherUpsert() {
    DBCollection collection = newCollection();
    //Fongo.metrics update: { "_id" : { "f" : "ca" , "1" : { "l" : 284000} , "t" : { "t" : 1323648000000}}} { "$inc" : { "n.!" : 1 , "n.a.b:false" : 1}} upsert? true multi? false
    BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().push("_id").
        append("f", "ca").push("1").append("l", 2).pop().push("t").append("t", 11).pop().pop();
    DBObject query = queryBuilder.get();
    
    DBObject update = BasicDBObjectBuilder.start().push("$inc").append("n.!", 1).append("n.a.b:false", 1).pop().get();
    collection.update(query, update, true, false);
    
    DBObject expected = queryBuilder.push("n").append("!", 1).push("a").append("b:false", 1).pop().pop().get();
    assertEquals(expected, collection.findOne());
  }
  
  @Test
  public void testFindAndModifyReturnOld() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1).append("a", 1));
    DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), 
        null, null, false, new BasicDBObject("$inc", new BasicDBObject("a", 1)), false, false);
    
    assertEquals(new BasicDBObject("_id", 1).append("a", 1), result);
    assertEquals(new BasicDBObject("_id", 1).append("a", 2), collection.findOne());
  }
  
  @Test
  public void testFindAndModifyReturnNew() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1).append("a", 1));
    DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), 
        null, null, false, new BasicDBObject("$inc", new BasicDBObject("a", 1)), true, false);
    
    assertEquals(new BasicDBObject("_id", 1).append("a", 2), result);
  }
  
  @Test
  public void testFindAndModifyUpsert() {
    DBCollection collection = newCollection();
    
    DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), 
        null, null, false, new BasicDBObject("$inc", new BasicDBObject("a", 1)), true, true);
    
    assertEquals(new BasicDBObject("_id", 1).append("a", 1), result);
    assertEquals(new BasicDBObject("_id", 1).append("a", 1), collection.findOne());
  }
  
  @Test
  public void testFindAndModifyRemove() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1).append("a", 1));
    DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), 
        null, null, true, null, false, false);
    
    assertEquals(new BasicDBObject("_id", 1).append("a", 1), result);
    assertEquals(null, collection.findOne());
  }
  
  @Test
  public void testRemove() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    collection.insert(new BasicDBObject("_id", 2));
    collection.insert(new BasicDBObject("_id", 3));
    collection.insert(new BasicDBObject("_id", 4));
    
    collection.remove(new BasicDBObject("_id", 2));
    
    assertEquals(null, 
        collection.findOne(new BasicDBObject("_id",2)));
  }
  
  @Test
  public void testConvertJavaListToDbList() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1).append("n", Arrays.asList(1,2)));
    DBObject result = collection.findOne();
    assertTrue("not a DBList", result.get("n") instanceof BasicDBList);
    
  }
  
  @Test
  public void testDistinctQuery() {
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("n", 1).append("_id", 1));
    collection.insert(new BasicDBObject("n", 2).append("_id", 2));
    collection.insert(new BasicDBObject("n", 3).append("_id", 3));
    collection.insert(new BasicDBObject("n", 1).append("_id", 4));
    collection.insert(new BasicDBObject("n", 1).append("_id", 5));
    assertEquals(Arrays.asList(
        new BasicDBObject("n", 1).append("_id", 1), 
        new BasicDBObject("n", 2).append("_id", 2), 
        new BasicDBObject("n", 3).append("_id", 3)
    ), collection.distinct("n"));
  }
  
  @Test
  public void testGetLastError(){
    Fongo fongo = newFongo();
    DB db = fongo.getDB("db");
    DBCollection collection = db.getCollection("coll");
    collection.insert(new BasicDBObject("_id", 1));
    CommandResult error = db.getLastError();
    assertTrue(error.ok());
  }
  
  @Test
  public void testSave() {
    DBCollection collection = newCollection();
    BasicDBObject inserted = new BasicDBObject("_id", 1);
    collection.insert(inserted);
    collection.save(inserted);
  }
  
  @Test(expected = MongoException.DuplicateKey.class)
  public void testInsertDuplicateThrows(){
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1));
    collection.insert(new BasicDBObject("_id", 1));
  }
  
  @Test
  public void testCreateIndexes() {
    DBCollection collection = newCollection();
    collection.ensureIndex("n");
    collection.ensureIndex("b");
    List<DBObject> indexes = collection.getDB().getCollection("system.indexes").find().toArray();
    assertEquals(
        Arrays.asList(
            new BasicDBObject("v", 1).append("key", new BasicDBObject("n", 1)).append("ns", "db.coll").append("name", "n_1"),
            new BasicDBObject("v", 1).append("key", new BasicDBObject("b", 1)).append("ns", "db.coll").append("name", "b_1")
        ), indexes);
  }
  
  @Test
  public void testSortByEmeddedKey(){
    DBCollection collection = newCollection();
    collection.insert(new BasicDBObject("_id", 1).append("a", new BasicDBObject("b", 1)));
    collection.insert(new BasicDBObject("_id", 2).append("a", new BasicDBObject("b", 2)));
    collection.insert(new BasicDBObject("_id", 3).append("a", new BasicDBObject("b", 3)));
    List<DBObject> results = collection.find().sort(new BasicDBObject("a.b", -1)).toArray();
    assertEquals(
        Arrays.asList(
            new BasicDBObject("_id", 3).append("a", new BasicDBObject("b", 3)),
            new BasicDBObject("_id", 2).append("a", new BasicDBObject("b", 2)),
            new BasicDBObject("_id", 1).append("a", new BasicDBObject("b", 1))
        ),results);
  }
  
  @Test
  public void testToString() {
    new Fongo("test", true).getMongo().toString();
  }

  private DBCollection newCollection() {
    Fongo fongo = newFongo();
    DB db = fongo.getDB("db");
    DBCollection collection = db.getCollection("coll");
    return collection;
  }

  public Fongo newFongo() {
    Fongo fongo = new Fongo("test", true);
    return fongo;
  }

}
