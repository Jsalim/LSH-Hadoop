package org.apache.mahout.cf.taste.impl.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;

/*
 * Sort data model matrix by number of preferences, both user and item.
 */

// TODO: Add feature in output for pref is real or just default 3.0
// TODO: Count number of pref/nonpref per user
// TODO: Count number pref/nonpref per item
// TODO: Assign color or something to this in diagrams

public class SortDataModel {
  
  final private DataModel model;
  
  public SortDataModel(DataModel model) {
    this.model = model;
  }
  
  public static void main(String[] args) throws IOException, TasteException {
    SortDataModel sorter;
    GroupLensDataModel model;
    
    if (args.length != 1)
      throw new TasteException("Usage: grouplens.dat");
//    MetadataModel<String> movieNames = new MetadataModel<String>(new HashMap<Long,String>(), "movies");
    model = new GroupLensDataModel(new File(args[0]));
    sorter = new SortDataModel(model);
    ArrayList<Long> userList = new ArrayList<Long>(model.getNumUsers());
    ArrayList<Long> itemList = new ArrayList<Long>(model.getNumItems());
    Map<Long,Integer> itemCounts = new HashMap<Long,Integer>();
    Map<Long,Integer> userCounts = new HashMap<Long,Integer>();
    Map<Long,Integer> userPrefYes = new HashMap<Long,Integer>();
    Map<Long,Integer> itemPrefYes = new HashMap<Long,Integer>();
    Map<Long,Integer> userPrefNo = new HashMap<Long,Integer>();
    Map<Long,Integer> itemPrefNo = new HashMap<Long,Integer>();
    int numUserPrefs = sorter.getUserCounts(model, userList, userCounts, userPrefYes, userPrefNo);
    int numItemPrefs = sorter.getItemCounts(model, itemList, itemCounts, itemPrefYes, itemPrefNo);
    
    sorter.sortPrefs(model.getUserIDs(), userList, userCounts);  
    sorter.sortPrefs(model.getItemIDs(), itemList, itemCounts);  
    GroupLensWriter modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_ratings_raw.dat");
//    sorter.printRawRatings(model, modelWriter, userList, itemList, userCounts, itemCounts);
//    modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_ratings_upward.dat");
//    sorter.printDualSortedRatings(model, modelWriter, userList, itemList, userCounts, itemCounts);
//    modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_ratings_user.dat");
//    sorter.printUserSortedRatings(modelWriter, userList, itemList, userCounts, itemCounts);
//    modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_ratings_item.dat");
//    sorter.printItemSortedRatings(modelWriter, userList, itemList, userCounts, itemCounts);
    modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_users.dat");
    sorter.printRatingCounts(modelWriter, userList, userPrefYes, userPrefNo);
    modelWriter = new GroupLensWriter(model, "/tmp/lsh_hadoop/gl_items.dat");
    sorter.printRatingCounts(modelWriter, itemList, itemPrefYes, itemPrefNo);
  }
  
  /* print all ratings in .dat format, as per DataModel order */
  private void printRawRatings(GroupLensDataModel model, GroupLensWriter modelWriter, ArrayList<Long> userList, ArrayList<Long> itemList, Map<Long,Integer> userCounts, Map<Long,Integer> itemCounts) throws TasteException {
    LongPrimitiveIterator userIter = model.getUserIDs();
    while(userIter.hasNext()) {
      long userID = userIter.nextLong();
      LongPrimitiveIterator itemIter = model.getItemIDs();
      while(itemIter.hasNext()) {
        long itemID = itemIter.nextLong();
        modelWriter.write(0, 0, userID, itemID);
      }
    }
  }
  
  /* print ratings in user/item count sorted order - neither goes backwards in count */
  // walk corners plus and plus
  private void printDualSortedRatings(GroupLensDataModel model, GroupLensWriter modelWriter, ArrayList<Long> userSorted, ArrayList<Long> itemSorted, Map<Long,Integer> userCounts, Map<Long,Integer> itemCounts) throws TasteException {
    int userI = 0;
    int itemI = 0;
    
    while(true) {
      for(int u = 0; u < userI; u++) {
        modelWriter.write(u, itemI, userSorted.get(u), itemSorted.get(itemI));
      }
      for(int i = 0; i < itemI; i++) {
        modelWriter.write(userI, i, userSorted.get(userI), itemSorted.get(i));
      }
      modelWriter.write(userI, itemI, userSorted.get(userI), itemSorted.get(itemI));
      userI++;
      if (userI == userSorted.size())
        break;
      itemI++;
      if (itemI == itemSorted.size())
        break;
    }
    if (userI == userSorted.size()) {
      for(int i = itemI + 1; i < itemSorted.size(); i++) {
        for(int u = 0; u < userI; u++) {
          modelWriter.write(u, i, userSorted.get(u), itemSorted.get(i));
        }
      }
    } else if (itemI == itemSorted.size()) {
      for(int u = itemI + 1; u < userSorted.size(); u++) {
        for(int i = 0; i < userI; i++) {
          modelWriter.write(u, i, userSorted.get(u), itemSorted.get(i));
        }
      }
    } 
  }
  
  /* print ratings in user order */
  private void printUserSortedRatings(GroupLensWriter modelWriter, ArrayList<Long> userSorted, ArrayList<Long> itemSorted, Map<Long,Integer> userCounts, Map<Long,Integer> itemCounts) throws TasteException {
    for(int u = 0; u < userSorted.size(); u++) {
      for(int i = 0; i < itemSorted.size(); i++) {
        modelWriter.write(u, i, userSorted.get(u), itemSorted.get(i));
      }
    }
  }
  
