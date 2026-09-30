package com.example.sistemabfake.service;

import com.example.sistemabfake.dto.MovimientoStockRequest;
import com.example.sistemabfake.dto.ProductoRequest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:file:./target/stock-persistence-test;DB_CLOSE_ON_EXIT=FALSE")
class StockInventoryPersistenceTest {

    private static final String DATABASE_URL =
            "jdbc:h2:file:./target/stock-persistence-test;DB_CLOSE_ON_EXIT=FALSE";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StockInventoryService inventoryService;

    @BeforeEach
    void clearInventory() {
        jdbcTemplate.update("DELETE FROM stock_productos");
    }

    @Test
    void keepsExactQuantityAfterDatabaseIsClosedAndReopened() {
        inventoryService.registrarProducto(new ProductoRequest("PRODUCTO-REAL-1"));
        inventoryService.registrarMovimiento(new MovimientoStockRequest(
                "ENTRADA",
                OffsetDateTime.parse("2026-09-24T16:20:35-03:00"),
                "COMPRA-PERSISTENTE-1",
                List.of(new com.example.sistemabfake.dto.ProductoMovimientoRequest("PRODUCTO-REAL-1", 37))
        ));

        jdbcTemplate.execute("SHUTDOWN");

        JdbcDataSource reopenedDatabase = new JdbcDataSource();
        reopenedDatabase.setURL(DATABASE_URL);
        reopenedDatabase.setUser("sa");
        reopenedDatabase.setPassword("");
        JdbcTemplate reopenedJdbcTemplate = new JdbcTemplate(reopenedDatabase);
        Integer persistedQuantity = reopenedJdbcTemplate.queryForObject(
                "SELECT cantidad_disponible FROM stock_productos WHERE codigo_producto = ?",
                Integer.class,
                "PRODUCTO-REAL-1"
        );

        assertEquals(37, persistedQuantity);
        reopenedJdbcTemplate.execute("SHUTDOWN");
    }
}