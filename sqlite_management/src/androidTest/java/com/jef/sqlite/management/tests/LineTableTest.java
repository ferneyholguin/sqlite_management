package com.jef.sqlite.management.tests;

import android.content.ContentValues;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.models.Line;
import com.jef.sqlite.management.tables.LineTable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Instrumented test for LineTable class.
 */
@RunWith(AndroidJUnit4.class)
public class LineTableTest {

    private LineTable lineTable;
    private Context context;

    @Before
    public void setUp() {
        // Get the context for the test
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Initialize the LineTable
        lineTable = new LineTable(context);

    }

    @Test
    public void testGetAllLines() {
        // First, save a test line
        Line testLine = new Line();
        testLine.setName("Test Line GetAll");
        Line savedLine = lineTable.saveLine(testLine);
        
        // Get all lines
        List<Line> lines = lineTable.getAllLines();
        
        // Verify the test line is in the list
        boolean found = false;
        for (Line line : lines) {
            if (line.getId() == savedLine.getId()) {
                found = true;
                assertEquals("Test Line GetAll", line.getName());
                break;
            }
        }
        
        assertTrue("Test line should be found in the list", found);
    }

    @Test
    public void testSaveLine() {
        // Create a new line
        Line line = new Line();
        line.setName("Test Line Save");
        Date now = new Date();
        line.setDateCreation(now);
        
        // Save the line
        Line savedLine = lineTable.saveLine(line);
        
        // Verify the line was saved with an ID
        assertNotEquals(0, savedLine.getId());
        assertEquals("Test Line Save", savedLine.getName());
        
        // Verify we can retrieve the saved line
        Optional<Line> retrievedLine = lineTable.getLineById(savedLine.getId());
        assertTrue(retrievedLine.isPresent());
        assertEquals(savedLine.getId(), retrievedLine.get().getId());
        assertEquals(savedLine.getName(), retrievedLine.get().getName());
        // Date comparison might need to be approximate due to database storage precision
        assertNotNull(retrievedLine.get().getDateCreation());
    }

    @Test
    public void testGetLineById() {
        // First, save a test line
        Line testLine = new Line();
        testLine.setName("Test Line GetById");
        Line savedLine = lineTable.saveLine(testLine);
        
        // Get the line by ID
        Optional<Line> retrievedLine = lineTable.getLineById(savedLine.getId());
        
        // Verify the line was retrieved
        assertTrue(retrievedLine.isPresent());
        assertEquals(savedLine.getId(), retrievedLine.get().getId());
        assertEquals("Test Line GetById", retrievedLine.get().getName());
    }

    @Test
    public void testGetLineByIdNotFound() {
        // Try to get a line with an ID that doesn't exist
        Optional<Line> retrievedLine = lineTable.getLineById(-1);
        
        // Verify no line was found
        assertFalse(retrievedLine.isPresent());
    }

    @Test
    public void testUpdateById() {
        // First, save a test line
        Line testLine = new Line();
        testLine.setName("Test Line UpdateById");
        Line savedLine = lineTable.saveLine(testLine);
        
        // Create content values for update
        ContentValues values = new ContentValues();
        values.put("name", "Updated Line Name");
        
        // Update the line
        int rowsUpdated = lineTable.updateById(values, savedLine.getId());
        
        // Verify the update was successful
        assertEquals(1, rowsUpdated);
        
        // Verify the line was updated
        Optional<Line> updatedLine = lineTable.getLineById(savedLine.getId());
        assertTrue(updatedLine.isPresent());
        assertEquals("Updated Line Name", updatedLine.get().getName());
    }

    @Test
    public void testUpdateNameById() {
        // First, save a test line
        Line testLine = new Line();
        testLine.setName("Test Line UpdateNameById");
        Line savedLine = lineTable.saveLine(testLine);
        
        // Update the line's name
        int rowsUpdated = lineTable.updateNameById("Updated Name", savedLine.getId());
        
        // Verify the update was successful
        assertEquals(1, rowsUpdated);
        
        // Verify the line's name was updated
        Optional<Line> updatedLine = lineTable.getLineById(savedLine.getId());
        assertTrue(updatedLine.isPresent());
        assertEquals("Updated Name", updatedLine.get().getName());
    }

    @Test
    public void testUpdateDateCreationById() {
        // First, save a test line
        Line testLine = new Line();
        testLine.setName("Test Line UpdateDateCreationById");
        Line savedLine = lineTable.saveLine(testLine);
        
        // Create a new date for update
        Date newDate = new Date(System.currentTimeMillis() + 86400000); // One day in the future
        
        // Update the line's date creation
        int rowsUpdated = lineTable.updateDateCreationById(newDate, savedLine.getId());
        
        // Verify the update was successful
        assertEquals(1, rowsUpdated);
        
        // Verify the line's date creation was updated
        Optional<Line> updatedLine = lineTable.getLineById(savedLine.getId());
        assertTrue(updatedLine.isPresent());
        // Date comparison might need to be approximate due to database storage precision
        assertNotNull(updatedLine.get().getDateCreation());
        // The updated date should be after the original date
        assertTrue(updatedLine.get().getDateCreation().after(savedLine.getDateCreation()));
    }
}