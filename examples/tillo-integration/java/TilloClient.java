import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Production-ready Java HTTP client example for Tillo POS integrating with RITZ Wallet
 */
public class TilloClient {

    private static final String BASE_URL = "http://localhost:8085";
    private static final String API_KEY = "ritz-secret-api-key-2026";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String studentId = "RIT2026001";
        String orderRef = "TILLO-ORDER-JAVA-" + System.currentTimeMillis();

        System.out.println("=== 1. Checking Student Balance ===");
        HttpRequest balanceReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/students/" + studentId + "/balance"))
                .header("X-API-KEY", API_KEY)
                .GET()
                .build();

        HttpResponse<String> balanceRes = client.send(balanceReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Balance Response: " + balanceRes.body());

        System.out.println("\n=== 2. Deducting 80 Tokens for Canteen Purchase ===");
        String debitPayload = String.format("""
            {
              "student_id": "%s",
              "amount": 80,
              "currency": "RITZ",
              "reference_id": "%s",
              "merchant_id": "TILLO",
              "description": "Lunch Canteen Order"
            }
            """, studentId, orderRef);

        HttpRequest debitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/transactions/debit"))
                .header("X-API-KEY", API_KEY)
                .header("Idempotency-Key", orderRef)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(debitPayload))
                .build();

        HttpResponse<String> debitRes = client.send(debitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Debit Response: " + debitRes.body());

        System.out.println("\n=== 3. Testing Idempotency (Sending Same Order Ref) ===");
        HttpResponse<String> duplicateRes = client.send(debitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Duplicate Response: " + duplicateRes.body());
    }
}
