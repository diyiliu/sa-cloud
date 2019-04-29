package com.tiza.air.cluster;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: HBaseClient
 * Author: DIYILIU
 * Update: 2018-05-08 13:55
 */

public class HBaseUtil {
    private Configuration config;

    private String table;

    private byte[] family;

    public HBaseUtil() {

    }

    public HBaseUtil(Configuration config, String table, byte[] family) {
        this.config = config;
        this.table = table;
        this.family = family;
    }

    /**
     * 查询HBase 历史数据
     *
     * @param id
     * @param tag
     * @param startTime
     * @param endTime
     * @return
     * @throws IOException
     */
    public List<String> scan(int id, String tag, long startTime, long endTime) throws IOException {
        List<String> list = new ArrayList();

        byte[] startRow = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(startTime));
        byte[] stopRow = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(endTime));

        try (Connection connection = ConnectionFactory.createConnection(config)) {
            TableName tableName = TableName.valueOf(table);
            Table table = connection.getTable(tableName);

            Scan scan = new Scan().withStartRow(startRow).withStopRow(stopRow);
            scan.addFamily(family);

            byte[] tagBytes = Bytes.toBytes(tag);
            FilterList qualifierFilters = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            qualifierFilters.addFilter(new QualifierFilter(CompareOperator.EQUAL, new BinaryComparator(tagBytes)));
            scan.setFilter(qualifierFilters);

            ResultScanner rs = table.getScanner(scan);
            try {
                for (Result r = rs.next(); r != null; r = rs.next()) {
                    // rowKey = id + time
                    byte[] bytes = r.getRow();
                    // int equipId = Bytes.toInt(bytes);
                    // long timestamp = Bytes.toLong(bytes, 4);
                    if (r.containsNonEmptyColumn(family, tagBytes)) {
                        String value = Bytes.toString(r.getValue(family, tagBytes));
                        list.add(value);
                    }
                }
            } finally {
                rs.close();
            }
        }

        return list;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setFamily(byte[] family) {
        this.family = family;
    }
}
