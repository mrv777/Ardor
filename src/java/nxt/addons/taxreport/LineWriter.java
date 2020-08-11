package nxt.addons.taxreport;

import java.io.Closeable;
import java.util.Map;

public interface LineWriter extends Closeable {
    void writeLine(Map<Column, String> line);

    @Override
    void close();
}
