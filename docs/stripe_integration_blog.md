# Tích Hợp Cổng Thanh Toán Stripe Checkout Vào Spring Boot: Hướng Dẫn Chi Tiết Từ A-Z

Chào các bạn, mình là **JavaBuilder**!

Trong thế giới thương mại điện tử hiện nay, việc tích hợp cổng thanh toán trực tuyến là một tính năng không thể thiếu đối với bất kỳ website bán hàng nào. Trong số các cổng thanh toán toàn cầu, **Stripe** nổi lên như một lựa chọn hàng đầu nhờ sự ổn định, tài liệu API xuất sắc và quy trình trải nghiệm người dùng vô cùng mượt mà.

Hôm nay, mình sẽ hướng dẫn các bạn từng bước xây dựng một hệ thống bán hàng và thanh toán trực tuyến hoàn chỉnh bằng **Spring Boot** kết hợp cổng thanh toán **Stripe Checkout**, kèm theo giao diện **Thymeleaf** cực kỳ chuyên nghiệp.

---

## 1. Kiến Trúc Hệ Thống

Để xây dựng một hệ thống thương mại điện tử sạch và dễ mở rộng, dự án áp dụng kiến trúc **tách biệt nhiệm vụ**:

* **Tạo đơn hàng trước (Order Process):** Hệ thống tiến hành trừ kho, lưu thông tin sản phẩm và giá bán tại thời điểm mua ở trạng thái chờ thanh toán (`PENDING`).
* **Thanh toán sau (Payment Process):** Khi khách hàng bấm nút thanh toán, hệ thống mới bắt đầu kết nối với Stripe để tạo phiên làm việc (Session) và lấy link chuyển hướng. Thiết kế này giúp khách hàng dễ dàng đổi phương thức thanh toán hoặc thanh toán lại nếu gặp lỗi mà không bị rác dữ liệu đơn hàng.

