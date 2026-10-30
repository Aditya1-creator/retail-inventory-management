package com.hcl.Inventory;

import com.hcl.Inventory.dto.*;
import com.hcl.Inventory.entity.*;
import com.hcl.Inventory.repository.*;
import com.hcl.Inventory.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiWorkflowTest {
    @Autowired Environment environment;
    @Autowired ProductService products;
    @Autowired WarehouseService warehouses;
    @Autowired StockService stocks;
    @Autowired OrderService orders;
    @Autowired SupplierService suppliers;
    @Autowired StockRepository stockRepository;
    @Autowired OrderRepository orderRepository;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private long productId, warehouseId;

    @BeforeEach void setup() {
        String unique = UUID.randomUUID().toString();
        productId = products.createProduct(new ProductRequestDto(unique, "Workflow product", "QA", 125.50, 5, null)).getId();
        warehouseId = warehouses.createWarehouse(new WarehouseRequestDto("QA " + unique, "Local test")).getId();
    }
    private HttpRequest request(String method, String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + "/api" + path))
            .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        return client.send(request(method, path, body), HttpResponse.BodyHandlers.ofString());
    }
    private String item(int quantity) {
        return "{\"productId\":" + productId + ",\"warehouseId\":" + warehouseId + ",\"quantity\":" + quantity + "}";
    }
    private int quantity() {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId).orElseThrow().getQuantity();
    }
    private void add(int count) { stocks.addStock(new StockRequestDto(productId, warehouseId, count)); }
    private OrderResponseDto order(int count) { return orders.createOrder(new OrderRequestDto("QA Customer", List.of(new OrderItemRequestDto(productId, warehouseId, count)))); }
    private List<Integer> concurrent(String method, String path, String body) {
        var a = client.sendAsync(request(method, path, body), HttpResponse.BodyHandlers.ofString());
        var b = client.sendAsync(request(method, path, body), HttpResponse.BodyHandlers.ofString());
        CompletableFuture.allOf(a,b).orTimeout(25, TimeUnit.SECONDS).join();
        return List.of(a.join().statusCode(), b.join().statusCode()).stream().sorted().toList();
    }
    @Test void orderAndCancellationRestoreOriginalStock() throws Exception {
        add(10);
        var result = call("POST", "/orders", "{\"customerName\":\"QA\",\"items\":[" + item(3) + "]}");
        assertEquals(201, result.statusCode(), result.body());
        assertTrue(result.body().contains("376.5"));
        assertEquals(7, quantity());
        long id = orderRepository.findAll().stream().filter(o -> o.getTotalAmount().equals(376.5)).max(Comparator.comparing(Order::getId)).orElseThrow().getId();
        assertEquals(200, call("PUT", "/orders/"+id+"/cancel", null).statusCode());
        assertEquals(10, quantity());
        assertEquals(409, call("PUT", "/orders/"+id+"/cancel", null).statusCode());
        assertEquals(10, quantity());
    }
    @Test void laterItemFailureRollsBackEarlierDeductionAndOrder() throws Exception {
        add(10); long before = orderRepository.count();
        var result = call("POST", "/orders", "{\"customerName\":\"QA\",\"items\":[" + item(3) + "," + item(99) + "]}");
        assertEquals(400, result.statusCode(), result.body());
        assertEquals(10, quantity()); assertEquals(before, orderRepository.count());
    }
    @Test void concurrentDeductionsCannotOversell() {
        add(10);
        assertEquals(List.of(200,400), concurrent("POST", "/stock/deduct", item(8)));
        assertEquals(2, quantity());
    }
    @Test void concurrentCancellationRestoresOnlyOnce() {
        add(10); long id=order(3).getId();
        assertEquals(List.of(200,409), concurrent("PUT", "/orders/"+id+"/cancel", null));
        assertEquals(10, quantity());
    }
    @Test void concurrentOrdersCannotOversell() {
        add(10);
        assertEquals(List.of(201,400), concurrent("POST", "/orders", "{\"customerName\":\"Concurrent QA\",\"items\":["+item(8)+"]}"));
        assertEquals(2,quantity());
    }
    @Test void concurrentFirstStockAdditionsDoNotLoseUnits() {
        assertEquals(List.of(200,200), concurrent("POST", "/stock/add", item(4)));
        assertEquals(8, quantity());
    }
    @Test void invalidStockQuantitiesAreRejected() throws Exception {
        for (int q : new int[]{0,-1}) assertEquals(400,call("POST","/stock/add",item(q)).statusCode());
        assertTrue(stockRepository.findByProductIdAndWarehouseId(productId,warehouseId).isEmpty());
    }
    @Test void nullOrderItemReturnsValidationError() throws Exception {
        assertEquals(400,call("POST","/orders","{\"customerName\":\"QA\",\"items\":[null]}").statusCode());
        assertEquals(400,call("POST","/orders","{\"customerName\":\"QA\",\"items\":[]}").statusCode());
    }
    @Test void lowStockAggregatesAcrossWarehousesIncludingZeroStock() {
        assertTrue(stocks.getLowStockProducts().stream().anyMatch(s->s.getProductId()==productId));
        add(3);
        long second=warehouses.createWarehouse(new WarehouseRequestDto("Second QA warehouse","Local")).getId();
        stocks.addStock(new StockRequestDto(productId,second,4));
        assertFalse(stocks.getLowStockProducts().stream().anyMatch(s->s.getProductId()==productId));
    }
    @Test void overflowCannotWrapStockNegative() throws Exception {
        add(Integer.MAX_VALUE);
        assertEquals(400,call("POST","/stock/add",item(1)).statusCode());
        assertEquals(Integer.MAX_VALUE,quantity());
    }
    @Test void missingProductAndReferencedDeletionReturnUsefulStatus() throws Exception {
        assertEquals(404,call("GET","/products/9223372036854775807",null).statusCode());
        add(1);
        assertEquals(409,call("DELETE","/products/"+productId,null).statusCode());
        assertEquals(1,quantity());
    }
    @Test void duplicateSkuReturnsConflict() throws Exception {
        String sku=products.getProductById(productId).getSku();
        var result=call("POST","/products","{\"sku\":\""+sku+"\",\"name\":\"QA\",\"price\":1,\"reorderLevel\":0}");
        assertEquals(409,result.statusCode(),result.body());
    }
    @Test void decimalProductCanBeUpdatedAndDeleted() throws Exception {
        var result=call("PUT","/products/"+productId,"{\"sku\":\""+UUID.randomUUID()+"\",\"name\":\"Updated\",\"price\":19.99,\"reorderLevel\":0}");
        assertEquals(200,result.statusCode(),result.body());
        assertEquals(19.99,products.getProductById(productId).getPrice());
        assertEquals(204,call("DELETE","/products/"+productId,null).statusCode());
        assertEquals(404,call("GET","/products/"+productId,null).statusCode());
    }
    @Test void corsPreflightAndDashboardWork() throws Exception {
        var req=HttpRequest.newBuilder(request("OPTIONS","/products",null).uri())
            .header("Origin","http://127.0.0.1:5173").header("Access-Control-Request-Method","POST").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        var response=client.send(req,HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
        assertEquals("http://127.0.0.1:5173",response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals(200,call("GET","/summary",null).statusCode());
    }
    @Test void registrationAndLoginIssueRealTokens() throws Exception {
        String email=UUID.randomUUID()+"@example.test";
        String body="{\"name\":\"QA User\",\"email\":\""+email+"\",\"password\":\"qa-test-123\",\"role\":\"STAFF\"}";
        var registration=call("POST","/auth/register",body);
        assertEquals(201,registration.statusCode(),registration.body());
        assertTrue(registration.body().contains("token"));
        var login=call("POST","/auth/login","{\"email\":\""+email+"\",\"password\":\"qa-test-123\"}");
        assertEquals(200,login.statusCode(),login.body());
        assertTrue(login.body().contains("token"));
        assertFalse(login.body().contains("passwordHash"));
        assertEquals(400,call("POST","/auth/login","{\"email\":\""+email+"\",\"password\":\"wrong-password\"}").statusCode());
    }
}
