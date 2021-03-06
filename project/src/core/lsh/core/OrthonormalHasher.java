package lsh.core;

/*
 * Orthonormal projection
 * 
 * Hash point to nearest grid corner.
 * stretch of 0.5 means grid to 0.5 instead of 1.0
 */

public class OrthonormalHasher implements Hasher {
    double[] stretch;
    final int dimensions;

    public OrthonormalHasher(int dim, Double stretch) {
	dimensions = dim;
	if (null != stretch) {
	    this.stretch = new double[dim];
	    for(int i = 0; i < dim; i++) {
		this.stretch[i] = stretch;
	    }
	}
    }

    public OrthonormalHasher(double stretch[]) {
	this.dimensions = stretch.length;
	this.stretch = stretch;
    }

    public OrthonormalHasher() {
	dimensions = 0;
    }

    @Override
    public void setStretch(double[] stretch) {
	this.stretch = stretch;
    }

    @Override
    public int[] hash(double[] values) {
	int[] hashed = new int[values.length];
	for(int i = 0; i < hashed.length; i++) {
	    if (null != stretch) {
		hashed[i] = (int) Math.floor(values[i] / stretch[i]);
	    } else {
		hashed[i] = (int) Math.floor(values[i]);
	    } 
	}
	return hashed;
    }

    @Override
    public void project(double[] values, double[] gp) {
	for(int i = 0; i < values.length; i++)
	    if (null != stretch)
		gp[i] = values[i] / stretch[i];
	    else
		gp[i] = values[i];
    }

    @Override
    public void unhash(int[] hash, double[] values) {
	for (int i = 0; i < hash.length; i++) {
	    if (null != stretch) 
		values[i] = hash[i] * stretch[i];
	    else
		values[i] = hash[i];
	}
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("Orthonormal Hasher: dim=" + dimensions);
	if (null != stretch) {
	    sb.append("[");
	    for(int i = 0; i < stretch.length; i++) {
		sb.append(stretch[i]);
		sb.append(',');
	    }
	    sb.setLength(sb.length() - 1);
	    sb.append("]");
	}
	return sb.toString();
    }

    @Override
    public int getNeighbors() {
	return 8;
    }

}
