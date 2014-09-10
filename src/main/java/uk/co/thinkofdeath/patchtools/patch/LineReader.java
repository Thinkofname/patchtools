package uk.co.thinkofdeath.patchtools.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class LineReader extends BufferedReader {

    private int lineNumber = 0;

    public LineReader(Reader in) {
        super(in);
    }

    @Override
    public String readLine() throws IOException {
        lineNumber++;
        return super.readLine();
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
