package nxt.addons.taxreport;

import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrintWriterLineWriter implements LineWriter {
    private final String quote;
    private final String delimiter;
    private final Column[] header;
    private final PrintWriter writer;

    public PrintWriterLineWriter(PrintWriter writer, String quote, String delimiter) {
        this.header = Column.values();
        this.quote = quote;
        this.delimiter = delimiter;
        this.writer = writer;
        write(Stream.of(this.header).map(Column::getLabel));
    }

    @Override
    public void writeLine(Map<Column, String> line) {
        if (line.isEmpty()) {
            return;
        }
        write(Stream.of(header)
                .map(line::get));
    }

    @Override
    public void close() {
        writer.close();
    }

    private void write(Stream<String> strings) {
        String line = strings
                .map(value -> value != null ? value : "")
                .map(this::escapeDSV)
                .collect(Collectors.joining(delimiter));
        writer.println(line);
    }

    // todo same as nxt.addons.DebugTrace.escapeCSV
    private String escapeDSV(String value) {
        return value.replace("\\", "\\\\") // escape escapes
                .replace(quote, quote + quote) // escape quotes (doubled)
                .replaceAll("\r", "\\r") // escape cr
                .replaceAll("\n", "\\n") // escape nl
                .replaceAll(delimiter, "\\s"); // escape separator
    }
}
