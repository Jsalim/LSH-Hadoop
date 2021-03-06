/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.math.stats;

import org.apache.mahout.math.list.DoubleArrayList;

/**
 * Computes on-line estimates of mean, variance and all five quartiles (notably including the
 * median).  Since this is done in a completely incremental fashion (that is what is meant by
 * on-line) estimates are available at any time and the amount of memory used is constant.  Somewhat
 * surprisingly, the quantile estimates are about as good as you would get if you actually kept all
 * of the samples.
 * <p/>
 * The method used for mean and variance is Welford's method.  See
 * <p/>
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm
 * <p/>
 * The method used for computing the quartiles is a simplified form of the stochastic approximation
 * method described in the article "Incremental Quantile Estimation for Massive Tracking" by Chen,
 * Lambert and Pinheiro
 * <p/>
 * See
 * <p/>
 * http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.105.1580
 */

// TODO: return assigned quartile

public class OnlineSummarizer {
  // maximum divisor for new samples
  // purely random stream stabilizes at 50,000 so this matches real data
  private static int STABLE = 10000;
  
  private boolean sorted = true;
  
  // the first several samples are kept so we can boot-strap our estimates cleanly
  private DoubleArrayList starter = new DoubleArrayList(100);
  
  // quartile estimates
  private final double[] q = new double[5];
  
  // mean and variance estimates
  private double mean;
  private double variance;
  
  // quartile scales
  private double bias = 0.5;
  
  // number of samples seen so far
  private int n = 0;
  
  public OnlineSummarizer() {
    
  }
  
  public OnlineSummarizer(double cut) throws Exception {
    if (cut <= 0.01 || cut >= 0.99)
      throw new Exception("cut value out of range");
    bias = cut;
  }
  
  public int add(double sample) {
    sorted = false;
    
    if (n < STABLE)
      n++;
    double oldMean = mean;
    mean += (sample - mean) / n;
    double diff = (sample - mean) * (sample - oldMean);
    variance += (diff - variance) / n;
    
    if (n < 100) {
      starter.add(sample);
      return 2;
    } else if (n == 100 && starter != null) {
      // when we first reach 100 elements, we switch to incremental operation
      starter.add(sample);
      for (int i = 0; i <= 4; i++) {
        q[i] = getQuartile(i);
      }
      // this signals any invocations of getQuartile at exactly 100 elements that we have
      // already switched to incremental operation
      starter = null;
      return 2;
    } else {
      int quartile = 4;
      if (sample <= q[0])
        quartile = 0;
      else if (sample < q[1])
         quartile = 1;
      else if (sample == q[2])
         quartile = 2;
      else if (sample < q[3])
         quartile = 3;
      // n >= 100 && starter == null
      q[0] = Math.min(sample, q[0]);
      q[4] = Math.max(sample, q[4]);
      
      // should this be different between -1 < x < 1?
      double rate = 2 * (q[3] - q[1]) / n;
      q[1] += (Math.signum(sample - q[1]) - bias) * rate;
      q[2] += (Math.signum(sample - q[2])) * rate;
      q[3] += (Math.signum(sample - q[3]) + bias) * rate;
      
      if (q[1] < q[0]) {
        q[1] = q[0];
      }
      
      if (q[3] > q[4]) {
        q[3] = q[4];
      }
      return quartile;
    }
  }
  
  public int getCount() {
    return n;
  }
  
  public double getMean() {
    return mean;
  }
  
  public double getSD() {
    return Math.sqrt(variance);
  }
  
  public double getMin() {
    return getQuartile(0);
  }
  
  private void sort() {
    if (!sorted && starter != null) {
      starter.sortFromTo(0, 99);
      sorted = true;
    }
  }
  
  public double getMax() {
    return getQuartile(4);
  }
  
  public double getQuartile(int i) {
    if (n < 100 || starter == null) {
      return q[i];
    } else {
      sort();
      switch (i) {
        case 0:
          if (n == 0) {
            throw new IllegalArgumentException("Must have at least one sample to estimate minimum value");
          }
          return starter.get(0);
        case 1:
        case 2:
        case 3:
          if (n >= 2) {
            double x = i * (n - 1) / 4.0;
            int k = (int) Math.floor(x);
            double u = x - k;
            return starter.get(k) * (1 - u) + starter.get(k + 1) * u;
          } else {
            throw new IllegalArgumentException("Must have at least two samples to estimate quartiles");
          }
        case 4:
          if (n == 0) {
            throw new IllegalArgumentException("Must have at least one sample to estimate maximum value");
          }
          return starter.get(starter.size() - 1);
        default:
          throw new IllegalArgumentException("Quartile number must be in the range [0..4] not " + i);
      }
    }
  }
  
  public double getMedian() {
    return getQuartile(2);
  }
  
  @Override
  public String toString() {
    return "[" + 
    pair("count", getCount()) + pair("sd", getSD()) + pair("mean", getMean()) + 
    pair("min", getMin()) + 
    pair(Integer.toString((int)(50 - bias*50)) + "%", getQuartile(1)) + 
    pair("median", getMedian()) +
    pair(Integer.toString(50 + ((int) ((bias*50)))) + "%", getQuartile(3)) + 
    pair("max", getMax()) + "]";
  }
  
  private String pair(String tag, double value) {
    String s = Double.toString(value);
    if (s.length() > 8 && -1 == s.indexOf('E'))
      s = s.substring(0, 7);
    return "(" + tag + "=" + s + "),";
  }
  
}
