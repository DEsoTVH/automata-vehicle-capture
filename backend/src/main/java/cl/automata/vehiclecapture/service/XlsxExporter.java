package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.config.AppProperties;
import cl.automata.vehiclecapture.processing.FieldSchemaProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Genera el .xlsx de salida según las columnas configuradas (app.export.columns).
 *
 * DUMMY: libro nuevo con encabezado simple. Cuando llegue el archivo oficial, esto pasa a abrir esa
 * plantilla y escribir las filas desde la fila de inicio configurada, preservando estilos y hojas.
 */
@Component
public class XlsxExporter {

    private final AppProperties props;
    private final FieldSchemaProvider schema;

    public XlsxExporter(AppProperties props, FieldSchemaProvider schema) {
        this.props = props;
        this.schema = schema;
    }

    public byte[] build(List<Map<String, String>> rows) {
        List<AppProperties.Export.Column> columns = props.export().columns();
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Registro");

            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle header = wb.createCellStyle();
            header.setFont(bold);

            Row h = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                var cell = h.createCell(c);
                cell.setCellValue(columns.get(c).header());
                cell.setCellStyle(header);
            }

            int r = 1;
            for (Map<String, String> data : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < columns.size(); c++) {
                    String key = columns.get(c).field();
                    String value = data.getOrDefault(key, "");
                    var cell = row.createCell(c);
                    boolean numeric = schema.byKey(key).map(f -> "number".equals(f.type())).orElse(false);
                    if (numeric && value.matches("\\d+")) {
                        cell.setCellValue(Double.parseDouble(value));
                    } else {
                        cell.setCellValue(value);
                    }
                }
            }
            for (int c = 0; c < columns.size(); c++) sheet.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