  /* print ratings in user order */
  private void printItemSortedRatings(GroupLensWriter modelWriter, ArrayList<Long> userSorted, ArrayList<Long> itemSorted, Map<Long,Integer> userCounts, Map<Long,Integer> itemCounts) throws TasteException {
    for(int i = 0; i < itemSorted.size(); i++) {
      for(int u = 0; u < userSorted.size(); u++) {
        modelWriter.write(u, i, userSorted.get(u), itemSorted.get(i));
      }
    }
  }
  
  /* print rating counts: total,  */
  private void printRatingCounts(GroupLensWriter modelWriter, List<Long> ids, Map<Long,Integer> prefCountYes, Map<Long,Integer> prefCountNo) throws TasteException {
    for(Long id: ids) {
      Integer yes = prefCountYes.get(id);
      Integer no = prefCountNo.get(id);
      if (yes == null)
        yes = 0;
      if (no == null)
        no = 0;
      modelWriter.write(id, yes + no, yes, no);
    }
  }
  
  private int getUserCounts(DataModel model, ArrayList<Long> userList,
      Map<Long,Integer> userCounts, Map<Long,Integer> userPrefYes, Map<Long,Integer> userPrefNo) throws TasteException {
    int total = 0;
    LongPrimitiveIterator userIter = model.getUserIDs();
    while(userIter.hasNext()) {
      long userID = userIter.nextLong();
      PreferenceArray prefs = model.getPreferencesFromUser(userID);
      int size = fillCounters(userList, userCounts, userPrefYes, userPrefNo, userID, prefs);
      total += size;
    }
    return total;
  }

  private int getItemCounts(DataModel model, ArrayList<Long> itemList,
      Map<Long,Integer> itemCounts, Map<Long,Integer> itemPrefYes, Map<Long,Integer> itemPrefNo) throws TasteException {
    int total = 0;
    LongPrimitiveIterator itemIter = model.getItemIDs();
    while(itemIter.hasNext()) {
      long itemID = itemIter.nextLong();
      PreferenceArray prefs = model.getPreferencesForItem(itemID);
      int size = fillCounters(itemList, itemCounts, itemPrefYes, itemPrefNo, itemID, prefs);
      total += size;
    }
    return total;
  }
  
  private int fillCounters(ArrayList<Long> userList,
      Map<Long,Integer> userCounts, Map<Long,Integer> userPrefYes,
      Map<Long,Integer> userPrefNo, long userID, PreferenceArray prefs) {
    int size = prefs.length();
    userList.add(userID);
    userCounts.put(userID, size);
    int yes = 0;
    int no = 0;
    for(int i = 0; i < size; i++) {
      double pref = prefs.getValue(i);
      if (pref == 3.0)
        no++;
      else
        yes++;
    }
    userPrefYes.put(userID, yes);
    userPrefNo.put(userID, no);
    return size;
  }
  

  /* 
   * aList[i] = id in order of #prefs
   */
  void sortPrefs(LongPrimitiveIterator iter, ArrayList<Long> aList, Map<Long,Integer> counts)  {
    ModelComparator modelComparator = new ModelComparator(counts);
    Collections.sort(aList, modelComparator);
  }
}

/*
 * Write GroupLens Data
 * TODO: timestamp
 */
class GroupLensWriter {
  private static final Float FLAT = 3.0f;
  PrintStream out;
  int count = 0;
  final DataModel model;
  boolean needHeader = true;
  
  GroupLensWriter(DataModel model, String file) throws IOException {
    this.model = model;
    File ratings = new File(file);
    if (ratings.isFile())
      ratings.delete();
    ratings.createNewFile();
    out = new PrintStream(ratings);
  }
  
  public void write(long id, int total, int yes, int no) {
    if (needHeader)
      out.println("id,total,rated,unrated,percent,masked");
    needHeader = false;
    double percent = (yes*1.0)/total;
    out.println(id + "," + total + "," + yes + "," + no + "," + percent + "," + (total < 10 ? 0 : percent));
    
  }

  void close() {
    out.close();
  }
  
  void write(int userI, int itemI, long userID, long itemID) {
    if (needHeader)
      out.println("ucount,icount,user,item,pref,rowid");
    needHeader = false;
    try {
      Float pref = model.getPreferenceValue(userID, itemID);
      if (pref != null)
        out.println(userI + "," + itemI + "," + userID + "," + itemID + "," + pref + "," + count++);
    } catch (TasteException e) {
      
    }
  }
  
}

/*
 * Compare two ids in a data model by the number of ratings
 * Want reverse order
 */
class ModelComparator implements Comparator<Long> {
  
  final private Map<Long,Integer> counts;
  // You'd think there would be one of these...
  final Comparator<Integer> intComparator = new Comparator<Integer> () {
    @Override
    public int compare(Integer o1, Integer o2) {
      if (o1 < o2)
        return -1;
      else if (o1 > o2)
        return 1;
      else
        return 0;
    };
  };
  
  public ModelComparator(Map<Long,Integer> counts) {
    this.counts = counts;
  }
  
  @Override
  public int compare(Long arg0, Long arg1) {
    return -1 * intComparator.compare(counts.get(arg0), counts.get(arg1));
  }
  
}
