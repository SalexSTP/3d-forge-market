package com.aleksandar.threedforgemarket.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String FEATURED_PRODUCTS = "featuredProducts";
    public static final String PRODUCT_CATALOG = "productCatalog";
    public static final String PRODUCT_DETAILS = "productDetails";
    public static final String ADMIN_PRODUCTS = "adminProducts";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                FEATURED_PRODUCTS,
                PRODUCT_CATALOG,
                PRODUCT_DETAILS,
                ADMIN_PRODUCTS
        );
    }
}
