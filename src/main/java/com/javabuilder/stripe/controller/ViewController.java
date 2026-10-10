package com.javabuilder.stripe.controller;

import com.javabuilder.stripe.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ProductService productService;

    @GetMapping("favicon.ico")
    @ResponseBody
    public void favicon() {
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("products", productService.getProducts(1, 20).getContent());
        return "products";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        return "product-detail";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }

    @GetMapping("/api/payments/success")
    public String success(@RequestParam(value = "session_id", required = false) String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId != null ? sessionId : "STRIPE-SUCCESS");
        return "success";
    }

    @GetMapping("/api/payments/vnpay-callback")
    public String vnpayCallback(@RequestParam(value = "vnp_ResponseCode", required = false) String responseCode,
                                @RequestParam(value = "vnp_TransactionNo", required = false) String transactionNo,
                                Model model) {
        if ("00".equals(responseCode)) {
            model.addAttribute("sessionId", transactionNo != null ? "VNPAY-" + transactionNo : "VNPAY-SUCCESS");
            return "success";
        }
        return "cancel";
    }

    @GetMapping("/api/payments/cancel")
    public String cancel() {
        return "cancel";
    }
}
