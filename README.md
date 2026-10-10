# E-Commerce Stripe Integration (Spring Boot 4)

Dự án mẫu hướng dẫn tích hợp cổng thanh toán trực tuyến **Stripe** vào ứng dụng Spring Boot một cách bảo mật, chuyên nghiệp, áp dụng nguyên lý tách biệt luồng Đặt hàng (Order) và Thanh toán (Payment).

---

## 📌 Giới thiệu dự án

Dự án này viết bằng Java để minh họa cách tích hợp cổng thanh toán quốc tế Stripe. Để hệ thống sạch và dễ mở rộng, chúng ta áp dụng kiến trúc **tách biệt nhiệm vụ**:
* **Tạo đơn hàng trước:** Hệ thống trừ kho, lưu thông tin sản phẩm và giá cả thời điểm mua ở trạng thái `PENDING`.
* **Thanh toán sau:** Người dùng bấm nút thanh toán mới tạo phiên làm việc với Stripe để lấy link redirect, giúp người dùng có thể đổi phương thức hoặc thanh toán lại nếu gặp lỗi mà không bị rác đơn hàng.

### Công nghệ sử dụng:
* **Backend:** Spring Boot 4.1.0, Spring Data JPA
* **Java Version:** JDK 25
* **Database:** MySQL 8.x
* **Thanh toán:** Stripe Java SDK

---

## 💻 Thiết lập & Khởi chạy nhanh

### 1. Yêu cầu hệ thống
* Java Development Kit (JDK) 25.
* Docker và Docker Compose (để khởi chạy MySQL).

### 2. Khởi chạy MySQL
Dự án đã có sẵn cấu hình Docker. Bạn mở Terminal tại thư mục gốc và chạy lệnh:
```bash
docker-compose up -d
```

### 3. Cấu hình Key của Stripe
Mở tệp [application.yaml](./src/main/resources/application.yaml) và điền Secret Key của tài khoản Stripe Test của bạn:
```yaml
stripe:
  api-key: sk_test_YOUR_SECRET_KEY
  success-url: http://localhost:8080/api/payments/success
  cancel-url: http://localhost:8080/api/payments/cancel
```

### 4. Chạy ứng dụng
Biên dịch và khởi chạy dự án:
```bash
mvn clean spring-boot:run
```

---

## 💳 Sơ đồ hoạt động của Stripe Checkout

Giao dịch được xử lý gián tiếp qua trang thanh toán an toàn của Stripe (Redirect Flow) để đảm bảo bảo mật thông tin thẻ của khách hàng:

