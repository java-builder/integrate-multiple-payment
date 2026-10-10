package com.javabuilder.stripe.service;

import com.javabuilder.stripe.dto.request.CreateProductRequest;
import com.javabuilder.stripe.dto.response.CreateProductResponse;
import com.javabuilder.stripe.dto.response.PageResponse;
import com.javabuilder.stripe.dto.response.ProductDetailResponse;
import com.javabuilder.stripe.entity.Product;
import com.javabuilder.stripe.exception.AppException;
import com.javabuilder.stripe.exception.ErrorCode;
import com.javabuilder.stripe.mapper.ProductMapper;
import com.javabuilder.stripe.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-SERVICE")
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public CreateProductResponse createProduct(CreateProductRequest request) {
        Product product = productMapper.toProduct(request);
        productRepository.save(product);
        return productMapper.toCreateProductResponse(product);
    }

    public PageResponse<ProductDetailResponse> getProducts(int page, int size) {
        Sort sort = Sort.by(Product::getCreatedAt).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Product> productPage = productRepository.findAll(pageable);
        List<Product> products = productPage.getContent();
        List<ProductDetailResponse> responses = products.stream()
                .map(productMapper::toProductDetailResponse)
                .toList();

        return PageResponse.<ProductDetailResponse>builder()
                .page(page)
                .size(pageable.getPageSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .content(responses)
                .build();
    }

    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toProductDetailResponse(product);
    }
}
