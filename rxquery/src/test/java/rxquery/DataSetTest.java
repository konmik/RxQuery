package rxquery;

import junit.framework.TestCase;

import java.util.regex.Pattern;

public class DataSetTest extends TestCase {

    public void testTable() throws Exception {
        assertEquals("users,groups", DataSet.fromTable("users").addTable("groups").getDescription());
    }

    public void testRow() throws Exception {
        assertEquals("users,groups:12,labels:1", DataSet.fromTable("users").addRow("groups", 12).addRow("labels", 1).getDescription());
    }

    public void testPattern() throws Exception {
        Pattern pattern = DataSet.fromTable("users").addRow("groups", 12).addRow("labels", 1).getPattern();
        assertEquals(".*\\busers\\b.*|.*\\bgroups:12\\b.*|.*\\blabels:1\\b.*", pattern.toString());
    }

    public void testAdd() throws Exception {
        assertEquals("users,groups", DataSet.fromTable("users").add(DataSet.fromTable("groups")).getDescription());
    }
}