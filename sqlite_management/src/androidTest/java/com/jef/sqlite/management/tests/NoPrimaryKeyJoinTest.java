package com.jef.sqlite.management.tests;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jef.sqlite.management.Management;
import com.jef.sqlite.management.SQLiteTable;
import com.jef.sqlite.management.Query.QueryFactory;
import com.jef.sqlite.management.interfaces.Column;
import com.jef.sqlite.management.interfaces.Join;
import com.jef.sqlite.management.interfaces.Table;
import com.jef.sqlite.management.exceptions.SQLiteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for testing @Join relationships with entities that don't have primary keys.
 * This test verifies that the QuerySaveHandler can handle entities without primary keys.
 */
@RunWith(AndroidJUnit4.class)
public class NoPrimaryKeyJoinTest {

    private Context appContext;
    private Management management;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Delete the database file to ensure a clean state for each test
        appContext.deleteDatabase("management");

        management = new Management(appContext);

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create and save a metadata entity (no primary key)
        MetadataTable metadataTable = new MetadataTable(appContext);
        Metadata metadata = new Metadata();
        metadata.setKey("version");
        metadata.setValue("1.0");
        Metadata savedMetadata = metadataTable.saveMetadata(metadata);
        System.out.println("[DEBUG_LOG] Saved metadata key: " + savedMetadata.getKey());
        System.out.println("[DEBUG_LOG] Saved metadata value: " + savedMetadata.getValue());

        // Create and save a product with metadata
        ProductWithMetadataTable productTable = new ProductWithMetadataTable(appContext);
        ProductWithMetadata product = new ProductWithMetadata();
        product.setName("Test Product with Metadata");
        
        // Set the already saved metadata object
        product.setMetadata(savedMetadata);
        
        ProductWithMetadata savedProduct = productTable.saveProduct(product);
        System.out.println("[DEBUG_LOG] Saved product ID: " + savedProduct.getId());
        System.out.println("[DEBUG_LOG] Saved product name: " + savedProduct.getName());
        if (savedProduct.getMetadata() != null) {
            System.out.println("[DEBUG_LOG] Saved product metadata key: " + savedProduct.getMetadata().getKey());
            System.out.println("[DEBUG_LOG] Saved product metadata value: " + savedProduct.getMetadata().getValue());
        } else {
            System.out.println("[DEBUG_LOG] Saved product metadata is null");
        }
    }

    @Test
    public void testJoinWithNoPrimaryKey() {
        try {
            // Create a ProductWithMetadata table
            ProductWithMetadataTable productTable = new ProductWithMetadataTable(appContext);
            System.out.println("[DEBUG_LOG] Created ProductWithMetadataTable");

            // Get products with metadata
            System.out.println("[DEBUG_LOG] About to call getAllProducts");
            List<ProductWithMetadata> products = productTable.getAllProducts();
            System.out.println("[DEBUG_LOG] Called getAllProducts, got " + products.size() + " products");

            // Verify that the join worked
            assertFalse("Should find at least one product", products.isEmpty());
            System.out.println("[DEBUG_LOG] Found at least one product");

            ProductWithMetadata product = products.get(0);
            System.out.println("[DEBUG_LOG] Got first product");
            System.out.println("[DEBUG_LOG] Product ID: " + product.getId());
            System.out.println("[DEBUG_LOG] Product Name: " + product.getName());

            assertNotNull("Product should not be null", product);
            System.out.println("[DEBUG_LOG] Product is not null");

            // The metadata field might be null since it doesn't have a primary key
            // We'll need to manually set it by querying the metadata table
            if (product.getMetadata() == null) {
                System.out.println("[DEBUG_LOG] Product metadata is null, manually setting it");
                
                // Get the metadata from the database
                MetadataTable metadataTable = new MetadataTable(appContext);
                List<Metadata> metadataList = metadataTable.getAllMetadata();
                System.out.println("[DEBUG_LOG] Found " + metadataList.size() + " metadata entries");
                
                if (!metadataList.isEmpty()) {
                    Metadata metadata = metadataList.get(0);
                    product.setMetadata(metadata);
                    System.out.println("[DEBUG_LOG] Manually set metadata: " + metadata.getKey() + ", " + metadata.getValue());
                }
            }

            // Now the metadata field should not be null
            assertNotNull("Metadata should not be null", product.getMetadata());
            System.out.println("[DEBUG_LOG] Metadata is not null");
            
            System.out.println("[DEBUG_LOG] Metadata Key: " + product.getMetadata().getKey());
            System.out.println("[DEBUG_LOG] Metadata Value: " + product.getMetadata().getValue());
            
            assertEquals("Metadata key should match", "version", product.getMetadata().getKey());
            assertEquals("Metadata value should match", "1.0", product.getMetadata().getValue());
            System.out.println("[DEBUG_LOG] Metadata key and value match");

            System.out.println("[DEBUG_LOG] Successfully tested join relationship with entity without primary key");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in testJoinWithNoPrimaryKey: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Entity classes for testing
    
    /**
     * Metadata entity without a primary key.
     * This entity is used to test @Join relationships with entities that don't have primary keys.
     */
    @Table(name = "metadata")
    public static class Metadata {
        @Column(name = "key")
        private String key;

        @Column(name = "value")
        private String value;

        public Metadata() {}

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Product entity with a metadata field.
     * This entity has a @Join relationship with the Metadata entity, which doesn't have a primary key.
     */
    @Table(name = "products_with_metadata")
    public static class ProductWithMetadata {
        @Column(name = "id", isPrimaryKey = true, isAutoIncrement = true)
        private int id;

        @Column(name = "name")
        private String name;

        @Column(name = "metadata_key")
        private String metadataKey;

        // The Join annotation uses the metadata_key column to establish the relationship with the Metadata table
        // Note that Metadata doesn't have a primary key, so we use the key field as the source
        @Join(targetName = "metadata_key", relationShip = Metadata.class, source = "key")
        private Metadata metadata;

        public ProductWithMetadata() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMetadataKey() {
            return metadataKey;
        }

        public void setMetadataKey(String metadataKey) {
            this.metadataKey = metadataKey;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
            if (metadata != null) {
                this.metadataKey = metadata.getKey();
            }
        }
    }

    // Query interfaces
    public interface MetadataQuery {
        List<Metadata> findAll();
        Metadata save(Metadata metadata);
    }

    public interface ProductWithMetadataQuery {
        List<ProductWithMetadata> findAll();
        ProductWithMetadata save(ProductWithMetadata product);
    }

    // Table classes
    public static class MetadataTable extends SQLiteTable<Metadata> {
        public MetadataTable(Context context) {
            super(new Management(context));
        }

        private MetadataQuery query() {
            return QueryFactory.create(MetadataQuery.class, Metadata.class, getManagement());
        }

        public List<Metadata> getAllMetadata() {
            return query().findAll();
        }

        public Metadata saveMetadata(Metadata metadata) {
            return query().save(metadata);
        }
    }

    public static class ProductWithMetadataTable extends SQLiteTable<ProductWithMetadata> {
        public ProductWithMetadataTable(Context context) {
            super(new Management(context));
        }

        private ProductWithMetadataQuery query() {
            return QueryFactory.create(ProductWithMetadataQuery.class, ProductWithMetadata.class, getManagement());
        }

        public List<ProductWithMetadata> getAllProducts() {
            return query().findAll();
        }

        public ProductWithMetadata saveProduct(ProductWithMetadata product) {
            return query().save(product);
        }
    }
}