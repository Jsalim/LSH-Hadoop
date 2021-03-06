package org.apache.mahout.randomvectorspace;

import java.util.Iterator;
import java.util.Random;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.stats.OnlineSummarizer;

/*
 * Random Vector Space: generate a set of random vectors, defining a space.
 * Quantize a vector by taking the cosine distance to each random vector.
 * Save the sign of the cosine distance. 
 * 
 * Hardcoded to cosine distance for sparse vector reasons.
 */

public class RVSFactory2  {
  
  private int nVectors;
  private final int dimensions;
  //  private final Long seed;
  private final Random rnd;
  private Vector[] plus;
  private Vector[] minus;
  private Vector[] delta;
  private final boolean isSparse;
  static private final CosineDistanceMeasure measure = new CosineDistanceMeasure();
  
  public RVSFactory2(int nVectors, int dimensions, Random rnd, boolean isSparse) {
    this.nVectors = nVectors;
    this.dimensions = dimensions;
    //    this.seed = seed;
    this.rnd = rnd;
    this.isSparse = isSparse;
    if (isSparse) {
      plus = new Vector[nVectors];
      minus = new Vector[nVectors];
      delta = null;
      for(int i = 0; i < nVectors; i++) {
        plus[i] = new RandomVector(dimensions, rnd.nextLong(), false);
        minus[i] = new RandomVector(dimensions, rnd.nextLong(), false);
      }
    } else {
      plus = null;
      minus = null;
      delta = new Vector[nVectors];
      for(int i = 0; i < nVectors; i++) {
        Vector v1 = new RandomVector(dimensions, rnd.nextLong(), false);
        Vector v2 = new RandomVector(dimensions, rnd.nextLong(), false);
        delta[i] = new DenseVector(v1.minus(v2));
      }
      
    }
  }
  
  //  private RandomVector getNormal(Long seed, int i, boolean isSparse) {
  //    Vector v1 = new RandomVector(dimensions, seed + i * dimensions, false);
  //    Vector v2 = new RandomVector(dimensions, seed + i * dimensions, false);
  //    if (isSparse) {
  //      Vector delta = new RandomDeltaVector(v1, v2);
  //    } else {
  //      Vector delta = new DenseVector(v1);
  //      delta.minus(v2);
  //    }
  //  }
  
  public RVS quantize(Vector value) {
    RVS position = new RVS(nVectors, value.getNumNondefaultElements());
//    char mask = 0;
    for(int i = 0; i < nVectors; i++) {
      double m = measure(value, i);
      if (m >= 1.0)
        position.setBit(i);
    }
//    System.out.println();
    return position;
  }
  
  private double measure(Vector v, int i) {
    if (isSparse) {
      int size = v.getNumNondefaultElements();
      double values[] = new double[size];
      double delta[] = new double[size];
      Iterator<Element> sparse = v.iterateNonZero();
      int index = 0;
      while(sparse.hasNext()) {
        Element e = sparse.next();
        int dim = e.index();
        double d = e.get();
        values[index] = d;
        delta[index] = plus[i].get(dim) - minus[i].get(dim);
        index++;
      }
      return CosineDistanceMeasure.distance(values, delta);
    } else {
      return measure.distance(v, delta[i]);
    }
  }
  
  static int RVECS = 1000;
  private static final int DIMS = 100;

  static public void main(String[] args) {
    Vector[] lots = new Vector[RVECS];
//    Vector bench = new DenseVector(DIMS);
//    for(int i = 0; i < DIMS; i++)
//      bench.set(i, 0.0);
    Random rnd = RandomUtils.getRandom(0);
    Vector bench = new DenseVector(new RandomVector(DIMS, RandomUtils.getRandom(0), false));
    for(int i = 0; i < RVECS; i++) {
      Vector v = new RandomVector(DIMS, RandomUtils.getRandom(i*DIMS), false);
      DenseVector dv = new DenseVector(v);
      lots[i] = dv;
    }
    System.out.println("Start test");
    RVSFactory2 fac = new RVSFactory2(RVECS, DIMS, RandomUtils.getRandom(RVECS * DIMS), false);
    RVS benchBits = fac.quantize(bench);
//    System.out.println("Zero bitset" + ": " + benchBits.toString());
    OnlineSummarizer summary = new OnlineSummarizer();
    for(int i = 0; i < RVECS; i++) {
      RVS bits = fac.quantize(lots[i]);
      int hamming = bits.hamming(benchBits);
      System.out.println("hamming: " + hamming + " sample: " + bits.toString());
      summary.add(hamming);
    }
    System.out.println(summary.toString());
  }
  
}