Để chuẩn bị cho bài thực hành này, bạn hãy clone mã nguồn của dự án tại đây (nhánh `master`) để chúng ta bắt đầu:
👉 **[GitHub: java-builder/spring-boot-stripe](https://github.com/java-builder/spring-boot-stripe)**

---

## 2. Sơ đồ hoạt động của Stripe Checkout

Trước khi đi vào cài đặt chi tiết, chúng ta hãy cùng hình dung luồng hoạt động này qua các bước đơn giản như sau:
1. **Đặt hàng (Order)**: Người dùng đặt hàng ➜ Backend tạo đơn hàng mới ở trạng thái **Chờ thanh toán (PENDING)** và trả về mã `orderCode`.
2. **Khởi tạo thanh toán**: Backend gửi thông tin đơn hàng lên Stripe để tạo phiên thanh toán (Checkout Session) ➜ Nhận về đường dẫn thanh toán **Stripe Checkout URL** ➜ Lưu giao dịch ở trạng thái `PENDING` vào database.
3. **Chuyển hướng (Redirect)**: Website chuyển hướng khách hàng sang trang thanh toán bảo mật của Stripe để nhập thông tin thẻ.
4. **Trở lại website (Redirect back)**: Thanh toán xong ➜ Stripe tự động đưa khách hàng quay lại trang kết quả (Thành công/Hủy) trên website.
5. **Cập nhật trạng thái (Webhook)**: Stripe gửi thông báo thanh toán thành công về backend ➜ Hệ thống tự động cập nhật trạng thái Đơn hàng & Giao dịch thành **Thành công (SUCCESS/PAID)**.

Dưới đây là sơ đồ chi tiết biểu diễn luồng tương tác này:

![Stripe Checkout Sequence Diagram](https://res.cloudinary.com/drdskl2up/image/upload/v1784366420/a9557fe0-4ba8-46f1-89fb-0baeaa8f8d8e.png)

---

## 3. Thiết Kế Cơ Sở Dữ Liệu (Database Schema)

Để lưu trữ thông tin giao dịch và đơn hàng ổn định, chúng ta thiết kế hệ thống cơ sở dữ liệu gồm 4 bảng chính với mối quan hệ chặt chẽ:

1. **products**: Lưu danh sách sản phẩm.
2. **orders**: Lưu thông tin đơn hàng với mã định danh duy nhất `order_code`.
3. **order_details**: Lưu thông tin chi tiết từng sản phẩm trong đơn hàng (lưu giá tại thời điểm mua).
4. **payments**: Lưu bản ghi giao dịch thanh toán, liên kết 1-1 với đơn hàng qua `order_id` và chứa `gateway_transaction_id` (Stripe Session ID) để đối soát.

Dưới đây là sơ đồ cơ sở dữ liệu trực quan:

![Stripe Database Schema](https://res.cloudinary.com/drdskl2up/image/upload/v1784370914/identity-service_rj0now.png)

---

## 4. Các Bước Cài Đặt & Cấu Hình

### Bước 1: Đăng ký & lấy Key thử nghiệm
1. Truy cập trang **[Đăng ký Stripe](https://dashboard.stripe.com/register)** hoặc **[Đăng nhập Stripe](https://dashboard.stripe.com/login)** (khuyên dùng Đăng nhập bằng Google cho nhanh).
2. Đi tới trang quản lý **[Stripe API Keys](https://dashboard.stripe.com/acct_1TuRF9JMfqNJ7ArA/test/apikeys)** để sao chép **Secret key** (mã `sk_test_...`).

### Bước 2: Thêm Thư Viện Vào `pom.xml`
Chúng ta sử dụng thư viện chính thức từ Stripe SDK để kết nối cổng thanh toán:

```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>33.1.1</version>
</dependency>
```

### Bước 3: Cấu Hình Cổng Thanh Toán Trong `application.yaml`
Đặt các khóa API của Stripe vào cấu hình như sau:

```yaml
stripe:
  api-key: ${STRIPE_API_KEY:sk_test_mock_key}
  success-url: ${STRIPE_SUCCESS_URL:http://localhost:8080/api/payments/success}
  cancel-url: ${STRIPE_CANCEL_URL:http://localhost:8080/api/payments/cancel}
```

### Bước 4: Tạo Cấu Hình `StripeClient` Bean
Để gọi API của Stripe một cách bảo mật và hiện đại, chúng ta định nghĩa một Bean cho `StripeClient`:

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

> **`StripeClient` dùng để làm gì?**
> 
> Đây là đối tượng giúp chúng ta kết nối và gọi trực tiếp các API của Stripe từ ứng dụng Backend.

---

## 5. Xây Dựng Backend (Spring Boot)

Để xây dựng phần Backend bằng Spring Boot, chúng ta sẽ đi theo luồng xử lý từ ngoài vào trong để dễ hình dung nhất:
1. **Lớp Controller**: Tiếp nhận yêu cầu HTTP từ Client gửi lên.
2. **Các lớp DTO (Data Transfer Object)**: Định nghĩa cấu trúc dữ liệu truyền nhận.
3. **Lớp Service nghiệp vụ**: Phối hợp tìm kiếm đơn hàng, lưu giao dịch thanh toán vào database.
4. **Lớp Service cổng thanh toán**: Chuyên trách gọi trực tiếp API của Stripe thông qua SDK.

---

### 1. Lớp Controller tiếp nhận yêu cầu: `PaymentController.java`

Để bắt đầu, chúng ta tạo một API Controller tiếp nhận yêu cầu thanh toán (endpoint `/api/payments/checkout`) từ Client. Lớp này nhận dữ liệu qua `@RequestBody` và gọi đến `PaymentService` để sinh link thanh toán Stripe:

```java
package com.javabuilder.stripe.controller;

import com.javabuilder.stripe.dto.request.CheckoutRequest;
import com.javabuilder.stripe.dto.response.ApiResponse;
import com.javabuilder.stripe.dto.response.CheckoutResponse;
import com.javabuilder.stripe.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    ApiResponse<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        var data = paymentService.checkout(request);
        return ApiResponse.<CheckoutResponse>builder()
                .status("success")
                .message("Stripe checkout session created successfully")
                .data(data)
                .build();
    }
}
```

### 2. Các lớp truyền nhận dữ liệu (DTO)

Để gửi nhận dữ liệu giữa client và backend, chúng ta sẽ định nghĩa một số lớp DTO (Data Transfer Object) như sau:

**`CheckoutRequest.java`** - Nhận thông tin thanh toán từ client gửi lên:
```java
package com.javabuilder.stripe.dto.request;

import com.javabuilder.stripe.common.PaymentGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank(message = "Order code cannot be blank")
        String orderCode,

        @NotNull(message = "Payment gateway cannot be null")
        PaymentGateway paymentGateway
) {}
```

**`CheckoutResponse.java`** - Trả về link thanh toán Stripe và Transaction ID cho Client:
```java
package com.javabuilder.stripe.dto.response;

import lombok.Builder;

@Builder
public record CheckoutResponse(
        String paymentUrl,
        String gatewayTransactionId
) {}
```

**`CreateLinkPaymentRequest.java`** - Sử dụng nội bộ để gửi mã đơn hàng khi khởi tạo session:
```java
package com.javabuilder.stripe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateLinkPaymentRequest(
        @NotBlank(message = "Order code cannot be blank")
        String orderCode
) {}
```

### 3. Lớp xử lý nghiệp vụ: `PaymentService.java`

Lớp này đóng vai trò trung tâm: Nhận yêu cầu từ Controller ➜ Lấy thông tin đơn hàng từ Database ➜ Gọi `StripePaymentService` để lấy link thanh toán từ Stripe ➜ Lưu lịch sử giao dịch ở trạng thái `PENDING` vào database của mình ➜ Trả kết quả về cho Controller:

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
        // 1. Tìm thông tin đơn hàng dựa trên mã orderCode
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

### 4. Lớp kết nối trực tiếp API Stripe: `StripePaymentService.java`

Đây là lớp dịch vụ chuyên trách việc tương tác trực tiếp với SDK của Stripe để tạo phiên thanh toán (Checkout Session) và trả về thông tin kết nối cho `PaymentService`:

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

    @Transactional(rollbackFor = Exception.class)
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
            log.error("Stripe Checkout Session creation failed", e);
            throw new RuntimeException("Stripe integration failed", e);
        }
    }

    private Long toStripeAmount(BigDecimal amount, Currency currency) {
        return currency.isZeroDecimal() ?
                amount.longValueExact() :
                amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
```

---

## 6. Giao Diện Người Dùng (Thymeleaf Frontend)

Dự án tích hợp sẵn giao diện mua hàng và thanh toán trực quan sử dụng **Thymeleaf** với luồng chuyển hướng hoàn chỉnh:
* **Trang chủ cửa hàng (`/`):** Hiển thị danh sách sản phẩm nổi bật với ảnh, tên, giá bán. Có nút **Xem chi tiết** sản phẩm và nút **Thêm vào giỏ**. Trên Header có biểu tượng giỏ hàng hiển thị số lượng (click vào sẽ chuyển tới trang Checkout).
* **Trang chi tiết sản phẩm (`/products/{id}`):** Hiển thị mô tả, hình ảnh của một sản phẩm cụ thể. Người dùng có thể nhấn **Mua ngay** để chuyển thẳng tới trang Checkout hoặc **Thêm vào giỏ** để tiếp tục chọn thêm sản phẩm khác.
* **Trang thanh toán đơn hàng (`/checkout`):** Hiển thị danh sách sản phẩm đang có trong giỏ hàng, cho phép tăng/giảm số lượng, xóa sản phẩm, chọn loại tiền tệ thanh toán (VND/USD). Nhấn nút **Tiến hành thanh toán** để mở cổng Stripe.
* **Trang thành công (`/api/payments/success`):** Hiển thị khi thanh toán thành công kèm theo Stripe Session ID.
* **Trang hủy thanh toán (`/api/payments/cancel`):** Hiển thị khi khách hàng chủ động hủy thanh toán.

## 7. Hướng Dẫn Kiểm Thử & Trải Nghiệm Thực Tế

Để kiểm tra dự án hoạt động như thế nào, bạn có thể thực hiện trải nghiệm trực tiếp trên giao diện theo các bước dưới đây:

1. Truy cập **`http://localhost:8080/`** trên trình duyệt của bạn.
2. Thêm các sản phẩm mẫu vào giỏ hàng và tùy chỉnh số lượng.
3. Vào trang Thanh toán (`/checkout`), chọn loại tiền tệ (VND hoặc USD) rồi bấm **Tiến hành thanh toán**.
4. Website sẽ tự động tạo đơn hàng và chuyển hướng bạn sang trang thanh toán bảo mật của Stripe.
5. Nhập thông tin thẻ thử nghiệm của Stripe để tiến hành thanh toán:
   * **Số thẻ (Card number):** `4242 4242 4242 4242`
   * **Hạn dùng (MM/YY):** Bất kỳ tháng/năm nào trong tương lai (Ví dụ: `12/34`)
   * **CVC:** `123`
6. Sau khi thanh toán thành công, Stripe sẽ tự động chuyển hướng bạn quay lại trang kết quả thành công kèm mã **Stripe Session ID**.

---

## 🔗 Mã Nguồn Tham Khảo (GitHub Source Code)

Bạn có thể tham khảo và tải toàn bộ mã nguồn đầy đủ của dự án tích hợp này tại đây:
👉 **[GitHub Repository: java-builder/spring-boot-stripe](https://github.com/github.com/java-builder/spring-boot-stripe)**

---

## Kết Luận

Tích hợp **Stripe Checkout** vào **Spring Boot** không hề phức tạp nếu chúng ta phân tách cấu trúc code rõ ràng và áp dụng mô hình giỏ hàng `localStorage` đơn giản phía Client để mang lại trải nghiệm thanh toán liền mạch và nhanh chóng nhất.

Hy vọng bài viết này giúp ích cho bạn trong việc hiện thực hóa cổng thanh toán cho dự án của mình. Đừng ngần ngại để lại câu hỏi ở phần bình luận bên dưới nhé!

Chúc các bạn code vui vẻ! 🚀
