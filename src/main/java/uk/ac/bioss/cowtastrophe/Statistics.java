package uk.ac.bioss.cowtastrophe;

import broadwick.utils.Pair;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * A class to store the statistics (number of infected farms etc.) in the simulation.
 */
public class Statistics implements Serializable {

    /**
     * This class records the measurements that are recorded.
     */
    private class Measurements implements Serializable {

        /**
         * Get a CSV of the measurements.
         * @return the CSV string.
         */
        @Override
        public String toString() {
            return String.format("%d, %d, %d, %d, %d, %f", numSusceptibleFarms, numSuspectedFarms,
                                 numConfirmedFarms, numCulledFarms,
                                 numVaccinatedFarms, cost);
        }

        private int numSusceptibleFarms = 0;
        private int numSuspectedFarms = 0;
        private int numConfirmedFarms = 0;
        private int numCulledFarms = 0;
        private int numVaccinatedFarms = 0;
        private double cost = 0.0;
        private static final long serialVersionUID = 745539930019474501L;
    }

    /**
     * Create the statistics class by initialising the measurements that will be taken.
     */
    public Statistics() {
        stats = new TreeMap<>();
        infectionTree = new LinkedHashSet<>();
    }

    /**
     * Set thte number of susceptible farms on the given day.
     * @param day the day to make the recording.
     * @param num the number of susceptible farms on that day.
     */
    void setSusceptibleFarms(final int day, final int num) {
        Measurements measurements = getMeasures(day);

        // Get the last days entry in the stats map and from it get the number of previously
        // infected farms.
        //Optional<Integer> prevDay = stats.keySet().stream()
        //        .filter(e -> (e < day)).max(Comparator.naturalOrder());
        //measurements.numSusceptibleFarms += (getMeasures(prevDay.orElse(0)).numSusceptibleFarms + num);    
        measurements.numSusceptibleFarms += num;
    }

    /**
     * Add a number of suspected farms for a given day.
     * @param day the day to make the recording.
     * @param num the number of suspected farms on that day.
     */
    public final void addSuspectedFarms(final int day, final int num) {
        Measurements measurements = getMeasures(day);

        measurements.numSuspectedFarms += num;
    }

    /**
     * Add a number of vaccinated farms for a given day.
     * @param day the day to make the recording.
     * @param num the number of vaccinated farms on that day.
     */
    public final void addVaccinatedFarms(final int day, final int num) {
        Measurements measurements = getMeasures(day);

        measurements.numVaccinatedFarms += num;
    }

    /**
     * Add a number of confirmed farms for a given day.
     * @param day the day to make the recording.
     * @param num the number of confirmed farms on that day.
     */
    public final void addConfirmedFarms(final int day, final int num) {
        Measurements measurements = getMeasures(day);

        measurements.numConfirmedFarms += num;
    }

    /**
     * Add a number of culled farms for a given day.
     * @param day the day to make the recording.
     * @param num the number of culled farms on that day.
     */
    public final void addCulledFarms(final int day, final int num) {
        Measurements measurements = getMeasures(day);

        measurements.numCulledFarms += num;
    }

    /**
     * Add a cost for culling farms for a given day.
     * @param day  the day to make the recording.
     * @param cost the cost of culling farms farms on that day.
     */
    public final void addCost(final int day, final double cost) {
        Measurements measurements = getMeasures(day);

        measurements.cost += cost;
    }

    /**
     * Get cumulative costs at end of given day.
     * @param day the day to get the cumulative costs up to.
     */
    public final double getCost(final int day) {
        Measurements measurements = getMeasures(day);
        return measurements.cost;
    }

    /**
     * Record a new infection.
     * @param source   the course of the infection.
     * @param infected the newly infected farm.
     */
    public final void addNewInfection(final Farm source, final Farm infected) {
        this.infectionTree.add(new Pair<>(source.getId(), infected.getId()));
    }

    /**
     * Get the measures for a particular day if they exist or create an empty measures object if they don't.
     * @param day the day for which we want the measurements.
     * @return a (guaranteed non-null) Measures object for that day.
     */
    private Measurements getMeasures(final int day) {
        Measurements measurements = stats.get(day);
        if (measurements == null) {
            measurements = new Measurements();
            stats.put(day, measurements);
        }
        return measurements;
    }

    /**
     * Get a string representation of the statistics in the form of a CSV of the time and measurements.
     * @return the string.
     */
    @Override
    public final String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("#Time, Susceptible Farms, Farms Suspected, ");
        sb.append("Farms Confirmed, Farms Culled, Farms Vaccinated, Cost on day, Total Cost\n");
        stats.entrySet().forEach((entry) -> {
           
            double totalCost = 0.0;
            final int day = entry.getKey();
            // cumulative cost thus far.
            totalCost = stats.entrySet().stream().filter((entry2) -> (entry2.getKey() <= day))
                    .map((entry2) -> entry2.getValue().cost)
                    .reduce(totalCost, (accumulator, _item) -> accumulator + _item);

            sb.append(entry.getKey()).append(", ").append(entry.getValue().toString())
                    .append(", ").append(totalCost)
                    .append("\n");
        });
        return sb.toString();
    }

    private final Map<Integer, Measurements> stats;
    private final Collection<Pair<Integer, Integer>> infectionTree;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 131879930019376509L;
}
