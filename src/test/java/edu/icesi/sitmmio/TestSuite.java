package edu.icesi.sitmmio;

import edu.icesi.sitmmio.io.DatagramCsvReaderTest;
import edu.icesi.sitmmio.service.DistanceCalculatorTest;

public class TestSuite {
    public static void main(String[] args) {
        DistanceCalculatorTest.run();
        DatagramCsvReaderTest.run();
        System.out.println("All unit tests passed.");
    }
}
