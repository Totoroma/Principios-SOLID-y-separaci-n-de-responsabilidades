package com.example.integracion.service;

import com.example.integracion.connector.StockConnector;
import com.example.integracion.dto.MovimientoManualRequest;
import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.ProductoMovimientoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MovimientoManualServiceTest {

    @Mock
    private StockConnector stockConnector;

    @InjectMocks
    private MovimientoManualService service;

    @Test
    void generatesReferenceAndDateAndSendsMovementToConnector() {
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
        MovimientoManualRequest request = new MovimientoManualRequest(
                "SALIDA",
                List.of(new ProductoMovimientoRequest("7791234567890", 3))
        );

        MovimientoStockRequest generated = service.procesar(request);

        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);
        assertEquals("SALIDA", generated.tipoMovimiento());
        assertTrue(generated.referencia().startsWith("SALIDA-"));
        assertTrue(generated.fecha().isAfter(before));
        assertTrue(generated.fecha().isBefore(after));
        assertEquals(request.productos(), generated.productos());

        ArgumentCaptor<MovimientoStockRequest> captor = ArgumentCaptor.forClass(MovimientoStockRequest.class);
        verify(stockConnector).registrarMovimiento(captor.capture());
        assertEquals(generated, captor.getValue());
    }
}