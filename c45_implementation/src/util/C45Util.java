package util;

import types.Attribute;
import types.Instance;

import java.util.*;
import java.util.stream.Collectors;

public class C45Util {

    public static List<String> possibleTargetValues;
    public static double threshold;
    public static double bestThreshold;

    /** @Author Cillian Fennell
     *
     * Subset an instance list for continuous values given an attribute
     *
     * @param attribute
     * @param instanceList
     * @param threshold
     * @return subset of instances
     */
    public static HashMap<String, List<Instance>> subsetInstanceListContinuous(Attribute attribute, List<Instance> instanceList, double threshold) {
        HashMap<String, List<Instance>> subsets = new HashMap<>();

        List<Instance> lessThanEqualToList = instanceList.stream().filter(x -> getAttributeValue(x, attribute) <= threshold).collect(Collectors.toList());
        List<Instance> greaterThanList = instanceList.stream().filter(x -> getAttributeValue(x, attribute) > threshold).collect(Collectors.toList());

        subsets.put("lessThanEqualTo", lessThanEqualToList);
        subsets.put("greaterThan", greaterThanList);

        return subsets;
    }



    /**
     * @Author Cillian Fennell
     *
     * Gets the best attribute from a list of instances and attributes
     *
     * @param instanceList - instance list
     * @param attributes - the attribute list
     * @return the attribute with the max gain
     */
    public static Attribute bestAttribute(List<Instance> instanceList, List<Attribute> attributes) {

        Attribute bestAttribute = attributes.stream()
                            .reduce((Attribute a, Attribute b) ->  gainRatio(instanceList, a) < gainRatio(instanceList, b) ? b:a)
                            .orElse(null);
        bestThreshold = bestAttribute.getThreshold();

        return bestAttribute;
    }


    /**
     * @Author Andre Godinez
     *
     * Calculate the information gain over instances for an attribute
     *
     * @param instanceList - the instance list
     * @param attribute - the attribute to calculate information gain
     * @return the information gain
     *
     */
    public static double gainRatio(List<Instance> instanceList, Attribute attribute) {
        //base case
        if (instanceList.isEmpty())
            return 0.0;

        if (attribute.isContinuous()){
            List<Double> possibleThresholdValues = calculatePossibleThresholds(instanceList, attribute);

            // Find maximum gain from among possible split points
            double maxGainRatio = 0.0;

            for (Double currentThreshold : possibleThresholdValues) {
                double gain = entropy(instanceList) - conditionalEntropy(instanceList, attribute, currentThreshold);
                double splitInfo = splitInfo(instanceList, attribute,currentThreshold);
                double gainRatio = gain / splitInfo;

                if (gainRatio > maxGainRatio) {
                    maxGainRatio = gain;

                    threshold = currentThreshold;
                    attribute.setThreshold(threshold);
                }
            }

            return maxGainRatio;
        }

        return 0.0;
    }



    /**
     * @Author Andre Godinez
     *
     * Non conditional entropy
     *
     * @param instanceList
     * @return entropy
     *
     */
    public static double entropy(List<Instance> instanceList) {
        //base case
        if(instanceList.isEmpty())
            return 0.0;

        //get total size
        int totalSize = instanceList.size();

        //set entropy
        double entropy = 0.0;

        int count = 0;

        for (String targetValue: possibleTargetValues) {
            for (Instance currentInstance: instanceList) {
                if(currentInstance.getTargetValue().equals(targetValue)) {
                    count++;
                }
            }
            double pr = (double) count/totalSize;
            entropy += pr * log2(pr);
            count = 0;
        }

        return -(entropy);
    }



