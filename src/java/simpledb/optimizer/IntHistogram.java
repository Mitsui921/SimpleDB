package simpledb.optimizer;

import simpledb.execution.Predicate;

import java.util.Arrays;

/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    private int buckets;
    private int min;
    private int max;
    private double avg;
    private Gram[] grams;
    private int ntups;

    public class Gram{
        double left;
        double right;
        double w;
        int h;

        public Gram(double left, double right){
            this.left = left;
            this.right = right;
            this.w = this.right - this.left;
            this.h = 0;
        }

        public boolean inRange(int v){
            if(v >= left && v <= right){
                return true;
            }
            return false;
        }

        @Override
        public String toString(){
            return "Gram{" +
                    "left=" + left +
                    ", right=" + right +
                    ", w=" + w +
                    ", h=" + h +
                    "}";
        }
    }

    /**
     * Create a new IntHistogram.
     * <p>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
        // some code goes here
        this.buckets = buckets;
        this.min = min;
        this.max = max;
        this.ntups = 0;
        this.grams = new Gram[buckets];
        this.avg = (double) (max - min) / (double) buckets;
        double l = min;
        if (this.avg % 1 != 0){
            this.avg = (int) (this.avg + 1);
        }
        for (int i=0; i<buckets; i++){
            grams[i] = new Gram(l, l+avg);
            l += avg;
        }
    }

    public int binarySearch(int v){
        int l = 0;
        int r = buckets - 1;
        while (l <= r){
            int mid = (l+r)/2;
            if(grams[mid].inRange(v)){
                return mid;
            } else if(grams[mid].left > v){
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return -1;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        // some code goes here
        int i = binarySearch(v);
        if (i != -1){
            grams[i].h++;
            ntups++;
        }
    }

    public void setNtups(int ntups){
        this.ntups = ntups;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        // some code goes here
        int i = binarySearch(v);
        Gram gram;
        gram = i != -1 ? grams[i] : null;
        if (op == Predicate.Op.EQUALS){
            if (gram != null){
                return (gram.h / gram.w) / (double) ntups;
            } else {
                return 0.0;
            }
        } else if (op == Predicate.Op.GREATER_THAN){
            if (v < min){
                return 1.0;
            } else if (v >= max){
                return 0.0;
            } else if (gram != null){
                double res = 0;
                double add = ((double) gram.h / (double) ntups) * ((gram.right - v) / gram.w);
                res += add;
                int j;
                for (j = i+1; j<buckets; j++){
                    res += (double) grams[j].h / (double) ntups;
                }
                return res;
            }
        } else if (op == Predicate.Op.LESS_THAN){
            if (v <= min){
                return 0.0;
            } else if (v >= max){
                return 1.0;
            } else if (gram != null){
                double res = 0;
                double add = ((double) gram.h / (double) ntups * (v - gram.left) / gram.w);
                res += add;
                int j;
                for (j = 0; j<i; j++){
                    res += (double) grams[j].h / (double) ntups;
                }
                return res;
            }
        } else if (op == Predicate.Op.NOT_EQUALS){
            if (gram != null){
                return 1 - (gram.h / gram.w / (double) ntups);
            } else {
                return 1.0;
            }
        } else if (op == Predicate.Op.GREATER_THAN_OR_EQ){
            if (v <= min){
                return 1.0;
            } else if (v > max){
                return 0.0;
            } else if (gram != null){
                double res = 0;
                double add = ((double) gram.h * ((gram.right - v + 1) / gram.w)) / (double) ntups;
                res += add;
                int j;
                for (j = i+1; j<buckets; j++){
                    res += (double) grams[j].h / (double) ntups;
                }
                return res;
            }
        } else if (op == Predicate.Op.LESS_THAN_OR_EQ){
            if (v < min){
                return 0.0;
            } else if (v >= max){
                return 1.0;
            } else if (gram != null){
                double res = 0;
                double add = ((double) gram.h / (double) ntups) * ((v - gram.left + 1) / gram.w);
                res += add;
                int j;
                for (j=0; j<i; j++){
                    res += (double) grams[j].h / (double) ntups;
                }
                return res;
            }
        }
        return 0.0;
    }

    /**
     * @return the average selectivity of this histogram.
     *         <p>
     *         This is not an indispensable method to implement the basic
     *         join optimization. It may be needed if you want to
     *         implement a more efficient optimization
     */
    public double avgSelectivity() {
        // some code goes here
        return avg;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    @Override
    public String toString() {
        // some code goes here
        return "IntHistogram{" +
                "buckets=" + buckets +
                ", min=" + min +
                ", max=" + max +
                ", avg=" + avg +
                ", grams=" + Arrays.toString(grams) +
                "}";
    }
}
