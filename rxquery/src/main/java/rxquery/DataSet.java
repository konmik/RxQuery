package rxquery;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This class describes a set of tables and rows. It can be used to notify about data updates or
 * to subscribe to data updates.
 */
public class DataSet implements DataPattern, DataDescription {

    private ArrayList<String> list = new ArrayList<>();

    /**
     * Creates a data set and adds a table to it.
     *
     * @param tableName a name of a table to addRow.
     */
    public static DataSet fromTable(String tableName) {
        return new DataSet().addTable(tableName);
    }

    /**
     * Creates a data set and adds a tow to it.
     *
     * @param tableName a name of a row's addTable.
     * @param id        id of a row
     */
    public static DataSet fromRow(String tableName, int id) {
        return new DataSet().addRow(tableName, id);
    }

    /**
     * Creates an empty data set.
     *
     * @return an empty data set.
     */
    public static DataSet empty() {
        return new DataSet();
    }

    /**
     * Creates an empty data set.
     */
    private DataSet() {
    }

    /**
     * Adds a addTable to the data set.
     *
     * @param tableName a name of a addTable to addRow.
     * @return a changed DataSet.
     */
    public DataSet addTable(String tableName) {
        list.add(tableName);
        return this;
    }

    /**
     * Adds a row to the data set.
     *
     * @param tableName a name of a row's addTable.
     * @param id        id of a row.
     * @return a changed DataSet.
     */
    public DataSet addRow(String tableName, int id) {
        list.add(tableName + ':' + id);
        return this;
    }

    /**
     * Adds another data set to the data set.
     *
     * @param dataSet a data set to add.
     * @return a changed DataSet.
     */
    public DataSet add(DataSet dataSet) {
        list.addAll(dataSet.list);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pattern getPattern() {
        StringBuilder builder = new StringBuilder();
        for (String r : list) {
            if (builder.length() > 0)
                builder.append('|');
            builder.append(".*\\b");
            builder.append(r);
            builder.append("\\b.*");
        }
        return Pattern.compile(builder.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, size = list.size(); i < list.size(); i++) {
            if (i > 0)
                sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
