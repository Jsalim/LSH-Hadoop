package working;

import java.util.Random;

public class Power {
  
  static public void main(String[] args) {
    check(4, 0.001);
    check(2, 0.0001);
    check(100, 2.0);
    
    //    benchmark();
    compare();
  }
  
  private static void compare() {
    int max = 100000;
    Random rnd = new Random();
    double sum = 0;
    double avg = 0;
    for(int i = 0; i < max; i++) {
      double d = rnd.nextInt(1000);
      double approx = pow(i, 0.001);
      double exact = Math.pow(i, 0.001);
      avg += (exact - approx);
      sum += Math.abs(exact - approx);
    }
    System.out.println("Jitter: " + (avg / max));
    System.out.println("Variance: " + (sum / max));
    
  }
  
  private static void benchmark() {
    long start = System.currentTimeMillis();
    for(int i = 0; i < 10000000; i++) 
      pow(i, 0.001);
    System.out.println("time for approx = " + (System.currentTimeMillis() - start) );
    start = System.currentTimeMillis();
    for(int i = 0; i < 10000000; i++) 
      Math.pow(i, 0.001);
    System.out.println("time for exact = " + (System.currentTimeMillis() - start) );
  }
  
  static void check(double sample, double power) {
    double approx = pow(sample, power);
    
    System.out.println("Exp (" + sample + "^" + power + " = " + approx + ", v.s. Math.pow() " + Math.pow(sample, power));
  }
  
  public static double pow(final double a, final double b) {
    final int x = (int) (Double.doubleToLongBits(a) >> 32);
    final int y = (int) (b * (x - 1072632447) + 1072632447);
    return Double.longBitsToDouble(((long) y) << 32);
  }
}