    /**
     * @Author Andre Godinez
     *
     * Gets conditional entropy for a continuous value ie.
     *
     * Entropy(type|body-length) = p(type|body-length).entropy(type|body-length)
     *
     * @param instanceList
     * @param attribute
     * @param threshold
     * @return
     *
     */
    public static double conditionalEntropy(List<Instance> instanceList, Attribute attribute, double threshold) {
        //base case
        if(instanceList.isEmpty()) {
            return 0.0;
        }

        int totalSize = instanceList.size();

        //get all instances and divide them to lessThanEqualTo threshold or greaterThan threshold
        List<Instance> lessThanEqualTo = instanceList.stream().filter(x -> getAttributeValue(x, attribute) <= threshold).collect(Collectors.toList());
        List<Instance> greaterThan = instanceList.stream().filter(x -> getAttributeValue(x, attribute) > threshold).collect(Collectors.toList());

        //calculate entropy for each division
        double prLessThanEqualTo = (double)lessThanEqualTo.size()/(double)totalSize;
        double prGreaterThan = (double)greaterThan.size()/(double) totalSize;

        double entropy = (prLessThanEqualTo * entropy(lessThanEqualTo)) + (prGreaterThan * entropy(greaterThan));

        return entropy;
    }


    /** @Author Cillian Fennell
     *
     * Returns value that is used to calculate gain ratio
     *
     * @param instanceList - list of instances
     * @param attribute
     * @param threshold
     * @return splitInfo
     */
    public static double splitInfo(List<Instance> instanceList, Attribute attribute, double threshold) {
        //base case
        if(instanceList.isEmpty()) {
            return 0.0;
        }

        int totalSize = instanceList.size();

        //get all instances and divide them to lessThanEqualTo threshold or greaterThan threshold
        List<Instance> lessThanEqualTo = instanceList.stream().filter(x -> getAttributeValue(x, attribute) <= threshold).collect(Collectors.toList());
        List<Instance> greaterThan = instanceList.stream().filter(x -> getAttributeValue(x, attribute) > threshold).collect(Collectors.toList());

        //calculate entropy for each division
        double prLessThanEqualTo = (double)lessThanEqualTo.size()/(double)totalSize;
        double prGreaterThan = (double)greaterThan.size()/(double) totalSize;

        double splitInfo = - (prLessThanEqualTo * log2(prLessThanEqualTo)) - (prGreaterThan * log2(prGreaterThan));

        return splitInfo;
    }

    /**
     * @Author Andre Godinez
     */
    public static double log2(double x) {
        return x == 0.0 ? 0.0 : (Math.log(x)/ Math.log(2.0));
    }

    /**
     * @Author Cillian Fennell
     */
    public static double getAttributeValue(Instance instance, Attribute attribute){
        return Double.parseDouble(instance.getAttributeValues().get(attribute.getName()));
    }

    /**
     * @Author Cillian Fennell
     *
     * Calculate possible thresholds from an instance list given a specific attribute
     *
     * @param instanceList
     * @param attribute
     * @return list of possible thresholds
     *
     */
    public static List<Double> calculatePossibleThresholds (List<Instance> instanceList, Attribute attribute) {

        //List of distinct attribute values
        List<Double> attributeValues = instanceList.stream()
                .map(x -> getAttributeValue(x, attribute))
                .distinct()
                .collect(Collectors.toList());
        //Sort list so that midpoints can be calculated
        Collections.sort(attributeValues);


        //Calculating midpoints of attribute values to be used as threshold values
        List<Double> midpointValues = new ArrayList<>();
        int size = attributeValues.size() - 1;
        for(int i = 0; i < size; i++){
            double mean = (attributeValues.get(i) + attributeValues.get(i + 1))/2;
            midpointValues.add(mean);
        }

        return midpointValues;
    }


    /**
     * @Author Cillian Fennell
     *
     * Returns the majority targetValue from the instanceList
     * @param instanceList
     * @return majority targetValue from the instanceList
     *
     */
    public static String majorityTarget(List<Instance> instanceList) {
        //Map of <targetValue, count>
        Map<String, Long> count = instanceList.stream().collect(
                Collectors.groupingBy(Instance::getTargetValue, Collectors.counting()));

        //Finds max count in Map and returns the key
        String majorityTarget = count.entrySet().stream()
                                                    .max(Map.Entry.comparingByValue())
                                                    .map(Map.Entry::getKey)
                                                    .orElse("");

        return majorityTarget;
    }

    /**
     * @Author Cillian Fennell
     *
     * Returns true if all instances have the same targetValue
     *
     * @return boolean
     */
    public static boolean unanimousTarget(List<Instance> instanceList) {
        //Counts distinct target values in instance list
        long count = instanceList.stream().map(Instance::getTargetValue).distinct().count();

        if(count != 1) return false;

        return true;
    }
}
