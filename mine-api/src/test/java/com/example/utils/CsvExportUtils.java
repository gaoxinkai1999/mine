package com.example.utils;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * CSV导出工具类
 */
public class CsvExportUtils {

    /**
     * 将List<Map<String, Object>>格式的数据导出为CSV文件
     *
     * @param data 要导出的数据
     * @param file 导出的文件
     * @param preferredColumnOrder 首选的列顺序（可选）
     * @throws IOException 如果写入失败
     */
    public static void exportMapListToCsv(List<Map<String, Object>> data, File file, 
                                          String... preferredColumnOrder) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }

        try (CSVWriter writer = new CSVWriter(
                new FileWriter(file),
                CSVWriter.DEFAULT_SEPARATOR, 
                CSVWriter.NO_QUOTE_CHARACTER,  // 不使用引号
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
        )) {
            // 收集所有列名
            Set<String> allColumns = new LinkedHashSet<>(Arrays.asList(preferredColumnOrder));
            
            // 添加数据中出现的所有列
            for (Map<String, Object> row : data) {
                allColumns.addAll(row.keySet());
            }
            
            // 写入标题行
            writer.writeNext(allColumns.toArray(new String[0]));
            
            // 写入数据行
            for (Map<String, Object> row : data) {
                String[] csvRow = new String[allColumns.size()];
                int i = 0;
                for (String column : allColumns) {
                    Object value = row.get(column);
                    csvRow[i++] = value != null ? value.toString() : "";
                }
                writer.writeNext(csvRow);
            }
        }
        
        System.out.println("CSV文件已成功导出到: " + file.getAbsolutePath());
    }
    
    /**
     * 将List<Map<String, Object>>格式的数据导出为CSV文件，使用默认列顺序
     *
     * @param data 要导出的数据
     * @param file 导出的文件
     * @throws IOException 如果写入失败
     */
    public static void exportMapListToCsv(List<Map<String, Object>> data, File file) throws IOException {
        exportMapListToCsv(data, file, "ds", "y");  // 默认将日期列和销量列放在前面
    }
    
    /**
     * 为Prophet格式数据导出CSV文件 (ds, y格式)，不使用引号
     * 适用于格式如 [{"y": value, "ds": "date"}] 的数据
     *
     * @param data 要导出的数据
     * @param file 导出的文件 
     * @throws IOException 如果写入失败
     */
    public static void exportProphetData(List<Map<String, Object>> data, File file) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }

        try (CSVWriter writer = new CSVWriter(
                new FileWriter(file),
                CSVWriter.DEFAULT_SEPARATOR,  // 使用默认分隔符（逗号）
                CSVWriter.NO_QUOTE_CHARACTER, // 不使用引号
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
        )) {
            // Prophet格式必须是ds和y列
            String[] headers = new String[]{"ds", "y"};
            writer.writeNext(headers);
            
            // 写入数据行
            for (Map<String, Object> row : data) {
                String[] csvRow = new String[2];
                csvRow[0] = row.get("ds") != null ? row.get("ds").toString() : "";
                csvRow[1] = row.get("y") != null ? row.get("y").toString() : "0";
                writer.writeNext(csvRow);
            }
        }
        
        System.out.println("Prophet格式CSV文件已成功导出到: " + file.getAbsolutePath());
    }
} 