![Stripe Checkout Sequence Diagram](https://res.cloudinary.com/drdskl2up/image/upload/v1784366420/a9557fe0-4ba8-46f1-89fb-0baeaa8f8d8e.png)

---

## 🛠️ Chi tiết các bước và mã nguồn tích hợp

### Bước 1: Đăng ký & lấy Key thử nghiệm
1. Truy cập trang **[Đăng ký Stripe](https://dashboard.stripe.com/register)** hoặc **[Đăng nhập Stripe](https://dashboard.stripe.com/login)** (khuyên dùng Đăng nhập bằng Google cho nhanh).
2. Đi tới trang quản lý **[Stripe API Keys](https://dashboard.stripe.com/acct_1TuRF9JMfqNJ7ArA/test/apikeys)** để copy **Secret key** (mã `sk_test_...`).

### Bước 2: Khai báo thư viện trong `pom.xml`
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>33.1.1</version>
</dependency>
```

### Bước 3: Tạo cấu hình `StripeClient` Bean
Khai báo cấu hình để Spring quản lý đối tượng kết nối của Stripe:
```java
@Configuration
public class StripeClientConfiguration {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Bean
    public StripeClient stripeClient() {
        return StripeClient.builder()
                .setApiKey(apiKey)
                .build();
    }
}
```

> [!TIP]
> **`StripeClient` dùng để làm gì?**
> 
> Đây là đối tượng giúp chúng ta kết nối và gọi trực tiếp các API của Stripe từ Java.

### Bước 4: Viết code nghiệp vụ thanh toán

#### Lớp kết nối API Stripe: `StripePaymentService.java`
File này chỉ chuyên gọi API sang Stripe để tạo phiên thanh toán và trả về kết quả:

```java
@Service
@RequiredArgsConstructor
@Slf4j(topic = "STRIPE-SERVICE")
public class StripePaymentService {

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final StripeClient stripeClient;
    private final OrderRepository orderRepository;

    @Transactional
    public Session createCheckoutSession(CreateLinkPaymentRequest request) {
        // 1. Tìm đơn hàng cần thanh toán
        Order order = orderRepository.findOrderWithOrderDetailsByOrderCode(request.orderCode())
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + request.orderCode()));

        // 2. Cài đặt các thông tin gửi lên Stripe Checkout
        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}") // Tự động điền mã session khi thành công
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(order.getOrderCode()) // Lưu mã đơn hàng để đối soát tự động qua Webhook
                .putMetadata("orderCode", order.getOrderCode())
                .setMode(SessionCreateParams.Mode.PAYMENT);

        // 3. Duyệt qua danh sách sản phẩm trong đơn để hiển thị hóa đơn chi tiết trên Stripe
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            // Đổi số tiền sang định dạng Stripe yêu cầu (VND giữ nguyên, USD nhân với 100 thành cents)
            Long itemAmount = toStripeAmount(detail.getPrice(), order.getCurrency());

            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(detail.getQuantity().longValue())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toStripeCode())
                            .setUnitAmount(itemAmount)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(product.getName())
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = sessionBuilder.build();

        // 4. Dùng Idempotency Key để đảm bảo không bị trừ tiền trùng lặp nếu lỗi mạng gửi lại request
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("checkout-" + order.getOrderCode())
                .build();

        try {
            return stripeClient.v1().checkout().sessions().create(params, options);
        } catch (StripeException e) {
            log.error("Tạo phiên thanh toán trên Stripe thất bại", e);
            throw new RuntimeException("Tích hợp Stripe gặp lỗi", e);
        }
    }

    private Long toStripeAmount(BigDecimal amount, Currency currency) {
        return currency.isZeroDecimal() ?
                amount.longValueExact() :
                amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
```

#### Lớp lưu cơ sở dữ liệu: `PaymentService.java`
File này đóng vai trò trung gian: Lấy thông tin đơn hàng ➜ Nhờ `StripePaymentService` lấy link thanh toán từ Stripe ➜ Lưu lịch sử giao dịch ở trạng thái `PENDING` vào Database của mình ➜ Trả kết quả về cho Controller:

```java
@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StripePaymentService stripePaymentService;

    @Transactional(rollbackFor = Exception.class)
    public CheckoutResponse checkout(CheckoutRequest request) {
        Order order = orderRepository.findByOrderCode(request.orderCode())
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + request.orderCode()));

        // 2. Gọi Stripe lấy thông tin phiên thanh toán
        CreateLinkPaymentRequest stripeRequest = new CreateLinkPaymentRequest(order.getOrderCode());
        Session session = stripePaymentService.createCheckoutSession(stripeRequest);

        // 3. Lưu thông tin giao dịch vào Database ở trạng thái PENDING
        Payment payment = Payment.builder()
                .order(order)
                .paymentGateway(request.paymentGateway())
                .amount(order.getTotalPrice())
                .currency(order.getCurrency())
                .gatewayTransactionId(session.getId()) // Lưu ID session của Stripe để đối soát Webhook
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        log.info("Payment transaction saved successfully. ID: {}, Stripe Session ID: {}", payment.getId(), session.getId());

        return CheckoutResponse.builder()
                .paymentUrl(session.getUrl())
                .gatewayTransactionId(session.getId())
                .build();
    }
}
```

### Bước 5: Cập nhật đơn hàng tự động qua Webhook
1. Khách hàng thanh toán thành công ➜ Stripe gửi thông báo không đồng bộ sự kiện `checkout.session.completed` về endpoint Webhook của bạn.
2. Backend nhận Webhook ➜ Đọc ra `Session ID` (hoặc `clientReferenceId`) ➜ Tìm bản ghi `Payment` tương ứng trong DB ➜ Cập nhật `Payment` và `Order` sang trạng thái `SUCCESS/PAID`.

## 🖥️ Giao diện người dùng (Thymeleaf Frontend)

Dự án tích hợp sẵn giao diện mua hàng và thanh toán trực quan sử dụng **Thymeleaf** với luồng chuyển hướng hoàn chỉnh:
* **Trang chủ cửa hàng (`/`):** Hiển thị danh sách sản phẩm nổi bật với ảnh, tên, giá bán. Có nút **Xem chi tiết** sản phẩm và nút **Thêm vào giỏ**. Trên Header có biểu tượng giỏ hàng hiển thị số lượng (click vào sẽ chuyển tới trang Checkout).
* **Trang chi tiết sản phẩm (`/products/{id}`):** Hiển thị mô tả, hình ảnh của một sản phẩm cụ thể. Người dùng có thể nhấn **Mua ngay** để chuyển thẳng tới trang Checkout hoặc **Thêm vào giỏ** để tiếp tục chọn thêm sản phẩm khác.
* **Trang thanh toán đơn hàng (`/checkout`):** Hiển thị danh sách sản phẩm đang có trong giỏ hàng, cho phép tăng/giảm số lượng, xóa sản phẩm, chọn loại tiền tệ thanh toán (VND/USD). Nhấn nút **Tiến hành thanh toán** để mở cổng Stripe.
* **Trang thành công (`/api/payments/success`):** Hiển thị khi thanh toán thành công kèm theo Stripe Session ID.
* **Trang hủy thanh toán (`/api/payments/cancel`):** Hiển thị khi khách hàng chủ động hủy thanh toán.

Bạn chỉ cần truy cập **`http://localhost:8080/`** trên trình duyệt để trải nghiệm luồng mua sắm trực quan này!

---

## 🧪 Hướng dẫn Kiểm thử (Testing)

Mở tệp tin **[api-requests.http](./api-requests.http)** và chạy lần lượt các bước:

1. **Tạo 5 sản phẩm mẫu:** Nhấp chạy lần lượt các request tạo từ Sản phẩm 1 đến Sản phẩm 5 để lưu vào DB.
2. **Tạo đơn hàng:** Nhấn chạy **Create Order** để tạo đơn hàng. Copy mã đơn hàng `"orderCode"` trả về.
3. **Thanh toán:**
   * Sử dụng request checkout trong tệp **[api-requests.http](./api-requests.http)**.
   * Thay thế `"orderCode"` bằng mã đơn hàng bạn vừa copy ở bước 2.
   * Gửi request `POST /api/payments/checkout`. Bạn sẽ nhận về link thanh toán `paymentUrl`.
4. **Thanh toán thử nghiệm trên Stripe:**
   * Mở link `paymentUrl` trên trình duyệt.
   * Sử dụng thông tin thẻ test của Stripe để điền:
     * **Số thẻ (Card number):** `4242 4242 4242 4242`
     * **Hạn dùng (MM/YY):** Bất kỳ tháng/năm nào trong tương lai (Ví dụ: `12 / 29`)
     * **CVC:** `123`
     * **Tên chủ thẻ:** Tên bất kỳ (Ví dụ: `JAVABUILDER`)
   * Nhấn **Pay** để hoàn thành thanh toán.
